package com.bilanee.octopus.adapter.ws;


import com.bilanee.octopus.basic.TokenUtils;
import com.google.common.collect.Sets;
import com.stellariver.milky.common.tool.util.Json;
import lombok.CustomLog;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.EOFException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


@Data
@CustomLog
@Component
@ServerEndpoint("/ws")
public class WebSocket {

    private String userId;
    private Session session;
    private static final Executor executor = Executors.newFixedThreadPool(100);

    private static final Map<String, Set<Session>> sessions = new ConcurrentHashMap<>();
 
    @OnOpen
    public void onOpen(Session session){
        this.session = session;
        this.userId = TokenUtils.getUserId(session.getRequestParameterMap().get("token").get(0));
        sessions.computeIfAbsent(this.userId, k -> Sets.newConcurrentHashSet()).add(this.session);
    }

    @OnClose
    public void onClose(){
        log.info("close session, userId: {}, session : {}", userId, session);
        if (userId == null) {
            return;
        }
        Optional.ofNullable(sessions.get(userId)).ifPresent(ss -> ss.remove(session));
    }

    @OnError
    public void onError(Session session, Throwable error) {
        if (error instanceof EOFException) {
            log.warn(error.getMessage());
        } else {
            log.error(" userId, {}, session: {}", userId, session, error);
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        Throwable backUp = null;
        try {
            synchronized (session) {
                session.getBasicRemote().sendText(message);
            }
        } catch (Throwable e) {
            backUp = e;
        }finally {
            if (backUp != null) {
                log.error("onMessage userId: {}, session : {}, wsMessage: {}", userId, session, message, backUp);
            }
        }
    }

    @SneakyThrows
    public static void cast(WsMessage wsMessage) {
        sessions.values().stream().flatMap(Collection::stream).forEach(s -> {
            executor.execute(() -> {
                Throwable backUp = null;
                try {
                    synchronized (s) {
                        s.getBasicRemote().sendText(Json.toJson(wsMessage));
                    }
                } catch (Throwable e) {
                    backUp = e;
                } finally {
                    if (backUp != null) {
                        log.error("cast session : {}, wsMessage: {}", s, wsMessage, backUp);
                    }
                }
            });

        });
    }

}