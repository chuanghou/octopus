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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;



@Data
@CustomLog
@Component
@ServerEndpoint("/ws")
public class WebSocket {

    private String userId;
    private Session session;

    private static final Map<String, Set<Session>> sessions = new ConcurrentHashMap<>();
 
    @OnOpen
    public void OnOpen(Session session){
        this.session = session;
        this.userId = TokenUtils.getUserId(session.getRequestParameterMap().get("token").get(0));
        sessions.computeIfAbsent(this.userId, k -> Sets.newConcurrentHashSet()).add(this.session);
    }

    @OnClose
    public void OnClose(){
        log.info("close session, userId: {}, session : {}", userId, session);
        if (userId == null) {
            return;
        }
        Optional.ofNullable(sessions.get(userId)).ifPresent(ss -> ss.remove(session));
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error(" userId, {}, session: {}", userId, session, error);
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        Throwable backUp = null;
        try {
            session.getBasicRemote().sendText(message);
        } catch (Throwable e) {
            backUp = e;
        }finally {
            if (backUp == null) {
                log.info("onMessage userId: {}, session : {}, wsMessage: {}", userId, session, message);
            } else {
                log.error("onMessage userId: {}, session : {}, wsMessage: {}", userId, session, message, backUp);
            }
        }
    }

    @SneakyThrows
    public static void cast(WsMessage wsMessage) {
        sessions.values().stream().flatMap(Collection::stream).forEach(s -> {
            Throwable backUp = null;
            try {
                s.getBasicRemote().sendText(Json.toJson(wsMessage));
            } catch (Throwable e) {
                backUp = e;
            } finally {
                if (backUp == null) {
                    log.info("cast session : {}, wsMessage: {}", s, wsMessage);
                } else {
                    log.error("cast session : {}, wsMessage: {}", s, wsMessage, backUp);
                }
            }
        });
    }

}