package com.example.order.config;

import com.example.order.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===== 合法 token → 设 SecurityContext =====

    @Test
    void shouldSetAuthenticationForValidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.validateAndGetUserId("valid-token")).thenReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
    }

    // ===== 无 Authorization 头 → 跳过 =====

    @Test
    void shouldSkipWhenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).validateAndGetUserId(anyString());
    }

    // ===== 过期 token → 不抛异常 + 不设 SecurityContext + doFilter 继续 =====

    @Test
    void shouldSkipWhenExpiredToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(jwtUtil.validateAndGetUserId("expired-token"))
                .thenThrow(ExpiredJwtException.class);

        // 关键断言 1: 不抛异常 (方法正常返回即通过)
        filter.doFilterInternal(request, response, filterChain);

        // 关键断言 2: 不设 SecurityContext
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        // 关键断言 3: filterChain 继续放行
        verify(filterChain).doFilter(request, response);
    }

    // ===== 乱码 token → 同上三点 =====

    @Test
    void shouldSkipWhenMalformedToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer garbage");
        when(jwtUtil.validateAndGetUserId("garbage"))
                .thenThrow(MalformedJwtException.class);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
