package br.com.posto.escala.domain;

public enum Marcacao {
    VAZIO("—", "—", "vazio", null, null, null),
    FOLGA("F", "F", "folga", StatusDia.F, null, null),
    FERIAS("FER", "FER", "ferias", StatusDia.V, null, null),
    PISTA_06("06:00", "06 P", "pista", StatusDia.T, Turno.MANHA, LocalTrabalho.PISTA),
    CAIXA_06("CX 06", "06 CX", "caixa", StatusDia.T, Turno.MANHA, LocalTrabalho.CAIXA),
    PISTA_14("14:00", "14 P", "pista", StatusDia.T, Turno.TARDE, LocalTrabalho.PISTA),
    CAIXA_14("CX 14", "14 CX", "caixa", StatusDia.T, Turno.TARDE, LocalTrabalho.CAIXA),
    PISTA_22("22:00", "22 P", "pista", StatusDia.T, Turno.NOITE, LocalTrabalho.PISTA),
    CAIXA_22("CX 22", "22 CX", "caixa", StatusDia.T, Turno.NOITE, LocalTrabalho.CAIXA);

    private final String resumo;
    private final String descricao;
    private final String classe;
    private final StatusDia status;
    private final Turno turno;
    private final LocalTrabalho local;

    Marcacao(String resumo, String descricao, String classe, StatusDia status, Turno turno, LocalTrabalho local) {
        this.resumo = resumo;
        this.descricao = descricao;
        this.classe = classe;
        this.status = status;
        this.turno = turno;
        this.local = local;
    }

    public String getResumo() { return resumo; }
    public String getDescricao() { return descricao; }
    public String getClasse() { return classe; }
    public StatusDia getStatus() { return status; }
    public Turno getTurno() { return turno; }
    public LocalTrabalho getLocal() { return local; }
}
