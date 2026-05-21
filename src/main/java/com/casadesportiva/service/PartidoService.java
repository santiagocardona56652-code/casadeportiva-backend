package com.casadesportiva.service;

import com.casadesportiva.model.Partido;
import com.casadesportiva.repository.PartidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PartidoService {

    @Autowired
    private PartidoRepository partidoRepository;

    public List<Partido> obtenerTodos() {
        return partidoRepository.findAll();
    }

    public List<Partido> obtenerPorEstado(String estado) {
        return partidoRepository.findByEstado(estado);
    }

    public void cargarPartidosDesdeAPI() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.football-data.org/v4/matches";
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Auth-Token", "demo");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            // Partidos de ejemplo mientras conseguimos la API key
            cargarPartidosEjemplo();

        } catch (Exception e) {
            cargarPartidosEjemplo();
        }
    }

    private void cargarPartidosEjemplo() {
        if (partidoRepository.count() == 0) {
            String[][] partidos = {
                {"Colombia", "Brasil", "PENDIENTE", "Copa América"},
                {"Argentina", "Uruguay", "PENDIENTE", "Copa América"},
                {"Francia", "España", "EN_VIVO", "UEFA Nations League"},
                {"Alemania", "Italia", "PENDIENTE", "UEFA Nations League"},
                {"México", "Estados Unidos", "FINALIZADO", "CONCACAF"},
                {"Portugal", "Croacia", "PENDIENTE", "UEFA Nations League"},
                {"Inglaterra", "Países Bajos", "EN_VIVO", "UEFA Nations League"},
                {"Chile", "Ecuador", "PENDIENTE", "Eliminatorias"}
            };

            for (String[] p : partidos) {
                Partido partido = new Partido();
                partido.setEquipoLocal(p[0]);
                partido.setEquipoVisitante(p[1]);
                partido.setEstado(p[2]);
                partido.setCompeticion(p[3]);
                partido.setFecha(LocalDateTime.now().plusDays((long)(Math.random() * 7)));
                partidoRepository.save(partido);
            }
        }
    }
}
