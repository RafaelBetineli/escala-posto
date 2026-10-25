package br.com.posto.escala.controller;

import br.com.posto.escala.domain.Funcionario;
import br.com.posto.escala.domain.Funcao;
import br.com.posto.escala.repository.FuncionarioRepository;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class FuncionarioController {
    private final FuncionarioRepository funcionarios;

    public FuncionarioController(FuncionarioRepository funcionarios) {
        this.funcionarios = funcionarios;
    }

    @GetMapping("/funcionarios")
    public String listar(Model model) {
        model.addAttribute("form", new FuncionarioForm());
        preencherCadastro(model);
        return "funcionarios";
    }

    @PostMapping("/funcionarios")
    public String cadastrar(@Valid @ModelAttribute("form") FuncionarioForm form, BindingResult erros,
                            Model model, RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            preencherCadastro(model);
            return "funcionarios";
        }
        if (nomeEmUso(form.getNome().trim(), null)) {
            erros.rejectValue("nome", "duplicado", "Já existe um funcionário com esse nome.");
            preencherCadastro(model);
            return "funcionarios";
        }
        funcionarios.save(new Funcionario(form.getNome().trim(), form.isAtivo(), form.getFuncao()));
        redirect.addFlashAttribute("mensagem", "Funcionário cadastrado.");
        return "redirect:/funcionarios";
    }

    @PostMapping("/funcionarios/{id}/editar")
    public String editar(@PathVariable Long id, @RequestParam String nome,
                         @RequestParam Funcao funcao,
                         @RequestParam(required = false) DayOfWeek folgaFixa,
                         @RequestParam(required = false) Long titularCobertoId,
                         RedirectAttributes redirect) {
        Funcionario funcionario = funcionarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));
        String nomeLimpo = nome.trim();
        if (nomeLimpo.isBlank() || nomeLimpo.length() > 120 || nomeEmUso(nomeLimpo, id)) {
            redirect.addFlashAttribute("erro", "Informe um nome único com até 120 caracteres.");
            return "redirect:/funcionarios";
        }
        Funcionario titular = null;
        if (titularCobertoId != null) {
            titular = funcionarios.findById(titularCobertoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Titular não encontrado"));
            if (titular.getId().equals(id) || titular.getFuncao() != Funcao.CAIXA) {
                redirect.addFlashAttribute("erro", "Escolha outro funcionário do caixa para a cobertura.");
                return "redirect:/funcionarios";
            }
        }
        funcionario.setNome(nomeLimpo);
        funcionario.setFuncao(funcao);
        funcionario.setFolgaFixa(folgaFixa);
        funcionario.setTitularCoberto(titular);
        funcionario.setRegrasInicializadas(true);
        funcionarios.save(funcionario);
        redirect.addFlashAttribute("mensagem", "Funcionário e regras atualizados.");
        return "redirect:/funcionarios";
    }

    private void preencherCadastro(Model model) {
        model.addAttribute("funcionarios", funcionarios.findAllByOrderByNomeAscIdAsc());
        model.addAttribute("funcoes", Funcao.values());
        Map<DayOfWeek, String> dias = new LinkedHashMap<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            dias.put(dia, dia.getDisplayName(TextStyle.FULL, Locale.of("pt", "BR")));
        }
        model.addAttribute("diasSemana", dias);
    }

    private boolean nomeEmUso(String nome, Long id) {
        return funcionarios.findAll().stream().anyMatch(outro ->
                (id == null || !outro.getId().equals(id)) && outro.getNome().equalsIgnoreCase(nome));
    }

    @PostMapping("/funcionarios/{id}/alternar-status")
    public String alternarStatus(@PathVariable Long id, RedirectAttributes redirect) {
        Funcionario funcionario = funcionarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));
        funcionario.setAtivo(!funcionario.isAtivo());
        funcionarios.save(funcionario);
        redirect.addFlashAttribute("mensagem", "Status atualizado.");
        return "redirect:/funcionarios";
    }
}
