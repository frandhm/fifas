package duoc.kafkaService.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de operaciones del administrador Kafka (KafkaConfig).
 * Verifica la definición y configuración de topics, particiones y factor de réplica.
 */
class KafkaConfigTest {

    private KafkaConfig kafkaConfig;

    @BeforeEach
    void setUp() {
        kafkaConfig = new KafkaConfig();
    }

    // ──── Nombres de Topics ────

    @Test
    void constantesDeTopicsSonCorrectas() {
        assertEquals("ventas.registradas", KafkaConfig.VENTAS);
        assertEquals("ventas.alertas", KafkaConfig.ALERTAS);
        assertEquals("actividad.usuario", KafkaConfig.ACTIVIDAD);
    }

    // ──── Topic: ventas.registradas ────

    @Test
    void topicVentasConfiguradoCorrectamente() {
        NewTopic topic = kafkaConfig.ventasTopic();

        assertNotNull(topic, "El bean NewTopic de ventas no debe ser nulo");
        assertEquals(KafkaConfig.VENTAS, topic.name());
        assertEquals(3, topic.numPartitions(), "Debe tener 3 particiones para distribución de carga");
        assertEquals((short) 1, topic.replicationFactor(), "Factor de réplica 1 para entorno local");
    }

    // ──── Topic: ventas.alertas ────

    @Test
    void topicAlertasConfiguradoCorrectamente() {
        NewTopic topic = kafkaConfig.alertasTopic();

        assertNotNull(topic, "El bean NewTopic de alertas no debe ser nulo");
        assertEquals(KafkaConfig.ALERTAS, topic.name());
        assertEquals(3, topic.numPartitions(), "Debe tener 3 particiones para paralelismo");
        assertEquals((short) 1, topic.replicationFactor(), "Factor de réplica 1");
    }

    // ──── Topic: actividad.usuario ────

    @Test
    void topicActividadConfiguradoCorrectamente() {
        NewTopic topic = kafkaConfig.actividadTopic();

        assertNotNull(topic, "El bean NewTopic de actividad no debe ser nulo");
        assertEquals(KafkaConfig.ACTIVIDAD, topic.name());
        assertEquals(3, topic.numPartitions(), "Debe tener 3 particiones");
        assertEquals((short) 1, topic.replicationFactor(), "Factor de réplica 1");
    }

    // ──── Validación de Particiones y Réplicas en Todos los Topics ────

    @Test
    void todosLosTopicsTienenConfiguracionConsistente() {
        NewTopic[] topics = {
            kafkaConfig.ventasTopic(),
            kafkaConfig.alertasTopic(),
            kafkaConfig.actividadTopic()
        };

        for (NewTopic topic : topics) {
            assertTrue(topic.numPartitions() >= 3,
                "Topic " + topic.name() + " debe tener al menos 3 particiones para soportar múltiples consumidores en el grupo");
            assertEquals((short) 1, topic.replicationFactor(),
                "Topic " + topic.name() + " réplica debe ser 1");
        }
    }
}

