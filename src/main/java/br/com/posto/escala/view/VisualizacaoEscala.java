package br.com.posto.escala.view;

import br.com.posto.escala.domain.Escala;
import java.util.List;

public record VisualizacaoEscala(Escala escala, List<DiaCabecalho> dias, List<LinhaEscala> linhas, List<String> avisos) {
}
