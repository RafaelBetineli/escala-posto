package br.com.posto.escala.service;

import br.com.posto.escala.domain.DiaEscala;
import br.com.posto.escala.domain.Escala;
import br.com.posto.escala.domain.Funcionario;
import br.com.posto.escala.domain.Funcao;
import br.com.posto.escala.domain.LocalTrabalho;
import br.com.posto.escala.domain.Marcacao;
import br.com.posto.escala.domain.StatusDia;
import br.com.posto.escala.domain.Turno;
import br.com.posto.escala.repository.DiaEscalaRepository;
import br.com.posto.escala.repository.EscalaRepository;
import br.com.posto.escala.repository.FuncionarioRepository;
import br.com.posto.escala.view.DiaCabecalho;
import br.com.posto.escala.view.LinhaEscala;
import br.com.posto.escala.view.ResumoDia;
import br.com.posto.escala.view.ResumoAlmocoDia;
import br.com.posto.escala.view.VisualizacaoAlmoco;
import br.com.posto.escala.view.VisualizacaoAlmocoMes;
import br.com.posto.escala.view.VisualizacaoEscala;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EscalaService {
    private static final Locale PORTUGUES = Locale.of("pt", "BR");

    private final EscalaRepository escalas;
    private final FuncionarioRepository funcionarios;
    private final DiaEscalaRepository dias;

    public EscalaService(EscalaRepository escalas, FuncionarioRepository funcionarios, DiaEscalaRepository dias) {
        this.escalas = escalas;
        this.funcionarios = funcionarios;
        this.dias = dias;
    }

    @Transactional
    public Escala criar(int mes, int ano) {
        YearMonth periodo = YearMonth.of(ano, mes);
        if (escalas.findByMesAndAno(mes, ano).isPresent()) {
            throw new IllegalArgumentException("Já existe uma escala para este mês.");
        }
        List<Funcionario> ativos = funcionarios.findByAtivoTrueOrderByNomeAscIdAsc();
        if (ativos.isEmpty()) {
            throw new IllegalArgumentException("Cadastre pelo menos um funcionário ativo antes de criar a escala.");
        }

        Escala escala = escalas.save(new Escala(mes, ano));
        List<DiaEscala> novosDias = new ArrayList<>(ativos.size() * periodo.lengthOfMonth());
        for (Funcionario funcionario : ativos) {
            for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
                novosDias.add(new DiaEscala(funcionario, escala, periodo.atDay(dia)));
            }
        }
        dias.saveAll(novosDias);
        return escala;
    }

    @Transactional
    public void excluir(Long escalaId) {
        Escala escala = obterEscala(escalaId);
        dias.excluirPorEscala(escalaId);
        escalas.delete(escala);
    }

    @Transactional
    public void salvar(Long escalaId, Map<String, String> campos) {
        if (!escalas.existsById(escalaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Escala não encontrada");
        }
        List<DiaEscala> celulas = dias.buscarPorEscala(escalaId);
        Map<DiaEscala, Marcacao> marcacoes = new LinkedHashMap<>();
        for (DiaEscala celula : celulas) {
            String valor = campos.get("dia_" + celula.getId());
            if (valor == null) throw new IllegalArgumentException("A escala enviada está incompleta. Reabra a página e tente novamente.");
            try {
                marcacoes.put(celula, Marcacao.valueOf(valor));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Há uma marcação inválida na escala.");
            }
        }
        marcacoes.forEach(DiaEscala::aplicar);
    }

    @Transactional
    public void marcarFerias(Long escalaId, Long funcionarioId, LocalDate inicio, LocalDate fim) {
        Escala escala = obterEscala(escalaId);
        YearMonth mes = YearMonth.of(escala.getAno(), escala.getMes());
        if (funcionarioId == null || inicio == null || fim == null || inicio.isAfter(fim)
                || !YearMonth.from(inicio).equals(mes) || !YearMonth.from(fim).equals(mes)) {
            throw new IllegalArgumentException("Escolha um funcionário e um intervalo válido dentro do mês da escala.");
        }
        List<DiaEscala> selecionados = dias.buscarPorEscala(escalaId).stream()
                .filter(dia -> dia.getFuncionario().getId().equals(funcionarioId)
                        && !dia.getData().isBefore(inicio) && !dia.getData().isAfter(fim))
                .toList();
        if (selecionados.size() != inicio.datesUntil(fim.plusDays(1)).count()) {
            throw new IllegalArgumentException("Funcionário não encontrado nesta escala.");
        }
        selecionados.forEach(dia -> dia.aplicar(Marcacao.FERIAS));
    }

    @Transactional(readOnly = true)
    public VisualizacaoAlmocoMes visualizarAlmocoMes(Long escalaId) {
        Escala escala = obterEscala(escalaId);
        YearMonth periodo = YearMonth.of(escala.getAno(), escala.getMes());
        List<DiaEscala> trabalhando = dias.buscarPorEscala(escalaId).stream()
                .filter(dia -> dia.getStatus() == StatusDia.T).toList();
        List<Funcionario> equipe = trabalhando.stream().map(DiaEscala::getFuncionario).distinct()
                .sorted(Comparator.comparing(Funcionario::getNome, String.CASE_INSENSITIVE_ORDER)).toList();
        List<ResumoAlmocoDia> resumos = periodo.atDay(1).datesUntil(periodo.plusMonths(1).atDay(1))
                .map(data -> {
                    List<DiaEscala> noDia = trabalhando.stream().filter(dia -> dia.getData().equals(data)).toList();
                    int definidos = (int) noDia.stream()
                            .filter(dia -> dia.getAlmocoInicio() != null && dia.getAlmocoFim() != null).count();
                    String semana = data.getDayOfWeek().getDisplayName(TextStyle.SHORT, PORTUGUES);
                    return new ResumoAlmocoDia(data, semana, noDia.size(), definidos);
                }).toList();
        int definidos = resumos.stream().mapToInt(ResumoAlmocoDia::definidos).sum();
        return new VisualizacaoAlmocoMes(escala, equipe, resumos, trabalhando.size(), definidos);
    }

    @Transactional(readOnly = true)
    public VisualizacaoAlmoco visualizarAlmoco(Long escalaId, LocalDate data) {
        Escala escala = obterEscala(escalaId);
        validarData(escala, data);
        List<DiaEscala> trabalhando = dias.buscarPorEscala(escalaId).stream()
                .filter(dia -> dia.getData().equals(data) && dia.getStatus() == StatusDia.T)
                .sorted(Comparator.comparing(DiaEscala::getTurno)
                        .thenComparing(dia -> dia.getFuncionario().getNome(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new VisualizacaoAlmoco(escala, data, trabalhando);
    }

    @Transactional
    public int aplicarAlmocoEmLote(Long escalaId, Long funcionarioId, LocalDate inicio, LocalDate fim,
                                   String horaInicio, String horaFim, boolean substituir) {
        Escala escala = obterEscala(escalaId);
        YearMonth mes = YearMonth.of(escala.getAno(), escala.getMes());
        if (funcionarioId == null || inicio == null || fim == null || inicio.isAfter(fim)
                || !YearMonth.from(inicio).equals(mes) || !YearMonth.from(fim).equals(mes)) {
            throw new IllegalArgumentException("Escolha uma pessoa e um período válido dentro do mês da escala.");
        }
        LocalTime comeco;
        LocalTime termino;
        try {
            comeco = LocalTime.parse(horaInicio);
            termino = LocalTime.parse(horaFim);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new IllegalArgumentException("Informe início e fim válidos para o almoço.");
        }

        List<DiaEscala> periodo = dias.buscarPorEscala(escalaId).stream()
                .filter(dia -> dia.getFuncionario().getId().equals(funcionarioId)
                        && !dia.getData().isBefore(inicio) && !dia.getData().isAfter(fim))
                .toList();
        if (periodo.isEmpty()) {
            throw new IllegalArgumentException("Funcionário não encontrado nesta escala.");
        }
        List<DiaEscala> trabalhando = periodo.stream().filter(dia -> dia.getStatus() == StatusDia.T).toList();
        if (trabalhando.isEmpty()) {
            throw new IllegalArgumentException("A pessoa não trabalha em nenhum dia desse período.");
        }
        List<DiaEscala> preencher = trabalhando.stream()
                .filter(dia -> substituir || (dia.getAlmocoInicio() == null && dia.getAlmocoFim() == null))
                .toList();
        if (preencher.isEmpty()) {
            throw new IllegalArgumentException("Não há dias trabalhados sem almoço nesse período. Marque a opção de substituir para alterar horários existentes.");
        }
        for (DiaEscala dia : preencher) {
            try {
                validarAlmocoNoTurno(dia, comeco, termino);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Dia " + dia.getData().getDayOfMonth() + ": " + ex.getMessage());
            }
        }
        preencher.forEach(dia -> dia.definirAlmoco(comeco, termino));
        return preencher.size();
    }

    @Transactional
    public void salvarAlmoco(Long escalaId, LocalDate data, Map<String, String> campos) {
        Escala escala = obterEscala(escalaId);
        validarData(escala, data);
        List<DiaEscala> trabalhando = dias.buscarPorEscala(escalaId).stream()
                .filter(dia -> dia.getData().equals(data) && dia.getStatus() == StatusDia.T)
                .toList();
        Map<DiaEscala, LocalTime[]> horarios = new LinkedHashMap<>();
        for (DiaEscala dia : trabalhando) {
            String inicio = campos.get("inicio_" + dia.getId());
            String fim = campos.get("fim_" + dia.getId());
            if (inicio == null || fim == null) {
                throw new IllegalArgumentException("O formulário de almoço está incompleto. Reabra o dia e tente novamente.");
            }
            if (inicio.isBlank() && fim.isBlank()) {
                horarios.put(dia, new LocalTime[] {null, null});
            } else if (inicio.isBlank() || fim.isBlank()) {
                throw new IllegalArgumentException("Informe início e fim do almoço para " + dia.getFuncionario().getNome() + ".");
            } else {
                try {
                    LocalTime horaInicio = LocalTime.parse(inicio);
                    LocalTime horaFim = LocalTime.parse(fim);
                    validarAlmocoNoTurno(dia, horaInicio, horaFim);
                    horarios.put(dia, new LocalTime[] {horaInicio, horaFim});
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("Horário de almoço inválido para " + dia.getFuncionario().getNome() + ".");
                }
            }
        }
        horarios.forEach((dia, horas) -> dia.definirAlmoco(horas[0], horas[1]));
    }

    private static void validarAlmocoNoTurno(DiaEscala dia, LocalTime inicio, LocalTime fim) {
        int inicioTurno = switch (dia.getTurno()) {
            case MANHA -> 6 * 60;
            case TARDE -> 14 * 60;
            case NOITE -> 22 * 60;
        };
        int fimTurno = inicioTurno + 8 * 60;
        int inicioAlmoco = minutosNoTurno(dia.getTurno(), inicio);
        int fimAlmoco = minutosNoTurno(dia.getTurno(), fim);
        if (inicioAlmoco < inicioTurno || fimAlmoco > fimTurno || fimAlmoco <= inicioAlmoco) {
            throw new IllegalArgumentException("O almoço de " + dia.getFuncionario().getNome()
                    + " deve começar e terminar dentro do turno de trabalho.");
        }
    }

    private static int minutosNoTurno(Turno turno, LocalTime horario) {
        int minutos = horario.getHour() * 60 + horario.getMinute();
        return turno == Turno.NOITE && minutos < 6 * 60 + 1 ? minutos + 24 * 60 : minutos;
    }

    private Escala obterEscala(Long id) {
        return escalas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escala não encontrada"));
    }

    private static void validarData(Escala escala, LocalDate data) {
        if (data == null || data.getYear() != escala.getAno() || data.getMonthValue() != escala.getMes()) {
            throw new IllegalArgumentException("O dia escolhido não pertence a esta escala.");
        }
    }

    @Transactional(readOnly = true)
    public VisualizacaoEscala visualizar(Long id) {
        Escala escala = obterEscala(id);
        YearMonth periodo = YearMonth.of(escala.getAno(), escala.getMes());
        List<DiaCabecalho> cabecalho = periodo.atDay(1).datesUntil(periodo.plusMonths(1).atDay(1))
                .map(data -> new DiaCabecalho(data, data.getDayOfWeek().getDisplayName(TextStyle.SHORT, PORTUGUES).toUpperCase(PORTUGUES)))
                .toList();

        Map<Long, LinhaEmConstrucao> porFuncionario = new LinkedHashMap<>();
        for (DiaEscala dia : dias.buscarPorEscala(id)) {
            Funcionario funcionario = dia.getFuncionario();
            LinhaEmConstrucao linha = porFuncionario.computeIfAbsent(funcionario.getId(),
                    chave -> new LinhaEmConstrucao(funcionario.getId(), funcionario.getNome(), funcionario.getFuncao()));
            linha.dias.add(dia);
        }
        List<LinhaEscala> linhas = porFuncionario.values().stream()
                .map(linha -> new LinhaEscala(linha.funcionarioId, linha.nome, linha.funcao, Collections.unmodifiableList(new ArrayList<>(linha.dias))))
                .sorted(Comparator.comparing((LinhaEscala linha) -> linha.funcao() != Funcao.CAIXA)
                        .thenComparing(LinhaEscala::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<Aviso> avisos = avisos(linhas);
        Map<LocalDate, ResumoDia> resumos = new LinkedHashMap<>();
        for (DiaCabecalho dia : cabecalho) {
            StringBuilder texto = new StringBuilder();
            StringBuilder detalhes = new StringBuilder();
            for (Turno turno : Turno.values()) {
                long total = linhas.stream().flatMap(linha -> linha.dias().stream())
                        .filter(celula -> celula.getData().equals(dia.data()) && celula.getStatus() == StatusDia.T
                                && celula.getTurno() == turno).count();
                long caixas = linhas.stream().flatMap(linha -> linha.dias().stream())
                        .filter(celula -> celula.getData().equals(dia.data()) && celula.getStatus() == StatusDia.T
                                && celula.getTurno() == turno && celula.getLocal() == LocalTrabalho.CAIXA).count();
                if (!texto.isEmpty()) {
                    texto.append("\n");
                    detalhes.append(" · ");
                }
                texto.append(switch (turno) { case MANHA -> "M"; case TARDE -> "T"; case NOITE -> "N"; })
                        .append(total).append("/5 C").append(caixas);
                detalhes.append(nomeTurno(turno)).append(": ").append(total).append("/5, caixa ").append(caixas);
            }
            int quantidade = (int) avisos.stream().filter(aviso -> aviso.data().equals(dia.data())).count();
            resumos.put(dia.data(), new ResumoDia(texto.toString(), detalhes.toString(), quantidade));
        }
        return new VisualizacaoEscala(escala, cabecalho, linhas,
                avisos.stream().map(Aviso::texto).toList(), resumos);
    }

    private static class LinhaEmConstrucao {
        private final Long funcionarioId;
        private final String nome;
        private final Funcao funcao;
        private final List<DiaEscala> dias = new ArrayList<>();

        private LinhaEmConstrucao(Long funcionarioId, String nome, Funcao funcao) {
            this.funcionarioId = funcionarioId;
            this.nome = nome;
            this.funcao = funcao;
        }
    }

    private record Aviso(LocalDate data, String texto) {}

    private List<Aviso> avisos(List<LinhaEscala> linhas) {
        List<Aviso> avisos = new ArrayList<>();

        for (LinhaEscala linha : linhas) {
            for (DiaEscala dia : linha.dias()) {
                if (dia.getFuncionario().getFolgaFixa() == dia.getData().getDayOfWeek()
                        && dia.getStatus() == StatusDia.T) {
                    avisos.add(new Aviso(dia.getData(), "Dia " + dia.getData().getDayOfMonth()
                            + ": conferir folga fixa de " + linha.nome() + "."));
                }
            }
        }

        for (LinhaEscala titular : linhas) {
            for (DiaEscala dia : titular.dias()) {
                if (dia.getFuncionario().getFuncao() != Funcao.CAIXA
                        || (dia.getStatus() != StatusDia.F && dia.getStatus() != StatusDia.V)) continue;
                List<DiaEscala> substitutos = linhas.stream().flatMap(linha -> linha.dias().stream())
                        .filter(outro -> outro.getFuncionario().getTitularCoberto() != null
                                && outro.getFuncionario().getTitularCoberto().getId().equals(dia.getFuncionario().getId())
                                && outro.getData().equals(dia.getData())).toList();
                if (substitutos.isEmpty()) {
                    avisos.add(new Aviso(dia.getData(), "Dia " + dia.getData().getDayOfMonth()
                            + ": falta definir quem cobre o caixa de " + titular.nome() + "."));
                } else if (substitutos.stream().noneMatch(outro -> outro.getStatus() == StatusDia.T
                        && outro.getLocal() == LocalTrabalho.CAIXA)) {
                    avisos.add(new Aviso(dia.getData(), "Dia " + dia.getData().getDayOfMonth()
                            + ": conferir cobertura do caixa de " + titular.nome() + " por "
                            + substitutos.getFirst().getFuncionario().getNome() + "."));
                }
            }
        }

        if (!linhas.isEmpty()) {
            for (DiaEscala referencia : linhas.getFirst().dias()) {
                boolean iniciado = linhas.stream().flatMap(linha -> linha.dias().stream())
                        .anyMatch(dia -> dia.getData().equals(referencia.getData()) && dia.getStatus() != null);
                for (Turno turno : Turno.values()) {
                    List<DiaEscala> noTurno = linhas.stream().flatMap(linha -> linha.dias().stream())
                            .filter(dia -> dia.getData().equals(referencia.getData()) && dia.getStatus() == StatusDia.T && dia.getTurno() == turno)
                            .toList();
                    long caixas = noTurno.stream().filter(dia -> dia.getLocal() == LocalTrabalho.CAIXA).count();
                    if (noTurno.size() > 5) avisos.add(new Aviso(referencia.getData(), "Dia " + referencia.getData().getDayOfMonth() + ": mais de 5 pessoas no " + nomeTurno(turno) + "."));
                    if (caixas > 1) avisos.add(new Aviso(referencia.getData(), "Dia " + referencia.getData().getDayOfMonth() + ": mais de 1 pessoa no caixa no " + nomeTurno(turno) + "."));
                    if (iniciado && noTurno.size() < 5) avisos.add(new Aviso(referencia.getData(), "Dia " + referencia.getData().getDayOfMonth() + ": faltam " + (5 - noTurno.size()) + " pessoa(s) no " + nomeTurno(turno) + "."));
                    if (iniciado && caixas == 0) avisos.add(new Aviso(referencia.getData(), "Dia " + referencia.getData().getDayOfMonth() + ": falta uma pessoa no caixa no " + nomeTurno(turno) + "."));
                }
            }
        }
        return avisos;
    }

    private static String nomeTurno(Turno turno) {
        return switch (turno) {
            case MANHA -> "1º turno";
            case TARDE -> "2º turno";
            case NOITE -> "3º turno";
        };
    }

}
