/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package itgmrest.websockets;

import itgmrest.MainSingleton;
import itgmrest.processos.LiveProcesso;
import java.io.IOException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author mfernandes
 */
@ServerEndpoint("/jriaccesslive/{token}/{server}")
public class JRIAccessEndpoint {

    MainSingleton getMainSingleton() {
        return MainSingleton.getInstance();
    }

    String getToken(Session session) {
        return session.getPathParameters().get("token");
    }

    String getServer(Session session) {
        return session.getPathParameters().get("server");
    }

    LiveProcesso getLiveProcesso(Session session) {
        return (LiveProcesso) getMainSingleton().getProcesso(getToken(session));
    }

    @OnOpen
    public void onOpen(Session session) {
        getMainSingleton().info(session.getId()
                + " sessão aberta: "
                + getToken(session) + " " + getServer(session) + " "
                + session.getNegotiatedSubprotocol());
        LiveProcesso liveProcess = getLiveProcesso(session);
        if (null != liveProcess) {
            liveProcess.addSession(getServer(session), session);
            getMainSingleton().info("sessão " + session.getId() + " adicionada com sucesso.");
        } else {
            getMainSingleton().error("websocket requerendo processo "
                    + getToken(session) + " inexistente", this.getClass(), 50);
            try {
                session.close();
            } catch (IOException ex) {
                getMainSingleton().error("impossivel encerrao "
                        + "sessão invalida de websockets: " + ex, this.getClass(), 55);
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        getMainSingleton().info(session.getId()
                + " sessão encerrada: " + getToken(session) + " " + getServer(session));
        LiveProcesso liveProcesso = getLiveProcesso(session);
        if (null != liveProcesso) {
            liveProcesso.removeSession(getServer(session), session);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {

        getMainSingleton().info(session.getId() + " mensagem recebida:"
                + getToken(session) + "<" + getServer(session)
                + ">" + message);

        try {
            LiveProcesso liveProcess = getLiveProcesso(session);
            if (null != liveProcess) {
                if ("STATUS".equals(getServer(session))) {
                    liveProcess.setStatusTime(message);
                    return;
                }
                liveProcess.writeLine(
                        java.util.Base64.getEncoder().encodeToString(message.getBytes())
                );
            }
        } catch (IOException ex) {
            getMainSingleton().error("impossivel escrever message: " + ex, this.getClass(), 90);
        }

    }

}
