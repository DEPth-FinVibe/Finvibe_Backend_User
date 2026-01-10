package depth.finvibe.user.modules.user.application.port.out;

import depth.finvibe.user.modules.user.dto.UserDto;

import java.util.UUID;

import depth.finvibe.user.modules.user.domain.enums.UserRole;

public interface TokenProvider {
    UserDto.TokenResponse generateToken(UUID userId, UserRole role);

    UserDto.TokenRefreshResponse refreshToken(String refreshToken);
}
