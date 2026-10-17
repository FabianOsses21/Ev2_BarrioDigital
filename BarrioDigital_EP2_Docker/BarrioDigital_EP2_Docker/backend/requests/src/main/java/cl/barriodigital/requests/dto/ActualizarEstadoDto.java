package cl.barriodigital.requests.dto;

import cl.barriodigital.requests.entity.EstadoTramite;
import jakarta.validation.constraints.NotNull;

public class ActualizarEstadoDto {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoTramite estado;

    public ActualizarEstadoDto() {
    }

    public ActualizarEstadoDto(EstadoTramite estado) {
        this.estado = estado;
    }

    public EstadoTramite getEstado() {
        return estado;
    }

    public void setEstado(EstadoTramite estado) {
        this.estado = estado;
    }
}
