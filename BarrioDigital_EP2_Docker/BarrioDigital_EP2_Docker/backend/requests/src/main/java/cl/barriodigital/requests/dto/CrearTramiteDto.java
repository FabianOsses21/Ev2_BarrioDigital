package cl.barriodigital.requests.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CrearTramiteDto {

    @NotBlank(message = "El tipoId es obligatorio")
    private String tipoId;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(min = 5, max = 100, message = "El asunto debe tener entre 5 y 100 caracteres")
    private String asunto;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 10, max = 1000, message = "La descripción debe tener entre 10 y 1000 caracteres")
    private String descripcion;

    private String propietarioId;
    private String propietarioNombre;
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Email
    @Size(max = 150)
    private String clienteEmail;

    public CrearTramiteDto() {
    }

    public CrearTramiteDto(String tipoId, String asunto, String descripcion) {
        this.tipoId = tipoId;
        this.asunto = asunto;
        this.descripcion = descripcion;
    }

    public String getTipoId() {
        return tipoId;
    }

    public void setTipoId(String tipoId) {
        this.tipoId = tipoId;
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
}
