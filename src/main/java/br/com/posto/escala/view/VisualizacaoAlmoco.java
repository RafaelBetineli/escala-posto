package br.com.posto.escala.view;

import br.com.posto.escala.domain.DiaEscala;
import br.com.posto.escala.domain.Escala;
import java.time.LocalDate;
import java.util.List;

public record VisualizacaoAlmoco(Escala escala, LocalDate data, List<DiaEscala> funcionarios) {
}
