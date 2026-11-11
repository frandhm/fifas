package duoc.kafkaService.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias del consumidor Kafka (AlertaListener).
 * Verifica recepción, almacenamiento en memoria, límite de 100 elementos,
 * consumer groups y devolución de copias inmutables.
 */
class AlertaListenerTest {

    private AlertaListener listener;

    @BeforeEach
    void setUp() {
        listener = new AlertaListener();
    }

    // ──── Consumidor de Alertas ────

    @Test
    void recibeYalmacenaAlerta() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("ventas.alertas", 0, 0L, "key", "{\"motivo\":\"Alto valor\"}");
        listener.alerta(record);

        assertEquals(1, listener.getAlertas().size());
        assertEquals("{\"motivo\":\"Alto valor\"}", listener.getAlertas().get(0));
    }

    @Test
    void recibeMultiplesAlertas() {
        for (int i = 0; i < 5; i++) {
            ConsumerRecord<String, String> r = new ConsumerRecord<>("ventas.alertas", 0, i, null, "alerta-" + i);
            listener.alerta(r);
        }
        assertEquals(5, listener.getAlertas().size());
        assertEquals("alerta-0", listener.getAlertas().get(0));
        assertEquals("alerta-4", listener.getAlertas().get(4));
    }

    // ──── Consumidor de Actividad ────

    @Test
    void recibeYalmacenaActividad() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("actividad.usuario", 1, 0L, "user-1", "{\"accion\":\"login\"}");
        listener.actividad(record);

        assertEquals(1, listener.getActividad().size());
        assertEquals("{\"accion\":\"login\"}", listener.getActividad().get(0));
    }

    @Test
    void alertasYactividadSonIndependientes() {
        listener.alerta(new ConsumerRecord<>("ventas.alertas", 0, 0L, null, "a1"));
        listener.actividad(new ConsumerRecord<>("actividad.usuario", 0, 0L, null, "act1"));

        assertEquals(1, listener.getAlertas().size());
        assertEquals(1, listener.getActividad().size());
    }

    // ──── Límite de 100 elementos ────

    @Test
    void eliminaElementoMasAntiguoAlSuperarLimite() {
        for (int i = 0; i < 101; i++) {
            listener.alerta(new ConsumerRecord<>("ventas.alertas", 0, i, null, "msg-" + i));
        }
        assertEquals(100, listener.getAlertas().size());
        // El primer elemento (msg-0) debería haberse eliminado
        assertEquals("msg-1", listener.getAlertas().get(0));
        assertEquals("msg-100", listener.getAlertas().get(99));
    }

    @Test
    void limiteDe100TambienAplicaAActividad() {
        for (int i = 0; i < 105; i++) {
            listener.actividad(new ConsumerRecord<>("actividad.usuario", 0, i, null, "act-" + i));
        }
        assertEquals(100, listener.getActividad().size());
        assertEquals("act-5", listener.getActividad().get(0));
    }

    // ──── Copias inmutables ────

    @Test
    void getAlertasDevuelveCopiaInmutable() {
        listener.alerta(new ConsumerRecord<>("ventas.alertas", 0, 0L, null, "x"));
        assertThrows(UnsupportedOperationException.class, () -> listener.getAlertas().add("hack"));
    }

    @Test
    void getActividadDevuelveCopiaInmutable() {
        listener.actividad(new ConsumerRecord<>("actividad.usuario", 0, 0L, null, "x"));
        assertThrows(UnsupportedOperationException.class, () -> listener.getActividad().add("hack"));
    }

    // ──── Consumer Groups ────

    @Test
    void alertaListenerUsaGroupIdCorrecto() throws Exception {
        // Verificar que la anotación @KafkaListener tiene el groupId esperado
        var method = AlertaListener.class.getMethod("alerta", ConsumerRecord.class);
        var annotation = method.getAnnotation(org.springframework.kafka.annotation.KafkaListener.class);
        assertNotNull(annotation);
        assertEquals("kafka-service-alertas", annotation.groupId());
        assertArrayEquals(new String[]{"ventas.alertas"}, annotation.topics());
    }

    @Test
    void actividadListenerUsaGroupIdCorrecto() throws Exception {
        var method = AlertaListener.class.getMethod("actividad", ConsumerRecord.class);
        var annotation = method.getAnnotation(org.springframework.kafka.annotation.KafkaListener.class);
        assertNotNull(annotation);
        assertEquals("kafka-service-actividad", annotation.groupId());
        assertArrayEquals(new String[]{"actividad.usuario"}, annotation.topics());
    }

    // ──── Manejo de diferentes particiones ────

    @Test
    void recibeDesdeDistintasParticiones() {
        listener.alerta(new ConsumerRecord<>("ventas.alertas", 0, 0L, null, "part0"));
        listener.alerta(new ConsumerRecord<>("ventas.alertas", 1, 0L, null, "part1"));
        listener.alerta(new ConsumerRecord<>("ventas.alertas", 2, 0L, null, "part2"));

        assertEquals(3, listener.getAlertas().size());
    }
}

