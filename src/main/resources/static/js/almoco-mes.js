const formLote = document.querySelector('#form-almoco-lote');
const mensagemLote = document.querySelector('#mensagem-lote');
const filtroPendentes = document.querySelector('#somente-pendentes');
const cartoesDia = [...document.querySelectorAll('.cartao-dia-almoco')];

formLote?.addEventListener('submit', (evento) => {
  const inicio = formLote.elements.inicio.value;
  const fim = formLote.elements.fim.value;
  if ([...formLote.querySelectorAll('[required]')].some((campo) => !campo.checkValidity())
      || inicio > fim || formLote.elements.horaInicio.value === formLote.elements.horaFim.value) {
    evento.preventDefault();
    mensagemLote.textContent = 'Escolha uma pessoa, um período válido e horários diferentes para início e fim.';
    mensagemLote.dataset.tom = 'erro';
    mensagemLote.hidden = false;
    mensagemLote.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  }
});

filtroPendentes.addEventListener('change', () => {
  cartoesDia.forEach((cartao) => {
    cartao.hidden = filtroPendentes.checked && Number(cartao.dataset.pendentes) === 0;
  });
  document.querySelector('#nenhum-dia-pendente').hidden = !filtroPendentes.checked
    || cartoesDia.some((cartao) => !cartao.hidden);
});
