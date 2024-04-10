package com.bilanee.octopus.adapter.ws;


import com.bilanee.octopus.basic.TokenUtils;
import com.stellariver.milky.common.tool.util.Json;
import lombok.CustomLog;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


@Data
@CustomLog
@Component
@ServerEndpoint("/ws")
public class WebSocket {

    private static final Executor executor = Executors.newFixedThreadPool(100);
    private static final Map<Session, String> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session){
        String userId = TokenUtils.getUserId(session.getRequestParameterMap().get("token").get(0));
        sessions.put(session, userId);
    }

    @OnClose
    public void onClose(Session session){
        String userId = sessions.remove(session);
        log.info("onClose userId: {}, session : {}", userId, session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        String userId = sessions.get(session);
        log.error("onError userId: {}, session : {}", userId, session, error);
    }

    @OnMessage
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void onMessage(Session session, String message) {
        Throwable backUp;
        String userId = sessions.get(session);
        try {
            synchronized (session) {
                if(session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            }
        } catch (Throwable error) {
            log.error("onMessage userId: {}, session : {}, wsMessage: {}", userId, session, message, error);
        }
    }

    @SneakyThrows
    public static void cast(WsMessage wsMessage) {
        sessions.keySet().forEach(session -> executor.execute(() -> {
            String userId = sessions.get(session);
            try {
                synchronized (session) {
                    if(session.isOpen()) {
                        session.getBasicRemote().sendText(Json.toJson(wsMessage));
                    }
                }
            } catch (Throwable error) {
                log.error("cast userId : {}, session : {}, wsMessage: {}", userId, session, wsMessage, error);
            }
        }));
    }

}