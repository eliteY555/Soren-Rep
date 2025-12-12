package com.tailai.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 *
 * @author Tailai
 */
@Slf4j
public class JwtUtil {

    /**
     * 密钥（至少32位）
     */
    private static final String SECRET_KEY = "tailai_cloud_employee_management_system_secret_key_2024";

    /**
     * Token有效期：1小时（单位：毫秒）
     */
    private static final long EXPIRATION_TIME = 1 * 60 * 60 * 1000;

    /**
     * 生成签名密钥
     */
    private static Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT Token
     *
     * @param userId   用户ID
     * @param userType 用户类型
     * @return JWT Token
     */
    public static String generateToken(String userId, String userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userType", userType);
        return createToken(claims, userId);
    }
    
    /**
     * 生成JWT Token（重载方法，兼容旧代码）
     *
     * @param userId   用户ID
     * @param username 用户名（accountNumber）
     * @return JWT Token
     */
    public static String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return createToken(claims, username);
    }

    /**
     * 生成JWT Token（包含真实姓名）
     *
     * @param userId   用户ID
     * @param username 用户名（accountNumber）
     * @param realName 真实姓名
     * @return JWT Token
     */
    public static String generateToken(Long userId, String username, String realName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("realName", realName);
        return createToken(claims, username);
    }

    /**
     * 创建Token
     *
     * @param claims  声明
     * @param subject 主题
     * @return Token
     */
    private static String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析Token
     *
     * @param token JWT Token
     * @return Claims
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Token解析失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 从Token中获取用户ID（字符串）
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public static String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            Object userId = claims.get("userId");
            return userId != null ? userId.toString() : null;
        }
        return null;
    }
    
    /**
     * 从Token中获取用户ID（Long类型）
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public static Long getUserIdAsLong(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
            if (userId instanceof String) {
                return Long.parseLong((String) userId);
            }
            return (Long) userId;
        }
        return null;
    }
    
    /**
     * 从Token中获取用户类型
     *
     * @param token JWT Token
     * @return 用户类型
     */
    public static String getUserTypeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("userType", String.class) : null;
    }

    /**
     * 从Token中获取用户名（accountNumber）
     *
     * @param token JWT Token
     * @return 用户名
     */
    public static String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("username", String.class) : null;
    }

    /**
     * 从Token中获取真实姓名
     *
     * @param token JWT Token
     * @return 真实姓名
     */
    public static String getRealNameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("realName", String.class) : null;
    }

    /**
     * 验证Token是否过期
     *
     * @param token JWT Token
     * @return true-已过期，false-未过期
     */
    public static boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) {
                return true;
            }
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token
     * @return true-有效，false-无效
     */
    public static boolean validateToken(String token) {
        return !isTokenExpired(token);
    }
}

