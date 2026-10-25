package br.com.posto.escala.view;

import br.com.posto.escala.domain.Escala;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

public record VisualizacaoEscala(Escala escala, List<DiaCabecalho> dias, List<LinhaEscala> linhas,
                                 List<String> avisos, Map<LocalDate, ResumoDia> resumos) {
}
