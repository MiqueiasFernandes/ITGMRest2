/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package itgmrest.processos;

import itgmrest.MainSingleton;
import itgmrest.ScriptBash;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Arrays.asList;

/**
 *
 * @author mfernandes
 */
public abstract class AbstractProcesso {

    private Contexto contexto;
    private ProcessBuilder processBuilder;
    private Process processo;
    private boolean PROCESSO_SUSPENSO = false, RUN = false;
    private int falhas = 0;
    private String pidR, pidJAVA;

    public AbstractProcesso(Contexto contexto) {
        this.contexto = contexto;
    }

    public Contexto getContexto() {
        return contexto;
    }

    public boolean setContexto(Contexto contexto) {
        if (RUN) {
            try {
                String pid = getStatus().getPid();
                if (pid != null && pid.matches("\\d+") && contexto.isValido()) {
                    ScriptBash.updatePID(pid, contexto.getCpuLimite(), contexto.getMemoriaLimite(), "echo\"alter context\"");
                }
                this.contexto = contexto;
                return true;
            } catch (Exception ex) {
                error("impossivel alterar contexto: " + ex, this, 51);
            }
        } else {
            warning("tentaiva de suspend antes de obter PID");
        }
        return false;
    }

    public boolean processar() {

        info("a startar processo: " + Arrays.toString(getArgs())
                + " em " + contexto.getDiretorioFile().getAbsolutePath());

        processBuilder
                = new ProcessBuilder(getArgs())
                        .directory(contexto.getDiretorioFile());

        if (!preProcesso()) {
            return false;
        }

        try {
            processo = processBuilder.start();
        } catch (IOException ex) {
            error("impossivel iniciar processo. " + ex, this, 52);
            return false;
        }

        if (!posProcesso()) {
            encerrarSessao("impossivel efetuar pos processo {AbstractProcesso/63}");
            return false;
        }

        setRecursos();

        info("processo startado...");

        return isAlive();

    }

    public void setRecursos() {
        new Thread(() -> {
            int cont = 0;
            debug("aguardando por PID...", this, 65);
            while (processo.isAlive()
                    && ((getStatus() == null)
                    || (getStatus().getPid() == null)
                    || (getStatus().getPidJava() == null))
                    && cont++ < MainSingleton.TIMEOUT_PID) {
                try {
                    warning("aguardando por PIDs...");
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    error("impossivel dormir thread recursos em pid" + ex, this, 71);
                }
            }

            if (!processo.isAlive()) {
                warning("processo morreu enquanto aguardava confirmação, lento?");
                return;
            }

            if (cont++ >= MainSingleton.TIMEOUT_PID) {
                error("impossivel configurar limites.", this, 87);
                encerrarSessao("impossivel configurar limites. {AbstreactProcesso/87}");
                return;
            }

            String pid = getStatus().getPid(), pidJAVA = getStatus().getPidJava();
            info("PID: " + pid);
            info("PID Java: " + pidJAVA);

            debug("aguardando por configuração de recursos de PID...", this, 74);
            ScriptBash.putPID(pid + "," + pidJAVA, contexto.getCpuLimite(), contexto.getMemoriaLimite());

            while (!ScriptBash.consultarPID(pid)
                    && processo.isAlive() && cont++ < MainSingleton.TIMEOUT_PID) {
                try {
                    warning("aguardando por configuração de PID...");
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    error("impossivel dormir thread recursos " + ex, this, 81);
                }
            }
            if (!processo.isAlive()) {
                warning("processo morreu enquanto aguardava confirmação, lento?");
                return;
            }

            if (cont++ >= MainSingleton.TIMEOUT_PID) {
                error("impossivel configurar limites.", this, 137);
                encerrarSessao("impossivel configurar limites {AbstreactProcesso/147}");
                return;
            }
            debug("configuraçao de recursos de PID efetuados com sucesso!", this, 84);
            RUN = true;
            this.pidR = getStatus().getPid();
            this.pidJAVA = getStatus().getPidJava();
        }).start();
        debug("thread config PID startada com sucesso!", this, 86);
    }

    public abstract boolean preProcesso();

    public String[] getArgs() {
        ArrayList<String> args = new ArrayList(asList(MainSingleton.ARGS_JRI.split(",")));
        args.addAll(asList(getContexto().getParametros()));
        return args.toArray(new String[]{});
    }

    public abstract boolean posProcesso();

    public abstract Status getStatus();

    public abstract boolean encerrarSessao(String motivo);

    public static MainSingleton getMainSingleton() {
        return MainSingleton.getInstance();
    }

    public ProcessBuilder getProcessBuilder() {
        return processBuilder;
    }

    public Process getProcesso() {
        return processo;
    }

    public void info(String text) {
        getMainSingleton().warning(contexto.getToken() + " - " + text);
    }

    public void warning(String text) {
        getMainSingleton().warning(contexto.getToken() + " - " + text);
    }

    public void error(String text, AbstractProcesso processo, int line) {
        getMainSingleton().error(contexto.getToken() + " - " + text, processo.getClass(), line);
    }

    public void debug(String text, AbstractProcesso processo, int line) {
        getMainSingleton().debug(contexto.getToken() + " - " + text, processo.getClass(), line);
    }

    @Override
    public String toString() {
        return "Processo token: " + contexto.getToken() + " - " + contexto.toString();
    }

    public boolean isAlive() {
        return processo != null && processo.isAlive();
    }

    public void suspend() {
        if (RUN) {
            PROCESSO_SUSPENSO = true;
            info("requisitado suspender processo.");
            ScriptBash.updatePID(
                    getStatus().getPid(),
                    getContexto().getCpuLimite(),
                    getContexto().getMemoriaLimite(),
                    "kill -STOP " + getStatus().getPid());
        }
    }

    public void resume() {
        PROCESSO_SUSPENSO = false;
        info("requisitado retomada de processo.");
        if (RUN) {
            ScriptBash.updatePID(
                    getStatus().getPid(),
                    getContexto().getCpuLimite(),
                    getContexto().getMemoriaLimite(),
                    "kill -CONT " + getStatus().getPid());
        }
    }

    public boolean isPROCESSO_SUSPENSO() {
        return PROCESSO_SUSPENSO;
    }

    public boolean isRUN() {
        return RUN;
    }

    public void limpar() {
        if (getContexto() != null) {
            info("limpando arquivos em " + getContexto().getDiretorioPath());
            String comando = "sudo rm " + getContexto().getDiretorioPath() + " -r -f";
            if (getStatus() != null) {
                ScriptBash.updatePID(getStatus().getPid(),
                        getContexto().getCpuLimite(),
                        getContexto().getMemoriaLimite(),
                        comando);
            } else {
                ScriptBash.updatePID("0", "0", "0", comando);
            }
        } else {
            warning("impossivel limpar: contexto indefinido.");
        }
    }

    public void writeToStreamProcess(String line) {
        try {
            processo.getOutputStream().write((line + System.lineSeparator()).getBytes());
            getProcesso().getOutputStream().flush();
        } catch (IOException ex) {
            error("impossivel enviar mensagem '" + line + "' a processo. ex: " + ex, this, 266);
        }
    }

    public Status verificarUsoDeRecursos(Status status) {
        try {
            float cpm = Float.parseFloat(getContexto().getCpuLimite());
            float cpu = Float.parseFloat(status.getCpu());
            if (cpu > cpm) {
                warning("cpu consome acima do permitido " + (cpu - cpm) + "%,"
                        + " usuario: " + getContexto().getUsuario());
            }
        } catch (Exception ex) {
            addFalha("impossivel comparar cpu LIM "
                    + getContexto().getCpuLimite()
                    + " com VAL " + status.getCpu() + " error: " + ex);
        }
        try {
            float mm = Float.parseFloat(getContexto().getMemoriaLimite());
            float mus = Float.parseFloat(status.getMemoria());
            if (mus > mm) {
                warning("memoria consome acima do permitido " + (mus - mm) + "MB,"
                        + " usuario: " + getContexto().getUsuario());
            }
        } catch (Exception ex) {
            addFalha("impossivel comparar memoria LIM "
                    + getContexto().getMemoriaLimite()
                    + " com VAL " + status.getMemoria() + " error: " + ex);
        }

        try {
            float dsm = Float.parseFloat(getContexto().getDiscoLimite());
            float dsc = Float.parseFloat(status.getDisco());
            if (dsc > (dsm * 0.75)) {
                warning("disco não pode consumir acima do permitido, restam " + (dsm - dsc) + "MB,"
                        + " usuario: " + getContexto().getUsuario());
            }
            if (dsc > dsm) {
                error("encerrando sessão por limite de uso de disco, uso: "
                        + dsc + "MB de " + dsm + "MB", this, 330);
                encerrarSessao("consumo de disco excedeu limite {AbstreactProcesso/319}");
            }
        } catch (Exception ex) {
            if (isRUN()) {
                error("encerrando sessão por falta de informação "
                        + "de consumo de disco LM"
                        + getContexto().getMemoriaLimite() + " e VL"
                        + status.getDisco()
                        + " EX: " + ex, this, 300);
                encerrarSessao("impossivel obter consumo de disco {AbstreactProcesso/328}");
            } else {
                addFalha("impossivel comparar disco LIM "
                        + getContexto().getMemoriaLimite()
                        + " com VAL " + status.getMemoria() + " error: " + ex);
            }
        }
        return status;
    }

    public void addFalha(String text) {
        falhas++;
        warning("falha: " + text + " é a " + falhas + "º sendo que "
                + MainSingleton.LIMITE_CONSECUTIVO_DE_FALHAS + " é o limite");
        if (falhas > MainSingleton.LIMITE_CONSECUTIVO_DE_FALHAS) {
            error("encerrando sesão por exceder limites de falahas.", this, 344);
            encerrarSessao("numero de falhas excedeu o limite permitido {AbstreactProcesso/344}");
        }

    }

    public String getPidR() {
        return pidR;
    }

    public String getPidJAVA() {
        return pidJAVA;
    }

    public void resetFalhas() {
        falhas = 0;
    }

    public abstract void stop();

    public boolean removeFile(String file) {
        String local = getContexto().getDiretorioPath() + file;
        info("deletando arquivo: " + local);
        try {
            return Files.deleteIfExists(new File(local).toPath());
        } catch (Exception ex) {
            error("impossivel remover arquivo: " + local, this, 344);
        }
        return false;
    }

}
