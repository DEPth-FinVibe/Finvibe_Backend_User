package depth.finvibe.user.modules.user.application.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.user.modules.user.application.port.in.AuthCommandUseCase;
import depth.finvibe.user.modules.user.application.port.out.RefreshTokenRepository;
import depth.finvibe.user.modules.user.application.port.out.TokenProvider;
import depth.finvibe.user.modules.user.application.port.out.TokenResolver;
import depth.finvibe.user.modules.user.application.port.out.UserEventPublisher;
import depth.finvibe.user.modules.user.application.port.out.UserRepository;
import depth.finvibe.user.modules.user.domain.RefreshToken;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import depth.finvibe.user.modules.user.dto.UserDto;
import depth.finvibe.user.shared.error.DomainException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService implements AuthCommandUseCase {

    private final UserRepository userRepository;
    private final UserEventPublisher userEventPublisher;
    private final TokenProvider tokenProvider;
    private final TokenResolver tokenResolver;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserDto.TokenResponse login(String deviceId, UserDto.LoginRequest request) {
        User user = userRepository.findByLoginId(new LoginId(request.getLoginId()))
                .orElseThrow(() -> new DomainException(UserErrorCode.USER_NOT_FOUND));

        user.validateLogin(request.getPassword(), passwordEncoder);

        userEventPublisher.publishUserSignInEvent(user.getId());
        UserDto.TokenResponse tokenResponse = tokenProvider.generateToken(user.getId(), user.getRole());
        storeRefreshToken(user.getId(), deviceId, tokenResponse.getRefreshToken());
        return tokenResponse;
    }

    @Override
    @Transactional
    public UserDto.TokenRefreshResponse refreshToken(String deviceId, UserDto.TokenRefreshRequest request) {
        RefreshToken refreshToken = getValidRefreshToken(request.getRefreshToken());
        validateRefreshTokenOwner(refreshToken.getUserId());

        if (!refreshToken.getDeviceId().equals(deviceId)) {
            throw new DomainException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        UserDto.TokenRefreshResponse response = tokenProvider.refreshToken(request.getRefreshToken());
        storeRefreshToken(refreshToken.getUserId(), deviceId, response.getRefreshToken());
        return response;
    }

    @Override
    @Transactional
    public void logout(UUID userId, String deviceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(UserErrorCode.USER_NOT_FOUND));

        user.validateActive();
        refreshTokenRepository.deleteByUserIdAndDeviceId(userId, deviceId);
    }

    private RefreshToken getValidRefreshToken(String refreshToken) {
        boolean isValid = tokenResolver.isTokenValid(refreshToken);
        if (!isValid) {
            throw new DomainException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        return refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new DomainException(UserErrorCode.INVALID_REFRESH_TOKEN));
    }

    private void validateRefreshTokenOwner(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(UserErrorCode.USER_NOT_FOUND));
        user.validateActive();
    }

    private void storeRefreshToken(UUID userId, String deviceId, String refreshToken) {
        if (refreshToken == null) {
            return;
        }

        refreshTokenRepository.deleteByUserIdAndDeviceId(userId, deviceId);
        refreshTokenRepository.save(RefreshToken.create(userId, deviceId, refreshToken));
    }
}
