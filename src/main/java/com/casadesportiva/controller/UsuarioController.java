package com.casadesportiva.controller;

import com.casadesportiva.model.Usuario;
import com.casadesportiva.security.JwtService;
import com.casadesportiva.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody Usuario usuario) {
        try {
            Usuario nuevo = usuarioService.registrar(usuario);
            // Generar token al registrarse también
            String token = jwtService.generarToken(nuevo.getEmail(), nuevo.getId());
            return ResponseEntity.ok(buildLoginResponse(nuevo, token));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String password = body.get("password");

            Usuario usuario = usuarioService.buscarPorEmail(email);

            if (!usuarioService.verificarPassword(password, usuario.getPassword())) {
                return ResponseEntity.badRequest().body("Contraseña incorrecta");
            }

            String token = jwtService.generarToken(usuario.getEmail(), usuario.getId());
            return ResponseEntity.ok(buildLoginResponse(usuario, token));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private Map<String, Object> buildLoginResponse(Usuario usuario, String token) {
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("usuarioId", usuario.getId());
        response.put("nombre", usuario.getNombre());
        response.put("email", usuario.getEmail());
        response.put("saldo", usuario.getSaldo());
        return response;
    }
}
