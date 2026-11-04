package duoc.kafkaService.controller;

import duoc.kafkaService.config.KafkaConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/kafka/eventos")
public class EventoController {
    private static final Set<String> TOPICS_PERMITIDOS =
            Set.of(KafkaConfig.VENTAS, KafkaConfig.ACTIVIDAD);

    private final KafkaTemplate<String, String> kafka;

    public EventoController(KafkaTemplate<String, String> kafka) { this.kafka = kafka; }

    @PostMapping("/{topic}")
    public ResponseEntity<String> publicar(@PathVariable String topic,
                                           @RequestParam(required = false) String key,
                                           @RequestBody String mensaje) {
        if (!TOPICS_PERMITIDOS.contains(topic)) {
            return ResponseEntity.badRequest().body("Topic no permitido: " + topic);
        }
        kafka.send(topic, key, mensaje);
        return ResponseEntity.ok("Evento publicado: " + topic);
    }
}