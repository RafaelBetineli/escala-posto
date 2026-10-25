package br.com.posto.escala;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EscalaApplication {
    public static void main(String[] args) {
        boolean desktop = Boolean.getBoolean("escala.desktop");
        if (desktop && !ModoDesktop.preparar()) return;
        var contexto = SpringApplication.run(EscalaApplication.class, args);
        if (desktop) ModoDesktop.iniciar(contexto);
    }
}
