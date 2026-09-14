package duoc.carritoService.service;

import duoc.carritoService.entity.Carrito;
import duoc.carritoService.repository.CarritoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CarritoService {

    @Autowired
    private CarritoRepository repository;

    public List<Carrito> listar() {
        return repository.findAll();
    }

    public Optional<Carrito> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Carrito guardar(Carrito carrito) {
        return repository.save(carrito);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}