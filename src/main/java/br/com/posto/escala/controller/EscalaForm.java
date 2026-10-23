package br.com.posto.escala.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class EscalaForm {
    @NotNull(message = "Escolha o mês.")
    @Min(1) @Max(12)
    private Integer mes = LocalDate.now().getMonthValue();

    @NotNull(message = "Informe o ano.")
    @Min(2000) @Max(2100)
    private Integer ano = LocalDate.now().getYear();

    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }
}
