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
import java.nio.ByteBuffer;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

/**
 *
 * @author mfernandes
 */
public class MainSingleton {

    public static String DIRETORIO = "/home/glassfish/ITGMRest/";
    public static final String LOG_NAME = "itgmrest.log.";
    public static final String BASH_SCRIPT = "cgConfig.sh";
    public static final String LOG_NIVEL = "DEBUG";
    public static final String LOG_TIME_FORMAT = "dd' de 'MMMMM' de 'yyyy' > 'HH'-'mm'-'ss";
    public static final String ARGS_JRI = "java,"
            + "-jar,"
            + DIRETORIO
            + "jriaccess.jar";
    public static final int TIMEOUT_PID = 30;
    public static final int TIMEOUT_STATUS = 10; ////deve ser menor q pid
    public static final int LIMITE_CONSECUTIVO_DE_FALHAS = 20;
    public static final String WWW = "/var/www/html/temp/";
    public static ILog LOG;
    private static final SecureRandom RANDON = new SecureRandom(), RANDONFILE = new SecureRandom();
    private static final HashMap<String, AbstractProcesso> PROCESSOS = new HashMap<>();
    public static final Locale LOCALE = new Locale("pt", "BR");
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(MainSingleton.LOG_TIME_FORMAT, LOCALE);
    public static final int SO_TIMEOUT = 60000;
    private static boolean MAIN_SINGLETON_INICIALIZADO = false;

    public static boolean change_dir(String diretorio) {
        if (!MAIN_SINGLETON_INICIALIZADO) {
            System.out.println("[MAINSINGLETON 70] Alterando diretorio de " + DIRETORIO + " para " + diretorio);
            DIRETORIO = diretorio;
            return true;
        } else {
            System.err.println("[MAINSINGLETON 70] ERRO AO TENTAR ALTERAR DIRETORIO! MAIN_SINGLETON INICIALIZADA! ->" + diretorio);
        }
        return false;
    }

    private MainSingleton() {
        MAIN_SINGLETON_INICIALIZADO = true;
        System.out.println("inicializando MainSingleton...");
        try {
            LOG = new TXTLog(DIRETORIO + LOG_NAME + getDateTime(), LogType.getLogType(LOG_NIVEL));
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
        debug("recuperando processo com token " + token, getClass(), 175);
        return PROCESSOS.get(token);
    }

    public void removeProcesso(String token, AbstractProcesso processo) {
        PROCESSOS.remove(token, processo);
        info(token + "processo removido.");
    }

    public Iterator<AbstractProcesso> getIterator() {
        return PROCESSOS.values().iterator();
    }

    /**
     * Copies a directory.
     * <p>
     * NOTE: This method is not thread-safe.
     * <p>
     *
     * @param source the directory to copy from
     * @param target the directory to copy into
     * @throws IOException if an I/O error occurs
     */
    public static void copyDirectory(final Path source, final Path target)
            throws IOException {
        Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                Integer.MAX_VALUE, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes sourceBasic) throws IOException {
                Path targetDir = Files.createDirectories(target
                        .resolve(source.relativize(dir)));
                AclFileAttributeView acl = Files.getFileAttributeView(dir,
                        AclFileAttributeView.class);
                if (acl != null) {
                    Files.getFileAttributeView(targetDir,
                            AclFileAttributeView.class).setAcl(acl.getAcl());
                }
                DosFileAttributeView dosAttrs = Files.getFileAttributeView(
                        dir, DosFileAttributeView.class);
                if (dosAttrs != null) {
                    DosFileAttributes sourceDosAttrs = dosAttrs
                            .readAttributes();
                    DosFileAttributeView targetDosAttrs = Files
                            .getFileAttributeView(targetDir,
                                    DosFileAttributeView.class);
                    targetDosAttrs.setArchive(sourceDosAttrs.isArchive());
                    targetDosAttrs.setHidden(sourceDosAttrs.isHidden());
                    targetDosAttrs.setReadOnly(sourceDosAttrs.isReadOnly());
                    targetDosAttrs.setSystem(sourceDosAttrs.isSystem());
                }
                FileOwnerAttributeView ownerAttrs = Files
                        .getFileAttributeView(dir, FileOwnerAttributeView.class);
                if (ownerAttrs != null) {
                    FileOwnerAttributeView targetOwner = Files
                            .getFileAttributeView(targetDir,
                                    FileOwnerAttributeView.class);
                    targetOwner.setOwner(ownerAttrs.getOwner());
                }
                PosixFileAttributeView posixAttrs = Files
                        .getFileAttributeView(dir, PosixFileAttributeView.class);
                if (posixAttrs != null) {
                    PosixFileAttributes sourcePosix = posixAttrs
                            .readAttributes();
                    PosixFileAttributeView targetPosix = Files
                            .getFileAttributeView(targetDir,
                                    PosixFileAttributeView.class);
                    targetPosix.setPermissions(sourcePosix.permissions());
                    targetPosix.setGroup(sourcePosix.group());
                }
                UserDefinedFileAttributeView userAttrs = Files
                        .getFileAttributeView(dir,
                                UserDefinedFileAttributeView.class);
                if (userAttrs != null) {
                    UserDefinedFileAttributeView targetUser = Files
                            .getFileAttributeView(targetDir,
                                    UserDefinedFileAttributeView.class);
                    for (String key : userAttrs.list()) {
                        ByteBuffer buffer = ByteBuffer.allocate(userAttrs
                                .size(key));
                        userAttrs.read(key, buffer);
                        buffer.flip();
                        targetUser.write(key, buffer);
                    }
                }
                // Must be done last, otherwise last-modified time may be
                // wrong
                BasicFileAttributeView targetBasic = Files
                        .getFileAttributeView(targetDir,
                                BasicFileAttributeView.class);
                targetBasic.setTimes(sourceBasic.lastModifiedTime(),
                        sourceBasic.lastAccessTime(),
                        sourceBasic.creationTime());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)),
                        StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult
                    visitFileFailed(Path file, IOException e)
                    throws IOException {
                throw e;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir,
                    IOException e) throws IOException {
                if (e != null) {
                    throw e;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static String getDateTime() {
        return DATE_FORMAT.format(new Date());
    }

    public static String getDateTime(long date) {
        return DATE_FORMAT.format(date);
    }

}
