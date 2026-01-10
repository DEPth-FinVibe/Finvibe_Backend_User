package depth.finvibe.user.modules.user.application.port.out;

import java.util.Optional;
import java.util.UUID;

import depth.finvibe.user.modules.user.domain.RefreshToken;

public interface RefreshTokenRepository {
    void save(RefreshToken refreshToken);

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(UUID userId);

    void deleteByUserIdAndDeviceId(UUID userId, String deviceId);
}
