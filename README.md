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

1. Os 17 funcionários informados são cadastrados ao iniciar, sem duplicar cadastros existentes. O status ativo/inativo anterior é preservado. Em `/funcionarios`, cadastre outros funcionários ou alterne o status.
2. Na página inicial, escolha mês e ano e crie a escala. Apenas funcionários ativos entram nela.
3. Na tabela mensal, marque cada célula e clique em **Salvar escala**. Os códigos são `—` (vazio), `F` (folga), `FER` (férias), `06/14/22 P` (pista) e `06/14/22 CX` (caixa). Os números são os horários de início dos turnos.
4. Para férias, abra **Marcar férias em um intervalo**, escolha funcionário e datas dentro do mês e aplique. Essa ação substitui o turno e limpa o almoço nesses dias. Férias que passam de um mês para outro devem ser lançadas nas duas escalas mensais.
5. Para almoço, clique no número do dia na tabela ou em **Horários de almoço**. Informe início e fim para cada pessoa marcada para trabalhar, e salve. A tela diária também pode ser impressa.
6. Consulte escalas anteriores pela página inicial. Para imprimir ou salvar como PDF, abra a escala salva e clique em **Imprimir / PDF**; no navegador, escolha a impressora ou a opção de PDF.

Cada combinação de mês e ano pode ter uma escala. Alterar o status de um funcionário depois da criação não remove os dias de escalas anteriores.

Marcar trabalho usa um turno e um local. Os horários dos turnos são 06h–14h, 14h–22h e 22h–06h. A impressão usa orientação paisagem e tamanho A3; ajuste o tamanho do papel no navegador se a impressora usar outro formato.

## Regras informadas

As planilhas de referência mostram uma visão mensal por funcionário e dia, com folgas destacadas, horários de início e cobertura do caixa em cores próprias. A outra visão detalha os horários de trabalho e almoço ao longo do dia. O sistema registra início e fim do almoço por pessoa e dia.

- Turnos: 06h–14h, 14h–22h e 22h–06h.
- Organização informada: 5 funcionários por período e 1 de folga; 1 no caixa e os demais na pista.
- Caixa: Gustavo, Franciele e Marcos Paulo. Coberturas: Joyce para Gustavo, Yago para Franciele e Marcos Arruda para Marcos Paulo.
- No período da manhã: Edson folga toda terça-feira; Pamela e Junior folgam toda quinta-feira.

As regras aparecem como lembretes e alguns avisos: trabalho em dia de folga fixa, cobertura de caixa ausente quando o titular folga ou está de férias, mais de cinco pessoas ou mais de uma pessoa no caixa no mesmo turno. Avisos são orientativos e aparecem depois de salvar; nenhuma marcação é alterada automaticamente.
