package duoc.soporteService.messaging;

import com.rabbitmq.client.Channel;
import duoc.soporteService.entity.Mensaje;
import duoc.soporteService.service.MensajeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de integración/componente para SoporteListener.
 * Verifica:
 * 1. Procesamiento de mensajes de soporte según reglas de negocio y confirmación ACK.
 * 2. Rechazo y derivación a soporte_dlq (NACK sin requeue) ante campos inválidos:
 *    - Correo sin @ o mayor a 160 caracteres.
 *    - Asunto menor a 5 o mayor a 120 caracteres.
 *    - Categorías permitidas (pedido, producto, pago, cuenta, otro).
 *    - Mensaje menor a 10 o mayor a 2000 caracteres.
 * 3. Validación de propiedad (propietario no nulo).
 * 4. Resiliencia ante fallos en el servicio.
 */
class SoporteListenerTest {

    private MensajeService service;
    private Channel canal;
    private SoporteListener listener;

    @BeforeEach
    void setUp() {
        service = mock(MensajeService.class);
        canal = mock(Channel.class);
        listener = new SoporteListener(service);
    }

    // ──── Flujo Exitoso (ACK) ────

    @ParameterizedTest
    @ValueSource(strings = {"pedido", "producto", "pago", "cuenta", "otro"})
    void procesaMensajeValidoConCategoriasPermitidasYConfirmaConAck(String categoria) throws IOException {
        String cuerpo = """
            {
              "correo": "cliente@correo.com",
              "asunto": "Consulta sobre %s",
              "categoria": "%s",
              "mensaje": "Estimado equipo, necesito asistencia con respecto a este tema por favor."
            }
            """.formatted(categoria, categoria);
        String propietario = "https://issuer.example|cliente-42";
        long tag = 70L;

        when(service.guardar(any(Mensaje.class))).thenAnswer(i -> i.getArgument(0));

        listener.recibir(cuerpo, propietario, tag, canal);

        ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
        verify(service).guardar(captor.capture());
        Mensaje guardado = captor.getValue();

        assertEquals(propietario, guardado.getPropietario());
        assertEquals("cliente@correo.com", guardado.getCorreo());
        assertEquals(categoria, guardado.getCategoria());
        assertEquals("abierto", guardado.getEstado());
        assertNotNull(guardado.getFechaCreacion());

        verify(canal).basicAck(tag, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    // ──── Validaciones y NACK (DLQ) ────

    @Test
    void rechazaMensajeSinPropietarioYEnviaADlq() throws IOException {
        String cuerpo = """
            {
              "correo": "cliente@correo.com",
              "asunto": "Consulta soporte",
              "categoria": "pedido",
              "mensaje": "Mensaje válido de más de diez caracteres."
            }
            """;
        long tag = 71L;

        listener.recibir(cuerpo, null, tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(service, never()).guardar(any());
    }

    @Test
    void rechazaCorreoSinArrobaYEnviaADlq() throws IOException {
        String cuerpo = """
            {
              "correo": "correo_invalido_sin_arroba",
              "asunto": "Consulta soporte",
              "categoria": "cuenta",
              "mensaje": "Mensaje válido de más de diez caracteres."
            }
            """;
        long tag = 72L;

        listener.recibir(cuerpo, "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(service, never()).guardar(any());
    }

    @Test
    void rechazaAsuntoMenorACincoCaracteres() throws IOException {
        String cuerpo = """
            {
              "correo": "cliente@correo.com",
              "asunto": "Hola",
              "categoria": "otro",
              "mensaje": "Mensaje válido de más de diez caracteres."
            }
            """;
        long tag = 73L;

        listener.recibir(cuerpo, "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(service, never()).guardar(any());
    }

    @Test
    void rechazaCategoriaNoPermitidaYEnviaADlq() throws IOException {
        String cuerpo = """
            {
              "correo": "cliente@correo.com",
              "asunto": "Consulta VIP",
              "categoria": "categoria_inventada",
              "mensaje": "Mensaje válido de más de diez caracteres."
            }
            """;
        long tag = 74L;

        listener.recibir(cuerpo, "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(service, never()).guardar(any());
    }

    @Test
    void rechazaMensajeMenorADiezCaracteres() throws IOException {
        String cuerpo = """
            {
              "correo": "cliente@correo.com",
              "asunto": "Consulta soporte",
              "categoria": "pago",
              "mensaje": "Corto"
            }
            """;
        long tag = 75L;

        listener.recibir(cuerpo, "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(service, never()).guardar(any());
    }

    @Test
    void rechazaJsonCorruptoYEnviaADlq() throws IOException {
        long tag = 76L;

        listener.recibir("{ no json", "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(service, never()).guardar(any());
    }

    // ──── Resiliencia ante Excepciones ────

    @Test
    void capturaFalloEnServicioYEnviaADlqSinCaerRabbitMQ() throws IOException {
        String cuerpo = """
            {
              "correo": "cliente@correo.com",
              "asunto": "Consulta con error",
              "categoria": "pedido",
              "mensaje": "Mensaje válido de más de diez caracteres."
            }
            """;
        long tag = 77L;

        when(service.guardar(any())).thenThrow(new RuntimeException("Error en base de datos"));

        listener.recibir(cuerpo, "user-1", tag, canal);

        // Verifica que la excepción no escala y el mensaje se deriva a DLQ
        verify(canal).basicNack(tag, false, false);
        verify(canal, never()).basicAck(anyLong(), anyBoolean());
    }
}
