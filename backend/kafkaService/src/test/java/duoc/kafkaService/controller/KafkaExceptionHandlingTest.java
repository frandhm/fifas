package duoc.kafkaService.controller;

import duoc.kafkaService.config.KafkaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de excepciones en el productor Kafka.
 * Verifica el comportamiento cuando KafkaTemplate lanza una excepción
 * (ej: broker no disponible, timeout de conexión, error de serialización).
 */
class KafkaExceptionHandlingTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final EventoController controller = new EventoController(kafka);

    @Test
    void propagaExcepcionCuandoBrokerNoEstaDisponible() {
        doThrow(new KafkaException("Broker no disponible / Timeout de conexión"))
                .when(kafka).send(eq(KafkaConfig.VENTAS), any(), anyString());

        KafkaException ex = assertThrows(KafkaException.class, () ->
                controller.publicar(KafkaConfig.VENTAS, "key-error", "{\"monto\": 50000}"));

        assertTrue(ex.getMessage().contains("Broker no disponible"));
        verify(kafka).send(KafkaConfig.VENTAS, "key-error", "{\"monto\": 50000}");
    }

    @Test
    void manejaErrorDeEnvioEnTopicActividad() {
        doThrow(new org.apache.kafka.common.errors.TimeoutException("Fallo de red en Kafka"))
                .when(kafka).send(eq(KafkaConfig.ACTIVIDAD), any(), anyString());

        assertThrows(org.apache.kafka.common.errors.TimeoutException.class, () ->
                controller.publicar(KafkaConfig.ACTIVIDAD, null, "{\"evento\":\"login\"}"));
    }
}

