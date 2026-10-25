package br.com.posto.escala.controller;

import br.com.posto.escala.service.ArquivoDados;
import br.com.posto.escala.service.TransferenciaDadosService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TransferenciaDadosController {
    private static final Logger LOG = LoggerFactory.getLogger(TransferenciaDadosController.class);
    private static final String PENDENTE = "importacaoPendente";
    private final TransferenciaDadosService transferencia;

    public TransferenciaDadosController(TransferenciaDadosService transferencia) {
        this.transferencia = transferencia;
    }

    @GetMapping("/dados/exportar")
    public ResponseEntity<byte[]> exportar() {
        String nome = "escalas-do-posto-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(transferencia.exportar());
    }

    @PostMapping("/dados/importar/revisar")
    public String revisar(@RequestParam("arquivo") MultipartFile arquivo, HttpSession sessao,
                          RedirectAttributes redirect) {
        try {
            ArquivoDados dados = transferencia.revisar(arquivo.getBytes());
            sessao.setAttribute(PENDENTE, new ImportacaoPendente(UUID.randomUUID().toString(), dados));
            return "redirect:/dados/importar/revisar";
        } catch (IllegalArgumentException | IOException ex) {
            redirect.addFlashAttribute("erro", ex instanceof IOException
                    ? "Não foi possível ler o arquivo selecionado." : ex.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/dados/importar/revisar")
    public String revisarPendente(HttpSession sessao, Model model, RedirectAttributes redirect) {
        ImportacaoPendente pendente = (ImportacaoPendente) sessao.getAttribute(PENDENTE);
        if (pendente == null) {
            redirect.addFlashAttribute("erro", "Selecione um arquivo para importar.");
            return "redirect:/";
        }
        model.addAttribute("pendente", pendente);
        return "importar-dados";
    }

    @PostMapping("/dados/importar")
    public String importar(@RequestParam String token, HttpSession sessao, RedirectAttributes redirect) {
        ImportacaoPendente pendente = (ImportacaoPendente) sessao.getAttribute(PENDENTE);
        if (pendente == null || !pendente.token().equals(token)) {
            redirect.addFlashAttribute("erro", "A confirmação expirou. Selecione o arquivo novamente.");
            return "redirect:/";
        }
        try {
            transferencia.importar(pendente.dados());
            sessao.removeAttribute(PENDENTE);
            redirect.addFlashAttribute("mensagem", "Dados importados. Funcionários e escalas deste computador foram substituídos pelo arquivo.");
            return "redirect:/";
        } catch (RuntimeException ex) {
            LOG.error("Falha ao importar arquivo de dados", ex);
            redirect.addFlashAttribute("erro", "A importação falhou. Os dados anteriores foram mantidos.");
            return "redirect:/dados/importar/revisar";
        }
    }

    @PostMapping("/dados/importar/cancelar")
    public String cancelar(HttpSession sessao) {
        sessao.removeAttribute(PENDENTE);
        return "redirect:/";
    }

    public record ImportacaoPendente(String token, ArquivoDados dados) {}
}
