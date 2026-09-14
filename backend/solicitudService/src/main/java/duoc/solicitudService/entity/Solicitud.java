package duoc.solicitudService.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String propietario;
    private String productoId;
    private String productoNombre;
    private Integer cantidad;
    private java.math.BigDecimal precioUnitario;
    private java.time.Instant fechaCreacion;
    private String equipo;
    private Integer anio;
    private String nombreEstampado;
    private Integer numeroEstampado;
    private String talla;
    private String estado; // ej: PENDIENTE, EN_PROCESO, LISTO

    public Solicitud() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPropietario() { return propietario; }
    public void setPropietario(String propietario) { this.propietario = propietario; }
    public String getProductoId() { return productoId; }
    public void setProductoId(String productoId) { this.productoId = productoId; }
    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public java.math.BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(java.math.BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public java.time.Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(java.time.Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }

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
