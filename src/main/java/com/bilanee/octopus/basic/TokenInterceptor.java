package com.bilanee.octopus.basic;

import com.bilanee.octopus.adapter.facade.UserFacade;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.stellariver.milky.common.base.BeanUtil;
import com.stellariver.milky.common.base.ExceptionType;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.util.Json;
import lombok.CustomLog;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@CustomLog
public class TokenInterceptor implements HandlerInterceptor {

    final private String key;

    final private boolean limit;

    public TokenInterceptor(String key, boolean limit) {
        this.key = key;
        this.limit = limit;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws IOException {

        if(request.getMethod().equals("OPTIONS")){
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json; charset=utf-8");
        String token = request.getHeader(key);

        if (StringUtils.isBlank(token) || !TokenUtils.verify(key, token)) {
            log.arg0(token);
            log.info("TOKEN_ERROR");
            Result<Void> result = Result.error(ErrorEnums.NOT_LOGIN, ExceptionType.BIZ);
            response.getWriter().append(Json.toJson(result));
            return false;
        }
        if (limit && BeanUtil.getBean(Tunnel.class).singleLoginLimit()) {
            String userId = TokenUtils.getUserId(token);
            String currentToken = BeanUtil.getBean(UserFacade.class).getTokens().get(userId);
            if (StringUtils.isBlank(currentToken) || !Objects.equals(token, currentToken)) {
                log.arg0(userId).arg1(currentToken).arg2(token).info("TOKEN_ERROR_SINGLE_LIMIT");
                Result<Void> result = Result.error(ErrorEnums.NOT_LOGIN, ExceptionType.BIZ);
                response.getWriter().append(Json.toJson(result));
                return false;
            }
        }
        return true;
    }

}