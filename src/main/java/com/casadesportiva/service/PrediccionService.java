package com.casadesportiva.service;

import com.casadesportiva.model.Partido;
import com.casadesportiva.model.Prediccion;
import com.casadesportiva.repository.PartidoRepository;
import com.casadesportiva.repository.PrediccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PrediccionService {

    @Autowired
    private PrediccionRepository prediccionRepository;

    @Autowired
    private PartidoRepository partidoRepository;

    private String geminiApiKey = "AIzaSyCpUK1SDKEN8cLgyXvRwrjHghmBpkx6K8g";

    public Prediccion obtenerPrediccion(Long partidoId) {
        Optional<Prediccion> cached = prediccionRepository.findByPartidoId(partidoId);
        if (cached.isPresent()) {
            Prediccion p = cached.get();
            if (p.getGeneradaEn().isAfter(LocalDateTime.now().minusHours(24))) {
                return p;
            }
            prediccionRepository.delete(p);
        }

        Partido partido = partidoRepository.findById(partidoId)
            .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        String texto = llamarGemini(partido);

        Prediccion prediccion = new Prediccion();
        prediccion.setPartido(partido);
        prediccion.setTexto(texto);
        prediccion.setGeneradaEn(LocalDateTime.now());

        return prediccionRepository.save(prediccion);
    }

    private String llamarGemini(Partido partido) {
        System.out.println("KEY USADA: " + geminiApiKey);

        try {
            String url ="https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key="+ geminiApiKey;

            String prompt = String.format(
                "Eres un analista experto en futbol. Analiza este partido: %s vs %s. Competicion: %s. Fecha: %s. " +
                "Proporciona prediccion del resultado, marcador probable, probabilidades y nivel de confianza en espanol. " +
                "IMPORTANTE: Responde en TEXTO PLANO sin usar markdown (no uses #, **, *, guiones para listas). " +
                "Usa mayusculas para titulos y saltos de linea simples para organizar las secciones." + "Tambien coloca en Negrilla los titulos y emoji para que sea mas atractivo." + "Muestrame tambien el sigo de porcentaje.",
            partido.getEquipoLocal(),
            partido.getEquipoVisitante(),
            partido.getCompeticion(),
            partido.getFecha()
            );

            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            System.out.println("Status Gemini: " + response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List candidates = (List) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map contentResp = (Map) candidate.get("content");
                    List parts = (List) contentResp.get("parts");
                    Map firstPart = (Map) parts.get(0);
                    return (String) firstPart.get("text");
                }
            }
            return generarPrediccionLocal(partido);

        } catch (Exception e) {
    System.out.println("========== ERROR GEMINI ==========");
    e.printStackTrace();
    System.out.println("==================================");

    return generarPrediccionLocal(partido);
         }
    }

    private String generarPrediccionLocal(Partido partido) {
        String local = partido.getEquipoLocal();
        String visitante = partido.getEquipoVisitante();
        int pLocal = 35 + (local.length() % 25);
        int pVisitante = 20 + (visitante.length() % 20);
        int pEmpate = 100 - pLocal - pVisitante;
        String ganador = pLocal > pVisitante ? local : visitante;
        int golesLocal = 1 + (local.length() % 2);
        int golesVisitante = pEmpate > 25 ? golesLocal : Math.max(0, golesLocal - 1);

        return String.format(
            "ANALISIS: %s vs %s - Competicion: %s\n1. Victoria de %s\n2. Marcador: %d-%d\n3. Local %d%% Empate %d%% Visitante %d%%\n4. Ventaja de local\n5. Confianza: Medio",
            local, visitante, partido.getCompeticion(),
            ganador, golesLocal, golesVisitante,
            pLocal, pEmpate, pVisitante
        );
    }
}
