package depth.finvibe.user.modules.user.application.port.in;

import java.util.UUID;

import depth.finvibe.user.modules.user.dto.UserDto;

public interface AuthCommandUseCase {
    UserDto.TokenResponse login(String deviceId, UserDto.LoginRequest request);

    UserDto.OAuthLoginResponse oauthLogin(String deviceId, UserDto.OAuthLoginRequest request);

    UserDto.TokenRefreshResponse refreshToken(String deviceId, UserDto.TokenRefreshRequest request);

    void logout(UUID userId, String deviceId);
}
