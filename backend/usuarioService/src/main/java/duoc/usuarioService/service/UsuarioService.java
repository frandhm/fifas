package duoc.usuarioService.service;

import duoc.usuarioService.entity.Usuario;
import duoc.usuarioService.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    public List<Usuario> listar() {
        return repository.findAll();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Usuario guardar(Usuario usuario) {
        return repository.save(usuario);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}