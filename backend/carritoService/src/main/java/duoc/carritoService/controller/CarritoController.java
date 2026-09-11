package duoc.carritoService.controller;

import duoc.carritoService.entity.Carrito;
import duoc.carritoService.service.CarritoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carritos")
public class CarritoController {

    @Autowired
    private CarritoService service;

    @GetMapping
    public List<Carrito> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Carrito> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Carrito crear(@RequestBody Carrito carrito) {
        return service.guardar(carrito);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Carrito> actualizar(@PathVariable Long id, @RequestBody Carrito carrito) {
        if (service.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        carrito.setId(id);
        return ResponseEntity.ok(service.guardar(carrito));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}