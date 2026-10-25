package br.com.posto.escala.controller;

import br.com.posto.escala.domain.Marcacao;
import br.com.posto.escala.repository.EscalaRepository;
import br.com.posto.escala.service.EscalaService;
import br.com.posto.escala.view.VisualizacaoEscala;
import jakarta.validation.Valid;
import java.time.Month;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EscalaController {
    private static final Locale PORTUGUES = Locale.of("pt", "BR");
    private final EscalaRepository escalas;
    private final EscalaService service;

    public EscalaController(EscalaRepository escalas, EscalaService service) {
        this.escalas = escalas;
        this.service = service;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("form", new EscalaForm());
        preencherInicio(model);
        return "inicio";
    }

    @PostMapping("/escalas")
    public String criar(@Valid @ModelAttribute("form") EscalaForm form, BindingResult erros,
                        Model model, RedirectAttributes redirect) {
        if (!erros.hasErrors()) {
            try {
                var escala = service.criar(form.getMes(), form.getAno());
                redirect.addFlashAttribute("mensagem", "Escala criada. Os dias estão prontos para preenchimento.");
                return "redirect:/escalas/" + escala.getId();
            } catch (IllegalArgumentException ex) {
                erros.reject("escala", ex.getMessage());
            }
        }
        preencherInicio(model);
        return "inicio";
    }

    @PostMapping("/escalas/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        service.excluir(id);
        redirect.addFlashAttribute("mensagem", "Escala excluída com seus turnos e horários de almoço.");
        return "redirect:/";
    }

    @GetMapping("/escalas/{id}")
    public String visualizar(@PathVariable Long id, Model model) {
        VisualizacaoEscala visualizacao = service.visualizar(id);
        model.addAttribute("visao", visualizacao);
        model.addAttribute("mesNome", Month.of(visualizacao.escala().getMes()).getDisplayName(TextStyle.FULL, PORTUGUES));
        model.addAttribute("opcoes", Marcacao.values());
        YearMonth periodo = YearMonth.of(visualizacao.escala().getAno(), visualizacao.escala().getMes());
        model.addAttribute("primeiroDia", periodo.atDay(1));
        model.addAttribute("ultimoDia", periodo.atEndOfMonth());
        return "escala";
    }

    @PostMapping("/escalas/{id}/dias")
    public String salvar(@PathVariable Long id, @RequestParam Map<String, String> campos, RedirectAttributes redirect) {
        try {
            service.salvar(id, campos);
            redirect.addFlashAttribute("mensagem", "Escala salva.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/escalas/" + id;
    }

    @PostMapping("/escalas/{id}/ferias")
    public String marcarFerias(@PathVariable Long id, @RequestParam(required = false) Long funcionarioId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                               RedirectAttributes redirect) {
        try {
            service.marcarFerias(id, funcionarioId, inicio, fim);
            redirect.addFlashAttribute("mensagem", "Férias marcadas no intervalo. Confira a tabela mensal.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/escalas/" + id;
    }

    @GetMapping("/escalas/{id}/almoco")
    public String almoco(@PathVariable Long id,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                         Model model) {
        if (data == null) {
            var visaoMes = service.visualizarAlmocoMes(id);
            model.addAttribute("visaoMes", visaoMes);
            model.addAttribute("mesNome", Month.of(visaoMes.escala().getMes()).getDisplayName(TextStyle.FULL, PORTUGUES));
            model.addAttribute("primeiroDia", LocalDate.of(visaoMes.escala().getAno(), visaoMes.escala().getMes(), 1));
            model.addAttribute("ultimoDia", YearMonth.of(visaoMes.escala().getAno(), visaoMes.escala().getMes()).atEndOfMonth());
            return "almoco-mes";
        }
        var visao = service.visualizarAlmoco(id, data);
        model.addAttribute("visao", visao);
        model.addAttribute("mesNome", Month.of(data.getMonthValue()).getDisplayName(TextStyle.FULL, PORTUGUES));
        model.addAttribute("diaSemana", data.getDayOfWeek().getDisplayName(TextStyle.FULL, PORTUGUES));
        model.addAttribute("diaAnterior", data.getDayOfMonth() > 1 ? data.minusDays(1) : null);
        model.addAttribute("diaSeguinte", data.getDayOfMonth() < data.lengthOfMonth() ? data.plusDays(1) : null);
        return "almoco";
    }

    @PostMapping("/escalas/{id}/almoco/lote")
    public String aplicarAlmocoEmLote(@PathVariable Long id,
                                      @RequestParam(required = false) Long funcionarioId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                      @RequestParam(required = false) String horaInicio,
                                      @RequestParam(required = false) String horaFim,
                                      @RequestParam(defaultValue = "false") boolean substituir,
                                      RedirectAttributes redirect) {
        try {
            int quantidade = service.aplicarAlmocoEmLote(id, funcionarioId, inicio, fim, horaInicio, horaFim, substituir);
            redirect.addFlashAttribute("mensagem", "Almoço aplicado em " + quantidade + " dia(s) trabalhado(s)."
                    + (substituir ? "" : " Horários já preenchidos foram mantidos."));
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/escalas/" + id + "/almoco";
    }

    @PostMapping("/escalas/{id}/almoco")
    public String salvarAlmoco(@PathVariable Long id,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                               @RequestParam Map<String, String> campos, RedirectAttributes redirect) {
        try {
            service.salvarAlmoco(id, data, campos);
            redirect.addFlashAttribute("mensagem", "Horários de almoço salvos.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/escalas/" + id + "/almoco?data=" + data;
    }

    private void preencherInicio(Model model) {
        model.addAttribute("escalas", escalas.findAllByOrderByAnoDescMesDesc());
        List<String> meses = Arrays.stream(Month.values())
                .map(mes -> mes.getDisplayName(TextStyle.FULL, PORTUGUES)).toList();
        model.addAttribute("meses", meses);
    }
}
