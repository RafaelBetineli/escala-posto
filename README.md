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

Para gerar o arquivo executável a partir do código, execute:

```powershell
.\mvnw.cmd package
java -jar target/escala-0.0.1-SNAPSHOT.jar
```

No Linux ou macOS, substitua `.\mvnw.cmd` por `./mvnw`. O arquivo compilado fica na pasta `target/` e não é incluído no Git. Para distribuí-lo pronto a outra pessoa, envie o `.jar` separadamente do código, por exemplo em um ZIP ou em uma versão publicada no GitHub.

### Programa Windows para cada computador

Com um JDK 21 instalado **apenas no computador usado para gerar o pacote**, execute `powershell -File scripts/gerar-windows.ps1`. O script cria um ZIP em `target/pacotes-windows/`. Copie esse ZIP para cada computador, descompacte a pasta inteira e abra `EscalasDoPosto.exe`. Os computadores que usam o programa não precisam instalar Java.

O programa inicia o sistema somente neste computador e abre a interface no navegador local. O ícone na área de notificação permite abrir a interface de novo ou sair. Os dados ficam em `%LOCALAPPDATA%\EscalasDoPosto\data`, fora da pasta do programa, para não serem perdidos ao atualizar o ZIP. Cada computador tem dados independentes; use **Baixar dados** e **Revisar importação** na página inicial para transferir uma cópia completa. A importação substitui todos os dados do computador de destino após uma revisão e confirmação; não há sincronização automática entre computadores. Exporte uma cópia do destino antes de importar se quiser guardar seus dados atuais.

Ao executar pelo Maven ou pelo JAR, acesse http://localhost:8080. Nesse modo, o H2 salva os dados em `data/escala.mv.db` na pasta de execução. Ao atualizar o projeto, **mantenha a pasta `data`** para preservar as escalas já criadas. O executável Windows usa a pasta local indicada acima. Para levar os dados da versão antiga ao executável, exporte pelo sistema antigo e importe o arquivo no novo programa.

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
