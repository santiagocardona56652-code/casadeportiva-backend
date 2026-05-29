package com.casadesportiva.controller;

import com.casadesportiva.model.Prediccion;
import com.casadesportiva.service.PrediccionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predicciones")
@CrossOrigin(origins = "*")
public class PrediccionController {

    @Autowired
    private PrediccionService prediccionService;

    @GetMapping("/{partidoId}")
    public ResponseEntity<?> obtenerPrediccion(@PathVariable Long partidoId) {
        try {
            Prediccion prediccion = prediccionService.obtenerPrediccion(partidoId);
            return ResponseEntity.ok(prediccion);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}