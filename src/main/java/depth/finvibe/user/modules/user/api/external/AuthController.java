package depth.finvibe.user.modules.user.api.external;

import depth.finvibe.user.boot.security.model.AuthenticatedUser;
import depth.finvibe.user.modules.user.application.port.in.AuthCommandUseCase;
import depth.finvibe.user.modules.user.dto.UserDto;
import depth.finvibe.user.shared.dto.Requester;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthCommandUseCase authCommandUseCase;

    @PostMapping("/login")
    public UserDto.TokenResponse login(@RequestBody @Valid UserDto.LoginRequest request) {
        return authCommandUseCase.login(request);
    }

    @PostMapping("/signup")
    public UserDto.SignUpResponse signUp(@RequestBody @Valid UserDto.SignUpRequest request) {
        return authCommandUseCase.signUp(request);
    }

    @PostMapping("/refresh")
    public UserDto.TokenRefreshResponse refreshToken(@RequestBody @Valid UserDto.TokenRefreshRequest request) {
        return authCommandUseCase.refreshToken(request);
    }

    @PostMapping("/logout")
    public void logout(@AuthenticatedUser Requester requester) {
        authCommandUseCase.logout(requester.getUserId());
    }
}
