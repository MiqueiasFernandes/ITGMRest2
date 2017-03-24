/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package itgmrest.log;

import itgmrest.MainSingleton;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TXTLog implements ILog {

    private final FileWriter fileWriter;
    private final LogType nivelDeLog;
    public static final Locale LOCALE = new Locale("pt", "BR");

    public TXTLog(String fileName, LogType type) throws IOException {
        this.nivelDeLog = type != null ? type : LogType.LOG_DEBUG;
        if (fileName == null || fileName.isEmpty()) {
            fileName = ".log";
        }
        fileWriter = new FileWriter(fileName, true);
    }

//    NIVEIS DE LOG    APARECEM
//      DEBUG --------DEBUG,INFO,WARNING,ERRORR
//      INFO ---------INFO,WARNING,ERROR
//      WARNING ------WARNING,ERROR
//      ERROR---------ERROR
    @Override
    public String printLog(LogType tipo, String texto) {
        String log = "";
        try {
            switch (tipo) {
                case LOG_DEBUG: { //// só aparece em modo debug
                    if (nivelDeLog != LogType.LOG_DEBUG) {
                        return "";
                    }
                    break;
                }
                case LOG_INFO: { //// ñ vai aparecer caso: error, warning
                    if (nivelDeLog == LogType.LOG_ERROR || nivelDeLog == LogType.LOG_WARNING) {
                        return "";
                    }
                    break;
                }
                case LOG_WARNING: { ///não vai aparecer caso: error
                    if (nivelDeLog == LogType.LOG_ERROR) {
                        return "";
                    }
                    break;
                }
            }
            log = LogType.getLogType(tipo) + " [" + getDateTime() + "] "
                    + texto + System.lineSeparator();
            fileWriter.append(log);
            fileWriter.flush();
        } catch (IOException ex) {
            System.err.println("erro ao gravar log: " + ex);
        }
        return log;
    }

    public static Locale getLocale() {
        return LOCALE;
    }

    public static String getDateTime() {
        return new SimpleDateFormat(MainSingleton.LOG_TIME_FORMAT, LOCALE).format(new Date());
    }

}
