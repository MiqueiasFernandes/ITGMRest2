/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package itgmrest.processos;

import itgmrest.MainSingleton;
import itgmrest.ScriptBash;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.websocket.Session;
import javax.ws.rs.core.MultivaluedHashMap;

/**
 *
 * @author mfernandes
 */
public class LiveProcesso extends AbstractProcesso {

    private Scanner scIn, scErr;
    private MultivaluedHashMap<String, Session> sessoes;
    private Thread output, error, status;
    private Status statusTemp = null;
    private int timeStatus = MainSingleton.TIMEOUT_STATUS;

    public LiveProcesso(Contexto contexto) {
        super(contexto);
    }

    @Override
    public boolean preProcesso() {
        debug("inicializando liveSession...", this, 27);
        sessoes = new MultivaluedHashMap<>();
        return true;
    }

    @Override
    public boolean posProcesso() {

        scIn = new Scanner(getProcesso().getInputStream());
        debug("InputStream inicializado...", this, 30);
        scErr = new Scanner(getProcesso().getErrorStream());
        debug("ErrorStream inicializado...", this, 32);
        debug("OutputStream inicializado...", this, 34);

        new Thread(() -> {
            int cont = 0;
            while (getProcesso().isAlive() && statusTemp == null
                    && cont++ < MainSingleton.TIMEOUT_PID) {
                warning("aguardando por recuperação de PID");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    error("impossivel aguardar por PID " + ex, this, 57);
                }
            }

            if (statusTemp == null) {
                error("abortando por falta de PID em timeout esgotado/process deined", this, 63);
                encerrarSessao(" processo morreu ou timeout para PID ou status não obtido {LiveProcesso/65}");
            }

        }).start();

        start();

        return isAlive();
    }

    public void start() {
        ArrayList<String> buffer = new ArrayList<>();
        info("startando threads de leitura do processo...");
        output = new Thread(() -> {
            while (getProcesso().isAlive() && scIn.hasNextLine()) {
                String line = scIn.nextLine();
                debug("line from process: \"" + line + "\"", LiveProcesso.this, 53);
                try {
                    line = new String(java.util.Base64.getDecoder().decode(line));
                    debug("line from process decoded: \"" + line + "\"", LiveProcesso.this, 53);
                } catch (Exception ex) {
                    error("impossivel decodificar: " + line + " ex: " + ex, LiveProcesso.this, 84);
                    continue;
                }
                if (!line.isEmpty() && line.startsWith("[STATUS]")) {
                    Contexto contexto = getContexto();
                    setStatusTemp(new Status(line.substring(8), getContexto(), isPROCESSO_SUSPENSO()));
                    debug("status recebido: " + statusTemp, LiveProcesso.this, 81);
                    line = "[STATUS]" + statusTemp.getJson();
                    debug("novo status: " + line, LiveProcesso.this, 92);
                    if (buffer.size() > 0) {
                        buffer.add(line);
                        for (String string : buffer) {
                            sendLine(string);
                        }
                        buffer.clear();
                    } else {
                        sendLine(line);
                    }
                    continue;
                }
                if (statusTemp == null) {
                    buffer.add(line);
                    continue;
                }
                sendLine(line);
            }
            encerrarSessao("faltou linhas em output ou processo morreu {LiveProcesso/111}");
        });

        error = new Thread(() -> {
            while (getProcesso().isAlive() && scErr.hasNextLine()) {
                String line = scErr.nextLine();
                debug("line ERROR from process: \"" + line + "\"", this, 83);
                sendMessage("ERROR", line);
            }
            encerrarSessao("faltou linhas em error ou processo morreu {LiveProcesso/119}");
        });

        status = new Thread(() -> {
            while (getProcesso().isAlive()) {
                int cont = 0;

                while (getProcesso().isAlive() && (cont++ < timeStatus)) {
                    try {
                        Thread.sleep(1000);
                        //debug("aguardando para enviar status: " + cont + " de " + timeStatus, this, 123);
                    } catch (InterruptedException ex) {
                        error("impossivel aguardar por thread status " + ex, this, 124);
                    }
                }

                try {
                    if ((sessoes.get("STATUS") != null) && !sessoes.get("STATUS").isEmpty()) {
                        writeLine("status");
                        debug("requeriemento de status enviado", this, 130);
                    }
                } catch (IOException ex) {
                    error("impossivel enviar mensagem de status: " + ex, this, 132);
                }
            }
        });

        if (!getProcesso().isAlive()) {
            error("processo FALHOU.", this, 89);
            return;
        }

        output.start();
        error.start();
        status.start();

        info("threads output, error startadas com sucesso!");
    }

    void sendLine(String line) {

        if (line.startsWith("[")) {

            String server = line.substring(1, line.split("]")[0].length());
            String message = line.substring(2 + server.length());

            debug("line:" + line, this, 106);
            debug("server:" + server, this, 107);
            debug("message:" + message, this, 108);

            sendMessage(server, message);
        } else {
            error("linha desconhecida: \"" + line + "\"", this, 114);
        }
    }

    void sendMessage(String server, String message) {
        List<Session> tmpSessions;
        debug("enviando a " + server + ": " + message, this, 120);
        int cont = 100;
        do {
            if (cont-- < 0) {
                warning("nenhuma sessao " + server + " conecatada em " + 100 + " segundos.");
                if (getContexto().isSalvar()) {
                    while (getProcesso().isAlive() && sessoes.get(server).size() < 1) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            error("impossivel aguardar por"
                                    + " abertura de sessoes em "
                                    + server + " ex: " + ex, this, 138);
                        }
                    }

                } else {
                    error("não há sessoes para: " + server, this, 139);
                    encerrarSessao("não há sessoes para: " + server + "{LiveProcesso/196}");
                    return;
                }
            }
            tmpSessions = sessoes.get(server);
            try {
                if (tmpSessions == null || tmpSessions.isEmpty() || tmpSessions.size() < 1) {
                    Thread.sleep(1000);
                    warning("não ha sessões para: " + server);
                }
            } catch (InterruptedException ex) {
                error("impossivel aguardar por conexões: " + ex, this, 150);
            }
        } while (getProcesso().isAlive() && (tmpSessions == null || tmpSessions.isEmpty()));

        if (tmpSessions != null) {
            info("há " + tmpSessions.size() + " para " + server);

            tmpSessions.forEach((sessao) -> {
                if (sessao.isOpen()) {
                    sessao.getAsyncRemote().sendText(message);
                    debug("mensagem de processo [" + server + "] enviada para " + sessao.getId(), this, 159);
                }
            });
        } else {
            error("não há sessoes paraenviar: " + server, this, 186);
        }
    }

    public void addSession(String server, Session session) {
        info("adicionando sessao " + session.getId() + " de " + server
                + " contexto: " + getContexto());
        sessoes.add(server, session);
    }

    public void removeSession(String server, Session session) {
        info("removendo sessao " + session.getId() + " de " + server
                + " result: " + sessoes.get(server).remove(session));
        if (sessoes.get(server).isEmpty() && !getContexto().isSalvar()) {
            warning("encerrando sessão por ausencia de conexoes em " + server);
            encerrarSessao("ausencia de conexoes em " + server + " {LiveProcesso/236}");
        }
    }

    public void writeLine(String message) throws IOException {
        if (getProcesso().isAlive() && statusTemp != null) {
            info("enviando messagem: " + message + " para processo");
            writeToStreamProcess(message);
        } else {
            error("tentando escrever em processo morto ou não identificado [" + statusTemp + "]", this, 180);
        }
    }

    @Override
    public boolean encerrarSessao(String motivo) {
        warning("encerrando sessao por " + motivo);
        try {
            ScriptBash.kill(statusTemp.getPid());
            ScriptBash.kill(statusTemp.getPidJava());
        } catch (Exception ex) {
            error("impossivel interromper processos: " + ex, this, 249);
        }
        try {
            this.status.interrupt();
        } catch (Exception ex) {
            error("impossivel interromper thread status: " + ex, this, 258);
        }
        try {
            for (List<Session> sess : sessoes.values()) {
                if (sess != null && !sess.isEmpty()) {
                    for (Session session : sess) {
                        try {
                            session.close();
                        } catch (Exception ex) {
                            error("impossivel fechar sessão " + session.getId()
                                    + " para abortar: " + ex, this, 224);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            error("impossivel fechar sessoes: " + ex, this, 272);
        }

        try {
            writeLine("stop");
            writeLine("stop");
        } catch (IOException ex) {
            error("impossivel enviar mensagem de stop " + ex, this, 236);
        }

        try {
            warning("interrompendo thread output");
            output.interrupt();
            warning("interrompendo thread error");
            error.interrupt();
            warning("interrompendo thread processo");
            getProcesso().destroy();
            getProcesso().destroyForcibly();
        } catch (Exception ex) {
            error("impossivel interromper threads: " + ex, this, 272);
        }

        if (!getContexto().isSalvar()) {
            limpar();
        }
        return true;
    }

    public void setStatusTemp(Status statusTemp) {
        this.statusTemp = statusTemp;
        verificarUsoDeRecursos(statusTemp);
    }

    public void setStatusTime(String message) {
        debug("requerendo de canal [STATUS] " + message, this, 337);
        switch (message) {
            case "stop":
                stop();
                return;
            case "suspend":
                suspend();
                return;
            case "resume":
                resume();
                return;
            case "status": {
                try {
                    writeLine(message);
                } catch (IOException ex) {
                    error("impossivel obter status " + ex, this, 287);
                }
            }
            break;
            default:
                int time = Integer.parseInt(message);
                if (time < 1 || time > MainSingleton.TIMEOUT_STATUS) {
                    time = MainSingleton.TIMEOUT_STATUS;
                }
                timeStatus = time;
        }
    }

    @Override
    public Status getStatus() {
        return statusTemp;
    }

    @Override
    public void stop() {
        encerrarSessao("usuario requisitou stop {LiveProcesso/147}");
    }

}
