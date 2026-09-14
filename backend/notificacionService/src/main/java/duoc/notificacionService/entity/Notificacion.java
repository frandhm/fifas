package duoc.notificacionService.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String propietario;
    private String tipo;
    private java.time.Instant fecha;
    public String getPropietario() { return propietario; }
    public void setPropietario(String value) { propietario = value; }
    public String getTipo() { return tipo; }
    public void setTipo(String value) { tipo = value; }
    public java.time.Instant getFecha() { return fecha; }
    public void setFecha(java.time.Instant value) { fecha = value; }
    private String mensaje;
    private Boolean leido;

    public Notificacion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public Boolean getLeido() { return leido; }
    public void setLeido(Boolean leido) { this.leido = leido; }
}