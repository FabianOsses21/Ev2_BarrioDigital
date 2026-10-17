package cl.barriodigital.requests.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tramites_pedidos")
public class Tramite {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tipoId;

    @Column(nullable = false, length = 120)
    private String tipoNombre;

    @Column(nullable = false, length = 150)
    private String asunto;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(nullable = false, length = 64)
    private String propietarioId;

    @Column(nullable = false, length = 150)
    private String propietarioNombre;

    @Column(length = 150)
    private String clienteEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoTramite estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    private LocalDateTime fechaAdmision;

    @Version
    private Long version;

    @OneToMany(mappedBy = "tramite", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<ItemPedido> items = new ArrayList<>();

    public Tramite() {
    }

    public Tramite(String id, String tipoId, String tipoNombre, String asunto, String descripcion,
                   String propietarioId, String propietarioNombre, EstadoTramite estado) {
        this.id = id;
        this.tipoId = tipoId;
        this.tipoNombre = tipoNombre;
        this.asunto = asunto;
        this.descripcion = descripcion;
        this.propietarioId = propietarioId;
        this.propietarioNombre = propietarioNombre;
        this.estado = estado != null ? estado : EstadoTramite.INGRESADO;
        LocalDateTime now = LocalDateTime.now();
        this.fechaCreacion = now;
        this.fechaActualizacion = now;
    }

    public void addItem(ItemPedido item) {
        items.add(item);
        item.setTramite(this);
    }

    public void removeItem(ItemPedido item) {
        items.remove(item);
        item.setTramite(null);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTipoId() {
        return tipoId;
    }

    public void setTipoId(String tipoId) {
        this.tipoId = tipoId;
    }

    public String getTipoNombre() {
        return tipoNombre;
    }

    public void setTipoNombre(String tipoNombre) {
        this.tipoNombre = tipoNombre;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPropietarioId() {
        return propietarioId;
    }

    public void setPropietarioId(String propietarioId) {
        this.propietarioId = propietarioId;
    }

    public String getPropietarioNombre() {
        return propietarioNombre;
    }

    public void setPropietarioNombre(String propietarioNombre) {
        this.propietarioNombre = propietarioNombre;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }

    public EstadoTramite getEstado() {
        return estado;
    }

    public void setEstado(EstadoTramite estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public LocalDateTime getFechaAdmision() {
        return fechaAdmision;
    }

    public void setFechaAdmision(LocalDateTime fechaAdmision) {
        this.fechaAdmision = fechaAdmision;
    }

    public List<ItemPedido> getItems() {
        return items;
    }

    public void setItems(List<ItemPedido> items) {
        this.items = items;
    }
}
