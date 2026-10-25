package br.com.posto.escala.service;

import br.com.posto.escala.domain.Funcao;
import br.com.posto.escala.domain.Funcionario;
import br.com.posto.escala.repository.FuncionarioRepository;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FuncionariosIniciais implements ApplicationRunner {
    private record Cadastro(String nome, Funcao funcao) {}

    private static final List<Cadastro> CADASTROS = List.of(
            new Cadastro("Gustavo", Funcao.CAIXA),
            new Cadastro("Franciele", Funcao.CAIXA),
            new Cadastro("Marcos Paulo", Funcao.CAIXA),
            new Cadastro("Edson", Funcao.FRENTISTA),
            new Cadastro("Joyce", Funcao.FRENTISTA),
            new Cadastro("Patricia", Funcao.FRENTISTA),
            new Cadastro("Pamela", Funcao.FRENTISTA),
            new Cadastro("Maria Eduarda", Funcao.FRENTISTA),
            new Cadastro("Maria", Funcao.FRENTISTA),
            new Cadastro("Junior", Funcao.FRENTISTA),
            new Cadastro("Yago", Funcao.FRENTISTA),
            new Cadastro("Marisol", Funcao.FRENTISTA),
            new Cadastro("Carol", Funcao.FRENTISTA),
            new Cadastro("Nathy", Funcao.FRENTISTA),
            new Cadastro("José João", Funcao.FRENTISTA),
            new Cadastro("Marcos Arruda", Funcao.FRENTISTA),
            new Cadastro("Celso", Funcao.FRENTISTA));

    private final FuncionarioRepository repositorio;

    public FuncionariosIniciais(FuncionarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Funcionario> existentes = new ArrayList<>(repositorio.findAll());
        if (existentes.isEmpty()) {
            for (Cadastro cadastro : CADASTROS) {
                existentes.add(repositorio.save(new Funcionario(cadastro.nome(), true, cadastro.funcao())));
            }
            aplicarRegrasIniciais(existentes, false);
        } else {
            // Apply the original examples once to databases created by earlier versions.
            aplicarRegrasIniciais(existentes, true);
        }
    }

    private void aplicarRegrasIniciais(List<Funcionario> existentes, boolean apenasLegados) {
        for (Funcionario funcionario : existentes) {
            if (apenasLegados && funcionario.getRegrasInicializadas() != null) continue;
            String nome = chave(funcionario.getNome());
            if (nome.equals("edson")) funcionario.setFolgaFixa(DayOfWeek.TUESDAY);
            if (nome.equals("pamela") || nome.equals("junior")) funcionario.setFolgaFixa(DayOfWeek.THURSDAY);
            String titular = switch (nome) {
                case "joyce" -> "gustavo";
                case "yago" -> "franciele";
                case "marcos arruda" -> "marcos paulo";
                default -> null;
            };
            if (titular != null) {
                existentes.stream().filter(item -> chave(item.getNome()).equals(titular))
                        .findFirst().ifPresent(funcionario::setTitularCoberto);
            }
            funcionario.setRegrasInicializadas(true);
        }
    }

    private static String chave(String nome) {
        String normalizado = Normalizer.normalize(nome.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return switch (normalizado) {
            case "francielly" -> "franciele";
            case "juniot" -> "junior";
            default -> normalizado;
        };
    }
}
