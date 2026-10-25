package br.com.posto.escala.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.DayOfWeek;

@Entity
public class Funcionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private boolean ativo;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private Funcao funcao;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private DayOfWeek folgaFixa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "titular_coberto_id")
    private Funcionario titularCoberto;

    // Null identifies records created before configurable rules were introduced.
    private Boolean regrasInicializadas;

    protected Funcionario() {
    }

    public Funcionario(String nome, boolean ativo, Funcao funcao) {
        this.nome = nome;
        this.ativo = ativo;
        this.funcao = funcao;
        this.regrasInicializadas = true;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Funcao getFuncao() { return funcao == null ? Funcao.FRENTISTA : funcao; }
    public void setFuncao(Funcao funcao) { this.funcao = funcao; }
    public DayOfWeek getFolgaFixa() { return folgaFixa; }
    public void setFolgaFixa(DayOfWeek folgaFixa) { this.folgaFixa = folgaFixa; }
    public Funcionario getTitularCoberto() { return titularCoberto; }
    public void setTitularCoberto(Funcionario titularCoberto) { this.titularCoberto = titularCoberto; }
    public Boolean getRegrasInicializadas() { return regrasInicializadas; }
    public void setRegrasInicializadas(Boolean regrasInicializadas) { this.regrasInicializadas = regrasInicializadas; }
}
