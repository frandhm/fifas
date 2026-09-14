package duoc.solicitudService.service;

import duoc.solicitudService.entity.Solicitud;
import duoc.solicitudService.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SolicitudService {

    @Autowired
    private SolicitudRepository repository;

    public List<Solicitud> listar() {
        return repository.findAll();
    }

    public Optional<Solicitud> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Solicitud guardar(Solicitud solicitud) {
        if (solicitud.getNumeroEstampado() != null &&
            (solicitud.getNumeroEstampado() < 0 || solicitud.getNumeroEstampado() > 99)) {
            throw new IllegalArgumentException("El numero debe estar entre 0 y 99");
        }
        if (solicitud.getEstado() == null) {
            solicitud.setEstado("PENDIENTE");
        }
        return repository.save(solicitud);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}