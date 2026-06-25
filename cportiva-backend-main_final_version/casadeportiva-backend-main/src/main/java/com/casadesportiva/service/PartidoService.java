package com.casadesportiva.service;

import com.casadesportiva.model.Partido;
import com.casadesportiva.repository.PartidoRepository;
import com.casadesportiva.repository.PrediccionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@EnableScheduling
public class PartidoService {

    @Autowired
    private PartidoRepository partidoRepository;

    @Autowired
    private PrediccionRepository prediccionRepository;

    // Tu API token de football-data.org
    private static final String API_TOKEN = "94185a231d3f4b448fd702c860505b50";

    // Competiciones disponibles en tu cuenta gratuita
    private static final String[] COMPETICIONES = {
            "WC",   // FIFA World Cup
            "CL",   // UEFA Champions League
            "BL1",  // Bundesliga
            "DED",  // Eredivisie
            "BSA",  // Brasileirao Serie A
            "PD",   // La Liga
            "FL1",  // Ligue 1
            "ELC",  // Championship
            "PPL",  // Primeira Liga
            "EC",   // European Championship
            "SA",   // Serie A
            "PL"    // Premier League
    };

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Actualización automática cada 5 minutos ──────────────────────────────
    @Scheduled(fixedDelay = 300000)
    public void actualizarPartidos() {
        try {
            System.out.println("🔄 Actualizando partidos desde football-data.org...");
            prediccionRepository.deleteAll();
            partidoRepository.deleteAll();

            int total = 0;
            for (String comp : COMPETICIONES) {
                total += cargarCompeticion(comp);
                // Respetar límite de 10 req/min del plan gratuito
                Thread.sleep(6500);
            }

            if (total == 0) {
                System.out.println("⚠ Sin partidos recientes en las APIs — cargando ejemplos");
                cargarPartidosEjemplo();
            } else {
                System.out.println("✅ Partidos actualizados: " + total);
            }
        } catch (Exception e) {
            System.out.println("❌ Error actualizando partidos: " + e.getMessage());
            if (partidoRepository.count() == 0) {
                cargarPartidosEjemplo();
            }
        }
    }

    // ── Carga partidos de una competición (EN_VIVO + PENDIENTES + FINALIZADOS recientes) ─
    private int cargarCompeticion(String codigo) {
        try {
            // Traer partidos en vivo primero
            int enVivo = cargarPorStatus(codigo, "LIVE");
            // Luego programados
            int programados = cargarPorStatus(codigo, "SCHEDULED");
            // Finalizados de hoy
            int finalizados = cargarPorStatus(codigo, "FINISHED");
            return enVivo + programados + finalizados;
        } catch (Exception e) {
            System.out.println("⚠ Competición " + codigo + ": " + e.getMessage());
            return 0;
        }
    }

    private int cargarPorStatus(String codigo, String status) {
        try {
            String url = "https://api.football-data.org/v4/competitions/" + codigo
                    + "/matches?status=" + status;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Auth-Token", API_TOKEN)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                System.out.println("⏳ Rate limit alcanzado, esperando 12s...");
                Thread.sleep(12000);
                return 0;
            }
            if (response.statusCode() != 200) return 0;

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode matches = root.path("matches");
            if (!matches.isArray() || matches.isEmpty()) return 0;

            String nombreCompeticion = root.path("competition").path("name").asText(codigo);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            LocalDateTime ahora = LocalDateTime.now();

            int count = 0;
            for (JsonNode match : matches) {
                try {
                    // Solo partidos de los últimos 2 días hasta 5 días en el futuro
                    String fechaStr = match.path("utcDate").asText("");
                    LocalDateTime fechaPartido;
                    try {
                        fechaPartido = LocalDateTime.parse(fechaStr, fmt);
                    } catch (Exception ex) {
                        fechaPartido = ahora;
                    }

                    if (fechaPartido.isBefore(ahora.minusDays(2)) ||
                            fechaPartido.isAfter(ahora.plusDays(5))) continue;

                    Partido partido = new Partido();
                    partido.setEquipoLocal(
                            match.path("homeTeam").path("name").asText("Local"));
                    partido.setEquipoVisitante(
                            match.path("awayTeam").path("name").asText("Visitante"));
                    partido.setCompeticion(nombreCompeticion);
                    partido.setFecha(fechaPartido);
                    partido.setEstado(mapearEstado(
                            match.path("status").asText("SCHEDULED")));

                    // Marcador — fullTime tiene el resultado final,
                    // score.fullTime también se actualiza en tiempo real durante EN_VIVO
                    JsonNode scoreNode = match.path("score");
                    int golesLocal = 0;
                    int golesVisitante = 0;

                    if (!scoreNode.isMissingNode()) {
                        // Intentar fullTime primero, luego regularTime, luego halfTime
                        int[] marcador = extraerMarcador(scoreNode);
                        golesLocal = marcador[0];
                        golesVisitante = marcador[1];
                    }

                    partido.setGolesLocal(golesLocal);
                    partido.setGolesVisitante(golesVisitante);

                    partidoRepository.save(partido);
                    count++;

                } catch (Exception ex) {
                    System.out.println("⚠ Error parseando partido: " + ex.getMessage());
                }
            }
            return count;

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return 0;
        } catch (Exception e) {
            System.out.println("⚠ Error " + codigo + "/" + status + ": " + e.getMessage());
            return 0;
        }
    }

    // Extrae el marcador más actualizado disponible
    private int[] extraerMarcador(JsonNode scoreNode) {
        // Orden de prioridad: fullTime > regularTime > halfTime
        String[] campos = {"fullTime", "regularTime", "halfTime"};
        for (String campo : campos) {
            JsonNode node = scoreNode.path(campo);
            if (!node.isMissingNode() && !node.isNull()
                    && !node.path("home").isNull()
                    && !node.path("away").isNull()) {
                return new int[]{
                        node.path("home").asInt(0),
                        node.path("away").asInt(0)
                };
            }
        }
        return new int[]{0, 0};
    }

    // Mapea los estados de la API al formato de CasaDeportiva
    private String mapearEstado(String status) {
        return switch (status) {
            case "IN_PLAY", "PAUSED", "HALFTIME", "EXTRA_TIME", "PENALTY_SHOOTOUT"
                    -> "EN_VIVO";
            case "FINISHED", "AWARDED"
                    -> "FINALIZADO";
            default // SCHEDULED, TIMED, POSTPONED, CANCELLED, SUSPENDED
                    -> "PENDIENTE";
        };
    }

    // ── Métodos públicos usados por el Controller ─────────────────────────────
    public List<Partido> obtenerTodos() {
        if (partidoRepository.count() == 0) actualizarPartidos();
        return partidoRepository.findAll();
    }

    public List<Partido> obtenerPorEstado(String estado) {
        return partidoRepository.findByEstado(estado.toUpperCase());
    }

    public void cargarPartidosDesdeAPI() {
        if (partidoRepository.count() == 0) actualizarPartidos();
    }

    // ── Fallback: partidos de ejemplo si la API no devuelve datos ─────────────
    private void cargarPartidosEjemplo() {
        if (partidoRepository.count() > 0) return;
        Object[][] ejemplos = {
                {"Real Madrid",   "Manchester City", "PENDIENTE",   "UEFA Champions League"},
                {"Barcelona",     "PSG",             "PENDIENTE",   "UEFA Champions League"},
                {"Bayern Munich", "Arsenal",         "PENDIENTE",   "UEFA Champions League"},
                {"Atlético Madrid","Juventus",       "PENDIENTE",   "UEFA Champions League"},
                {"Liverpool",     "Inter Milan",     "FINALIZADO",  "UEFA Champions League"},
                {"Chelsea",       "Dortmund",        "FINALIZADO",  "UEFA Champions League"},
                {"Colombia",      "Brasil",          "PENDIENTE",   "Copa América"},
                {"Argentina",     "Uruguay",         "PENDIENTE",   "Copa América"},
        };
        for (Object[] e : ejemplos) {
            Partido p = new Partido();
            p.setEquipoLocal((String) e[0]);
            p.setEquipoVisitante((String) e[1]);
            p.setEstado((String) e[2]);
            p.setCompeticion((String) e[3]);
            p.setFecha(LocalDateTime.now().plusHours(2));
            p.setGolesLocal(0);
            p.setGolesVisitante(0);
            partidoRepository.save(p);
        }
 dir       System.out.println("✅ Partidos de ejemplo cargados como fallback");
    }
}