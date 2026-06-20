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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public UserResponse register(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        return UserResponse.fromEntity(userRepository.save(user));
    }

    public LoginResponse login(UserLoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("用户名或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getUsername());
        return new LoginResponse(token, user.getUsername(), jwtUtil.getExpirationMs());
    }
}
