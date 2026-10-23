package br.com.posto.escala.controller;

import br.com.posto.escala.domain.Funcionario;
import br.com.posto.escala.domain.Funcao;
import br.com.posto.escala.repository.FuncionarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
        model.addAttribute("funcionarios", funcionarios.findAllByOrderByNomeAscIdAsc());
        model.addAttribute("funcoes", Funcao.values());
        return "funcionarios";
    }

    @PostMapping("/funcionarios")
    public String cadastrar(@Valid @ModelAttribute("form") FuncionarioForm form, BindingResult erros,
                            Model model, RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            model.addAttribute("funcionarios", funcionarios.findAllByOrderByNomeAscIdAsc());
            model.addAttribute("funcoes", Funcao.values());
            return "funcionarios";
        }
        funcionarios.save(new Funcionario(form.getNome().trim(), form.isAtivo(), form.getFuncao()));
        redirect.addFlashAttribute("mensagem", "Funcionário cadastrado.");
        return "redirect:/funcionarios";
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
