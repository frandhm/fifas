package duoc.rabbit_service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eventos")
public class EventoController {

    private final RabbitTemplate rabbitTemplate;

    public EventoController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/{routingKey}")
    public ResponseEntity<String> publicar(@PathVariable String routingKey,
                                           @RequestBody String mensaje) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, mensaje);
        return ResponseEntity.ok("Evento publicado: " + routingKey);
    }
}