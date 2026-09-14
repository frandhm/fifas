package duoc.carritoService.controller;

import duoc.carritoService.entity.Carrito;
import duoc.carritoService.repository.CarritoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/carritos")
public class CarritoController {
    private final CarritoRepository repository;
    public CarritoController(CarritoRepository repository) { this.repository = repository; }

    public record Linea(@NotBlank @Size(max=100) String productoId,
        @NotNull @Min(1) @Max(99) Integer cantidad, @Size(max=10) String talla,
        @Size(max=30) String nombreJugador, @Min(0) @Max(99) Integer dorsal) {}

    private String propietario(Jwt jwt) { return jwt.getIssuer() + "|" + jwt.getSubject(); }

    @GetMapping public List<Carrito> listar(@AuthenticationPrincipal Jwt jwt) {
        return repository.findByPropietarioOrderByIdAsc(propietario(jwt));
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Carrito guardar(@Valid @RequestBody Linea body, @AuthenticationPrincipal Jwt jwt) {
        String owner = propietario(jwt);
        Carrito item = repository.findLine(owner, body.productoId(), body.talla(),
            body.nombreJugador(), body.dorsal()).orElseGet(Carrito::new);
        item.setPropietario(owner); item.setProductoId(body.productoId());
        item.setCantidad(body.cantidad()); item.setTalla(body.talla());
        item.setNombreJugador(body.nombreJugador()); item.setDorsal(body.dorsal());
        return repository.save(item);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Carrito item = repository.findByIdAndPropietario(id, propietario(jwt))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(item);
    }

    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vaciar(@AuthenticationPrincipal Jwt jwt) {
        repository.deleteByPropietario(propietario(jwt));
    }
}
