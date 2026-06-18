package com.casadesportiva.repository;

import com.casadesportiva.model.Apuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApuestaRepository extends JpaRepository<Apuesta, Long> {
    List<Apuesta> findByUsuarioId(Long usuarioId);
    List<Apuesta> findByPartidoId(Long partidoId);
}
