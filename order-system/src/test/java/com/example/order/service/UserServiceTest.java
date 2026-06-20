package com.example.order.service;

import com.example.order.common.DuplicateUsernameException;
import com.example.order.common.InvalidCredentialsException;
import com.example.order.dto.LoginResponse;
import com.example.order.dto.UserLoginRequest;
import com.example.order.dto.UserRegisterRequest;
import com.example.order.dto.UserResponse;
import com.example.order.entity.User;
import com.example.order.repository.UserRepository;
import com.example.order.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    // ===== 注册测试 =====

    @Test
    void shouldRegisterSuccessfully() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setUsername("alice");
        req.setPassword("secret123");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashedsecret");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse resp = userService.register(req);

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getUsername()).isEqualTo("alice");
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setUsername("alice");
        req.setPassword("secret123");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessageContaining("用户名已存在");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    // ===== 登录测试 =====

    @Test
    void shouldLoginSuccessfully() {
        UserLoginRequest req = new UserLoginRequest();
        req.setUsername("alice");
        req.setPassword("secret123");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("alice");
        mockUser.setPassword("$2a$10$hashedsecret");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("secret123", "$2a$10$hashedsecret")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "alice")).thenReturn("fake-jwt-token");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

        LoginResponse resp = userService.login(req);

        assertThat(resp.getToken()).isEqualTo("fake-jwt-token");
        assertThat(resp.getUsername()).isEqualTo("alice");
        assertThat(resp.getExpiresInMs()).isEqualTo(86400000L);
        verify(jwtUtil).generateToken(1L, "alice");
    }

    @Test
    void shouldRejectLoginWrongPassword() {
        UserLoginRequest req = new UserLoginRequest();
        req.setUsername("alice");
        req.setPassword("wrongpass");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("alice");
        mockUser.setPassword("$2a$10$hashedsecret");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongpass", "$2a$10$hashedsecret")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("用户名或密码错误");

        verify(jwtUtil, never()).generateToken(anyLong(), anyString());
    }

    @Test
    void shouldRejectLoginUserNotFound() {
        UserLoginRequest req = new UserLoginRequest();
        req.setUsername("nobody");
        req.setPassword("secret123");

        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("用户名或密码错误");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyLong(), anyString());
    }
}
