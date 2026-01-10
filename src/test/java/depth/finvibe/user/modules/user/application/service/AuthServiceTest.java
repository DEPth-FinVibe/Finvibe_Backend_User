package depth.finvibe.user.modules.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import depth.finvibe.user.modules.user.application.port.out.RefreshTokenRepository;
import depth.finvibe.user.modules.user.application.port.out.TemporaryTokenProvider;
import depth.finvibe.user.modules.user.application.port.out.TokenProvider;
import depth.finvibe.user.modules.user.application.port.out.TokenResolver;
import depth.finvibe.user.modules.user.application.port.out.UserEventPublisher;
import depth.finvibe.user.modules.user.application.port.out.UserRepository;
import depth.finvibe.user.modules.user.domain.RefreshToken;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.enums.AuthProvider;
import depth.finvibe.user.modules.user.domain.enums.UserRole;
import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import depth.finvibe.user.modules.user.domain.vo.OAuthInfo;
import depth.finvibe.user.modules.user.domain.vo.PasswordHash;
import depth.finvibe.user.modules.user.dto.UserDto;
import depth.finvibe.user.shared.error.DomainException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @InjectMocks
  private AuthService authService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserEventPublisher userEventPublisher;

  @Mock
  private TokenProvider tokenProvider;

  @Mock
  private TokenResolver tokenResolver;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private TemporaryTokenProvider temporaryTokenProvider;

  @Nested
  @DisplayName("login")
  class LoginTest {
    @Test
    @DisplayName("success")
    void login_Success() {
      // given
      UserDto.LoginRequest request = UserDto.LoginRequest.builder()
          .loginId("testuser")
          .password("password")
          .build();

      User user = User.builder()
          .id(UUID.randomUUID())
          .loginId(new LoginId("testuser"))
          .role(UserRole.USER)
          .isDeleted(false)
          .build();
      given(passwordEncoder.encode("password")).willReturn("encodedPassword");
      PasswordHash passwordHash = PasswordHash.create("password", passwordEncoder);
      ReflectionTestUtils.setField(user, "passwordHash", passwordHash);

      given(userRepository.findByLoginId(any(LoginId.class))).willReturn(Optional.of(user));
      given(passwordEncoder.matches(request.getPassword(), "encodedPassword")).willReturn(true);
      given(tokenProvider.generateToken(user.getId(), user.getRole()))
          .willReturn(UserDto.TokenResponse.builder()
              .accessToken("token")
              .refreshToken("refresh-token")
              .build());

      // when
      UserDto.TokenResponse response = authService.login("device-1", request);

      // then
      assertThat(response.getAccessToken()).isEqualTo("token");
      verify(userEventPublisher, times(1)).publishUserSignInEvent(user.getId());
      verify(refreshTokenRepository, times(1)).deleteByUserIdAndDeviceId(user.getId(), "device-1");
      verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("user not found")
    void login_Fail_UserNotFound() {
      // given
      UserDto.LoginRequest request = UserDto.LoginRequest.builder()
          .loginId("unknown")
          .build();
      given(userRepository.findByLoginId(any(LoginId.class))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> authService.login("device-1", request))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("oauthLogin")
  class OAuthLoginTest {
    @Test
    @DisplayName("existing user - success login")
    void oauthLogin_ExistingUser_Success() {
      // given
      UserDto.OAuthLoginRequest request = UserDto.OAuthLoginRequest.builder()
          .provider(AuthProvider.GOOGLE)
          .providerId("google-id")
          .build();

      User user = User.builder()
          .id(UUID.randomUUID())
          .oAuthInfo(OAuthInfo.ofSocial(AuthProvider.GOOGLE, "google-id"))
          .role(UserRole.USER)
          .isDeleted(false)
          .build();

      given(userRepository.findByOAuthInfo(any(OAuthInfo.class))).willReturn(Optional.of(user));
      given(tokenProvider.generateToken(user.getId(), user.getRole()))
          .willReturn(UserDto.TokenResponse.builder()
              .accessToken("token")
              .refreshToken("refresh-token")
              .build());

      // when
      UserDto.OAuthLoginResponse response = authService.oauthLogin("device-1", request);

      // then
      assertThat(response.isRegistrationRequired()).isFalse();
      assertThat(response.getTokens().getAccessToken()).isEqualTo("token");
      verify(userEventPublisher, times(1)).publishUserSignInEvent(user.getId());
    }

    @Test
    @DisplayName("new user - returns temporary token")
    void oauthLogin_NewUser_ReturnsTemporaryToken() {
      // given
      UserDto.OAuthLoginRequest request = UserDto.OAuthLoginRequest.builder()
          .provider(AuthProvider.GOOGLE)
          .providerId("google-id")
          .email("new@example.com")
          .build();

      given(userRepository.findByOAuthInfo(any(OAuthInfo.class))).willReturn(Optional.empty());
      given(temporaryTokenProvider.generateTemporaryToken(AuthProvider.GOOGLE, "google-id", "new@example.com"))
          .willReturn("temp-token");

      // when
      UserDto.OAuthLoginResponse response = authService.oauthLogin("device-1", request);

      // then
      assertThat(response.isRegistrationRequired()).isTrue();
      assertThat(response.getTemporaryToken()).isEqualTo("temp-token");
      assertThat(response.getTokens()).isNull();
    }
  }

  @Nested
  @DisplayName("refreshToken")
  class RefreshTokenTest {
    @Test
    @DisplayName("success")
    void refreshToken_Success() {
      // given
      UUID userId = UUID.randomUUID();
      String refreshToken = "refresh-token";
      String deviceId = "device-1";
      String newRefreshToken = "new-refresh-token";
      UserDto.TokenRefreshRequest request = UserDto.TokenRefreshRequest.builder()
          .refreshToken(refreshToken)
          .build();

      RefreshToken storedToken = RefreshToken.create(userId, deviceId, refreshToken);
      User user = User.builder()
          .id(userId)
          .isDeleted(false)
          .build();

      given(tokenResolver.isTokenValid(refreshToken)).willReturn(true);
      given(refreshTokenRepository.findByToken(refreshToken)).willReturn(Optional.of(storedToken));
      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(tokenProvider.refreshToken(refreshToken))
          .willReturn(UserDto.TokenRefreshResponse.builder()
              .accessToken("new-token")
              .refreshToken(newRefreshToken)
              .build());

      // when
      UserDto.TokenRefreshResponse response = authService.refreshToken(deviceId, request);

      // then
      assertThat(response.getAccessToken()).isEqualTo("new-token");
      assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);
      verify(refreshTokenRepository, times(1)).deleteByUserIdAndDeviceId(userId, deviceId);
      verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("invalid token")
    void refreshToken_Fail_InvalidToken() {
      // given
      String refreshToken = "invalid";
      UserDto.TokenRefreshRequest request = UserDto.TokenRefreshRequest.builder()
          .refreshToken(refreshToken)
          .build();

      given(tokenResolver.isTokenValid(refreshToken)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> authService.refreshToken("device-1", request))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("not stored")
    void refreshToken_Fail_NotStored() {
      // given
      String refreshToken = "refresh-token";
      UserDto.TokenRefreshRequest request = UserDto.TokenRefreshRequest.builder()
          .refreshToken(refreshToken)
          .build();

      given(tokenResolver.isTokenValid(refreshToken)).willReturn(true);
      given(refreshTokenRepository.findByToken(refreshToken)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> authService.refreshToken("device-1", request))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.INVALID_REFRESH_TOKEN);
    }
  }

  @Nested
  @DisplayName("logout")
  class LogoutTest {
    @Test
    @DisplayName("success")
    void logout_Success() {
      // given
      UUID userId = UUID.randomUUID();
      String deviceId = "device-1";
      User user = User.builder()
          .id(userId)
          .isDeleted(false)
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(user));

      // when
      authService.logout(userId, deviceId);

      // then
      verify(refreshTokenRepository, times(1)).deleteByUserIdAndDeviceId(userId, deviceId);
    }
  }
}
