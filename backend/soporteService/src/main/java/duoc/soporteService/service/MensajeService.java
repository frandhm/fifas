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

    public List<Mensaje> listar(String propietario) {
        return repository.findByPropietarioOrderByIdDesc(propietario);
    }

    public Optional<Mensaje> buscarPorId(Long id, String propietario) {
        return repository.findByIdAndPropietario(id, propietario);
    }

    public Mensaje guardar(Mensaje mensaje) {
        return repository.save(mensaje);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
