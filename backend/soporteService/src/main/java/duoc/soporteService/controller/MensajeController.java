package duoc.soporteService.controller;

import duoc.soporteService.entity.Mensaje;
import duoc.soporteService.service.MensajeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mensajes")
public class MensajeController {

    @Autowired
    private MensajeService service;

    @GetMapping
    public List<Mensaje> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mensaje> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Mensaje> crear(@RequestBody Mensaje mensaje) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.guardar(mensaje));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}