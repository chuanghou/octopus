package com.bilanee.octopus.adapter.ws;
 
 
import com.bilanee.octopus.basic.TokenUtils;
import com.stellariver.milky.common.tool.util.Json;
import lombok.Getter;
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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;



@Slf4j
@Component
@ServerEndpoint("/ws")
public class WebSocket {

    @Getter
    private Session session;
    private String userId;

    private static final Map<String, Map<String, WebSocket>> sessions = new ConcurrentHashMap<>();
 
    @OnOpen
    public void OnOpen(Session session){
        this.session = session;
        this.userId = TokenUtils.getUserId(session.getRequestParameterMap().get("token").get(0));
        String sessionId = session.getId();
        sessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(session.getId(), this);
        log.info("OnOpen sessions \n\n {}\n\n {} \n\n {}", Json.toJson(sessions), Json.toJson(this), session );
    }

    @OnClose
    public void OnClose(){
        log.info("prepare close {}, {}, {}", userId, session, Json.toJson(sessions));
        Map<String, WebSocket> map = sessions.get(userId);
        if (map == null) {
            log.error(Objects.toString(this));
            return;
        }
        WebSocket remove = map.remove(session.getId());
        log.info("finish close \n\n {} \n\n {} \n\n {} \n\n {}", userId, session, remove, Json.toJson(sessions));
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("sessions \n\n {} \n\n {}", Json.toJson(sessions), session, error);
    }

    @SneakyThrows
    public static void cast(WsMessage wsMessage) {
        List<Session> ss = sessions.values().stream().flatMap(m -> m.values().stream()).map(WebSocket::getSession).collect(Collectors.toList());
        for (Session session : ss) {
            session.getBasicRemote().sendText(Json.toJson(wsMessage));
        }
    }

}