document.querySelectorAll('.marcacao').forEach((select) => {
  select.addEventListener('change', () => {
    const option = select.selectedOptions[0];
    const cell = select.closest('.celula');
    cell.classList.remove('vazio', 'folga', 'ferias', 'pista', 'caixa');
    cell.classList.add(option.dataset.classe);
    cell.querySelector('.valor-impressao').textContent = option.dataset.resumo;
  });
});
