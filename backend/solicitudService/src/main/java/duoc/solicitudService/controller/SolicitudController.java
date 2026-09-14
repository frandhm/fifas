package duoc.solicitudService.controller;

import duoc.solicitudService.entity.Solicitud;
import duoc.solicitudService.repository.SolicitudRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {
    private final SolicitudRepository repository;
    public SolicitudController(SolicitudRepository repository) { this.repository = repository; }

    public record NuevaSolicitud(
        @NotBlank @Size(max=100) String productoId,
        @NotBlank @Size(max=160) String productoNombre,
        @NotNull @Min(1) @Max(99) Integer cantidad,
        @NotNull @DecimalMin("0.0") BigDecimal precioUnitario,
        @Size(max=10) String talla,
        @Size(max=30) String nombreEstampado,
        @Min(0) @Max(99) Integer numeroEstampado) {}
    public record NuevaCompra(@NotEmpty @Size(max=50) List<@Valid NuevaSolicitud> items) {}

    private String propietario(Jwt jwt) { return jwt.getIssuer() + "|" + jwt.getSubject(); }
    private Solicitud find(Long id, Jwt jwt) {
        return repository.findByIdAndPropietario(id, propietario(jwt))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping public List<Solicitud> listar(@AuthenticationPrincipal Jwt jwt) {
        return repository.findByPropietarioOrderByIdDesc(propietario(jwt));
    }
    @GetMapping("/{id}") public Solicitud buscar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return find(id, jwt);
    }
    private Solicitud crearEntidad(NuevaSolicitud body, Jwt jwt) {
        var solicitud = new Solicitud();
        solicitud.setPropietario(propietario(jwt)); solicitud.setProductoId(body.productoId());
        solicitud.setProductoNombre(body.productoNombre().trim()); solicitud.setCantidad(body.cantidad());
        solicitud.setPrecioUnitario(body.precioUnitario()); solicitud.setTalla(body.talla());
        solicitud.setNombreEstampado(body.nombreEstampado()); solicitud.setNumeroEstampado(body.numeroEstampado());
        solicitud.setEstado("PENDIENTE"); solicitud.setFechaCreacion(Instant.now());
        return solicitud;
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public List<Solicitud> crear(@Valid @RequestBody NuevaCompra body, @AuthenticationPrincipal Jwt jwt) {
        return repository.saveAll(body.items().stream().map(item -> crearEntidad(item, jwt)).toList());
    }
}
