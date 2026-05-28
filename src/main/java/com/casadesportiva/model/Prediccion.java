package com.casadesportiva.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "predicciones")
public class Prediccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "partido_id")
    private Partido partido;

    @Column(columnDefinition = "TEXT")
    private String texto;

    @Column
    private LocalDateTime generadaEn;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Partido getPartido() { return partido; }
    public void setPartido(Partido partido) { this.partido = partido; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public LocalDateTime getGeneradaEn() { return generadaEn; }
    public void setGeneradaEn(LocalDateTime generadaEn) { this.generadaEn = generadaEn; }
}