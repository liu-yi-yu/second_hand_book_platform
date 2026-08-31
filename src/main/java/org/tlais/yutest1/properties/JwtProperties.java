package org.tlais.yutest1.properties;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.tlais.yutest1.context.BaseContext;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
@ConfigurationProperties(prefix = "jwt")
@Slf4j
public class JwtProperties {

    @Value("${jwt.admin-secret-key}")
    private String secretKeyStr;

    @Value("${jwt.admin-ttl}")
    private Long expireTime;

    private SecretKey secretKey;

    // 初始化密钥
    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKeyStr);
        this.secretKey = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * 生成JWT token
     * @param userId 用户ID（UUID）
     * @return token字符串
     */
    public String generateToken(String userId) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireTime);
//        BaseContext.setCurrentId(userId);
//        log.info("当前用户ID:{}",BaseContext.getCurrentId());
        return Jwts.builder()
                // 存入主体（用户唯一标识UUID）
                .setSubject(userId)
                // 签发时间
                .setIssuedAt(now)
                // 过期时间
                .setExpiration(expireDate)
                // 签名加密
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析token，获取用户ID
     */
    public String getUserIdByToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * 校验token是否有效（是否过期、签名是否正确）
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            //log.info("token校验:{}",token);
            return true;
        } catch (ExpiredJwtException e) {
            // token过期
            log.warn("token过期:{}",token);
        } catch (MalformedJwtException e) {
            // token格式错误
            log.warn("token格式错误:{}",token);
        } catch (SignatureException e) {
            // 签名错误，密钥不一致
            log.warn("签名错误，密钥不一致:{}",token);
        } catch (Exception e) {
            log.error("token校验异常:{}",token);
        }
        return false;
    }

    /**
     * 判断token是否过期
     */
    public boolean isTokenExpired(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration().before(new Date());
    }
}