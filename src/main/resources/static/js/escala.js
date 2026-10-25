const formEscala = document.querySelector('#form-escala');
const seletores = [...document.querySelectorAll('.marcacao')];
const filtroSemana = document.querySelector('#filtro-semana');
const filtroTurno = document.querySelector('#filtro-turno');
const estadoEdicao = document.querySelector('#estado-edicao');
const mensagemInteracao = document.querySelector('#mensagem-interacao');
const painelCopia = document.querySelector('#painel-copia');
const destinoCopia = document.querySelector('#copiar-destino');
const confirmarCopia = document.querySelector('#confirmar-copia');
const aplicarLote = document.querySelector('#aplicar-lote');
const ultimoDia = Math.max(...seletores.map((item) => Number(item.closest('[data-dia]').dataset.dia)));
let alterado = false;
let copiaConfirmada = false;
let loteConfirmado = false;

function mostrarMensagem(texto, tom = 'aviso', acoes = []) {
  mensagemInteracao.replaceChildren(document.createTextNode(texto));
  mensagemInteracao.dataset.tom = tom;
  mensagemInteracao.hidden = false;
  acoes.forEach(({ titulo, executar, secundario }) => {
    const botao = document.createElement('button');
    botao.type = 'button';
    botao.textContent = titulo;
    if (secundario) botao.className = 'botao-neutro';
    botao.addEventListener('click', executar);
    mensagemInteracao.append(botao);
  });
  mensagemInteracao.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
}

function limparMensagem() {
  mensagemInteracao.hidden = true;
  mensagemInteracao.replaceChildren();
}

function marcarAlterado() {
  alterado = true;
  document.body.classList.add('alterado');
  estadoEdicao.textContent = 'Alterações não salvas. Salve a escala antes de sair ou imprimir.';
  document.querySelectorAll('.contagem-avisos').forEach((aviso) => {
    aviso.textContent = 'Avisos salvos';
  });
}

function atualizarResumo(dia) {
  const contagem = { '06': 0, '14': 0, '22': 0 };
  const caixas = { '06': 0, '14': 0, '22': 0 };
  document.querySelectorAll(`.celula[data-dia="${dia}"] .marcacao`).forEach((select) => {
    const valor = select.value;
    const turno = valor.slice(-2);
    if (Object.hasOwn(contagem, turno)) {
      contagem[turno]++;
      if (valor.startsWith('CAIXA')) caixas[turno]++;
    }
  });
  const resumo = document.querySelector(`th[data-dia="${dia}"] .resumo-turnos`);
  resumo.textContent = `M${contagem['06']}/5 C${caixas['06']}\nT${contagem['14']}/5 C${caixas['14']}\nN${contagem['22']}/5 C${caixas['22']}`;
  resumo.title = `Manhã: ${contagem['06']}/5, caixa ${caixas['06']} · Tarde: ${contagem['14']}/5, caixa ${caixas['14']} · Noite: ${contagem['22']}/5, caixa ${caixas['22']}`;
}

function atualizarCelula(select, filtrarDepois = true) {
  const option = select.selectedOptions[0];
  const cell = select.closest('.celula');
  cell.classList.remove('vazio', 'folga', 'ferias', 'pista', 'caixa');
  cell.classList.add(option.dataset.classe);
  cell.querySelector('.valor-impressao').textContent = option.dataset.resumo;
  atualizarResumo(Number(cell.dataset.dia));
  marcarAlterado();
  if (filtrarDepois) filtrar();
}

seletores.forEach((select) => select.addEventListener('change', () => atualizarCelula(select)));

for (let inicio = 1; inicio <= ultimoDia; inicio += 7) {
  const option = document.createElement('option');
  option.value = String(inicio);
  option.textContent = `Dias ${inicio} a ${Math.min(inicio + 6, ultimoDia)}`;
  filtroSemana.append(option);
  if (inicio > 1) destinoCopia.append(option.cloneNode(true));
}

function filtrar() {
  const inicio = filtroSemana.value === 'todos' ? 1 : Number(filtroSemana.value);
  const fim = filtroSemana.value === 'todos' ? ultimoDia : Math.min(inicio + 6, ultimoDia);
  document.querySelectorAll('[data-dia]').forEach((celula) => {
    const dia = Number(celula.dataset.dia);
    celula.hidden = dia < inicio || dia > fim;
  });
  document.querySelectorAll('tbody tr[data-funcionario]').forEach((linha) => {
    linha.hidden = filtroTurno.value !== 'todos' && ![...linha.querySelectorAll('.celula:not([hidden]) .marcacao')]
      .some((select) => select.value.endsWith(`_${filtroTurno.value}`));
  });
}

filtroSemana.addEventListener('change', filtrar);
filtroTurno.addEventListener('change', filtrar);
filtroSemana.value = '1';
filtrar();

function confirmarSubstituicao(tipo, mensagem, botao, rotulo) {
  mostrarMensagem(mensagem, 'aviso');
  botao.textContent = rotulo;
  if (tipo === 'copia') copiaConfirmada = true;
  else loteConfirmado = true;
}

['#lote-funcionario', '#lote-inicio', '#lote-fim', '#lote-marcacao'].forEach((id) => {
  document.querySelector(id).addEventListener('change', () => {
    loteConfirmado = false;
    aplicarLote.textContent = 'Aplicar no intervalo';
  });
});

aplicarLote.addEventListener('click', () => {
  const funcionario = document.querySelector('#lote-funcionario').value;
  const inicio = Number(document.querySelector('#lote-inicio').value);
  const fim = Number(document.querySelector('#lote-fim').value);
  const marcacao = document.querySelector('#lote-marcacao').value;
  if (!funcionario || !Number.isInteger(inicio) || !Number.isInteger(fim)
      || inicio < 1 || fim > ultimoDia || inicio > fim) {
    mostrarMensagem('Escolha um funcionário e um intervalo válido dentro do mês.', 'erro');
    return;
  }
  const linha = document.querySelector(`tr[data-funcionario="${funcionario}"]`);
  const destinos = [...linha.querySelectorAll('.celula')].filter((celula) =>
    Number(celula.dataset.dia) >= inicio && Number(celula.dataset.dia) <= fim);
  if (!loteConfirmado && destinos.some((celula) => celula.querySelector('select').value !== 'VAZIO')) {
    confirmarSubstituicao('lote', 'O intervalo já tem marcações. Confira os dias e clique novamente para substituí-las.', aplicarLote, 'Substituir intervalo');
    return;
  }
  destinos.forEach((celula) => {
    const select = celula.querySelector('select');
    select.value = marcacao;
    atualizarCelula(select, false);
  });
  filtrar();
  loteConfirmado = false;
  aplicarLote.textContent = 'Aplicar no intervalo';
  mostrarMensagem(`Marcação aplicada a ${destinos.length} dia(s). Salve a escala para gravar.`, 'sucesso');
});

document.querySelector('#copiar-semana').addEventListener('click', () => {
  painelCopia.hidden = !painelCopia.hidden;
  document.querySelector('#copiar-semana').setAttribute('aria-expanded', String(!painelCopia.hidden));
  if (!painelCopia.hidden) {
    destinoCopia.value = filtroSemana.value !== 'todos' && Number(filtroSemana.value) > 1 ? filtroSemana.value : '8';
    destinoCopia.focus();
  }
  copiaConfirmada = false;
  confirmarCopia.textContent = 'Aplicar cópia';
  limparMensagem();
});

destinoCopia.addEventListener('change', () => {
  copiaConfirmada = false;
  confirmarCopia.textContent = 'Aplicar cópia';
  limparMensagem();
});

document.querySelector('#cancelar-copia').addEventListener('click', () => {
  painelCopia.hidden = true;
  document.querySelector('#copiar-semana').setAttribute('aria-expanded', 'false');
  copiaConfirmada = false;
  confirmarCopia.textContent = 'Aplicar cópia';
  limparMensagem();
});

confirmarCopia.addEventListener('click', () => {
  const inicio = Number(destinoCopia.value);
  const pares = [];
  document.querySelectorAll('tbody tr[data-funcionario]').forEach((linha) => {
    for (let dia = inicio; dia <= Math.min(inicio + 6, ultimoDia); dia++) {
      const origem = linha.querySelector(`.celula[data-dia="${dia - 7}"] select`);
      const destino = linha.querySelector(`.celula[data-dia="${dia}"] select`);
      if (origem.value !== 'FERIAS' && destino.value !== 'FERIAS') pares.push([origem, destino]);
    }
  });
  if (!copiaConfirmada && pares.some(([, destino]) => destino.value !== 'VAZIO')) {
    confirmarSubstituicao('copia', 'Os dias de destino já têm marcações. Confira o período e clique novamente para substituí-las. Férias serão preservadas.', confirmarCopia, 'Substituir e copiar');
    return;
  }
  pares.forEach(([origem, destino]) => {
    destino.value = origem.value;
    atualizarCelula(destino, false);
  });
  filtroSemana.value = String(inicio);
  filtroTurno.value = 'todos';
  filtrar();
  painelCopia.hidden = true;
  document.querySelector('#copiar-semana').setAttribute('aria-expanded', 'false');
  copiaConfirmada = false;
  confirmarCopia.textContent = 'Aplicar cópia';
  mostrarMensagem(`Dias ${inicio} a ${Math.min(inicio + 6, ultimoDia)} preenchidos com os sete dias anteriores. Férias preservadas. Salve a escala.`, 'sucesso');
});

formEscala.addEventListener('submit', () => { alterado = false; document.body.classList.remove('alterado'); });
window.addEventListener('beforeunload', (event) => {
  if (alterado) {
    event.preventDefault();
    event.returnValue = '';
  }
});
document.querySelector('.imprimir').addEventListener('click', () => {
  if (alterado) {
    mostrarMensagem('Salve a escala antes de imprimir para que a cópia corresponda aos dados gravados.', 'erro');
    return;
  }
  window.print();
});

document.querySelectorAll('a[href]').forEach((link) => {
  link.addEventListener('click', (evento) => {
    if (!alterado || evento.button !== 0 || evento.ctrlKey || evento.metaKey || evento.shiftKey) return;
    evento.preventDefault();
    mostrarMensagem('Você tem alterações não salvas. Salve a escala antes de sair ou descarte as mudanças.', 'aviso', [
      { titulo: 'Salvar escala', executar: () => formEscala.requestSubmit() },
      { titulo: 'Descartar e sair', secundario: true, executar: () => { alterado = false; window.location.href = link.href; } }
    ]);
  });
});

document.querySelector('.ferias-painel form').addEventListener('submit', (evento) => {
  if (!alterado) return;
  evento.preventDefault();
  mostrarMensagem('Salve a escala antes de aplicar férias para não perder as alterações pendentes.', 'erro');
});
