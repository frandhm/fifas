package duoc.notificacionService.messaging;

import com.rabbitmq.client.Channel;
import duoc.notificacionService.entity.Notificacion;
import duoc.notificacionService.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de integración/componente para NotificacionListener.
 * Verifica:
 * 1. Procesamiento exitoso de eventos válidos según reglas de negocio y confirmación ACK.
 * 2. Rechazo sin requeue (NACK -> DLQ) ante datos inválidos (tipo desconocido, mensaje vacío, mensaje > 240 chars).
 * 3. Rechazo de mensajes sin propietario (aislamiento de cuentas).
 * 4. Manejo de excepciones para que errores no detengan RabbitMQ ni el consumidor.
 */
class NotificacionListenerTest {

    private NotificacionRepository repository;
    private Channel canal;
    private NotificacionListener listener;

    @BeforeEach
    void setUp() {
        repository = mock(NotificacionRepository.class);
        canal = mock(Channel.class);
        listener = new NotificacionListener(repository);
    }

    // ──── Caso Exitoso (ACK) ────

    @ParameterizedTest
    @ValueSource(strings = {"sesion", "carrito", "perfil", "compra"})
    void procesaEventoValidoYConfirmaConAck(String tipo) throws IOException {
        String cuerpo = "{\"tipo\":\"" + tipo + "\",\"mensaje\":\"Mensaje de prueba para " + tipo + "\"}";
        String propietario = "https://issuer.example|alice";
        long tag = 10L;

        when(repository.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));

        listener.recibir(cuerpo, propietario, tag, canal);

        // Verifica que se guardó la entidad correctamente
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository).save(captor.capture());
        Notificacion guardada = captor.getValue();

        assertEquals(propietario, guardada.getPropietario());
        assertEquals(tipo, guardada.getTipo());
        assertEquals("Mensaje de prueba para " + tipo, guardada.getMensaje());
        assertFalse(guardada.getLeido());
        assertNotNull(guardada.getFecha());

        // Verifica confirmación con ACK manual
        verify(canal).basicAck(tag, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    // ──── Reglas de Negocio / Validaciones Fallidas (NACK -> DLQ) ────

    @Test
    void rechazaEventoSinPropietarioYEnviaADlq() throws IOException {
        String cuerpo = "{\"tipo\":\"compra\",\"mensaje\":\"Compra exitosa\"}";
        long tag = 1L;

        listener.recibir(cuerpo, null, tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(canal, never()).basicAck(anyLong(), anyBoolean());
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaTipoDesconocidoYEnviaADlq() throws IOException {
        String cuerpo = "{\"tipo\":\"tipo_invalido\",\"mensaje\":\"Hola\"}";
        long tag = 2L;

        listener.recibir(cuerpo, "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaMensajeEnBlancoYEnviaADlq() throws IOException {
        String cuerpo = "{\"tipo\":\"carrito\",\"mensaje\":\"   \"}";
        long tag = 3L;

        listener.recibir(cuerpo, "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaMensajeExcediendoLimiteDe240Caracteres() throws IOException {
        String mensajeLargo = "A".repeat(241);
        String cuerpo = "{\"tipo\":\"perfil\",\"mensaje\":\"" + mensajeLargo + "\"}";
        long tag = 4L;

        listener.recibir(cuerpo, "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaJsonCorruptoYEnviaADlq() throws IOException {
        String cuerpoInvalido = "{ corrupto }";
        long tag = 5L;

        listener.recibir(cuerpoInvalido, "user-1", tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(repository, never()).save(any());
    }

    // ──── Manejo de Excepciones y Resiliencia ────

    @Test
    void capturaExcepcionEnBaseDeDatosYEnviaADlqSinDetenerServicio() throws IOException {
        String cuerpo = "{\"tipo\":\"sesion\",\"mensaje\":\"Login exitoso\"}";
        long tag = 6L;

        when(repository.save(any(Notificacion.class))).thenThrow(new RuntimeException("Fallo de conexión con RDS"));

        listener.recibir(cuerpo, "user-1", tag, canal);

        // El error en BD debe derivar a DLQ sin propagar la excepción al contenedor RabbitMQ
        verify(canal).basicNack(tag, false, false);
        verify(canal, never()).basicAck(anyLong(), anyBoolean());
    }
}

