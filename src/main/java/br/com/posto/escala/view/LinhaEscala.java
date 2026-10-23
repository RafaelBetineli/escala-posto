package br.com.posto.escala.view;

import br.com.posto.escala.domain.DiaEscala;
import br.com.posto.escala.domain.Funcao;
import java.util.List;

public record LinhaEscala(Long funcionarioId, String nome, Funcao funcao, List<DiaEscala> dias) {
}
