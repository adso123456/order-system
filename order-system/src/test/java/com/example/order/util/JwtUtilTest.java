package com.example.order.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET_A;
    private static final String SECRET_B;
    static {
        SecureRandom rng = new SecureRandom();
        byte[] a = new byte[32]; // 256-bit for HS256
        byte[] b = new byte[32];
        rng.nextBytes(a);
        rng.nextBytes(b);
        SECRET_A = Base64.getEncoder().encodeToString(a);
        SECRET_B = Base64.getEncoder().encodeToString(b);
    }

    // ===== 正常生成 + 解析 =====

    @Test
    void shouldGenerateAndValidateToken() {
        JwtUtil util = new JwtUtil(SECRET_A, 86_400_000L);
        String token = util.generateToken(42L, "bob");

        Long userId = util.validateAndGetUserId(token);
        assertThat(userId).isEqualTo(42L);
    }

    // ===== 过期 token =====
    // 用 1ms 过期 + Thread.sleep(5) 保证已过期,避免 JVM 时钟毫秒粒度导致的偶发通过

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        JwtUtil util = new JwtUtil(SECRET_A, 1L); // 1ms 后过期
        String token = util.generateToken(1L, "test");
        Thread.sleep(5); // 确保已过过期时间

        assertThatThrownBy(() -> util.validateAndGetUserId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // ===== 乱码 token =====

    @Test
    void shouldRejectMalformedToken() {
        JwtUtil util = new JwtUtil(SECRET_A, 86_400_000L);

        assertThatThrownBy(() -> util.validateAndGetUserId("this.is.not.a.jwt.token"))
                .isInstanceOf(JwtException.class);
    }

    // ===== 错误签名 =====

    @Test
    void shouldRejectWrongSignature() {
        JwtUtil utilA = new JwtUtil(SECRET_A, 86_400_000L);
        JwtUtil utilB = new JwtUtil(SECRET_B, 86_400_000L);
        String token = utilA.generateToken(1L, "test");

        assertThatThrownBy(() -> utilB.validateAndGetUserId(token))
                .isInstanceOf(SignatureException.class);
    }
}
