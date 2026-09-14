package duoc.soporteService.repository;

import duoc.soporteService.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {
    java.util.List<Mensaje> findByPropietarioOrderByIdDesc(String propietario);
    java.util.Optional<Mensaje> findByIdAndPropietario(Long id, String propietario);
}
