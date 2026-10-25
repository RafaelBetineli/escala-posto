package br.com.posto.escala;

import br.com.posto.escala.controller.TransferenciaDadosController.ImportacaoPendente;
import br.com.posto.escala.domain.Marcacao;
import br.com.posto.escala.domain.LocalTrabalho;
import br.com.posto.escala.domain.StatusDia;
import br.com.posto.escala.domain.Turno;
import br.com.posto.escala.repository.DiaEscalaRepository;
import br.com.posto.escala.repository.EscalaRepository;
import br.com.posto.escala.repository.FuncionarioRepository;
import br.com.posto.escala.service.EscalaService;
import br.com.posto.escala.service.TransferenciaDadosService;
import br.com.posto.escala.service.ArquivoDados;
import java.time.LocalTime;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:transferencia;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class TransferenciaDadosTest {
    @Autowired MockMvc navegador;
    @Autowired TransferenciaDadosService transferencia;
    @Autowired EscalaService servicoEscalas;
    @Autowired EscalaRepository escalas;
    @Autowired FuncionarioRepository funcionarios;
    @Autowired DiaEscalaRepository dias;

    @Test
    void exportaRevisaESubstituiTodosOsDadosAposConfirmacao() throws Exception {
        var original = servicoEscalas.criar(3, 2028);
        var dia = dias.buscarPorEscala(original.getId()).getFirst();
        dia.aplicar(Marcacao.PISTA_06);
        dia.definirAlmoco(LocalTime.of(10, 0), LocalTime.of(10, 30));
        dias.save(dia);
        long totalFuncionarios = funcionarios.count();
        long totalDias = dias.count();

        byte[] copia = navegador.perform(get("/dados/exportar"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(copia)).contains("escalas-do-posto", "almocoInicio", "2028-03");

        var outroMes = servicoEscalas.criar(4, 2028);
        assertThat(escalas.count()).isEqualTo(2);

        var envio = navegador.perform(multipart("/dados/importar/revisar")
                        .file(new MockMultipartFile("arquivo", "copia.json", "application/json", copia)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dados/importar/revisar"))
                .andReturn();
        MockHttpSession sessao = (MockHttpSession) envio.getRequest().getSession();
        ImportacaoPendente pendente = (ImportacaoPendente) sessao.getAttribute("importacaoPendente");
        assertThat(pendente).isNotNull();
        String revisao = navegador.perform(get("/dados/importar/revisar").session(sessao))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(revisao).contains("Confira antes de substituir", "Substituir dados deste computador");
        assertThat(escalas.findById(outroMes.getId())).isPresent();

        navegador.perform(post("/dados/importar").session(sessao).param("token", pendente.token()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        assertThat(escalas.count()).isEqualTo(1);
        assertThat(escalas.findByMesAndAno(3, 2028)).isPresent();
        assertThat(escalas.findByMesAndAno(4, 2028)).isEmpty();
        assertThat(funcionarios.count()).isEqualTo(totalFuncionarios);
        assertThat(dias.count()).isEqualTo(totalDias);
        var restaurado = dias.buscarPorEscala(escalas.findByMesAndAno(3, 2028).orElseThrow().getId()).stream()
                .filter(item -> item.getData().equals(dia.getData()))
                .filter(item -> item.getFuncionario().getNome().equals(dia.getFuncionario().getNome()))
                .findFirst().orElseThrow();
        assertThat(restaurado.getMarcacao()).isEqualTo(Marcacao.PISTA_06);
        assertThat(restaurado.getAlmocoInicio()).isEqualTo(LocalTime.of(10, 0));
        assertThat(restaurado.getAlmocoFim()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    void rejeitaArquivoInvalidoSemAlterarDados() {
        long totalFuncionarios = funcionarios.count();
        long totalEscalas = escalas.count();
        assertThatThrownBy(() -> transferencia.revisar("{\"formato\":\"outro\"}".getBytes()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(funcionarios.count()).isEqualTo(totalFuncionarios);
        assertThat(escalas.count()).isEqualTo(totalEscalas);
    }

    @Test
    void naoApagaDadosSeArquivoTemAlmocoForaDoTurno() {
        servicoEscalas.criar(5, 2028);
        ArquivoDados original = transferencia.revisar(transferencia.exportar());
        long totalDias = dias.count();
        var itens = new ArrayList<>(original.dias());
        var primeiro = itens.getFirst();
        itens.set(0, new ArquivoDados.DiaDados(primeiro.escalaId(), primeiro.funcionarioId(),
                primeiro.data(), StatusDia.T, Turno.MANHA, LocalTrabalho.PISTA,
                LocalTime.of(3, 0), LocalTime.of(4, 0)));
        var invalido = new ArquivoDados(original.formato(), original.versao(), original.criadoEm(),
                original.funcionarios(), original.escalas(), itens);

        assertThatThrownBy(() -> transferencia.importar(invalido))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(dias.count()).isEqualTo(totalDias);
    }
}
