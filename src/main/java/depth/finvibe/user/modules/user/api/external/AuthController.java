package depth.finvibe.user.modules.user.api.external;

import depth.finvibe.user.boot.security.model.AuthenticatedUser;
import depth.finvibe.user.modules.user.application.port.in.AuthCommandUseCase;
import depth.finvibe.user.modules.user.dto.UserDto;
import depth.finvibe.user.shared.dto.Requester;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Auth", description = "인증/인가 관련 API")
public class AuthController {

    private final AuthCommandUseCase authCommandUseCase;

    @Operation(summary = "일반 로그인", description = "아이디와 비밀번호를 사용하여 로그인을 진행합니다.")
    @PostMapping("/login")
    public UserDto.TokenResponse login(@RequestBody @Valid UserDto.LoginRequest request) {
        return authCommandUseCase.login(request);
    }

    @Operation(summary = "일반 회원가입", description = "새로운 회원을 등록합니다.")
    @PostMapping("/signup")
    public UserDto.SignUpResponse signUp(@RequestBody @Valid UserDto.SignUpRequest request) {
        return authCommandUseCase.signUp(request);
    }

    @Operation(summary = "소셜 회원가입", description = "임시 토큰을 사용하여 소셜 회원가입을 완료합니다.")
    @PostMapping("/oauth-signup")
    public UserDto.SignUpResponse oauthSignUp(@RequestBody @Valid UserDto.OAuthSignUpRequest request) {
        return authCommandUseCase.oauthSignUp(request);
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
    @PostMapping("/refresh")
    public UserDto.TokenRefreshResponse refreshToken(@RequestBody @Valid UserDto.TokenRefreshRequest request) {
        return authCommandUseCase.refreshToken(request);
    }

    @Operation(summary = "로그아웃", description = "현재 사용자의 세션을 종료하고 Refresh Token을 무효화합니다.")
    @PostMapping("/logout")
    public void logout(@AuthenticatedUser Requester requester) {
        authCommandUseCase.logout(requester.getUserId());
    }
}
