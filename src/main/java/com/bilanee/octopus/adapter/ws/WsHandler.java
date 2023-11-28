package com.bilanee.octopus.adapter.ws;

import com.bilanee.octopus.basic.TokenUtils;
import com.stellariver.milky.common.tool.util.Json;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@ServerEndpoint("/ws")
public class WsHandler {

    // userId -> sessionId -> session
    static private final Map<String, Map<String, Session>> sessions = new ConcurrentHashMap<>();

    // sessionId -> userId
    static private final Map<String, String> idMap = new ConcurrentHashMap<>();

    @SneakyThrows
    public static void cast(WsMessage wsMessage) {
        List<Session> ss = sessions.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toList());
        for (Session session : ss) {
            session.getBasicRemote().sendText(Json.toJson(wsMessage));
        }
    }

    @SneakyThrows
    static public void push(String userId, WsMessage wsMessage) {
        Map<String, Session> sMap = sessions.get(userId);
        if (sMap == null) {
            log.warn("userId: {} not login, wsMessage: {}", userId, wsMessage.toString());
            return;
        }

        for (Session s : sMap.values()) {
            s.getBasicRemote().sendText(Json.toJson(wsMessage));
        }
    }


    @OnOpen
    public void onOpen(Session session) {
        String token = session.getRequestParameterMap().get("token").get(0);
        String userId = TokenUtils.getUserId(token);
        String sessionId = session.getId();
        sessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
        idMap.put(sessionId, userId);
    }

    @OnClose
    public void onClose(Session session) {
        String sessionId = session.getId();
        String userId = idMap.remove(sessionId);
        Map<String, Session> map = sessions.get(userId);
        if (map != null) {
           map.remove(sessionId);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        String sessionId = session.getId();
        String userId = idMap.remove(sessionId);
        Map<String, Session> map = sessions.get(userId);
        if (map != null) {
            map.remove(sessionId);
        }
        log.error(session.toString(), error);
    }


}
