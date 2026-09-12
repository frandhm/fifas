package duoc.notificacionService.controller;

import duoc.notificacionService.entity.Notificacion;
import duoc.notificacionService.repository.NotificacionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {
    private final NotificacionRepository repository;
    public NotificacionController(NotificacionRepository repository) { this.repository = repository; }
    public record Nueva(@NotBlank @Size(max=240) String mensaje,
        @NotNull @Pattern(regexp="sesion|carrito|perfil|compra") String tipo) {}
    public record Lectura(@NotNull Boolean leido) {}
    private String owner(Jwt jwt) { return jwt.getIssuer() + "|" + jwt.getSubject(); }
    private Notificacion find(Long id, Jwt jwt) {
        return repository.findByIdAndPropietario(id, owner(jwt))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @GetMapping public List<Notificacion> listar(@AuthenticationPrincipal Jwt jwt) {
        return repository.findByPropietarioOrderByIdDesc(owner(jwt));
    }
    @GetMapping("/{id}") public Notificacion buscar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return find(id, jwt);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Notificacion crear(@Valid @RequestBody Nueva body, @AuthenticationPrincipal Jwt jwt) {
        var item = new Notificacion();
        item.setPropietario(owner(jwt)); item.setMensaje(body.mensaje()); item.setTipo(body.tipo());
        item.setLeido(false); item.setFecha(Instant.now());
        return repository.save(item);
    }
    @PutMapping("/{id}") public Notificacion leer(@PathVariable Long id,
        @Valid @RequestBody Lectura body, @AuthenticationPrincipal Jwt jwt) {
        var item = find(id, jwt); item.setLeido(body.leido()); return repository.save(item);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) { repository.delete(find(id, jwt)); }
}
