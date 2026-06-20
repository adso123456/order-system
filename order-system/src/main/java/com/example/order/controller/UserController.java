package com.example.order.controller;

import com.example.order.common.ApiResponse;
import com.example.order.dto.LoginResponse;
import com.example.order.dto.UserLoginRequest;
import com.example.order.dto.UserRegisterRequest;
import com.example.order.dto.UserResponse;
import com.example.order.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return ApiResponse.success(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }
}
