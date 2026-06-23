package com.casadesportiva.service;

import com.casadesportiva.model.Partido;
import com.casadesportiva.model.Prediccion;
import com.casadesportiva.repository.PartidoRepository;
import com.casadesportiva.repository.PrediccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${groq.api.key}")
    private String groqApiKey;

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

        String texto = llamarGroq(partido);

        Prediccion prediccion = new Prediccion();
        prediccion.setPartido(partido);
        prediccion.setTexto(texto);
        prediccion.setGeneradaEn(LocalDateTime.now());

        return prediccionRepository.save(prediccion);
    }

    private String llamarGroq(Partido partido) {
        System.out.println("Llamando a Groq...");

        try {
            String url = "https://api.groq.com/openai/v1/chat/completions";

            String prompt = String.format(
                "Eres un analista experto en futbol. Analiza este partido: %s vs %s. Competicion: %s. Fecha: %s. " +
                "Dame una PREDICCION DETALLADA con lo siguiente: " +
                "1. RESULTADO FINAL PROBABLE: como terminaria el partido y quien ganaria. " +
                "2. MARCADOR EXACTO: el marcador mas probable al final del partido. " +
                "3. PROBABILIDADES: porcentaje de victoria local, empate y victoria visitante. " +
                "4. JUGADORES CLAVE: quienes pueden marcar la diferencia o anotar. " +
                "5. NIVEL DE CONFIANZA: que tan segura es esta prediccion. " +
                "Responde en TEXTO PLANO sin markdown (sin #, **, *, guiones). " +
                "Usa MAYUSCULAS para los titulos de cada seccion, emojis para hacerlo atractivo y saltos de linea entre secciones.",
                partido.getEquipoLocal(),
                partido.getEquipoVisitante(),
                partido.getCompeticion(),
                partido.getFecha()
            );

            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Eres un analista experto en futbol que da predicciones detalladas en español sobre como terminaran los partidos.");

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("messages", List.of(systemMessage, userMessage));
            requestBody.put("max_tokens", 1024);
            requestBody.put("temperature", 0.7);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            System.out.println("Status Groq: " + response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List choices = (List) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map choice = (Map) choices.get(0);
                    Map message = (Map) choice.get("message");
                    return (String) message.get("content");
                }
            }
            return generarPrediccionLocal(partido);

        } catch (Exception e) {
            System.out.println("========== ERROR GROQ ==========");
            e.printStackTrace();
            System.out.println("================================");
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
