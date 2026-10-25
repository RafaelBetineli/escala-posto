let alterado = false;
const mensagemInteracao = document.querySelector('#mensagem-interacao');
function mostrarMensagem(texto, acoes = [], tom = 'aviso') {
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
document.querySelectorAll('.hora-almoco').forEach((input) => {
  input.addEventListener('input', () => {
    input.closest('.campo-hora').querySelector('.valor-impressao').textContent = input.value || '—';
    alterado = true;
    document.body.classList.add('alterado');
    document.querySelector('#estado-edicao').textContent = 'Horários não salvos. Salve antes de mudar de dia ou imprimir.';
  });
});
document.querySelector('#form-almoco')?.addEventListener('submit', () => { alterado = false; document.body.classList.remove('alterado'); });
window.addEventListener('beforeunload', (event) => {
  if (alterado) {
    event.preventDefault();
    event.returnValue = '';
  }
});
document.querySelector('.imprimir').addEventListener('click', () => {
  if (alterado) {
    mostrarMensagem('Salve os horários antes de imprimir.');
    return;
  }
  window.print();
});
document.querySelectorAll('a[href]').forEach((link) => {
  link.addEventListener('click', (evento) => {
    if (!alterado || evento.button !== 0 || evento.ctrlKey || evento.metaKey || evento.shiftKey) return;
    evento.preventDefault();
    mostrarMensagem('Você tem horários não salvos. Salve antes de sair ou descarte as mudanças.', [
      { titulo: 'Salvar horários', executar: () => document.querySelector('#form-almoco').requestSubmit() },
      { titulo: 'Descartar e sair', secundario: true, executar: () => { alterado = false; window.location.href = link.href; } }
    ]);
  });
});
