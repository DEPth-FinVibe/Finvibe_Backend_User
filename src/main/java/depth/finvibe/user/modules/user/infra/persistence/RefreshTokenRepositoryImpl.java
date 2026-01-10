package depth.finvibe.user.modules.user.infra.persistence;

import depth.finvibe.user.modules.user.application.port.out.RefreshTokenRepository;
import depth.finvibe.user.modules.user.domain.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USER_DEVICE_PREFIX = "user_device_token:";
    private static final long REFRESH_TOKEN_TTL = 14; // 14 days

    @Override
    public void save(RefreshToken refreshToken) {
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken.getToken();
        String userDeviceKey = USER_DEVICE_PREFIX + refreshToken.getUserId() + ":" + refreshToken.getDeviceId();
        
        String value = refreshToken.getUserId() + ":" + refreshToken.getDeviceId();
        
        redisTemplate.opsForValue().set(tokenKey, value, REFRESH_TOKEN_TTL, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(userDeviceKey, refreshToken.getToken(), REFRESH_TOKEN_TTL, TimeUnit.DAYS);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        String value = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + token);
        if (value == null) {
            return Optional.empty();
        }
        
        String[] parts = value.split(":");
        if (parts.length != 2) {
            return Optional.empty();
        }
        
        UUID userId = UUID.fromString(parts[0]);
        String deviceId = parts[1];
        
        return Optional.of(RefreshToken.create(userId, deviceId, token));
    }

    @Override
    public void deleteByUserIdAndDeviceId(UUID userId, String deviceId) {
        String userDeviceKey = USER_DEVICE_PREFIX + userId + ":" + deviceId;
        String token = redisTemplate.opsForValue().get(userDeviceKey);
        if (token != null) {
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + token);
            redisTemplate.delete(userDeviceKey);
        }
    }
}
