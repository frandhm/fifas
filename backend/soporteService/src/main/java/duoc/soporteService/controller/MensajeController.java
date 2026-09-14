package duoc.soporteService.controller;

import duoc.soporteService.entity.Mensaje;
import duoc.soporteService.service.MensajeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;

import java.util.List;

@RestController
@RequestMapping("/api/mensajes")
public class MensajeController {

    @Autowired
    private MensajeService service;

    public record NuevoMensaje(
            @NotBlank @Email @Size(max = 160) String correo,
            @NotBlank @Size(min = 5, max = 120) String asunto,
            @NotNull @Pattern(regexp = "pedido|producto|pago|cuenta|otro") String categoria,
            @NotBlank @Size(min = 10, max = 2000) String mensaje) {}

    private String propietario(Jwt jwt) {
        return jwt.getIssuer() + "|" + jwt.getSubject();
    }

    @GetMapping
    public List<Mensaje> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(propietario(jwt));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mensaje> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.buscarPorId(id, propietario(jwt))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Mensaje> crear(@Valid @RequestBody NuevoMensaje body, @AuthenticationPrincipal Jwt jwt) {
        Mensaje mensaje = new Mensaje();
        mensaje.setCorreo(body.correo().trim());
        mensaje.setAsunto(body.asunto().trim());
        mensaje.setCategoria(body.categoria());
        mensaje.setMensaje(body.mensaje().trim());
        mensaje.setEstado("abierto");
        mensaje.setFechaCreacion(Instant.now());
        mensaje.setPropietario(propietario(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.guardar(mensaje));
    }

}
