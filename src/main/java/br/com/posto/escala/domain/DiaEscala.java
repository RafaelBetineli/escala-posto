package br.com.posto.escala.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"escala_id", "funcionario_id", "data"}))
public class DiaEscala {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escala_id", nullable = false)
    private Escala escala;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private StatusDia status;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Turno turno;

    @Enumerated(EnumType.STRING)
    @Column(name = "local_trabalho", length = 10)
    private LocalTrabalho local;

    private LocalTime almocoInicio;

    private LocalTime almocoFim;

    protected DiaEscala() {
    }

    public DiaEscala(Funcionario funcionario, Escala escala, LocalDate data) {
        this.funcionario = funcionario;
        this.escala = escala;
        this.data = data;
    }

    public Funcionario getFuncionario() { return funcionario; }
    public Long getId() { return id; }
    public LocalDate getData() { return data; }
    public StatusDia getStatus() { return status; }
    public Turno getTurno() { return turno; }
    public LocalTrabalho getLocal() { return local; }
    public LocalTime getAlmocoInicio() { return almocoInicio; }
    public LocalTime getAlmocoFim() { return almocoFim; }

    public Marcacao getMarcacao() {
        if (status == null) return Marcacao.VAZIO;
        if (status == StatusDia.F) return Marcacao.FOLGA;
        if (status == StatusDia.V) return Marcacao.FERIAS;
        if (turno == null || local == null) return Marcacao.VAZIO;
        for (Marcacao opcao : Marcacao.values()) {
            if (opcao.getStatus() == status && opcao.getTurno() == turno && opcao.getLocal() == local) return opcao;
        }
        return Marcacao.VAZIO;
    }

    public void aplicar(Marcacao marcacao) {
        if (marcacao.getStatus() != StatusDia.T || turno != marcacao.getTurno()) {
            this.almocoInicio = null;
            this.almocoFim = null;
        }
        this.status = marcacao.getStatus();
        this.turno = marcacao.getTurno();
        this.local = marcacao.getLocal();
    }

    public void definirAlmoco(LocalTime inicio, LocalTime fim) {
        if (status != StatusDia.T) throw new IllegalStateException("Almoço só pode ser registrado em dia trabalhado.");
        this.almocoInicio = inicio;
        this.almocoFim = fim;
    }
}
