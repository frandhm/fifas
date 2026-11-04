package duoc.kafkaService.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class AlertaListener {
    private static final Logger log = LoggerFactory.getLogger(AlertaListener.class);
    private final List<String> alertas = new CopyOnWriteArrayList<>();
    private final List<String> actividad = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "ventas.alertas", groupId = "kafka-service-alertas")
    public void alerta(ConsumerRecord<String, String> r) {
        log.warn("Alerta recibida partición={} offset={}: {}", r.partition(), r.offset(), r.value());
        guardar(alertas, r.value());
    }

    @KafkaListener(topics = "actividad.usuario", groupId = "kafka-service-actividad")
    public void actividad(ConsumerRecord<String, String> r) {
        log.info("Actividad partición={} offset={}: {}", r.partition(), r.offset(), r.value());
        guardar(actividad, r.value());
    }

    private void guardar(List<String> lista, String valor) {
        lista.add(valor);
        if (lista.size() > 100) lista.remove(0);
    }

    public List<String> getAlertas() { return List.copyOf(alertas); }
    public List<String> getActividad() { return List.copyOf(actividad); }
}