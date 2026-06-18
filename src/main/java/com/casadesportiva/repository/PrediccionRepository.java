package com.casadesportiva.repository;

import com.casadesportiva.model.Prediccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PrediccionRepository extends JpaRepository<Prediccion, Long> {
    Optional<Prediccion> findByPartidoId(Long partidoId);
}