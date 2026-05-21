package com.casadesportiva.controller;

import com.casadesportiva.model.Partido;
import com.casadesportiva.service.PartidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/partidos")
@CrossOrigin(origins = "*")
public class PartidoController {

    @Autowired
    private PartidoService partidoService;

    @GetMapping
    public ResponseEntity<List<Partido>> obtenerTodos() {
        partidoService.cargarPartidosDesdeAPI();
        return ResponseEntity.ok(partidoService.obtenerTodos());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Partido>> obtenerPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(partidoService.obtenerPorEstado(estado));
    }
}