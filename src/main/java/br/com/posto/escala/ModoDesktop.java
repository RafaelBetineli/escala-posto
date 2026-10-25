package br.com.posto.escala;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

final class ModoDesktop {
    private static final int ESPERA_MS = 15_000;
    private static Path enderecoArquivo;
    private static FileChannel canal;
    private static FileLock trava;

    private ModoDesktop() {}

    static boolean preparar() {
        try {
            String local = System.getenv("LOCALAPPDATA");
            Path pasta = Path.of(local == null || local.isBlank()
                    ? Path.of(System.getProperty("user.home"), "AppData", "Local").toString() : local,
                    "EscalasDoPosto");
            Files.createDirectories(pasta.resolve("data"));
            Files.createDirectories(pasta.resolve("logs"));
            enderecoArquivo = pasta.resolve("endereco-local.txt");
            canal = FileChannel.open(pasta.resolve("aplicacao.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            try {
                trava = canal.tryLock();
            } catch (OverlappingFileLockException ex) {
                trava = null;
            }
            if (trava == null) {
                canal.close();
                abrirInstanciaExistente();
                return false;
            }
            Files.deleteIfExists(enderecoArquivo);
            System.setProperty("spring.datasource.url", "jdbc:h2:file:"
                    + pasta.resolve("data").resolve("escala").toString().replace('\\', '/')
                    + ";DB_CLOSE_ON_EXIT=FALSE");
            System.setProperty("server.address", "127.0.0.1");
            System.setProperty("server.port", "0");
            System.setProperty("logging.file.name", pasta.resolve("logs").resolve("aplicacao.log").toString());
            System.setProperty("spring.main.banner-mode", "off");
            return true;
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível preparar os dados locais do programa.", ex);
        }
    }

    static void iniciar(ConfigurableApplicationContext contexto) {
        int porta = ((WebServerApplicationContext) contexto).getWebServer().getPort();
        String endereco = "http://127.0.0.1:" + porta + "/";
        try {
            Files.writeString(enderecoArquivo, endereco);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível registrar o endereço local do programa.", ex);
        }
        instalarIcone(contexto, endereco);
        abrirNavegador(endereco);
    }

    private static void abrirInstanciaExistente() throws IOException {
        long fim = System.currentTimeMillis() + ESPERA_MS;
        while (!Files.exists(enderecoArquivo) && System.currentTimeMillis() < fim) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (Files.exists(enderecoArquivo)) abrirNavegador(Files.readString(enderecoArquivo).trim());
    }

    private static void abrirNavegador(String endereco) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(endereco));
        } catch (Exception ex) {
            System.err.println("Abra o programa em " + endereco);
        }
    }

    private static void instalarIcone(ConfigurableApplicationContext contexto, String endereco) {
        if (!SystemTray.isSupported()) return;
        try {
            PopupMenu menu = new PopupMenu();
            MenuItem abrir = new MenuItem("Abrir Escalas do Posto");
            abrir.addActionListener(evento -> abrirNavegador(endereco));
            MenuItem sair = new MenuItem("Sair");
            sair.addActionListener(evento -> {
                contexto.close();
                System.exit(0);
            });
            menu.add(abrir);
            menu.addSeparator();
            menu.add(sair);
            TrayIcon icone = new TrayIcon(criarIcone(), "Escalas do Posto", menu);
            icone.setImageAutoSize(true);
            icone.addActionListener(evento -> abrirNavegador(endereco));
            SystemTray.getSystemTray().add(icone);
        } catch (Exception ex) {
            System.err.println("Não foi possível mostrar o ícone da área de notificação: " + ex.getMessage());
        }
    }

    private static BufferedImage criarIcone() {
        BufferedImage imagem = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D grafico = imagem.createGraphics();
        grafico.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        grafico.setColor(new Color(7, 107, 71));
        grafico.fillRoundRect(0, 0, 32, 32, 8, 8);
        grafico.setColor(Color.WHITE);
        grafico.setFont(new Font("Segoe UI", Font.BOLD, 22));
        grafico.drawString("E", 9, 24);
        grafico.dispose();
        return imagem;
    }
}
