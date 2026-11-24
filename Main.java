import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

    // Interface de comunicação com a DLL da impressora Elgin.
    // Não altere os nomes dos métodos: eles precisam corresponder à DLL.
    public interface ImpressoraDLL extends Library {
        ImpressoraDLL INSTANCE = (ImpressoraDLL) Native.load(
                Paths.get("E1_Impressora01.dll").toAbsolutePath().toString(),
                ImpressoraDLL.class
        );

        int AbreConexaoImpressora(int tipo, String modelo, String conexao, int param);
        int FechaConexaoImpressora();
        int ImpressaoTexto(String dados, int posicao, int estilo, int tamanho);
        int Corte(int avanco);
        int ImpressaoQRCode(String dados, int tamanho, int nivelCorrecao);
        int ImpressaoCodigoBarras(int tipo, String dados, int altura, int largura, int HRI);
        int AvancaPapel(int linhas);
        int StatusImpressora(int param);
        int AbreGaveta(int pino, int ti, int tf);
        int SinalSonoro(int qtd, int tempoInicio, int tempoFim);
        int ImprimeXMLSAT(String dados, int param);
        int ImprimeXMLCancelamentoSAT(String dados, String assQRCode, int param);
    }

    private static final String USUARIO = "admin";
    private static final String SENHA = "1234";

    // Chave fornecida no projeto para o cancelamento do XML SAT.
    private static final String ASS_QRCODE =
            "Q5DLkpdRijIRGY6YSSNsTWK1TztHL1vD0V1Jc4spo/CEUqICEb9SFy82ym8EhBRZjbh3btsZhF+sjHqEMR159i4agru9x6KsepK/q0E2e5xlU5cv3m1woYfgHyOkWDNcSdMsS6bBh2Bpq6s89yJ9Q6qh/J8YHi306ce9Tqb/drKvN2XdE5noRSS32TAWuaQEVd7u+TrvXlOQsE3fHR1D5f1saUwQLPSdIv01NF6Ny7jZwjCwv1uNDgGZONJdlTJ6p0ccqnZvuE70aHOI09elpjEO6Cd+orI7XHHrFCwhFhAcbalc+ZfO5b/+vkyAHS6CYVFCDtYR9Hi5qgdk31v23w==";

    private static boolean conexaoAberta = false;
    private static int tipo = 1;
    private static String modelo = "i9";
    private static String conexao = "USB";
    private static int parametro = 0;

    private static final Scanner ler = new Scanner(System.in);

    public static String capturarEntrada(String mensagem) {
        System.out.print(mensagem);
        return ler.nextLine();
    }

    // Lê números sem deixar o Scanner preso quando o usuário digita letras.
    public static int lerInteiro(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String entrada = ler.nextLine();

            try {
                return Integer.parseInt(entrada.trim());
            } catch (NumberFormatException e) {
                System.out.println("Digite apenas um número inteiro.");
            }
        }
    }

    // Lê o conteúdo de um XML. O caminho pode ser absoluto ou relativo
    // à pasta de execução do projeto.
    public static String lerArquivo(String nome) {
        try {
            Path caminho = Paths.get(nome);
            if (!Files.exists(caminho)) {
                System.out.println("Arquivo não encontrado: " + caminho.toAbsolutePath());
                return null;
            }
            return new String(Files.readAllBytes(caminho), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Não foi possível ler o arquivo: " + e.getMessage());
            return null;
        }
    }

    public static boolean fazerLogin() {
        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            System.out.println("\n--- LOGIN ---");
            String usuario = capturarEntrada("Usuário: ");
            String senha = capturarEntrada("Senha: ");

            if (usuario.equals(USUARIO) && senha.equals(SENHA)) {
                return true;
            }

            int restantes = 3 - tentativa;
            if (restantes > 0) {
                System.out.println("Usuário ou senha incorretos. Tentativas restantes: " + restantes);
            }
        }

        System.out.println("Limite de tentativas atingido. Programa encerrado.");
        return false;
    }

    public static void exibirMenu() {
        System.out.println("\n==============================================");
        System.out.println("          MENU - IMPRESSORA ELGIN i9");
        System.out.println("==============================================");
        System.out.println("1  - Configurar conexão");
        System.out.println("2  - Abrir conexão");
        System.out.println("3  - Imprimir texto");
        System.out.println("4  - Imprimir QR Code");
        System.out.println("5  - Imprimir código de barras");
        System.out.println("6  - Imprimir cupom XML SAT");
        System.out.println("7  - Imprimir cancelamento XML SAT");
        System.out.println("8  - Abrir gaveta");
        System.out.println("9  - Sinal sonoro");
        System.out.println("10 - Avançar papel");
        System.out.println("0  - Sair");
        System.out.println("==============================================");
        System.out.println("Conexão: " + (conexaoAberta ? "ABERTA" : "FECHADA"));
    }

    public static void configurarConexao() {
        if (conexaoAberta) {
            System.out.println("Feche a conexão antes de alterar as configurações.");
            return;
        }

        System.out.println("\n--- CONFIGURAÇÃO DA IMPRESSORA ---");
        System.out.println("Tipo: 1 = USB, 2 = Serial, 3 = Rede, 4 = Bluetooth");
        tipo = lerInteiro("Tipo de conexão: ");

        modelo = capturarEntrada("Modelo da impressora (i8 ou i9): ").trim();
        conexao = capturarEntrada("Porta/conexão (para USB, digite USB): ").trim();
        parametro = lerInteiro("Parâmetro (para USB, digite 0): ");

        System.out.println("Configuração armazenada.");
    }

    public static void abrirConexao() {
        if (conexaoAberta) {
            System.out.println("A conexão já está aberta.");
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.AbreConexaoImpressora(
                tipo, modelo, conexao, parametro
        );

        if (retorno == 0) {
            conexaoAberta = true;
            System.out.println("Conexão aberta com sucesso.");
        } else {
            System.out.println("Erro ao abrir a conexão. Código: " + retorno);
        }
    }

    public static void fecharConexao() {
        if (!conexaoAberta) {
            System.out.println("A conexão já está fechada.");
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.FechaConexaoImpressora();
        if (retorno == 0) {
            conexaoAberta = false;
            System.out.println("Conexão fechada com sucesso.");
        } else {
            System.out.println("Erro ao fechar a conexão. Código: " + retorno);
        }
    }

    public static void imprimirTexto() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        String texto = capturarEntrada("Texto a imprimir: ");
        int copias = lerInteiro("Quantas cópias? ");

        if (copias <= 0) {
            System.out.println("A quantidade de cópias deve ser maior que zero.");
            return;
        }

        for (int i = 1; i <= copias; i++) {
            // ImpressaoTexto retorna quantidade de dados enviados:
            // somente valores negativos indicam erro.
            int retorno = ImpressoraDLL.INSTANCE.ImpressaoTexto(texto, 1, 4, 0);
            if (retorno >= 0) {
                System.out.println("Cópia " + i + " enviada para impressão.");
            } else {
                System.out.println("Erro ao imprimir a cópia " + i + ". Código: " + retorno);
            }
        }

        int corte = ImpressoraDLL.INSTANCE.Corte(5);
        if (corte != 0) {
            System.out.println("Erro ao cortar o papel. Código: " + corte);
        }
    }

    public static void imprimirQRCode() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        String dados = capturarEntrada("Conteúdo do QR Code: ");
        int retorno = ImpressoraDLL.INSTANCE.ImpressaoQRCode(dados, 6, 4);

        if (retorno == 0) {
            System.out.println("QR Code impresso com sucesso.");
            int corte = ImpressoraDLL.INSTANCE.Corte(5);
            if (corte != 0) {
                System.out.println("Erro ao cortar o papel. Código: " + corte);
            }
        } else {
            System.out.println("Erro ao imprimir QR Code. Código: " + retorno);
        }
    }

    public static void imprimirCodigoBarras() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        String dados = capturarEntrada(
                "Código de barras (CODE 128; use {A no início, conforme o padrão do trabalho): "
        );
        if (!dados.startsWith("{A")) {
            dados = "{A" + dados;
        }

        int retorno = ImpressoraDLL.INSTANCE.ImpressaoCodigoBarras(8, dados, 100, 2, 3);
        if (retorno == 0) {
            System.out.println("Código de barras impresso com sucesso.");
            int corte = ImpressoraDLL.INSTANCE.Corte(5);
            if (corte != 0) {
                System.out.println("Erro ao cortar o papel. Código: " + corte);
            }
        } else {
            System.out.println("Erro ao imprimir código de barras. Código: " + retorno);
        }
    }

    public static void imprimirXMLSAT() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        String nomeArquivo = capturarEntrada("Nome/caminho do XML (ex.: XMLSAT.xml): ");
        String xml = lerArquivo(nomeArquivo);
        if (xml == null) {
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.ImprimeXMLSAT(xml, 0);
        if (retorno == 0) {
            System.out.println("Cupom XML SAT impresso com sucesso.");
        } else {
            System.out.println("Erro ao imprimir XML SAT. Código: " + retorno);
        }
    }

    public static void imprimirXMLCancelamentoSAT() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        String nomeArquivo = capturarEntrada("Nome/caminho do XML de cancelamento (ex.: CANC_SAT.xml): ");
        String xml = lerArquivo(nomeArquivo);
        if (xml == null) {
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.ImprimeXMLCancelamentoSAT(xml, ASS_QRCODE, 0);
        if (retorno == 0) {
            System.out.println("Cancelamento XML SAT impresso com sucesso.");
        } else {
            System.out.println("Erro ao imprimir cancelamento XML SAT. Código: " + retorno);
        }
    }

    public static void abrirGaveta() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.AbreGaveta(1, 5, 10);
        if (retorno == 0) {
            System.out.println("Comando para abrir a gaveta enviado com sucesso.");
        } else {
            System.out.println("Erro ao abrir a gaveta. Código: " + retorno);
        }
    }

    public static void sinalSonoro() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        int quantidade = lerInteiro("Quantos bips? (1 a 63): ");
        if (quantidade < 1 || quantidade > 63) {
            System.out.println("A quantidade deve estar entre 1 e 63.");
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.SinalSonoro(quantidade, 50, 5);
        if (retorno == 0) {
            System.out.println("Sinal sonoro executado com sucesso.");
        } else {
            System.out.println("Erro no sinal sonoro. Código: " + retorno);
        }
    }

    public static void avancarPapel() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        int linhas = lerInteiro("Quantas linhas deseja avançar? ");
        if (linhas <= 0) {
            System.out.println("Informe uma quantidade de linhas maior que zero.");
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.AvancaPapel(linhas);
        if (retorno == 0) {
            System.out.println("Papel avançado em " + linhas + " linha(s).");
        } else {
            System.out.println("Erro ao avançar o papel. Código: " + retorno);
        }
    }

    public static void main(String[] args) {
        if (!fazerLogin()) {
            return;
        }

        System.out.println("Login realizado com sucesso.");

        int opcao = -1;
        while (opcao != 0) {
            exibirMenu();
            opcao = lerInteiro("Digite a opção desejada: ");

            if (opcao == 1) {
                configurarConexao();
            } else if (opcao == 2) {
                abrirConexao();
            } else if (opcao == 3) {
                imprimirTexto();
            } else if (opcao == 4) {
                imprimirQRCode();
            } else if (opcao == 5) {
                imprimirCodigoBarras();
            } else if (opcao == 6) {
                imprimirXMLSAT();
            } else if (opcao == 7) {
                imprimirXMLCancelamentoSAT();
            } else if (opcao == 8) {
                abrirGaveta();
            } else if (opcao == 9) {
                sinalSonoro();
            } else if (opcao == 10) {
                avancarPapel();
            } else if (opcao == 0) {
                fecharConexao();
                System.out.println("Programa encerrado.");
            } else {
                System.out.println("Opção inválida. Escolha uma opção do menu.");
            }
        }

        ler.close();
    }
}
