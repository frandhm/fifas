package duoc.rabbit_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de EventoController en rabbit-service.
 * Verifica:
 * 1. Publicación de mensajes en eventos_exchange con routing keys específicas.
 * 2. Decodificación de JWT y adjunción de header 'propietario' (iss|sub).
 * 3. Publicación sin header Authorization.
 * 4. Manejo de tokens inválidos o malformados.
 */
class EventoControllerTest {

    private RabbitTemplate rabbitTemplate;
    private EventoController controller;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        controller = new EventoController(rabbitTemplate);
    }

    // ──── Publicación básica ────

    @Test
    void publicaMensajeEnExchangeConRoutingKey() {
        String routingKey = "carrito.agregado";
        String mensaje = "{\"productoId\":\"CAM-01\",\"cantidad\":1}";

        ResponseEntity<String> resp = controller.publicar(routingKey, null, mensaje);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().contains("carrito.agregado"));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(routingKey), eq(mensaje), any(MessagePostProcessor.class));
    }

    // ──── JWT y Header 'propietario' ────

    @Test
    void adjuntaHeaderPropietarioDesdeJwtValido() {
        // Crear un JWT simulado: header.payload.signature
        String payloadJson = "{\"iss\":\"https://login.microsoftonline.com/tenant/v2.0\",\"sub\":\"user-12345\"}";
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String token = "Bearer header." + encodedPayload + ".signature";

        String routingKey = "compra.confirmada";
        String mensaje = "{\"ordenId\": 100}";

        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        controller.publicar(routingKey, token, mensaje);

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(routingKey), eq(mensaje), captor.capture());

        // Simular ejecución del MessagePostProcessor
        Message originalMessage = new Message(mensaje.getBytes(StandardCharsets.UTF_8));
        Message processedMessage = captor.getValue().postProcessMessage(originalMessage);

        assertNotNull(processedMessage);
        assertEquals("https://login.microsoftonline.com/tenant/v2.0|user-12345",
                processedMessage.getMessageProperties().getHeader("propietario"));
    }

    @Test
    void publicaSinHeaderPropietarioCuandoAuthorizationEsNull() {
        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        controller.publicar("notif.general", null, "{}");

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq("notif.general"), eq("{}"), captor.capture());

        Message originalMessage = new Message("{}".getBytes());
        Message processedMessage = captor.getValue().postProcessMessage(originalMessage);

        assertNull(processedMessage.getMessageProperties().getHeader("propietario"));
    }

    @Test
    void manejaTokenMalformadoSinLanzarExcepcion() {
        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        // Token que no tiene formato JWT
        ResponseEntity<String> resp = controller.publicar("perfil.actualizado", "Bearer token_invalido_sin_puntos", "{}");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq("perfil.actualizado"), eq("{}"), captor.capture());

        Message msg = captor.getValue().postProcessMessage(new Message("{}".getBytes()));
        assertNull(msg.getMessageProperties().getHeader("propietario"));
    }

    @Test
    void manejaTokenSinPrefijoBearer() {
        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        ResponseEntity<String> resp = controller.publicar("soporte.mensaje", "Basic user:pass", "{}");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq("soporte.mensaje"), eq("{}"), captor.capture());

        Message msg = captor.getValue().postProcessMessage(new Message("{}".getBytes()));
        assertNull(msg.getMessageProperties().getHeader("propietario"));
    }
}

