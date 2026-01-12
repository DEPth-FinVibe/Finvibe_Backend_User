package depth.finvibe.user.modules.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
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
import depth.finvibe.user.modules.user.application.port.out.TemporaryTokenResolver;
import depth.finvibe.user.modules.user.application.port.out.TokenProvider;
import depth.finvibe.user.modules.user.application.port.out.TokenResolver;
import depth.finvibe.user.modules.user.application.port.out.UserEventPublisher;
import depth.finvibe.user.modules.user.application.port.out.UserRepository;
import depth.finvibe.user.modules.user.domain.RefreshToken;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.enums.AuthProvider;
import depth.finvibe.user.modules.user.domain.enums.UserRole;
import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.modules.user.domain.vo.Email;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import depth.finvibe.user.modules.user.domain.vo.OAuthInfo;
import depth.finvibe.user.modules.user.domain.vo.PasswordHash;
import depth.finvibe.user.modules.user.domain.vo.PersonalDetails;
import depth.finvibe.user.modules.user.domain.vo.PhoneNumber;
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

  @Mock
  private TemporaryTokenResolver temporaryTokenResolver;

  @Nested
  @DisplayName("signUp")
  class SignUpTest {
    @Test
    @DisplayName("local signUp success")
    void signUp_Local_Success() {
      // given
      UserDto.SignUpRequest request = UserDto.SignUpRequest.builder()
          .loginId("user123")
          .password("password")
          .email("test@example.com")
          .name("홍길동")
          .nickname("길동이")
          .birthDate(LocalDate.of(1990, 1, 1))
          .phoneNumber("010-1234-5678")
          .build();

      given(userRepository.existsByEmail(any(Email.class))).willReturn(false);
      given(userRepository.existsByLoginId(any(LoginId.class))).willReturn(false);
      given(passwordEncoder.encode(anyString())).willReturn("encoded");

      LoginId loginId = new LoginId(request.getLoginId());
      PasswordHash passwordHash = PasswordHash.create(request.getPassword(), passwordEncoder);
      PersonalDetails personalDetails = PersonalDetails.of(
          PhoneNumber.parse(request.getPhoneNumber()),
          request.getBirthDate(),
          request.getName(),
          request.getNickname(),
          new Email(request.getEmail()));

      User savedUser = User.create(loginId, passwordHash, personalDetails);

      given(userRepository.save(any(User.class))).willReturn(savedUser);
      given(tokenProvider.generateToken(any(UUID.class), any(UserRole.class)))
          .willReturn(UserDto.TokenResponse.builder()
              .accessToken("access")
              .refreshToken("refresh")
              .build());

      // when
      UserDto.SignUpResponse response = authService.signUp(request);

      // then
      assertThat(response.getUser().getUserId()).isEqualTo(savedUser.getId());
      assertThat(response.getTokens().getAccessToken()).isEqualTo("access");
      verify(userEventPublisher, times(1)).publishUserSignUpEvent(savedUser.getId());
      verify(refreshTokenRepository, times(1)).deleteByUserId(any(UUID.class));
      verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("email already exists")
    void signUp_Fail_EmailExists() {
      // given
      UserDto.SignUpRequest request = UserDto.SignUpRequest.builder()
          .loginId("user123")
          .email("test@example.com")
          .build();
      given(userRepository.existsByEmail(any(Email.class))).willReturn(true);

      // when & then
      assertThatThrownBy(() -> authService.signUp(request))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("loginId already exists")
    void signUp_Fail_LoginIdExists() {
      // given
      UserDto.SignUpRequest request = UserDto.SignUpRequest.builder()
          .loginId("user123")
          .email("test@example.com")
          .build();
      given(userRepository.existsByEmail(any(Email.class))).willReturn(false);
      given(userRepository.existsByLoginId(any(LoginId.class))).willReturn(true);

      // when & then
      assertThatThrownBy(() -> authService.signUp(request))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.LOGIN_ID_ALREADY_EXISTS);
    }
  }

  @Nested
  @DisplayName("oauthSignUp")
  class OAuthSignUpTest {
    @Test
    @DisplayName("oauth signUp success")
    void oauthSignUp_Success() {
      // given
      String tempToken = "temp-token";
      UserDto.OAuthSignUpRequest request = UserDto.OAuthSignUpRequest.builder()
          .temporaryToken(tempToken)
          .email("google@example.com")
          .name("김영희")
          .nickname("영희")
          .birthDate(LocalDate.of(1990, 1, 1))
          .phoneNumber("010-1234-5678")
          .build();

      given(temporaryTokenResolver.isTokenValid(tempToken)).willReturn(true);
      given(temporaryTokenResolver.getOAuthInfoFromTemporaryToken(tempToken))
          .willReturn(OAuthInfo.ofSocial(AuthProvider.GOOGLE, "google-id"));
      given(userRepository.existsByEmail(any(Email.class))).willReturn(false);

      User savedUser = User.createSocial(
          OAuthInfo.ofSocial(AuthProvider.GOOGLE, "google-id"),
          PersonalDetails.of(
              PhoneNumber.parse(request.getPhoneNumber()),
              request.getBirthDate(),
              request.getName(),
              request.getNickname(),
              new Email(request.getEmail())),
          passwordEncoder);
      given(userRepository.save(any(User.class))).willReturn(savedUser);

      given(tokenProvider.generateToken(any(UUID.class), any(UserRole.class)))
          .willReturn(UserDto.TokenResponse.builder()
              .accessToken("access")
              .refreshToken("refresh")
              .build());

      // when
      UserDto.SignUpResponse response = authService.oauthSignUp(request);

      // then
      assertThat(response.getUser().getEmail()).isEqualTo("google@example.com");
      assertThat(response.getTokens().getAccessToken()).isEqualTo("access");
      verify(userEventPublisher, times(1)).publishUserSignUpEvent(savedUser.getId());
    }
  }

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
      UserDto.TokenResponse response = authService.login(request);

      // then
      assertThat(response.getAccessToken()).isEqualTo("token");
      verify(userEventPublisher, times(1)).publishUserSignInEvent(user.getId());
      verify(refreshTokenRepository, times(1)).deleteByUserId(user.getId());
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
      assertThatThrownBy(() -> authService.login(request))
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
          .oauthInfo(OAuthInfo.ofSocial(AuthProvider.GOOGLE, "google-id"))
          .role(UserRole.USER)
          .isDeleted(false)
          .build();

      given(userRepository.findByOauthInfo(any(OAuthInfo.class))).willReturn(Optional.of(user));
      given(tokenProvider.generateToken(user.getId(), user.getRole()))
          .willReturn(UserDto.TokenResponse.builder()
              .accessToken("token")
              .refreshToken("refresh-token")
              .build());

      // when
      UserDto.OAuthLoginResponse response = authService.oauthLogin(request);

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
          .build();

      given(userRepository.findByOauthInfo(any(OAuthInfo.class))).willReturn(Optional.empty());
      given(temporaryTokenProvider.generateTemporaryToken(AuthProvider.GOOGLE, "google-id"))
          .willReturn("temp-token");

      // when
      UserDto.OAuthLoginResponse response = authService.oauthLogin(request);

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
      String newRefreshToken = "new-refresh-token";
      UserDto.TokenRefreshRequest request = UserDto.TokenRefreshRequest.builder()
          .refreshToken(refreshToken)
          .build();

      RefreshToken storedToken = RefreshToken.create(userId, refreshToken);
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
      UserDto.TokenRefreshResponse response = authService.refreshToken(request);

      // then
      assertThat(response.getAccessToken()).isEqualTo("new-token");
      assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);
      verify(refreshTokenRepository, times(1)).deleteByUserId(userId);
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
      assertThatThrownBy(() -> authService.refreshToken(request))
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
      assertThatThrownBy(() -> authService.refreshToken(request))
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
      User user = User.builder()
          .id(userId)
          .isDeleted(false)
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(user));

      // when
      authService.logout(userId);

      // then
      verify(refreshTokenRepository, times(1)).deleteByUserId(userId);
    }
  }
}
