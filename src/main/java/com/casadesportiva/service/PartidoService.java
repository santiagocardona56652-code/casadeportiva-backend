package com.casadesportiva.service;

import com.casadesportiva.model.Partido;
import com.casadesportiva.repository.PartidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PartidoService {

    @Autowired
    private PartidoRepository partidoRepository;

    @Value("${football.api.key}")
    private String footballApiKey;

    @Value("${football.api.host}")
    private String footballApiHost;

    public List<Partido> obtenerTodos() {
        return partidoRepository.findAll();
    }

    public List<Partido> obtenerPorEstado(String estado) {
        return partidoRepository.findByEstado(estado);
    }

    public void cargarPartidosDesdeAPI() {
        try {
            if (partidoRepository.count() > 0) return;

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-rapidapi-key", footballApiKey);
            headers.set("x-rapidapi-host", footballApiHost);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = "https://free-api-live-football-data.p.rapidapi.com/football-get-matches-by-date?date=2026-05-22";

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map body = response.getBody();
                Map data = (Map) body.get("response");

                if (data != null) {
                    List matches = (List) data.get("matches");
                    if (matches != null && !matches.isEmpty()) {
                        for (Object match : matches) {
                            Map m = (Map) match;
                            try {
                                Map homeTeam = (Map) m.get("homeTeam");
                                Map awayTeam = (Map) m.get("awayTeam");
                                String status = (String) m.get("status");

                                Partido partido = new Partido();
                                partido.setEquipoLocal(homeTeam.get("name").toString());
                                partido.setEquipoVisitante(awayTeam.get("name").toString());
                                partido.setCompeticion("FIFA / Partido Real");
                                partido.setFecha(LocalDateTime.now().plusDays(1));
                                partido.setEstado(mapearEstado(status));

                                partidoRepository.save(partido);
                            } catch (Exception e) {
                                System.out.println("Error parseando partido: " + e.getMessage());
                            }
                        }
                        return;
                    }
                }
            }
            cargarPartidosEjemplo();

        } catch (Exception e) {
            System.out.println("Error API football: " + e.getMessage());
            cargarPartidosEjemplo();
        }
    }

    private String mapearEstado(String status) {
        if (status == null) return "PENDIENTE";
        return switch (status.toUpperCase()) {
            case "LIVE", "IN_PLAY", "HALFTIME" -> "EN_VIVO";
            case "FINISHED", "FULL_TIME" -> "FINALIZADO";
            default -> "PENDIENTE";
        };
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