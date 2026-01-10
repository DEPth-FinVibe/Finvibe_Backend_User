package depth.finvibe.user.modules.user.application.port.out;

import depth.finvibe.user.modules.user.dto.UserDto;

import java.util.UUID;

public interface TokenProvider {
    UserDto.TokenResponse generateToken(UUID userId);

    UserDto.TokenRefreshResponse refreshToken(String refreshToken);
}
