package com.test.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        byte[] keyBytes = secret.getBytes();
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(String email, long version) {
        return buildToken(email, accessTokenExpiration, "access", version);
    }

    public String generateAccessToken(String email) {
        return buildToken(email, accessTokenExpiration, "access");
    }

    public String generateRefreshToken(String email, long version) {
        return buildToken(email, refreshTokenExpiration, "refresh", version);
    }

    public String generateRefreshToken(String email) {
        return buildToken(email, refreshTokenExpiration, "refresh");
    }

    // 비밀번호 재설정용 단기 토큰(30분). AuthService가 사용자 버전을 잠금 하에 검증·소비한다.
    public String generateResetToken(String email, long version) {
        return buildToken(email, 30 * 60 * 1000L, "reset", version);
    }

    public String generateResetToken(String email) {
        return buildToken(email, 30 * 60 * 1000L, "reset");
    }

    private String buildToken(String email, long expiration, String type) {
        return buildToken(email, expiration, type, 0);
    }

    private String buildToken(String email, long expiration, String type, long version) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(email)
                .claim("type", type)
                .claim("version", version)
                .setId(java.util.UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    // version 클레임이 없는 토큰 = V9 배포 이전에 발급된 것. 0(=신규 사용자 기본값)으로
    // 취급해 배포 순간 전원 강제 로그아웃되는 걸 막는다. 비밀번호 변경/재설정으로
    // auth_version이 한 번이라도 오르면 그 시점에 함께 폐기된다.
    public long getVersion(String token) {
        Number version = (Number) Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("version");
        return version == null ? 0 : version.longValue();
    }

    public String getEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String getTokenType(String token) {
        return (String) Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("type");
    }
}
