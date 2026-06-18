package com.casadesportiva.service;

import com.casadesportiva.model.Apuesta;
import com.casadesportiva.model.Partido;
import com.casadesportiva.model.Usuario;
import com.casadesportiva.repository.ApuestaRepository;
import com.casadesportiva.repository.PartidoRepository;
import com.casadesportiva.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ApuestaService {

    @Autowired
    private ApuestaRepository apuestaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PartidoRepository partidoRepository;

    public Apuesta crearApuesta(Long usuarioId, Long partidoId, Double monto, String prediccion) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Partido partido = partidoRepository.findById(partidoId)
            .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        if (!partido.getEstado().equals("PENDIENTE")) {
            throw new RuntimeException("No se puede apostar en este partido");
        }

        if (usuario.getSaldo() < monto) {
            throw new RuntimeException("Saldo insuficiente");
        }

        if (monto <= 0) {
            throw new RuntimeException("El monto debe ser mayor a 0");
        }

        usuario.setSaldo(usuario.getSaldo() - monto);
        usuarioRepository.save(usuario);

        Apuesta apuesta = new Apuesta();
        apuesta.setUsuario(usuario);
        apuesta.setPartido(partido);
        apuesta.setMonto(monto);
        apuesta.setPrediccion(prediccion);
        apuesta.setEstado("PENDIENTE");

        return apuestaRepository.save(apuesta);
    }

    public List<Apuesta> obtenerApuestasPorUsuario(Long usuarioId) {
        return apuestaRepository.findByUsuarioId(usuarioId);
    }
}