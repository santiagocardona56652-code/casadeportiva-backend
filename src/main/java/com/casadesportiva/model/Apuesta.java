package com.casadesportiva.model;

import jakarta.persistence.*;

@Entity
@Table(name = "apuestas")
public class Apuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "partido_id")
    private Partido partido;

    @Column(nullable = false)
    private Double monto;

    @Column(nullable = false)
    private String prediccion; // LOCAL, EMPATE, VISITANTE

    @Column
    private String estado = "PENDIENTE";

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Partido getPartido() { return partido; }
    public void setPartido(Partido partido) { this.partido = partido; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public String getPrediccion() { return prediccion; }
    public void setPrediccion(String prediccion) { this.prediccion = prediccion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}