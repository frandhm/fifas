package duoc.bff.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/api/public/ping")
    public String publicPing() {
        return "pong publico, sin token";
    }

    @GetMapping("/api/ping")
    public String privatePing() {
        return "pong privado, necesitas token valido";
    }
}
