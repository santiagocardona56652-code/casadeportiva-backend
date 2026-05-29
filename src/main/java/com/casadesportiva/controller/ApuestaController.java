package com.casadesportiva.controller;

import com.casadesportiva.model.Apuesta;
import com.casadesportiva.service.ApuestaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apuestas")
@CrossOrigin(origins = "*")
public class ApuestaController {

    @Autowired
    private ApuestaService apuestaService;

    @PostMapping
    public ResponseEntity<?> crearApuesta(@RequestBody Map<String, Object> body) {
        try {
            Long usuarioId = Long.valueOf(body.get("usuarioId").toString());
            Long partidoId = Long.valueOf(body.get("partidoId").toString());
            Double monto = Double.valueOf(body.get("monto").toString());
            String prediccion = body.get("prediccion").toString();

            Apuesta apuesta = apuestaService.crearApuesta(usuarioId, partidoId, monto, prediccion);
            return ResponseEntity.ok(apuesta);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Apuesta>> obtenerPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(apuestaService.obtenerApuestasPorUsuario(usuarioId));
    }
}