package depth.finvibe.user.modules.user.adapter.in.web;

import depth.finvibe.user.modules.user.application.port.in.AuthCommandUseCase;
import depth.finvibe.user.modules.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthCommandUseCase authCommandUseCase;

    @PostMapping("/login")
    public ResponseEntity<UserDto.TokenResponse> login(
            @RequestBody UserDto.LoginRequest request) {
        return ResponseEntity.ok(authCommandUseCase.login(request));
    }

    @PostMapping("/oauth/login")
    public ResponseEntity<UserDto.OAuthLoginResponse> oauthLogin(
            @RequestBody UserDto.OAuthLoginRequest request) {
        return ResponseEntity.ok(authCommandUseCase.oauthLogin(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserDto.TokenRefreshResponse> refresh(
            @RequestBody UserDto.TokenRefreshRequest request) {
        return ResponseEntity.ok(authCommandUseCase.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestParam UUID userId) {
        authCommandUseCase.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
