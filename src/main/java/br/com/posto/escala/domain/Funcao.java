package br.com.posto.escala.domain;

public enum Funcao {
    CAIXA("Caixa"), FRENTISTA("Frentista");

    private final String descricao;

    Funcao(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
