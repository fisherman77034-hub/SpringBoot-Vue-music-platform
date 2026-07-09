package com.music.platform.web;

import com.music.platform.common.ApiResponse;
import com.music.platform.web.dto.AuthLoginRequest;
import com.music.platform.web.dto.AuthRegisterRequest;
import com.music.platform.web.dto.AuthTokenResponse;
import com.music.platform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody AuthRegisterRequest req) {
        userService.register(req.getUsername(), req.getPassword(), req.getNickname());
        return ApiResponse.ok();
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody AuthLoginRequest req) {
        String token = userService.login(req.getUsername(), req.getPassword());
        return ApiResponse.ok(new AuthTokenResponse(token));
    }
}

