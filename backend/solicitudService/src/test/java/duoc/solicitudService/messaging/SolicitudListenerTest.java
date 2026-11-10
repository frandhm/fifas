package duoc.solicitudService.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import duoc.solicitudService.entity.Solicitud;
import duoc.solicitudService.repository.SolicitudRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de integración/componente para SolicitudListener.
 * Verifica la cadena completa de mensajería asíncrona:
 * 1. Consumo de 'solicitudes_queue' (evento carrito.confirmado).
 * 2. Validación de reglas de negocio vía Jakarta Validation.
 * 3. Persistencia de solicitudes con estado PENDIENTE.
 * 4. Re-publicación del evento 'solicitud.creada' hacia 'eventos_exchange' (comunicación inter-microservicios).
 * 5. Confirmación ACK al finalizar el flujo.
 * 6. Rechazo NACK sin requeue (envío a DLQ) ante datos inválidos o excepciones.
 */
class SolicitudListenerTest {

    private SolicitudRepository repository;
    private RabbitTemplate rabbitTemplate;
    private Channel canal;
    private final ObjectMapper mapper = new ObjectMapper();
    private Validator validator;
    private SolicitudListener listener;

    @BeforeEach
    void setUp() {
        repository = mock(SolicitudRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        canal = mock(Channel.class);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        listener = new SolicitudListener(repository, mapper, validator, rabbitTemplate);
    }

    // ──── Flujo Exitoso: Consumo + Persistencia + Re-publicación + ACK ────

    @Test
    void procesaCompraGuardaSolicitudesYPublicaEventoEnExchange() throws IOException {
        String cuerpo = """
            {
              "items": [
                {
                  "productoId": "CAM-01",
                  "productoNombre": "Camiseta Local Chile",
                  "cantidad": 2,
                  "precioUnitario": 49990.0,
                  "talla": "L",
                  "nombreEstampado": "SANCHEZ",
                  "numeroEstampado": 7
                },
                {
                  "productoId": "CAM-02",
                  "productoNombre": "Camiseta Visita Chile",
                  "cantidad": 1,
                  "precioUnitario": 39990.0,
                  "talla": "M",
                  "nombreEstampado": "",
                  "numeroEstampado": 0
                }
              ]
            }
            """;
        String propietario = "https://issuer.example|bob";
        long tag = 100L;

        // Mock repository.saveAll retornando los elementos guardados
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        listener.recibir(cuerpo, propietario, tag, canal);

        // 1) Verificar persistencia en base de datos
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Solicitud>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<Solicitud> guardadas = captor.getValue();

        assertEquals(2, guardadas.size());
        assertEquals("CAM-01", guardadas.get(0).getProductoId());
        assertEquals(propietario, guardadas.get(0).getPropietario());
        assertEquals("PENDIENTE", guardadas.get(0).getEstado());
        assertEquals("SANCHEZ", guardadas.get(0).getNombreEstampado());
        assertEquals(7, guardadas.get(0).getNumeroEstampado());
        assertNotNull(guardadas.get(0).getFechaCreacion());

        // 2) Verificar re-publicación del evento a productoService
        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(rabbitTemplate).convertAndSend(exchangeCaptor.capture(), routingKeyCaptor.capture(), payloadCaptor.capture());

        assertEquals("eventos_exchange", exchangeCaptor.getValue());
        assertEquals("solicitud.creada", routingKeyCaptor.getValue());

        // Verificar el contenido del evento publicado
        JsonNode eventoPublicado = mapper.readTree(payloadCaptor.getValue());
        assertTrue(eventoPublicado.has("items"));
        assertEquals(2, eventoPublicado.get("items").size());
        assertEquals("CAM-01", eventoPublicado.get("items").get(0).get("productoId").asText());
        assertEquals(2, eventoPublicado.get("items").get(0).get("cantidad").asInt());

        // 3) Verificar confirmación ACK manual
        verify(canal).basicAck(tag, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    // ──── Reglas de Validación y NACK (DLQ) ────

    @Test
    void rechazaCompraSinPropietarioYEnviaADlq() throws IOException {
        String cuerpo = """
            {
              "items": [
                {
                  "productoId": "CAM-01",
                  "productoNombre": "Camiseta",
                  "cantidad": 1,
                  "precioUnitario": 10000.0
                }
              ]
            }
            """;
        long tag = 101L;

        listener.recibir(cuerpo, null, tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(repository, never()).saveAll(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void rechazaCompraConCantidadInvalidaYEnviaADlq() throws IOException {
        // cantidad = 0 viola @Min(1)
        String cuerpo = """
            {
              "items": [
                {
                  "productoId": "CAM-01",
                  "productoNombre": "Camiseta",
                  "cantidad": 0,
                  "precioUnitario": 10000.0
                }
              ]
            }
            """;
        long tag = 102L;

        listener.recibir(cuerpo, "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(repository, never()).saveAll(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void rechazaCompraConPrecioNegativoYEnviaADlq() throws IOException {
        String cuerpo = """
            {
              "items": [
                {
                  "productoId": "CAM-01",
                  "productoNombre": "Camiseta",
                  "cantidad": 1,
                  "precioUnitario": -500.0
                }
              ]
            }
            """;
        long tag = 103L;

        listener.recibir(cuerpo, "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(repository, never()).saveAll(any());
    }

    @Test
    void rechazaCompraSinItemsYEnviaADlq() throws IOException {
        String cuerpo = "{\"items\": []}";
        long tag = 104L;

        listener.recibir(cuerpo, "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(repository, never()).saveAll(any());
    }

    @Test
    void rechazaJsonCorruptoYEnviaADlq() throws IOException {
        long tag = 105L;

        listener.recibir("no es json", "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(repository, never()).saveAll(any());
    }

    // ──── Manejo de Excepciones ────

    @Test
    void capturaErrorAlPublicarEventoYEnviaADlq() throws IOException {
        String cuerpo = """
            {
              "items": [
                {
                  "productoId": "CAM-01",
                  "productoNombre": "Camiseta",
                  "cantidad": 1,
                  "precioUnitario": 10000.0
                }
              ]
            }
            """;
        long tag = 106L;

        when(repository.saveAll(any())).thenReturn(List.of(new Solicitud()));
        doThrow(new RuntimeException("Error al conectar con RabbitMQ exchange"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        listener.recibir(cuerpo, "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
    }
}

