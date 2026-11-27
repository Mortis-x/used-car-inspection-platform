package com.usedcar.platform.auth;

import com.usedcar.platform.auth.dto.LoginRequest;
import com.usedcar.platform.auth.dto.LoginResponse;
import com.usedcar.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        UserPrincipal principal = AuthContext.require();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", principal.id());
        data.put("username", principal.username());
        data.put("realName", principal.realName());
        data.put("role", principal.role());
        data.put("roleLabel", Role.of(principal.role()).getLabel());
        return ApiResponse.ok(data);
    }
}
