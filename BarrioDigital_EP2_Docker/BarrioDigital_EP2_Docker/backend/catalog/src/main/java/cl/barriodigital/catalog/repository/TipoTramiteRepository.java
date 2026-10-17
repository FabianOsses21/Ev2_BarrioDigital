package cl.barriodigital.catalog.repository;

import cl.barriodigital.catalog.entity.TipoTramite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoTramiteRepository extends JpaRepository<TipoTramite, String> {

    List<TipoTramite> findByActivoTrue();

    Optional<TipoTramite> findByNombreIgnoreCase(String nombre);
}
