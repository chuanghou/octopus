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

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {

        if(request.getMethod().equals("OPTIONS")){
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json; charset=utf-8");
        String token = request.getHeader("token");
        if (StringUtils.isBlank(token) || !TokenUtils.verify(token)) {
            Result<Void> result = Result.error(ErrorEnums.NOT_LOGIN, ExceptionType.BIZ);
            response.getWriter().append(Json.toJson(result));
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println(TokenUtils.sign("0"));
        System.out.println(TokenUtils.sign("1000"));
    }

}