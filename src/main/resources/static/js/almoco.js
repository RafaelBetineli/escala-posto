document.querySelectorAll('.hora-almoco').forEach((input) => {
  input.addEventListener('input', () => {
    input.closest('.campo-hora').querySelector('.valor-impressao').textContent = input.value || '—';
  });
});
