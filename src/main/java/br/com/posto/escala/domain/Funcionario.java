package br.com.posto.escala.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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

    protected Funcionario() {
    }

    public Funcionario(String nome, boolean ativo, Funcao funcao) {
        this.nome = nome;
        this.ativo = ativo;
        this.funcao = funcao;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Funcao getFuncao() { return funcao == null ? Funcao.FRENTISTA : funcao; }
    public void setFuncao(Funcao funcao) { this.funcao = funcao; }
}
