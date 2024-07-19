package com.bilanee.octopus.adapter.ws;


import com.bilanee.octopus.adapter.facade.UserFacade;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.TokenUtils;
import com.stellariver.milky.common.base.BeanUtil;
import com.stellariver.milky.common.tool.executor.EnhancedExecutor;
import com.stellariver.milky.common.tool.util.Json;
import lombok.CustomLog;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.EOFException;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


@Data
@CustomLog
@Component
@ServerEndpoint("/ws")
public class WebSocket {

    private static final Map<Session, String> sessions = new ConcurrentHashMap<>();

    private static final Executor executor = Executors.newFixedThreadPool(100);




    private  static  final CloseReason closeReason = new CloseReason(new CloseReason.CloseCode() {
        @Override
        public int getCode() {
            return 2024;
        }
    }, "一个用户ID不能同时登录两个页面");



    @OnOpen
    public void onOpen(Session session) throws IOException {
        String token = session.getRequestParameterMap().get("token").get(0);

        if (StringUtils.isBlank(token) || !TokenUtils.verify("token", token)) {
            log.info("active close by " + token);
            session.close(closeReason);
            return;
        }

        String userId = TokenUtils.getUserId(token);

        boolean singleLoginLimit = BeanUtil.getBean(Tunnel.class).singleLoginLimit();
        if (singleLoginLimit) {
            String currentToken = BeanUtil.getBean(UserFacade.class).getTokens().get(userId);
            if (!Objects.equals(currentToken, token)) {
                log.info("active close by current token " + currentToken + "input token " + token);
                session.close(closeReason);
                return;
            }
        }

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
        String userId = sessions.get(session);
        try {
            synchronized (session) {
                boolean singleLoginLimit = BeanUtil.getBean(Tunnel.class).singleLoginLimit();
                if (singleLoginLimit) {
                    String token = session.getRequestParameterMap().get("token").get(0);
                    String currentToken = BeanUtil.getBean(UserFacade.class).getTokens().get(userId);
                    boolean equals = Objects.equals(currentToken, token);
                    if (!equals) {
                        log.info(userId + " active close by current token " + currentToken + " input token " + token);
                        session.close(closeReason);
                        return;
                    }
                }
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
                if (!(error instanceof EOFException)) {
                    log.error("onMessage userId: {}, session : {}", userId, session, error);
                }
            }
        }));
    }

}