package br.com.posto.escala.view;

import java.time.LocalDate;

public record ResumoAlmocoDia(LocalDate data, String semana, int escalados, int definidos) {
    public int pendentes() {
        return escalados - definidos;
    }
}
