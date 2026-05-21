package com.casadesportiva.repository;

import com.casadesportiva.model.Partido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PartidoRepository extends JpaRepository<Partido, Long> {
    List<Partido> findByEstado(String estado);
    List<Partido> findByCompeticion(String competicion);
}