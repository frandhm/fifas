package duoc.kafkaService.controller;

import duoc.kafkaService.messaging.AlertaListener;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kafka")
public class ConsultaController {
    private final AlertaListener listener;
    public ConsultaController(AlertaListener listener) { this.listener = listener; }

    @GetMapping("/alertas")
    public List<String> alertas() { return listener.getAlertas(); }

    @GetMapping("/actividad")
    public List<String> actividad() { return listener.getActividad(); }
}