package br.com.posto.escala.service;

import br.com.posto.escala.domain.DiaEscala;
import br.com.posto.escala.domain.Escala;
import br.com.posto.escala.domain.Funcao;
import br.com.posto.escala.domain.Funcionario;
import br.com.posto.escala.domain.Marcacao;
import br.com.posto.escala.domain.StatusDia;
import br.com.posto.escala.domain.Turno;
import br.com.posto.escala.repository.DiaEscalaRepository;
import br.com.posto.escala.repository.EscalaRepository;
import br.com.posto.escala.repository.FuncionarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferenciaDadosService {
    private static final String FORMATO = "escalas-do-posto";
    private static final int VERSAO = 1;
    private static final int TAMANHO_MAXIMO = 25_000_000;

    private final FuncionarioRepository funcionarios;
    private final EscalaRepository escalas;
    private final DiaEscalaRepository dias;
    private final ObjectMapper json;
    private final EntityManager entidade;

    public TransferenciaDadosService(FuncionarioRepository funcionarios, EscalaRepository escalas,
                                     DiaEscalaRepository dias, ObjectMapper json, EntityManager entidade) {
        this.funcionarios = funcionarios;
        this.escalas = escalas;
        this.dias = dias;
        this.json = json;
        this.entidade = entidade;
    }

    @Transactional(readOnly = true)
    public byte[] exportar() {
        List<ArquivoDados.FuncionarioDados> pessoas = funcionarios.findAllByOrderByNomeAscIdAsc().stream()
                .map(pessoa -> new ArquivoDados.FuncionarioDados(pessoa.getId(), pessoa.getNome(),
                        pessoa.isAtivo(), pessoa.getFuncao(), pessoa.getFolgaFixa(),
                        pessoa.getTitularCoberto() == null ? null : pessoa.getTitularCoberto().getId(),
                        pessoa.getRegrasInicializadas())).toList();
        List<Escala> meses = escalas.findAllByOrderByAnoDescMesDesc();
        List<ArquivoDados.EscalaDados> escalasArquivo = meses.stream()
                .map(escala -> new ArquivoDados.EscalaDados(escala.getId(), escala.getMes(), escala.getAno()))
                .toList();
        List<ArquivoDados.DiaDados> diasArquivo = new ArrayList<>();
        for (Escala escala : meses) {
            for (DiaEscala dia : dias.buscarPorEscala(escala.getId())) {
                diasArquivo.add(new ArquivoDados.DiaDados(escala.getId(), dia.getFuncionario().getId(),
                        dia.getData(), dia.getStatus(), dia.getTurno(), dia.getLocal(),
                        dia.getAlmocoInicio(), dia.getAlmocoFim()));
            }
        }
        try {
            return json.writeValueAsBytes(new ArquivoDados(FORMATO, VERSAO, Instant.now(),
                    pessoas, escalasArquivo, diasArquivo));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível gerar o arquivo de dados.", ex);
        }
    }

    public ArquivoDados revisar(byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0 || conteudo.length > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("Escolha um arquivo de dados válido com até 25 MB.");
        }
        try {
            ArquivoDados arquivo = json.readValue(conteudo, ArquivoDados.class);
            validar(arquivo);
            return arquivo;
        } catch (IOException ex) {
            throw new IllegalArgumentException("O arquivo não é um backup válido do sistema.");
        }
    }

    @Transactional
    public void importar(ArquivoDados arquivo) {
        validar(arquivo);
        dias.deleteAllInBatch();
        escalas.deleteAllInBatch();
        entidade.createQuery("update Funcionario f set f.titularCoberto = null").executeUpdate();
        funcionarios.deleteAllInBatch();
        entidade.clear();

        Map<Long, Funcionario> pessoas = new HashMap<>();
        for (ArquivoDados.FuncionarioDados registro : arquivo.funcionarios()) {
            Funcionario pessoa = new Funcionario(registro.nome(), registro.ativo(), registro.funcao());
            pessoa.setFolgaFixa(registro.folgaFixa());
            pessoa.setRegrasInicializadas(registro.regrasInicializadas());
            pessoas.put(registro.id(), funcionarios.save(pessoa));
        }
        for (ArquivoDados.FuncionarioDados registro : arquivo.funcionarios()) {
            if (registro.titularCobertoId() != null) {
                pessoas.get(registro.id()).setTitularCoberto(pessoas.get(registro.titularCobertoId()));
            }
        }
        funcionarios.flush();

        Map<Long, Escala> meses = new HashMap<>();
        for (ArquivoDados.EscalaDados registro : arquivo.escalas()) {
            meses.put(registro.id(), escalas.save(new Escala(registro.mes(), registro.ano())));
        }
        List<DiaEscala> novosDias = new ArrayList<>(arquivo.dias().size());
        for (ArquivoDados.DiaDados registro : arquivo.dias()) {
            DiaEscala dia = new DiaEscala(pessoas.get(registro.funcionarioId()),
                    meses.get(registro.escalaId()), registro.data());
            dia.aplicar(marcacao(registro));
            if (registro.almocoInicio() != null) {
                dia.definirAlmoco(registro.almocoInicio(), registro.almocoFim());
            }
            novosDias.add(dia);
        }
        dias.saveAll(novosDias);
    }

    private static void validar(ArquivoDados arquivo) {
        if (arquivo == null || !FORMATO.equals(arquivo.formato()) || arquivo.versao() != VERSAO
                || arquivo.criadoEm() == null || arquivo.funcionarios() == null
                || arquivo.funcionarios().isEmpty() || arquivo.escalas() == null || arquivo.dias() == null) {
            throw new IllegalArgumentException("O arquivo não é um backup compatível do sistema.");
        }
        Map<Long, ArquivoDados.FuncionarioDados> pessoas = new HashMap<>();
        Set<String> nomes = new HashSet<>();
        for (ArquivoDados.FuncionarioDados pessoa : arquivo.funcionarios()) {
            if (pessoa == null || pessoa.id() == null || pessoa.id() <= 0 || pessoa.nome() == null
                    || pessoa.nome().isBlank() || pessoa.nome().length() > 120 || pessoa.funcao() == null
                    || pessoas.putIfAbsent(pessoa.id(), pessoa) != null
                    || !nomes.add(pessoa.nome().trim().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("O arquivo contém um cadastro de funcionário inválido ou repetido.");
            }
        }
        for (ArquivoDados.FuncionarioDados pessoa : arquivo.funcionarios()) {
            Long titularId = pessoa.titularCobertoId();
            if (titularId != null && (titularId.equals(pessoa.id()) || !pessoas.containsKey(titularId)
                    || pessoas.get(titularId).funcao() != Funcao.CAIXA)) {
                throw new IllegalArgumentException("O arquivo contém uma cobertura de caixa inválida.");
            }
        }

        Map<Long, YearMonth> meses = new HashMap<>();
        Set<YearMonth> periodos = new HashSet<>();
        for (ArquivoDados.EscalaDados escala : arquivo.escalas()) {
            if (escala == null || escala.id() == null || escala.id() <= 0 || escala.mes() < 1
                    || escala.mes() > 12 || escala.ano() < 2000 || escala.ano() > 2100) {
                throw new IllegalArgumentException("O arquivo contém uma escala inválida.");
            }
            YearMonth periodo = YearMonth.of(escala.ano(), escala.mes());
            if (meses.putIfAbsent(escala.id(), periodo) != null || !periodos.add(periodo)) {
                throw new IllegalArgumentException("O arquivo contém escalas repetidas.");
            }
        }

        Set<String> celulas = new HashSet<>();
        for (ArquivoDados.DiaDados dia : arquivo.dias()) {
            if (dia == null || dia.data() == null || !meses.containsKey(dia.escalaId())
                    || !pessoas.containsKey(dia.funcionarioId())
                    || !YearMonth.from(dia.data()).equals(meses.get(dia.escalaId()))
                    || !celulas.add(dia.escalaId() + ":" + dia.funcionarioId() + ":" + dia.data())
                    || marcacao(dia) == null
                    || (dia.almocoInicio() == null) != (dia.almocoFim() == null)
                    || (dia.almocoInicio() != null && (dia.status() != StatusDia.T
                            || !almocoValido(dia)))) {
                throw new IllegalArgumentException("O arquivo contém um dia de escala inválido ou repetido.");
            }
        }
    }

    private static boolean almocoValido(ArquivoDados.DiaDados dia) {
        int inicioTurno = switch (dia.turno()) {
            case MANHA -> 6 * 60;
            case TARDE -> 14 * 60;
            case NOITE -> 22 * 60;
        };
        int inicio = minutosNoTurno(dia.turno(), dia.almocoInicio());
        int fim = minutosNoTurno(dia.turno(), dia.almocoFim());
        return inicio >= inicioTurno && fim <= inicioTurno + 8 * 60 && fim > inicio;
    }

    private static int minutosNoTurno(Turno turno, java.time.LocalTime horario) {
        int minutos = horario.getHour() * 60 + horario.getMinute();
        return turno == Turno.NOITE && minutos <= 6 * 60 ? minutos + 24 * 60 : minutos;
    }

    private static Marcacao marcacao(ArquivoDados.DiaDados dia) {
        for (Marcacao opcao : Marcacao.values()) {
            if (Objects.equals(opcao.getStatus(), dia.status())
                    && Objects.equals(opcao.getTurno(), dia.turno())
                    && Objects.equals(opcao.getLocal(), dia.local())) {
                return opcao;
            }
        }
        return null;
    }
}
