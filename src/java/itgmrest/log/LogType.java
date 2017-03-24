/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package itgmrest.log;

/**
 *
 * @author mfernandes
 */
public enum LogType {
    LOG_INFO,
    LOG_WARNING,
    LOG_ERROR,
    LOG_DEBUG;

    public static LogType getLogType(String tipo) {
        if ("DEBUG".equals(tipo)) {
            return LogType.LOG_DEBUG;
        }
        if ("INFO".equals(tipo)) {
            return LogType.LOG_INFO;
        }
        if ("WARNING".equals(tipo)) {
            return LogType.LOG_WARNING;
        }
        if ("ERROR".equals(tipo)) {
            return LogType.LOG_ERROR;
        }
        return null;
    }

    public static String getLogType(LogType tipo) {
        switch (tipo) {
            case LOG_DEBUG:
                return "DEBUG";
            case LOG_ERROR:
                return "ERROR";
            case LOG_WARNING:
                return "WARNING";
            case LOG_INFO:
                return "INFO";
            default:
                return null;
        }
    }
    
}
