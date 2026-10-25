package br.com.posto.escala.view;

import br.com.posto.escala.domain.Escala;
import br.com.posto.escala.domain.Funcionario;
import java.util.List;

public record VisualizacaoAlmocoMes(Escala escala, List<Funcionario> equipe,
                                    List<ResumoAlmocoDia> dias, int escalados, int definidos) {
    public int pendentes() {
        return escalados - definidos;
    }
}
