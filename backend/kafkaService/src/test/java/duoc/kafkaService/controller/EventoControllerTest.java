package duoc.kafkaService.controller;

import duoc.kafkaService.config.KafkaConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del productor Kafka (EventoController).
 * Verifica la publicación en topics permitidos y el rechazo de topics no autorizados.
 */
class EventoControllerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final EventoController controller = new EventoController(kafka);

    // ──── Publicación exitosa ────

    @Test
    void publicaEnTopicVentasRegistradas() {
        String mensaje = """
            {"solicitudId":1,"productoId":"CAM-01","cantidad":2,"precioUnitario":30000,"total":60000,"timestamp":"2026-11-10T12:00:00Z"}
            """;
        ResponseEntity<String> resp = controller.publicar(KafkaConfig.VENTAS, "key-1", mensaje);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().contains("ventas.registradas"));
        verify(kafka).send(KafkaConfig.VENTAS, "key-1", mensaje);
    }

    @Test
    void publicaEnTopicActividadUsuario() {
        String mensaje = """
            {"accion":"login","userId":"user-abc"}
            """;
        ResponseEntity<String> resp = controller.publicar(KafkaConfig.ACTIVIDAD, null, mensaje);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(kafka).send(KafkaConfig.ACTIVIDAD, null, mensaje);
    }

    @Test
    void publicaConKeyCuandoSeProvee() {
        controller.publicar(KafkaConfig.VENTAS, "mi-clave", "{}");
        verify(kafka).send(KafkaConfig.VENTAS, "mi-clave", "{}");
    }

    @Test
    void publicaSinKeyCuandoEsNull() {
        controller.publicar(KafkaConfig.VENTAS, null, "{}");
        verify(kafka).send(KafkaConfig.VENTAS, null, "{}");
    }

    // ──── Topics no permitidos ────

    @ParameterizedTest
    @ValueSource(strings = {"topic.inexistente", "ventas.alertas", "otro.topic", "", "VENTAS.REGISTRADAS"})
    void rechazaTopicNoPermitido(String topicInvalido) {
        ResponseEntity<String> resp = controller.publicar(topicInvalido, null, "{}");

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().contains("Topic no permitido"));
        verify(kafka, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void noPermitePublicarEnTopicAlertas() {
        // ventas.alertas es solo de salida del Streams, no de entrada manual
        ResponseEntity<String> resp = controller.publicar("ventas.alertas", null, "{}");
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }
}

