package cl.barriodigital.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipos_tramite")
public class TipoTramite {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Integer cupoDiario;

    @Column(nullable = false)
    private Boolean activo = true;

    public TipoTramite() {
    }

    public TipoTramite(String id, String nombre, String descripcion, Integer cupoDiario, Boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.cupoDiario = cupoDiario;
        this.activo = activo != null ? activo : true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCupoDiario() {
        return cupoDiario;
    }

    public void setCupoDiario(Integer cupoDiario) {
        this.cupoDiario = cupoDiario;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
