package duoc.solicitudService.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;
    private Long productoId;
    private String equipo;
    private Integer anio;
    private String nombreEstampado;
    private Integer numeroEstampado;
    private String talla;
    private String estado; // ej: PENDIENTE, EN_PROCESO, LISTO

    public Solicitud() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public String getEquipo() { return equipo; }
    public void setEquipo(String equipo) { this.equipo = equipo; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public String getNombreEstampado() { return nombreEstampado; }
    public void setNombreEstampado(String nombreEstampado) { this.nombreEstampado = nombreEstampado; }

    public Integer getNumeroEstampado() { return numeroEstampado; }
    public void setNumeroEstampado(Integer numeroEstampado) { this.numeroEstampado = numeroEstampado; }

    public String getTalla() { return talla; }
    public void setTalla(String talla) { this.talla = talla; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}