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
import lombok.CustomLog;
import lombok.SneakyThrows;

import java.util.Date;

@CustomLog
public class TokenUtils {

    private static final long EXPIRE_TIME = 3600 * 1000L;
    public static String TOKEN_SECRET = new Date().toString();

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
            log.info("verify failure , not valid token " + token);
            return false;
        }
        if (key.equals("token")) {
            String userId = TokenUtils.getUserId(token);
            UserDO userDO = BeanUtil.getBean(UserDOMapper.class).selectById(TokenUtils.getUserId(token));
            if (userDO == null) {
                log.info("verify failure , thanks to userId of trader " + userId + " not existed!");
                return false;
            }
        } else if (key.equals("adminToken")) {
            String userId = TokenUtils.getUserId(token);
            AdminDO adminDO = BeanUtil.getBean(AdminDOMapper.class).selectById(TokenUtils.getUserId(token));
            if (adminDO == null) {
                log.info("verify failure , thanks to userId of admin" + userId + "not existed!");
                return false;
            }
        } else {
            log.info("verify failure , thanks to key " + key);
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