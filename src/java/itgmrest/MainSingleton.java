/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package itgmrest;

import itgmrest.log.ILog;
import itgmrest.log.LogType;
import itgmrest.log.TXTLog;
import itgmrest.processos.AbstractProcesso;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author mfernandes
 */
public class MainSingleton {

    public static final String DIRETORIO = "/home/mfernandes/ITGMRest/";
    public static final String LOG_NAME = "itgmrest.log.";
    public static final String BASH_SCRIPT = "cgConfig.sh";
    public static final String LOG_NIVEL = "DEBUG";
    public static final String LOG_TIME_FORMAT = "dd' de 'MMMMM' de 'yyyy' > 'HH'-'mm'-'ss";
    public static final String ARGS_JRI = "java,"
            + "-jar,"
            + "/home/mfernandes/NetBeansProjects/JRIAccess3/store/jriaccess.jar";
    public static final int TIMEOUT_PID = 30;
    public static final int TIMEOUT_STATUS = 10; ////deve ser menor q pid
    public static final int LIMITE_CONSECUTIVO_DE_FALHAS = 20;
    public static final String WWW = "/var/www/html/temp/";
    public static ILog LOG;
    private static final SecureRandom RANDON = new SecureRandom(), RANDONFILE = new SecureRandom();
    private static final HashMap<String, AbstractProcesso> PROCESSOS = new HashMap<>();

    private MainSingleton() {
        System.out.println("inicializando MainSingleton...");
        try {
            LOG = new TXTLog(DIRETORIO + LOG_NAME + TXTLog.getDateTime(), LogType.getLogType(LOG_NIVEL));
            debug("log inicializado...", getClass(), 29);
        } catch (IOException ex) {
            System.err.println("ERROR: impossivel iniciar LOG: " + ex);
            System.exit(-1);
        }
        ScriptBash.saveFileScript(this);
        debug("script bash inicializado...", getClass(), 31);
        debug("MainSingleton inicializada...", getClass(), 36);

        new Thread(new Runnable() {
            @Override
            public void run() {
                HashSet<String> tokens = new HashSet<>();
                while (true) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                        error("impossivel aguardar por coletor de lixo " + ex, MainSingleton.this.getClass(), 56);
                    }
                    if (!PROCESSOS.isEmpty()) {

                        try {
                            PROCESSOS.values().stream().filter((processo) -> (!processo.isAlive())).map((processo) -> {
                                return processo;
                            }).forEachOrdered((processo) -> {
                                tokens.add(processo.getContexto().getToken());
                            });

                            tokens.forEach((token) -> {
                                info("removendo processo invalido: " + token);
                                PROCESSOS.remove(token);
                            });

                            tokens.clear();

                        } catch (Exception ex) {
                            error("falhou ao remover processos invalidos " + ex, getClass(), 69);
                        }
                        //debug("verificando processos invalidos...");
                    }
                }
            }
        }).start();

    }

    public static MainSingleton getInstance() {
        return MainSingletonHolder.INSTANCE;
    }

    private static class MainSingletonHolder {

        private static final MainSingleton INSTANCE = new MainSingleton();
    }

    public void warning(String text) {
        log(LogType.LOG_WARNING, text);
    }

    public void error(String text, Class classe, int linha) {
        log(LogType.LOG_ERROR, "  " + text + "{" + classe.getName() + ":" + linha + "} ");
    }

    public void info(String text) {
        log(LogType.LOG_INFO, "   " + text);
    }

    public void debug(String text, Class classe, int linha) {
        log(LogType.LOG_DEBUG, "  " + text + "{" + classe.getName() + ":" + linha + "} ");
    }

    public void log(LogType type, String text) {
        System.out.println("@" + LOG.printLog(type, text));
    }

    public String nextToken() {
        String token = new BigInteger(130, RANDON).toString(32).toUpperCase();
        debug("novo token: " + token, getClass(), 77);
        return token;
    }

    public String nextTokenFile() {
        String token = new BigInteger(130, RANDONFILE).toString(32);
        debug("novo token: " + token, getClass(), 77);
        return token;
    }

    public void addProcesso(AbstractProcesso processo) {
        PROCESSOS.put(processo.getContexto().getToken(), processo);
        info(processo.getContexto().getToken() + "processo adicionado.");
    }

    public AbstractProcesso getProcesso(String token) {
        debug("recuperando processo com token " + token, getClass(), 87);
        return PROCESSOS.get(token);
    }

    public void removeProcesso(String token, AbstractProcesso processo) {
        PROCESSOS.remove(token, processo);
        info(token + "processo removido.");
    }

    public Iterator<AbstractProcesso> getIterator() {
        return PROCESSOS.values().iterator();
    }

}
