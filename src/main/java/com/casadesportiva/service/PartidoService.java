package com.casadesportiva.service;

import com.casadesportiva.model.Partido;
import com.casadesportiva.repository.PartidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@EnableScheduling
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

    // Se ejecuta cada 60 segundos automáticamente
    @Scheduled(fixedDelay = 60000)
    public void actualizarPartidos() {
        cargarPartidosDesdeAPI();
    }

    public void cargarPartidosDesdeAPI() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-rapidapi-key", footballApiKey);
            headers.set("x-rapidapi-host", footballApiHost);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String today = java.time.LocalDate.now().toString();
            String url = "https://free-api-live-football-data.p.rapidapi.com/football-get-matches-by-date?date=" + today;

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map body = response.getBody();
                Map data = (Map) body.get("response");

                if (data != null) {
                    List matches = (List) data.get("matches");
                    if (matches != null && !matches.isEmpty()) {
                        // Limpiar partidos anteriores y cargar nuevos
                        partidoRepository.deleteAll();
                        for (Object match : matches) {
                            Map m = (Map) match;
                            try {
                                Map homeTeam = (Map) m.get("homeTeam");
                                Map awayTeam = (Map) m.get("awayTeam");
                                Map score = (Map) m.get("score");
                                String status = (String) m.get("status");
                                String competition = "";
                                if (m.get("competition") != null) {
                                    Map comp = (Map) m.get("competition");
                                    competition = comp.get("name") != null ? comp.get("name").toString() : "Internacional";
                                }

                                Partido partido = new Partido();
                                partido.setEquipoLocal(homeTeam.get("name").toString());
                                partido.setEquipoVisitante(awayTeam.get("name").toString());
                                partido.setCompeticion(competition);
                                partido.setFecha(LocalDateTime.now());
                                partido.setEstado(mapearEstado(status));

                                // Guardar marcador si existe
                                if (score != null) {
                                    Object golesLocal = score.get("home");
                                    Object golesVisitante = score.get("away");
                                    if (golesLocal != null) partido.setGolesLocal(Integer.parseInt(golesLocal.toString()));
                                    if (golesVisitante != null) partido.setGolesVisitante(Integer.parseInt(golesVisitante.toString()));
                                }

                                partidoRepository.save(partido);
                            } catch (Exception e) {
                                System.out.println("Error parseando partido: " + e.getMessage());
                            }
                        }
                        System.out.println("✅ Partidos actualizados desde API: " + matches.size());
                        return;
                    }
                }
            }
            System.out.println("⚠️ API sin datos hoy, cargando ejemplos...");
            cargarPartidosEjemplo();

        } catch (Exception e) {
            System.out.println("Error API football: " + e.getMessage());
            cargarPartidosEjemplo();
        }
    }

    private String mapearEstado(String status) {
        if (status == null) return "PENDIENTE";
        return switch (status.toUpperCase()) {
            case "LIVE", "IN_PLAY", "HALFTIME", "1H", "2H", "HT" -> "EN_VIVO";
            case "FINISHED", "FULL_TIME", "FT" -> "FINALIZADO";
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