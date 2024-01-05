package com.bilanee.octopus.basic;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
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

    public static void main(String[] args) {
        System.out.println(TokenUtils.sign("1001"));
    }

    @SneakyThrows
    public static Boolean verify(String token){

        try {
            JWTVerifier jwtVerifier=JWT.require(Algorithm.HMAC256(TOKEN_SECRET)).withIssuer("auth0").build();
            jwtVerifier.verify(token);
        } catch (IllegalArgumentException | JWTVerificationException e) {
            return false;
        }
        return true;
    }

    @SneakyThrows
    public static String getUserId(String token){
        JWTVerifier jwtVerifier=JWT.require(Algorithm.HMAC256(TOKEN_SECRET)).withIssuer("auth0").build();
        return jwtVerifier.verify(token).getClaim("userId").asString();
    }

}