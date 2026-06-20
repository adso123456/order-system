package com.example.order.dto;

public class LoginResponse {

    private String token;
    private String username;
    private long expiresInMs;

    public LoginResponse(String token, String username, long expiresInMs) {
        this.token = token;
        this.username = username;
        this.expiresInMs = expiresInMs;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public long getExpiresInMs() { return expiresInMs; }
    public void setExpiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; }
}
