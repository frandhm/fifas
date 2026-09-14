package duoc.usuarioService.controller;

import duoc.usuarioService.entity.Usuario;
import duoc.usuarioService.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios/me")
public class PerfilController {
    private final UsuarioRepository repository;
    public PerfilController(UsuarioRepository repository) { this.repository = repository; }

    public record Datos(@NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotBlank @Email @Size(max = 254) String correo,
        @NotNull @Size(max = 30) String telefono,
        @NotNull @Size(max = 255) String direccion) {}
    public record Perfil(Long id, String nombre, String apellido, String correo,
                         String telefono, String direccion, List<String> roles) {
        static Perfil de(Usuario u, Jwt jwt) {
            return new Perfil(u.getId(), u.getNombre(), u.getApellido(), u.getCorreo(),
                u.getTelefono(), u.getDireccion(), PerfilController.roles(jwt));
        }
    }
    private static List<String> roles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles == null ? List.of() : roles.stream()
            .filter(role -> role != null && !role.isBlank()).distinct().toList();
    }
    private String propietario(Jwt jwt) {
        if (jwt.getIssuer() == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return jwt.getIssuer() + "|" + jwt.getSubject();
    }
    @GetMapping
    public ResponseEntity<?> obtener(@AuthenticationPrincipal Jwt jwt) {
        var usuario = repository.findByPropietario(propietario(jwt));
        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "PROFILE_NOT_FOUND", "roles", roles(jwt)));
        }
        return ResponseEntity.ok(Perfil.de(usuario.get(), jwt));
    }
    @PutMapping
    @Transactional
    public Perfil guardar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Datos datos) {
        String propietario = propietario(jwt);
        Usuario usuario = repository.findByPropietario(propietario).orElseGet(Usuario::new);
        usuario.setPropietario(propietario);
        usuario.setNombre(datos.nombre().trim());
        usuario.setApellido(datos.apellido().trim());
        usuario.setCorreo(datos.correo().trim());
        usuario.setTelefono(datos.telefono().trim());
        usuario.setDireccion(datos.direccion().trim());
        try {
            return Perfil.de(repository.saveAndFlush(usuario), jwt);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El perfil cambió durante el guardado. Recarga e intenta nuevamente.", ex);
        }
    }
}
