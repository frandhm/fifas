package duoc.carritoService.repository;

import duoc.carritoService.entity.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    java.util.List<Carrito> findByPropietarioOrderByIdAsc(String propietario);
    java.util.Optional<Carrito> findByIdAndPropietario(Long id, String propietario);
    @Query("""
        select c from Carrito c where c.propietario = :owner and c.productoId = :product
        and ((c.talla is null and :size is null) or c.talla = :size)
        and ((c.nombreJugador is null and :name is null) or c.nombreJugador = :name)
        and ((c.dorsal is null and :number is null) or c.dorsal = :number)
        """)
    java.util.Optional<Carrito> findLine(@Param("owner") String owner,
        @Param("product") String product, @Param("size") String size,
        @Param("name") String name, @Param("number") Integer number);
    @Transactional void deleteByPropietario(String propietario);
}
