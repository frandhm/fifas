package duoc.soporteService.service;

import duoc.soporteService.entity.Mensaje;
import duoc.soporteService.repository.MensajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MensajeService {

    @Autowired
    private MensajeRepository repository;

    public List<Mensaje> listar() {
        return repository.findAll();
    }

    public Optional<Mensaje> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Mensaje guardar(Mensaje mensaje) {
        return repository.save(mensaje);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}