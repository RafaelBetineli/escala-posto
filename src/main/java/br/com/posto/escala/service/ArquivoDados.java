package br.com.posto.escala.service;

import br.com.posto.escala.domain.Funcao;
import br.com.posto.escala.domain.LocalTrabalho;
import br.com.posto.escala.domain.StatusDia;
import br.com.posto.escala.domain.Turno;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ArquivoDados(String formato, int versao, Instant criadoEm,
                           List<FuncionarioDados> funcionarios, List<EscalaDados> escalas,
                           List<DiaDados> dias) {
    public record FuncionarioDados(Long id, String nome, boolean ativo, Funcao funcao,
                                   DayOfWeek folgaFixa, Long titularCobertoId, Boolean regrasInicializadas) {}

    public record EscalaDados(Long id, int mes, int ano) {}

    public record DiaDados(Long escalaId, Long funcionarioId, LocalDate data,
                           StatusDia status, Turno turno, LocalTrabalho local,
                           LocalTime almocoInicio, LocalTime almocoFim) {}
}
