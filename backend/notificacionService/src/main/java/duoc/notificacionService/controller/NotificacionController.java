package duoc.notificacionService.controller;

import duoc.notificacionService.entity.Notificacion;
import duoc.notificacionService.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService service;

    @GetMapping
    public List<Notificacion> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notificacion> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Notificacion crear(@RequestBody Notificacion notificacion) {
        return service.guardar(notificacion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Notificacion> actualizar(@PathVariable Long id, @RequestBody Notificacion notificacion) {
        if (service.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        notificacion.setId(id);
        return ResponseEntity.ok(service.guardar(notificacion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}