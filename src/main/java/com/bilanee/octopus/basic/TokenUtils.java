package com.bilanee.octopus.basic;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.bilanee.octopus.infrastructure.entity.AdminDO;
import com.bilanee.octopus.infrastructure.entity.UserDO;
import com.bilanee.octopus.infrastructure.mapper.AdminDOMapper;
import com.bilanee.octopus.infrastructure.mapper.UserDOMapper;
import com.stellariver.milky.common.base.BeanUtil;
import lombok.SneakyThrows;

import java.util.Date;

public class TokenUtils {

    private static final long EXPIRE_TIME = 12 * 60 * 60 * 1000 * 10000L;
    private static final String TOKEN_SECRET="ljdyaishijin**zxxzddaa3nkjnj??";

    public static String sign(String userId){
        String token;
        Date expireAt=new Date(System.currentTimeMillis() + EXPIRE_TIME);
        token = JWT.create()
                .withIssuer("auth0")
                .withClaim("userId", userId)
                .withExpiresAt(expireAt)
                .sign(Algorithm.HMAC256(TOKEN_SECRET));
        return token;
    }


    @SneakyThrows
    public static Boolean verify(String key, String token){

        try {
            JWTVerifier jwtVerifier=JWT.require(Algorithm.HMAC256(TOKEN_SECRET)).withIssuer("auth0").build();
            jwtVerifier.verify(token);
        } catch (IllegalArgumentException | JWTVerificationException e) {
            return false;
        }
        if (key.equals("token")) {
            UserDO userDO = BeanUtil.getBean(UserDOMapper.class).selectById(TokenUtils.getUserId(token));
            return userDO != null;
        } else if (key.equals("adminToken")) {
            AdminDO adminDO = BeanUtil.getBean(AdminDOMapper.class).selectById(TokenUtils.getUserId(token));
            return adminDO != null;
        } else {
            return false;
        }
    }

    @SneakyThrows
    public static String getUserId(String token){
        JWTVerifier jwtVerifier=JWT.require(Algorithm.HMAC256(TOKEN_SECRET)).withIssuer("auth0").build();
        return jwtVerifier.verify(token).getClaim("userId").asString();
    }

    public static void main(String[] args) {
        System.out.println(TokenUtils.sign("1000"));
    }

}