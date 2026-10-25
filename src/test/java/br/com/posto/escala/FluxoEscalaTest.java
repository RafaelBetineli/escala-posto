package br.com.posto.escala;

import br.com.posto.escala.domain.DiaEscala;
import br.com.posto.escala.domain.LocalTrabalho;
import br.com.posto.escala.domain.StatusDia;
import br.com.posto.escala.domain.Marcacao;
import br.com.posto.escala.domain.Funcao;
import br.com.posto.escala.repository.DiaEscalaRepository;
import br.com.posto.escala.repository.EscalaRepository;
import br.com.posto.escala.repository.FuncionarioRepository;
import br.com.posto.escala.service.EscalaService;
import br.com.posto.escala.service.FuncionariosIniciais;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Autowired EscalaRepository escalaRepository;
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
        assertThat(almoco).contains("Joyce", "Início do almoço").doesNotContain("Início do almoço de Edson");
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

    @Test
    void apontaFaltaDeCoberturaEValidaAlmocoNosTresTurnos() {
        var escala = escalas.criar(12, 2026);
        var dia = dias.buscarPorEscala(escala.getId()).stream()
                .filter(item -> item.getFuncionario().getNome().equals("Edson")
                        && item.getData().equals(LocalDate.of(2026, 12, 1))).findFirst().orElseThrow();
        dia.aplicar(Marcacao.PISTA_06);
        dias.save(dia);

        var visao = escalas.visualizar(escala.getId());
        assertThat(visao.avisos()).anyMatch(aviso -> aviso.contains("folga fixa de Edson"));
        assertThat(visao.avisos()).anyMatch(aviso -> aviso.contains("faltam 4 pessoa(s) no 1º turno"));
        assertThat(visao.avisos()).anyMatch(aviso -> aviso.contains("falta uma pessoa no caixa"));
        assertThat(visao.resumos().get(dia.getData()).texto()).contains("M1");

        assertThatThrownBy(() -> escalas.salvarAlmoco(escala.getId(), dia.getData(),
                Map.of("inicio_" + dia.getId(), "03:00", "fim_" + dia.getId(), "04:00")))
                .isInstanceOf(IllegalArgumentException.class);
        escalas.salvarAlmoco(escala.getId(), dia.getData(),
                Map.of("inicio_" + dia.getId(), "11:00", "fim_" + dia.getId(), "12:00"));

        dia.aplicar(Marcacao.PISTA_22);
        dias.save(dia);
        assertThatThrownBy(() -> escalas.salvarAlmoco(escala.getId(), dia.getData(),
                Map.of("inicio_" + dia.getId(), "21:00", "fim_" + dia.getId(), "22:30")))
                .isInstanceOf(IllegalArgumentException.class);
        escalas.salvarAlmoco(escala.getId(), dia.getData(),
                Map.of("inicio_" + dia.getId(), "23:30", "fim_" + dia.getId(), "00:15"));
        assertThat(dias.findById(dia.getId()).orElseThrow().getAlmocoFim()).isEqualTo(LocalTime.of(0, 15));
    }

    @Test
    void aplicaAlmocoEmVariosDiasSemApagarHorariosOuFolgas() throws Exception {
        var escala = escalas.criar(10, 2026);
        var franciele = dias.buscarPorEscala(escala.getId()).stream()
                .filter(dia -> dia.getFuncionario().getNome().equals("Franciele"))
                .filter(dia -> dia.getData().getDayOfMonth() <= 5).toList();
        franciele.get(0).aplicar(Marcacao.CAIXA_06);
        franciele.get(1).aplicar(Marcacao.CAIXA_06);
        franciele.get(1).definirAlmoco(LocalTime.of(11, 0), LocalTime.of(11, 30));
        franciele.get(2).aplicar(Marcacao.CAIXA_06);
        franciele.get(3).aplicar(Marcacao.FOLGA);
        franciele.get(4).aplicar(Marcacao.CAIXA_22);
        dias.saveAll(franciele);

        String pagina = navegador.perform(get("/escalas/" + escala.getId() + "/almoco"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(pagina).contains("PLANEJAMENTO DE ALMOÇO", "Aplicar horário a um período",
                "Substituir horários já preenchidos", "Dias do mês");
        String paginaDia = navegador.perform(get("/escalas/" + escala.getId() + "/almoco")
                        .param("data", "2026-10-01"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(paginaDia).contains("EDIÇÃO DO DIA", "Franciele").doesNotContain("form-almoco-lote");

        navegador.perform(post("/escalas/" + escala.getId() + "/almoco/lote")
                        .param("funcionarioId", String.valueOf(franciele.getFirst().getFuncionario().getId()))
                        .param("inicio", "2026-10-01").param("fim", "2026-10-04")
                        .param("horaInicio", "10:00").param("horaFim", "10:30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/escalas/" + escala.getId() + "/almoco"));
        var preenchidos = dias.buscarPorEscala(escala.getId()).stream()
                .filter(dia -> dia.getFuncionario().getId().equals(franciele.getFirst().getFuncionario().getId()))
                .filter(dia -> dia.getData().getDayOfMonth() <= 5).toList();
        assertThat(preenchidos.get(0).getAlmocoInicio()).isEqualTo(LocalTime.of(10, 0));
        assertThat(preenchidos.get(1).getAlmocoInicio()).isEqualTo(LocalTime.of(11, 0));
        assertThat(preenchidos.get(2).getAlmocoInicio()).isEqualTo(LocalTime.of(10, 0));
        assertThat(preenchidos.get(3).getAlmocoInicio()).isNull();

        assertThatThrownBy(() -> escalas.aplicarAlmocoEmLote(escala.getId(),
                franciele.getFirst().getFuncionario().getId(), LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5), "10:30", "11:00", true))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Dia 5");
        assertThat(dias.findById(franciele.getFirst().getId()).orElseThrow().getAlmocoInicio())
                .isEqualTo(LocalTime.of(10, 0));

        assertThat(escalas.aplicarAlmocoEmLote(escala.getId(), franciele.getFirst().getFuncionario().getId(),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), "10:30", "11:00", true)).isEqualTo(3);
        assertThat(dias.findById(franciele.get(1).getId()).orElseThrow().getAlmocoInicio())
                .isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    void excluiEscalaComSeusDiasSemAfetarOutroMes() throws Exception {
        var excluida = escalas.criar(1, 2027);
        var mantida = escalas.criar(2, 2027);
        Long diaExcluido = dias.buscarPorEscala(excluida.getId()).getFirst().getId();
        int quantidadeMantida = dias.buscarPorEscala(mantida.getId()).size();

        String inicio = navegador.perform(get("/"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(inicio).contains("confirmar-exclusao-" + excluida.getId(),
                "/escalas/" + excluida.getId() + "/excluir", "Excluir escala");

        navegador.perform(post("/escalas/" + excluida.getId() + "/excluir"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        assertThat(escalaRepository.findById(excluida.getId())).isEmpty();
        assertThat(dias.findById(diaExcluido)).isEmpty();
        assertThat(escalaRepository.findById(mantida.getId())).isPresent();
        assertThat(dias.buscarPorEscala(mantida.getId())).hasSize(quantidadeMantida);
        assertThat(escalas.criar(1, 2027).getId()).isNotEqualTo(excluida.getId());
    }

    @Test
    @Transactional
    void naoRestauraNomeOuFuncaoEditadosAoReinicializar() {
        var edson = funcionarios.findAll().stream()
                .filter(item -> item.getNome().equals("Edson")).findFirst().orElseThrow();
        edson.setNome("Edson Atualizado");
        edson.setFuncao(Funcao.CAIXA);
        edson.setFolgaFixa(DayOfWeek.MONDAY);
        funcionarios.save(edson);
        cargaInicial.run(null);
        var atualizado = funcionarios.findById(edson.getId()).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("Edson Atualizado");
        assertThat(atualizado.getFuncao()).isEqualTo(Funcao.CAIXA);
        assertThat(atualizado.getFolgaFixa()).isEqualTo(DayOfWeek.MONDAY);
    }
}
