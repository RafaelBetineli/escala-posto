# Escalas do posto

Aplicação monolítica em Java 21, Spring Boot, Thymeleaf e H2 para criar e imprimir escalas mensais manuais.

## Executar

Instale um **JDK 21** (não apenas o ambiente de execução Java). O Maven é baixado automaticamente pelo wrapper na primeira execução; é necessária conexão à internet nessa primeira vez.

No PowerShell do Windows, dentro da pasta do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```bash
./mvnw spring-boot:run
```

O ZIP de entrega também traz `escala-posto.jar` já compilado. Com Java 21 instalado, é possível iniciar sem Maven nem compilador:

```bash
java -jar escala-posto.jar
```

Acesse http://localhost:8080. O H2 salva os dados em `data/escala.mv.db` na pasta de execução. Ao atualizar o projeto, **mantenha a pasta `data`** para preservar as escalas já criadas. Faça uma cópia dessa pasta com o sistema fechado antes de mover a instalação para outro computador.

## Fluxo disponível

1. Em uma instalação nova, os 17 funcionários informados são cadastrados ao iniciar. Em `/funcionarios`, cadastre outros funcionários, altere nome, função, folga fixa e quem cobre um titular no caixa, ou alterne o status. Cadastros existentes não são restaurados automaticamente ao reiniciar.
2. Na página inicial, escolha mês e ano e crie a escala. Apenas funcionários ativos entram nela.
3. Na tabela mensal, marque cada célula e clique em **Salvar escala**. Os códigos são `—` (vazio), `F` (folga), `FER` (férias), `06/14/22 P` (pista) e `06/14/22 CX` (caixa). Os números são os horários de início dos turnos. O filtro pode mostrar blocos de sete dias ou apenas pessoas já marcadas em um turno. Para preencher mais rápido, escolha uma pessoa e um intervalo, ou clique em **Copiar 7 dias anteriores**, escolha os dias de destino e aplique. O destino independe do filtro de visualização. A cópia preserva as férias e não as propaga.
4. Para férias, abra **Marcar férias em um intervalo**, escolha funcionário e datas dentro do mês e aplique. Essa ação substitui o turno e limpa o almoço nesses dias. Férias que passam de um mês para outro devem ser lançadas nas duas escalas mensais.
5. Para planejar os almoços do mês, clique em **Horários de almoço**. A página mensal mostra o total preenchido e os dias pendentes. Em **Aplicar horário a um período**, escolha a pessoa, as datas e o horário para preencher de uma vez os dias em que ela trabalha. Folgas e férias ficam de fora; horários existentes são mantidos, a menos que você marque **Substituir horários já preenchidos**. Se o horário não couber em algum turno do período, nada é aplicado. Para revisar ou ajustar um dia específico, clique no seu cartão na página mensal ou no número do dia na escala. O terceiro turno permite que o almoço atravesse a meia-noite. A tela diária também pode ser impressa.
6. Consulte escalas anteriores pela página inicial. Para imprimir ou salvar como PDF, abra a escala salva e clique em **Imprimir / PDF**; no navegador, escolha a impressora ou a opção de PDF. Se precisar remover um mês, clique em **Excluir** no histórico e confirme dentro da página. A exclusão apaga os turnos e horários de almoço daquele mês e não pode ser desfeita.

Cada combinação de mês e ano pode ter uma escala. Alterar o status de um funcionário depois da criação não remove os dias de escalas anteriores. As regras de folga e cobertura são consultadas do cadastro atual, inclusive ao revisar escalas antigas.

As alterações na escala e no almoço só são gravadas ao clicar em **Salvar**. Avisos e confirmações de edição aparecem dentro das páginas. A página avisa quando existem mudanças pendentes e impede imprimir uma versão diferente da que está gravada. Os contadores por dia são atualizados durante a edição; os avisos detalhados são recalculados depois de salvar.

Marcar trabalho usa um turno e um local. Os horários dos turnos são 06h–14h, 14h–22h e 22h–06h. A impressão usa orientação paisagem e tamanho A3; ajuste o tamanho do papel no navegador se a impressora usar outro formato.

## Regras informadas

As planilhas de referência mostram uma visão mensal por funcionário e dia, com folgas destacadas, horários de início e cobertura do caixa em cores próprias. A outra visão detalha os horários de trabalho e almoço ao longo do dia. O sistema registra início e fim do almoço por pessoa e dia.

- Turnos: 06h–14h, 14h–22h e 22h–06h.
- Organização informada: 5 funcionários por período e 1 de folga; 1 no caixa e os demais na pista.
- Caixa: Gustavo, Franciele e Marcos Paulo. Coberturas: Joyce para Gustavo, Yago para Franciele e Marcos Arruda para Marcos Paulo.
- No período da manhã: Edson folga toda terça-feira; Pamela e Junior folgam toda quinta-feira.

As regras aparecem como lembretes e avisos: trabalho em dia de folga fixa, cobertura de caixa ausente quando o titular folga ou está de férias, falta ou excesso de pessoas e falta ou excesso de caixas por turno. Os avisos de falta começam após a primeira marcação do dia, para não encher uma escala nova de alertas. Avisos são orientativos e aparecem depois de salvar; nenhuma marcação é alterada automaticamente.
