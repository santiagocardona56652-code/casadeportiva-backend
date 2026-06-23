package com.casadesportiva.service;

import com.casadesportiva.model.Partido;
import com.casadesportiva.repository.ApuestaRepository;
import com.casadesportiva.repository.PartidoRepository;
import com.casadesportiva.repository.PrediccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private PrediccionRepository prediccionRepository;

    @Autowired
    private ApuestaRepository apuestaRepository;

    public List<Partido> obtenerTodos() {
        return partidoRepository.findAll();
    }

    public List<Partido> obtenerPorEstado(String estado) {
        return partidoRepository.findByEstado(estado);
    }

    @Scheduled(fixedDelay = 21600000)
    public void actualizarPartidos() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            apuestaRepository.deleteAll();
            prediccionRepository.deleteAll();
            partidoRepository.deleteAll();

            String[] fechas = {
                java.time.LocalDate.now().toString(),
                java.time.LocalDate.now().plusDays(1).toString(),
                java.time.LocalDate.now().plusDays(2).toString(),
                java.time.LocalDate.now().plusDays(3).toString()
            };

            int totalPartidos = 0;

            for (String fecha : fechas) {
                String url = "https://www.thesportsdb.com/api/v1/json/3/eventsday.php?d=" + fecha + "&s=Soccer";
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    List matches = (List) response.getBody().get("events");

                    if (matches != null && !matches.isEmpty()) {
                        for (Object match : matches) {
                            Map m = (Map) match;
                            try {
                                Partido partido = new Partido();
                                partido.setEquipoLocal(m.get("strHomeTeam").toString());
                                partido.setEquipoVisitante(m.get("strAwayTeam").toString());
                                partido.setCompeticion(m.get("strLeague") != null
                                    ? m.get("strLeague").toString() : "Internacional");

                                String fechaStr = m.get("dateEvent") != null ? m.get("dateEvent").toString() : fecha;
                                String horaStr = m.get("strTime") != null ? m.get("strTime").toString() : "00:00:00";
                                try {
                                    String horaLimpia = horaStr.length() >= 8 ? horaStr.substring(0, 8) : horaStr + ":00";
                                    partido.setFecha(LocalDateTime.parse(fechaStr + "T" + horaLimpia));
                                } catch (Exception ex) {
                                    partido.setFecha(LocalDateTime.now());
                                }

                                String strProgress = m.get("strProgress") != null ? m.get("strProgress").toString() : "";
                                String strStatus = m.get("strStatus") != null ? m.get("strStatus").toString() : "";

                                if (strStatus.equals("Match Finished") || strStatus.equals("FT") || strStatus.equals("AET")) {
                                    partido.setGolesLocal(m.get("intHomeScore") != null ?
                                        Integer.parseInt(m.get("intHomeScore").toString()) : 0);
                                    partido.setGolesVisitante(m.get("intAwayScore") != null ?
                                        Integer.parseInt(m.get("intAwayScore").toString()) : 0);
                                    partido.setEstado("FINALIZADO");

                                } else if (!strProgress.isEmpty() || strStatus.equals("1H") || strStatus.equals("2H")
                                        || strStatus.equals("HT") || strStatus.equals("Live")
                                        || strStatus.equals("In Progress")) {
                                    partido.setGolesLocal(m.get("intHomeScore") != null ?
                                        Integer.parseInt(m.get("intHomeScore").toString()) : 0);
                                    partido.setGolesVisitante(m.get("intAwayScore") != null ?
                                        Integer.parseInt(m.get("intAwayScore").toString()) : 0);
                                    partido.setEstado("EN_VIVO");

                                } else {
                                    partido.setGolesLocal(0);
                                    partido.setGolesVisitante(0);
                                    partido.setEstado("PENDIENTE");
                                }

                                partidoRepository.save(partido);
                                totalPartidos++;
                            } catch (Exception e) {
                                System.out.println("Error parseando partido: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            if (totalPartidos > 0) {
                System.out.println("✅ Partidos actualizados: " + totalPartidos);
                return;
            }

            cargarPartidosEjemplo();

        } catch (Exception e) {
            System.out.println("Error TheSportsDB: " + e.getMessage());
            cargarPartidosEjemplo();
        }
    }

    public void cargarPartidosDesdeAPI() {
        if (partidoRepository.count() > 0) return;
        actualizarPartidos();
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
                partido.setGolesLocal(0);
                partido.setGolesVisitante(0);
                partidoRepository.save(partido);
            }
        }
    }
}