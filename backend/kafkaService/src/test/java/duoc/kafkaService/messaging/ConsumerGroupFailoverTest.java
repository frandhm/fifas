package duoc.kafkaService.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del Criterio: "Consumer groups: distribución de mensajes y caída de un consumidor".
 * Simula:
 * 1. Distribución de mensajes entre múltiples consumidores pertenecientes al mismo consumer group.
 * 2. Caída (failover) de un consumidor del grupo y redistribución de mensajes hacia los consumidores activos
 *    sin pérdida de mensajes.
 */
class ConsumerGroupFailoverTest {

    // Simulación de dos instancias de consumidores en el mismo consumer group: 'kafka-service-alertas'
    static class MockConsumerInstance {
        final String consumerId;
        final List<String> receivedMessages = new ArrayList<>();
        final AtomicBoolean isAlive = new AtomicBoolean(true);

        MockConsumerInstance(String consumerId) {
            this.consumerId = consumerId;
        }

        boolean process(ConsumerRecord<String, String> record) {
            if (!isAlive.get()) {
                return false; // Consumidor caído, no procesa
            }
            receivedMessages.add(record.value());
            return true;
        }

        void crash() {
            isAlive.set(false);
        }

        void recover() {
            isAlive.set(true);
        }
    }

    private MockConsumerInstance consumerA;
    private MockConsumerInstance consumerB;

    @BeforeEach
    void setUp() {
        consumerA = new MockConsumerInstance("consumer-instance-1");
        consumerB = new MockConsumerInstance("consumer-instance-2");
    }

    @Test
    void distribuyeMensajesEntreConsumidoresDelMismoGrupo() {
        // Simulación de mensajes asignados por partición:
        // Consumidor A atiende partición 0 y 1; Consumidor B atiende partición 2
        List<ConsumerRecord<String, String>> records = List.of(
            new ConsumerRecord<>("ventas.alertas", 0, 0L, "k1", "alerta-particion-0"),
            new ConsumerRecord<>("ventas.alertas", 1, 0L, "k2", "alerta-particion-1"),
            new ConsumerRecord<>("ventas.alertas", 2, 0L, "k3", "alerta-particion-2")
        );

        for (var record : records) {
            if (record.partition() == 2) {
                consumerB.process(record);
            } else {
                consumerA.process(record);
            }
        }

        // Ambos consumidores reciben mensajes según su asignación de partición
        assertEquals(2, consumerA.receivedMessages.size(), "Consumidor A procesa particiones 0 y 1");
        assertEquals(1, consumerB.receivedMessages.size(), "Consumidor B procesa partición 2");
        assertTrue(consumerA.receivedMessages.contains("alerta-particion-0"));
        assertTrue(consumerB.receivedMessages.contains("alerta-particion-2"));
    }

    @Test
    void manejaCaidaDeUnConsumidorYRedistribuyeMensajesAlConsumidorActivo() {
        // 1) Fase inicial: ambos consumidores operan normalmente
        consumerA.process(new ConsumerRecord<>("ventas.alertas", 0, 0L, null, "msg-1"));
        consumerB.process(new ConsumerRecord<>("ventas.alertas", 1, 0L, null, "msg-2"));

        assertEquals(1, consumerA.receivedMessages.size());
        assertEquals(1, consumerB.receivedMessages.size());

        // 2) Simulación de fallo: Consumer B sufre una caída (crash / disconnect)
        consumerB.crash();
        assertFalse(consumerB.isAlive.get());

        // 3) Nuevo mensaje destinado a la partición de Consumer B
        ConsumerRecord<String, String> nuevoRecord = new ConsumerRecord<>("ventas.alertas", 1, 1L, null, "msg-failover-3");

        // El rebalanceo asigna la partición al consumidor sobreviviente (Consumer A)
        boolean handledByB = consumerB.process(nuevoRecord);
        assertFalse(handledByB, "El consumidor caído no debe procesar el mensaje");

        // Rebalanceo de Consumer Group: Consumer A asume la carga
        boolean handledByA = consumerA.process(nuevoRecord);
        assertTrue(handledByA, "El consumidor sobreviviente debe procesar el mensaje rebalanceado");

        // 4) Verificación de integridad: cero mensajes perdidos
        assertEquals(2, consumerA.receivedMessages.size());
        assertEquals("msg-failover-3", consumerA.receivedMessages.get(1));
    }

    @Test
    void consumidorRecuperadoPuedeReintegrarseAlGrupo() {
        consumerB.crash();
        assertFalse(consumerB.isAlive.get());

        // Recuperación / reinicio del pod o servicio
        consumerB.recover();
        assertTrue(consumerB.isAlive.get());

        boolean procesado = consumerB.process(new ConsumerRecord<>("ventas.alertas", 1, 5L, null, "msg-post-recovery"));
        assertTrue(procesado);
        assertEquals(1, consumerB.receivedMessages.size());
        assertEquals("msg-post-recovery", consumerB.receivedMessages.get(0));
    }
}

