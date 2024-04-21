package com.bilanee.octopus.basic;

import com.stellariver.milky.common.base.ExceptionType;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.util.Json;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TokenInterceptor implements HandlerInterceptor {

    final private String key;

    public TokenInterceptor(String key) {
        this.key = key;
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
            Result<Void> result = Result.error(ErrorEnums.NOT_LOGIN, ExceptionType.BIZ);
            response.getWriter().append(Json.toJson(result));
            return false;
        }
        return true;
    }

}