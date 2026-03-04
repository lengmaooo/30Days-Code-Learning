package com.example.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {
    // 密钥字符串（至少 256 位 / 32 字符，用于 HS256）
    private static final String SECRET_STRING = "abc123456xyz789abc123456xyz789abc123456xyz789";
    // 生成安全的密钥
    private static final SecretKey SECRET = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());
    // 过期时间 1天
    private static final long EXPIRE = 1000 * 60 * 60 * 24;

    // 生成 token
    public static String createToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(SECRET)
                .compact();
    }

    // 校验 token 是否正确
    public static boolean checkToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(SECRET).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 从 token 取出用户信息
    public static String getUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}
