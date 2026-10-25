const registrosEscala = document.querySelectorAll('.item-escala-registro');

registrosEscala.forEach((registro) => {
    const botao = registro.querySelector('.botao-excluir-escala');
    const confirmacao = registro.querySelector('.confirmacao-exclusao');
    const cancelar = registro.querySelector('.botao-cancelar-exclusao');

    botao.addEventListener('click', () => {
        const abrir = confirmacao.hidden;
        registrosEscala.forEach((outroRegistro) => {
            outroRegistro.querySelector('.confirmacao-exclusao').hidden = true;
            outroRegistro.querySelector('.botao-excluir-escala').setAttribute('aria-expanded', 'false');
        });
        confirmacao.hidden = !abrir;
        botao.setAttribute('aria-expanded', String(abrir));
        if (abrir) cancelar.focus();
    });

    cancelar.addEventListener('click', () => {
        confirmacao.hidden = true;
        botao.setAttribute('aria-expanded', 'false');
        botao.focus();
    });
});
