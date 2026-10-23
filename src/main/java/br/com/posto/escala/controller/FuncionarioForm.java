package br.com.posto.escala.controller;

import br.com.posto.escala.domain.Funcao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FuncionarioForm {
    @NotBlank(message = "Informe o nome.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nome;
    private boolean ativo = true;
    @NotNull
    private Funcao funcao = Funcao.FRENTISTA;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Funcao getFuncao() { return funcao; }
    public void setFuncao(Funcao funcao) { this.funcao = funcao; }
}
