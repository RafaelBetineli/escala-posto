package br.com.posto.escala.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"ano", "mes"}))
public class Escala {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int mes;

    @Column(nullable = false)
    private int ano;

    protected Escala() {
    }

    public Escala(int mes, int ano) {
        this.mes = mes;
        this.ano = ano;
    }

    public Long getId() { return id; }
    public int getMes() { return mes; }
    public int getAno() { return ano; }
}
