package depth.finvibe.user.modules.user.application.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.user.modules.user.application.port.in.AuthCommandUseCase;
import depth.finvibe.user.modules.user.application.port.out.RefreshTokenRepository;
import depth.finvibe.user.modules.user.application.port.out.TemporaryTokenProvider;
import depth.finvibe.user.modules.user.application.port.out.TemporaryTokenResolver;
import depth.finvibe.user.modules.user.application.port.out.TokenProvider;
import depth.finvibe.user.modules.user.application.port.out.TokenResolver;
import depth.finvibe.user.modules.user.application.port.out.UserEventPublisher;
import depth.finvibe.user.modules.user.application.port.out.UserRepository;
import depth.finvibe.user.modules.user.domain.RefreshToken;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.modules.user.domain.vo.Email;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import depth.finvibe.user.modules.user.domain.vo.OAuthInfo;
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
    private final TemporaryTokenProvider temporaryTokenProvider;
    private final TemporaryTokenResolver temporaryTokenResolver;

    @Override
    @Transactional
    public UserDto.SignUpResponse signUp(UserDto.SignUpRequest request) {
        User savedUser = (request.getTemporaryToken() != null && !request.getTemporaryToken().isBlank())
                ? signUpWithOAuth(request)
                : signUpWithLocal(request);

        userEventPublisher.publishUserSignUpEvent(savedUser.getId());

        UserDto.TokenResponse tokens = issueTokens(savedUser);

        return UserDto.SignUpResponse.builder()
                .user(UserDto.UserResponse.from(savedUser))
                .tokens(tokens)
                .build();
    }

    private User signUpWithLocal(UserDto.SignUpRequest request) {
        checkUserAlreadyExist(request);
        User user = createUserFromSignUpRequest(request);
        return userRepository.save(user);
    }

    private User signUpWithOAuth(UserDto.SignUpRequest request) {
        if (!temporaryTokenResolver.isTokenValid(request.getTemporaryToken())) {
            throw new DomainException(UserErrorCode.INVALID_TEMPORARY_TOKEN);
        }

        OAuthInfo oAuthInfo = temporaryTokenResolver.getOAuthInfoFromTemporaryToken(request.getTemporaryToken());
        String email = temporaryTokenResolver.getEmailFromTemporaryToken(request.getTemporaryToken());
        if (email == null || email.isBlank()) {
            email = request.getEmail();
        }
        if (email == null || email.isBlank()) {
            throw new DomainException(UserErrorCode.INVALID_EMAIL_FORMAT);
        }

        if (userRepository.existsByEmail(new Email(email))) {
            throw new DomainException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.createSocial(oAuthInfo, email, request.getBirthDate(), request.getPhoneNumber());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public UserDto.TokenResponse login(UserDto.LoginRequest request) {
        User user = userRepository.findByLoginId(new LoginId(request.getLoginId()))
                .orElseThrow(() -> new DomainException(UserErrorCode.USER_NOT_FOUND));

        user.validateLogin(request.getPassword(), passwordEncoder);

        return completeLogin(user);
    }

    @Override
    @Transactional
    public UserDto.OAuthLoginResponse oauthLogin(UserDto.OAuthLoginRequest request) {
        OAuthInfo oAuthInfo = OAuthInfo.ofSocial(request.getProvider(), request.getProviderId());

        return userRepository.findByOauthInfo(oAuthInfo)
                .map(this::handleExistingOAuthUser)
                .orElseGet(() -> handleNewOAuthUser(request));
    }

    private UserDto.OAuthLoginResponse handleExistingOAuthUser(User user) {
        user.validateActive();
        return UserDto.OAuthLoginResponse.builder()
                .tokens(completeLogin(user))
                .registrationRequired(false)
                .build();
    }

    private UserDto.OAuthLoginResponse handleNewOAuthUser(UserDto.OAuthLoginRequest request) {
        String temporaryToken = temporaryTokenProvider.generateTemporaryToken(
                request.getProvider(),
                request.getProviderId(),
                request.getEmail());

        return UserDto.OAuthLoginResponse.builder()
                .temporaryToken(temporaryToken)
                .registrationRequired(true)
                .build();
    }

    private UserDto.TokenResponse completeLogin(User user) {
        userEventPublisher.publishUserSignInEvent(user.getId());
        return issueTokens(user);
    }

    @Override
    @Transactional
    public UserDto.TokenRefreshResponse refreshToken(UserDto.TokenRefreshRequest request) {
        RefreshToken refreshToken = getValidRefreshToken(request.getRefreshToken());
        validateRefreshTokenOwner(refreshToken.getUserId());

        UserDto.TokenRefreshResponse response = tokenProvider.refreshToken(request.getRefreshToken());
        storeRefreshToken(refreshToken.getUserId(), response.getRefreshToken());
        return response;
    }

    @Override
    @Transactional
    public void logout(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(UserErrorCode.USER_NOT_FOUND));

        user.validateActive();
        refreshTokenRepository.deleteByUserId(userId);
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

    private void storeRefreshToken(UUID userId, String refreshToken) {
        if (refreshToken == null) {
            return;
        }

        refreshTokenRepository.deleteByUserId(userId);
        refreshTokenRepository.save(RefreshToken.create(userId, refreshToken));
    }

    private void checkUserAlreadyExist(UserDto.SignUpRequest request) {
        if (userRepository.existsByEmail(new Email(request.getEmail()))) {
            throw new DomainException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByLoginId(new LoginId(request.getLoginId()))) {
            throw new DomainException(UserErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }
    }

    private User createUserFromSignUpRequest(UserDto.SignUpRequest request) {
        return User.create(request.getLoginId(), request.getPassword(), request.getEmail(), request.getBirthDate(),
                request.getPhoneNumber(), passwordEncoder);
    }

    private UserDto.TokenResponse issueTokens(User user) {
        UserDto.TokenResponse tokenResponse = tokenProvider.generateToken(user.getId(), user.getRole());
        storeRefreshToken(user.getId(), tokenResponse.getRefreshToken());
        return tokenResponse;
    }
}
