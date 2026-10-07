package duoc.rabbit_service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/rabbit/eventos")
public class EventoController {

    private final RabbitTemplate rabbitTemplate;

    public EventoController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/{routingKey}")
    public ResponseEntity<String> publicar(@PathVariable String routingKey,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody String mensaje) {

        String propietario = propietarioDelToken(authorization);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, mensaje, m -> {
            if (propietario != null) {
                m.getMessageProperties().setHeader("propietario", propietario);
            }
            return m;
        });
        return ResponseEntity.ok("Evento publicado: " + routingKey);
    }

    // Misma regla que usa notificación: issuer|subject del token
    private String propietarioDelToken(String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) return null;
            String[] partes = authorization.substring(7).split("\\.");
            String json = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = JsonParserFactory.getJsonParser().parseMap(json);
            return claims.get("iss") + "|" + claims.get("sub");
        } catch (Exception e) {
            return null;
        }
    }
}