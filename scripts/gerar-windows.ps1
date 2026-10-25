param([switch]$SemCompilar)

$ErrorActionPreference = 'Stop'
$raiz = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$javaHome = $env:JAVA_HOME
if ([string]::IsNullOrWhiteSpace($javaHome)) {
    throw 'Defina JAVA_HOME apontando para um JDK 21 antes de gerar o programa.'
}
$empacotador = Join-Path $javaHome 'bin\jpackage.exe'
if (-not (Test-Path -LiteralPath $empacotador)) {
    throw 'jpackage.exe não foi encontrado no JDK informado em JAVA_HOME.'
}

if (-not $SemCompilar) {
    & (Join-Path $raiz 'mvnw.cmd') package
    if ($LASTEXITCODE -ne 0) { throw 'A compilação falhou.' }
}

$jar = Join-Path $raiz 'target\escala-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Arquivo JAR não encontrado. Compile o projeto primeiro.' }

$numero = Get-Date -Format 'yyyyMMdd-HHmmss'
$pastaPacote = Join-Path $raiz "target\pacotes-windows\$numero"
$entrada = Join-Path $pastaPacote 'entrada'
$saida = Join-Path $pastaPacote 'saida'
New-Item -ItemType Directory -Path $entrada, $saida -Force | Out-Null
Copy-Item -LiteralPath $jar -Destination (Join-Path $entrada 'escala.jar')

& $empacotador --type app-image --name EscalasDoPosto --app-version 1.0.0 `
    --input $entrada --main-jar escala.jar `
    --main-class org.springframework.boot.loader.launch.JarLauncher `
    --java-options '-Descala.desktop=true' --dest $saida
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível gerar o programa Windows.' }

$aplicativo = Join-Path $saida 'EscalasDoPosto'
$zip = Join-Path $pastaPacote 'EscalasDoPosto-Windows.zip'
Compress-Archive -LiteralPath $aplicativo -DestinationPath $zip -CompressionLevel Optimal
Write-Host "Programa gerado: $zip"
Write-Host 'Descompacte o ZIP inteiro e abra EscalasDoPosto.exe. O banco de dados fica em %LOCALAPPDATA%\EscalasDoPosto\data.'
