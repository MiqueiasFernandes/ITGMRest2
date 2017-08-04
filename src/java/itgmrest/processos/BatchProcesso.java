/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package itgmrest.processos;

import itgmrest.MainSingleton;
import itgmrest.ScriptBash;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Arrays.asList;

/**
 *
 * @author mfernandes
 */
public class BatchProcesso extends AbstractProcesso {

    private File output;
    private BufferedReader reader;
    private File error;
    private String script;
    private Status status;

    public BatchProcesso(Contexto contexto) {
        super(contexto);
        script = contexto.getToken() + ".R";
        info("iniciando processo batch para script: " + script);
        try {
            output = File.createTempFile("processo", "Output");
            debug("criado arquivo temporario de output: " + output.getAbsolutePath(), this, 33);
            error = File.createTempFile("processo", "Error");
            debug("criado arquivo temporario de error: " + error.getAbsolutePath(), this, 33);
        } catch (IOException ex) {
            error("impossivel criar arquivos temporarios: " + ex, this, 26);
        }
    }

    @Override
    public boolean preProcesso() {

        debug("redirecionando error e output stream", this, 42);
        getProcessBuilder()
                .redirectOutput(output)
                .redirectError(error);
        try {
            debug("criando arquivo de script", this, 48);
            FileWriter fw = new FileWriter(new File(getContexto().getDiretorioPath() + script));
            fw.write("write(Sys.getpid(), \"" + getContexto().getToken() + ".pid\")" + System.lineSeparator());
            for (String line : getContexto().getConteudoDecoded().split(System.lineSeparator())) {
                fw.write(line + System.lineSeparator());
            }
            fw.close();
            debug("arquivo de script criado com sucesso", this, 55);
        } catch (IOException ex) {
            error("impossivel criar arquivo de script: " + ex, this, 55);
            return false;
        }
        return true;
    }

    @Override
    public String[] getArgs() {
        ArrayList<String> params = new ArrayList<>(Arrays.asList(super.getArgs()));
        params.addAll(asList(new String[]{
            script,
            getContexto().getDiretorioPath(),
            getContexto().getToken()
        }));
        String[] args = params.toArray(new String[]{});
        warning("novos argumentos do processo: " + Arrays.toString(args));
        return args;
    }

    @Override
    public boolean posProcesso() {
        try {
            reader = new BufferedReader(new FileReader(output));
        } catch (FileNotFoundException ex) {
            error("impossivel criar reader para output: EX: " + ex, this, 92);
            reader = null;
        }

        new Thread(() -> {
            int cont = 0, time = 0;
            boolean continuar = true;
            while (getProcesso().isAlive() && continuar) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    error("impossivel aguardar por status: " + ex, this, 83);
                }
                time++;
                if (!isPROCESSO_SUSPENSO() && cont++ < MainSingleton.TIMEOUT_STATUS) {
                    cont = 0;
                    try {
                        debug("requerendo status de processo...", this, 103);
                        writeToStreamProcess("status");
                        Thread.sleep(500);
                        status = new Status(reader.readLine(), getContexto(), isPROCESSO_SUSPENSO());
                        debug("status de processo: " + status, this, 107);
                        if (!status.isIsAlive() && time > MainSingleton.TIMEOUT_PID) {
                            encerrarSessao("processo morto " + status);
                            continuar = false;
                        }
                        verificarUsoDeRecursos(status);
                    } catch (Exception ex) {
                        error("impossivel enviar requisição de status a processo. ex: " + ex, this, 114);
                        addFalha("impossivel enviar requisição de status a processo.");
                    }
                }
            }

            encerrarSessao("processo morreu. { BATCH/120}");

        }).start();
        return isAlive();
    }

    @Override
    public boolean encerrarSessao(String motivo) {
        info("encerrando sessão: " + motivo);
        try {
            debug("enviando EXIT a processo", this, 146);
            writeToStreamProcess("exit");
//            if (isAlive()) {
                debug("matando processo PIDS " + getPidJAVA() + " " + getPidR(), this, 150);
                ScriptBash.kill(getPidR());
                ScriptBash.kill(getPidJAVA());
                debug("destruindo processo", this, 148);
                getProcesso().destroy();
//            }
        } catch (Exception ex) {
            error("pode ter ocorrido uma falha ao tentar encerrar processo: " + ex, this, 100);
        } finally {
            if (!getContexto().isSalvar()) {
                limpar();
                debug("diretorio do processo foi excluido.", this, 182);
            } else {
                debug("os arquivos do processo foram mantidos no diretorio: " + getContexto().getDiretorioPath(), this, 181);
            }
        }
        return true;
    }

    @Override
    public Status getStatus() {
        if (isPROCESSO_SUSPENSO()) {
            return status = new Status(
                    "{\"pid\":\"" + status.getPid() + "\","
                    + "\"pidJava\":\"" + status.getPidJava() + "\","
                    + "\"cpu\":\"0\", \n"
                    + "\"memoria\":\"" + status.getMemoria() + "\","
                    + "\"disco\":\"" + status.getDisco() + "\","
                    + "\"isAlive\":true}", getContexto(), true);
        }
        return status;
    }

    @Override
    public void stop() {
        writeToStreamProcess("stop");
    }

}
