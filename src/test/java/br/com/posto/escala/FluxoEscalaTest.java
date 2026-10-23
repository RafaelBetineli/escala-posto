package br.com.posto.escala;

import br.com.posto.escala.domain.DiaEscala;
import br.com.posto.escala.domain.LocalTrabalho;
import br.com.posto.escala.domain.StatusDia;
import br.com.posto.escala.domain.Marcacao;
import br.com.posto.escala.repository.DiaEscalaRepository;
import br.com.posto.escala.repository.FuncionarioRepository;
import br.com.posto.escala.service.EscalaService;
import br.com.posto.escala.service.FuncionariosIniciais;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:fluxo;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class FluxoEscalaTest {
    @Autowired MockMvc navegador;
    @Autowired FuncionarioRepository funcionarios;
    @Autowired DiaEscalaRepository dias;
    @Autowired EscalaService escalas;
    @Autowired FuncionariosIniciais cargaInicial;

    @Test
    void cadastraUmaVezESalvaTabelaComAvisos() throws Exception {
        assertThat(funcionarios.count()).isEqualTo(17);
        cargaInicial.run(null);
        assertThat(funcionarios.count()).isEqualTo(17);

        var escala = escalas.criar(11, 2026);
        var celulas = dias.buscarPorEscala(escala.getId());
        assertThat(celulas).hasSize(17 * 30);

        MockHttpServletRequestBuilder envio = post("/escalas/" + escala.getId() + "/dias");
        for (DiaEscala celula : celulas) {
            String marcacao = "VAZIO";
            if (celula.getData().equals(LocalDate.of(2026, 11, 2))) {
                if (celula.getFuncionario().getNome().equals("Gustavo")) marcacao = "FOLGA";
                if (celula.getFuncionario().getNome().equals("Joyce")) marcacao = "CAIXA_06";
            }
            if (celula.getData().equals(LocalDate.of(2026, 11, 3))
                    && celula.getFuncionario().getNome().equals("Edson")) marcacao = "PISTA_06";
            envio.param("dia_" + celula.getId(), marcacao);
        }
        navegador.perform(envio).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/escalas/" + escala.getId()));

        DiaEscala joyce = dias.buscarPorEscala(escala.getId()).stream()
                .filter(dia -> dia.getFuncionario().getNome().equals("Joyce")
                        && dia.getData().equals(LocalDate.of(2026, 11, 2))).findFirst().orElseThrow();
        assertThat(joyce.getStatus()).isEqualTo(StatusDia.T);
        assertThat(joyce.getLocal()).isEqualTo(LocalTrabalho.CAIXA);

        String pagina = navegador.perform(get("/escalas/" + escala.getId()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(pagina).contains("CX 06", "conferir folga fixa de Edson", "Imprimir / PDF");
        assertThat(pagina).doesNotContain("conferir cobertura do caixa de Gustavo");

        String almoco = navegador.perform(get("/escalas/" + escala.getId() + "/almoco")
                        .param("data", "2026-11-02"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(almoco).contains("Joyce", "Início do almoço").doesNotContain("Edson");
        navegador.perform(post("/escalas/" + escala.getId() + "/almoco")
                        .param("data", "2026-11-02")
                        .param("inicio_" + joyce.getId(), "11:00")
                        .param("fim_" + joyce.getId(), "12:00"))
                .andExpect(status().is3xxRedirection());
        DiaEscala comAlmoco = dias.buscarPorEscala(escala.getId()).stream()
                .filter(dia -> dia.getId().equals(joyce.getId())).findFirst().orElseThrow();
        assertThat(comAlmoco.getAlmocoInicio()).isEqualTo(LocalTime.of(11, 0));
        assertThat(comAlmoco.getAlmocoFim()).isEqualTo(LocalTime.of(12, 0));

        navegador.perform(post("/escalas/" + escala.getId() + "/ferias")
                        .param("funcionarioId", String.valueOf(joyce.getFuncionario().getId()))
                        .param("inicio", "2026-11-02")
                        .param("fim", "2026-11-03"))
                .andExpect(status().is3xxRedirection());
        DiaEscala ferias = dias.buscarPorEscala(escala.getId()).stream()
                .filter(dia -> dia.getId().equals(joyce.getId())).findFirst().orElseThrow();
        assertThat(ferias.getMarcacao()).isEqualTo(Marcacao.FERIAS);
        assertThat(ferias.getAlmocoInicio()).isNull();
        String paginaComFerias = navegador.perform(get("/escalas/" + escala.getId()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(paginaComFerias).contains("conferir cobertura do caixa de Gustavo", "FER", "06 CX");
        assertThat(paginaComFerias).doesNotContain("Sem marcação");
    }
}
