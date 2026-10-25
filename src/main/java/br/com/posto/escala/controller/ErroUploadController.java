package br.com.posto.escala.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class ErroUploadController {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String arquivoGrande(RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", "O arquivo é grande demais. Escolha uma cópia com até 25 MB.");
        return "redirect:/";
    }
}
