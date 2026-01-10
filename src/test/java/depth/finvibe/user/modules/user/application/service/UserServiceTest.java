package depth.finvibe.user.modules.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import depth.finvibe.user.modules.user.application.port.out.InterestStockRepository;
import depth.finvibe.user.modules.user.application.port.out.MarketClient;
import depth.finvibe.user.modules.user.application.port.out.RefreshTokenRepository;
import depth.finvibe.user.modules.user.application.port.out.TemporaryTokenResolver;
import depth.finvibe.user.modules.user.application.port.out.TokenProvider;
import depth.finvibe.user.modules.user.application.port.out.UserEventPublisher;
import depth.finvibe.user.modules.user.application.port.out.UserRepository;
import depth.finvibe.user.modules.user.domain.InterestStock;
import depth.finvibe.user.modules.user.domain.RefreshToken;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.enums.AuthProvider;
import depth.finvibe.user.modules.user.domain.enums.UserRole;
import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.modules.user.domain.vo.Email;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import depth.finvibe.user.modules.user.domain.vo.OAuthInfo;
import depth.finvibe.user.modules.user.domain.vo.PhoneNumber;
import depth.finvibe.user.modules.user.dto.UserDto;
import depth.finvibe.user.shared.dto.Requester;
import depth.finvibe.user.shared.error.DomainException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private InterestStockRepository interestStockRepository;

  @Mock
  private UserEventPublisher userEventPublisher;

  @Mock
  private MarketClient marketClient;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private TemporaryTokenResolver temporaryTokenResolver;

  @Mock
  private TokenProvider tokenProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

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
          .birthDate(LocalDate.of(1990, 1, 1))
          .phoneNumber("010-1234-5678")
          .build();

      given(userRepository.existsByEmail(any(Email.class))).willReturn(false);
      given(userRepository.existsByLoginId(any(LoginId.class))).willReturn(false);
      given(passwordEncoder.encode(anyString())).willReturn("encoded");

      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      given(userRepository.save(userCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

      given(tokenProvider.generateToken(any(UUID.class), any(UserRole.class)))
          .willReturn(UserDto.TokenResponse.builder()
              .accessToken("access")
              .refreshToken("refresh")
              .build());

      // when
      UserDto.SignUpResponse response = userService.signUp(request);

      // then
      User savedUser = userCaptor.getValue();
      assertThat(response.getUser().getUserId()).isEqualTo(savedUser.getId());
      assertThat(response.getTokens().getAccessToken()).isEqualTo("access");
      verify(userEventPublisher, times(1)).publishUserSignUpEvent(savedUser.getId());
      verify(refreshTokenRepository, times(1)).deleteByUserId(any(UUID.class));
      verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("oauth signUp success")
    void signUp_OAuth_Success() {
      // given
      String tempToken = "temp-token";
      UserDto.SignUpRequest request = UserDto.SignUpRequest.builder()
          .temporaryToken(tempToken)
          .birthDate(LocalDate.of(1990, 1, 1))
          .phoneNumber("010-1234-5678")
          .build();

      given(temporaryTokenResolver.isTokenValid(tempToken)).willReturn(true);
      given(temporaryTokenResolver.getOAuthInfoFromTemporaryToken(tempToken))
          .willReturn(OAuthInfo.ofSocial(AuthProvider.GOOGLE, "google-id"));
      given(temporaryTokenResolver.getEmailFromTemporaryToken(tempToken))
          .willReturn("google@example.com");
      given(userRepository.existsByEmail(any(Email.class))).willReturn(false);

      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      given(userRepository.save(userCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

      given(tokenProvider.generateToken(any(UUID.class), any(UserRole.class)))
          .willReturn(UserDto.TokenResponse.builder()
              .accessToken("access")
              .refreshToken("refresh")
              .build());

      // when
      UserDto.SignUpResponse response = userService.signUp(request);

      // then
      User savedUser = userCaptor.getValue();
      assertThat(savedUser.getOauthInfo().getProvider()).isEqualTo(AuthProvider.GOOGLE);
      assertThat(response.getUser().getEmail()).isEqualTo("google@example.com");
      assertThat(response.getTokens().getAccessToken()).isEqualTo("access");
      verify(userEventPublisher, times(1)).publishUserSignUpEvent(savedUser.getId());
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
      assertThatThrownBy(() -> userService.signUp(request))
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
      assertThatThrownBy(() -> userService.signUp(request))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.LOGIN_ID_ALREADY_EXISTS);
    }
  }

  @Nested
  @DisplayName("update")
  class UpdateTest {
    @Test
    @DisplayName("success")
    void update_Success() {
      // given
      User user = createActiveUser(UUID.randomUUID());
      Requester requester = new Requester(user.getId(), UserRole.USER);
      UserDto.UpdateUserRequest request = UserDto.UpdateUserRequest.builder()
          .loginId("user456")
          .password("new-password")
          .birthDate(LocalDate.of(1991, 2, 2))
          .phoneNumber("010-0000-0000")
          .build();

      given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
      given(userRepository.existsByLoginId(any(LoginId.class))).willReturn(false);
      given(passwordEncoder.encode(anyString())).willReturn("encoded");

      // when
      UserDto.UserResponse response = userService.update(user.getId(), request, requester);

      // then
      assertThat(response.getUserId()).isEqualTo(user.getId());
      assertThat(user.getLoginId().getValue()).isEqualTo("user456");
      assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(1991, 2, 2));
      assertThat(user.getPhoneNumber().toString()).isEqualTo("010-0000-0000");
    }

    @Test
    @DisplayName("user not found")
    void update_Fail_UserNotFound() {
      // given
      UUID userId = UUID.randomUUID();
      UserDto.UpdateUserRequest request = UserDto.UpdateUserRequest.builder().build();
      Requester requester = new Requester(userId, UserRole.USER);
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.update(userId, request, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("loginId already exists")
    void update_Fail_LoginIdExists() {
      // given
      User user = createActiveUser(UUID.randomUUID());
      Requester requester = new Requester(user.getId(), UserRole.USER);
      UserDto.UpdateUserRequest request = UserDto.UpdateUserRequest.builder()
          .loginId("user456")
          .build();

      given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
      given(userRepository.existsByLoginId(any(LoginId.class))).willReturn(true);

      // when & then
      assertThatThrownBy(() -> userService.update(user.getId(), request, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.LOGIN_ID_ALREADY_EXISTS);
    }
  }

  @Nested
  @DisplayName("getMe")
  class GetMeTest {
    @Test
    @DisplayName("success")
    void getMe_Success() {
      // given
      User user = createActiveUser(UUID.randomUUID());
      given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

      // when
      UserDto.UserResponse response = userService.getMe(user.getId());

      // then
      assertThat(response.getUserId()).isEqualTo(user.getId());
      assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("deleted user")
    void getMe_Fail_DeletedUser() {
      // given
      User user = createActiveUser(UUID.randomUUID());
      user.withdraw();
      given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

      // when & then
      assertThatThrownBy(() -> userService.getMe(user.getId()))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.USER_DELETED);
    }
  }

  @Nested
  @DisplayName("addFavoriteStock")
  class AddFavoriteStockTest {
    @Test
    @DisplayName("success")
    void addFavoriteStock_Success() {
      // given
      UUID userId = UUID.randomUUID();
      Long stockId = 1L;
      Requester requester = new Requester(userId, UserRole.USER);

      given(interestStockRepository.findByUserIdAndStockId(userId, stockId)).willReturn(Optional.empty());
      given(marketClient.getStockNameByStockId(stockId)).willReturn(Optional.of("ACME"));
      given(interestStockRepository.save(any(InterestStock.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      UserDto.FavoriteStockResponse response = userService.addFavoriteStock(userId, stockId, requester);

      // then
      assertThat(response.getStockId()).isEqualTo(stockId);
      assertThat(response.getName()).isEqualTo("ACME");
      assertThat(response.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("already added")
    void addFavoriteStock_Fail_AlreadyAdded() {
      // given
      UUID userId = UUID.randomUUID();
      Long stockId = 1L;
      Requester requester = new Requester(userId, UserRole.USER);
      InterestStock interestStock = InterestStock.create(userId, stockId, "ACME");
      given(interestStockRepository.findByUserIdAndStockId(userId, stockId)).willReturn(Optional.of(interestStock));

      // when & then
      assertThatThrownBy(() -> userService.addFavoriteStock(userId, stockId, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.INTEREST_STOCK_ALREADY_EXISTS);
      verify(marketClient, never()).getStockNameByStockId(anyLong());
    }

    @Test
    @DisplayName("market data not found")
    void addFavoriteStock_Fail_MarketDataNotFound() {
      // given
      UUID userId = UUID.randomUUID();
      Long stockId = 1L;
      Requester requester = new Requester(userId, UserRole.USER);
      given(interestStockRepository.findByUserIdAndStockId(userId, stockId)).willReturn(Optional.empty());
      given(marketClient.getStockNameByStockId(stockId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.addFavoriteStock(userId, stockId, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.MARKET_DATA_NOT_FOUND);
    }

    @Test
    @DisplayName("unauthorized requester")
    void addFavoriteStock_Fail_Unauthorized() {
      // given
      UUID userId = UUID.randomUUID();
      Long stockId = 1L;
      Requester requester = new Requester(UUID.randomUUID(), UserRole.USER);
      given(interestStockRepository.findByUserIdAndStockId(userId, stockId)).willReturn(Optional.empty());
      given(marketClient.getStockNameByStockId(stockId)).willReturn(Optional.of("ACME"));

      // when & then
      assertThatThrownBy(() -> userService.addFavoriteStock(userId, stockId, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.UNAUTHORIZED_INTEREST_STOCK_CREATION);
      verify(interestStockRepository, never()).save(any(InterestStock.class));
    }
  }

  @Nested
  @DisplayName("removeFavoriteStock")
  class RemoveFavoriteStockTest {
    @Test
    @DisplayName("success")
    void removeFavoriteStock_Success() {
      // given
      UUID userId = UUID.randomUUID();
      Long stockId = 1L;
      Requester requester = new Requester(userId, UserRole.USER);
      InterestStock interestStock = InterestStock.create(userId, stockId, "ACME");
      given(interestStockRepository.findByUserIdAndStockId(userId, stockId))
          .willReturn(Optional.of(interestStock));

      // when
      UserDto.FavoriteStockResponse response = userService.removeFavoriteStock(userId, stockId, requester);

      // then
      assertThat(response.getStockId()).isEqualTo(stockId);
      verify(interestStockRepository, times(1)).deleteByUserIdAndStockId(userId, stockId);
    }

    @Test
    @DisplayName("not found")
    void removeFavoriteStock_Fail_NotFound() {
      // given
      UUID userId = UUID.randomUUID();
      Long stockId = 1L;
      Requester requester = new Requester(userId, UserRole.USER);
      given(interestStockRepository.findByUserIdAndStockId(userId, stockId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.removeFavoriteStock(userId, stockId, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.INTEREST_STOCK_NOT_FOUND);
    }

    @Test
    @DisplayName("unauthorized requester")
    void removeFavoriteStock_Fail_Unauthorized() {
      // given
      UUID userId = UUID.randomUUID();
      Long stockId = 1L;
      Requester requester = new Requester(UUID.randomUUID(), UserRole.USER);
      InterestStock interestStock = InterestStock.create(userId, stockId, "ACME");
      given(interestStockRepository.findByUserIdAndStockId(userId, stockId))
          .willReturn(Optional.of(interestStock));

      // when & then
      assertThatThrownBy(() -> userService.removeFavoriteStock(userId, stockId, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.UNAUTHORIZED_INTEREST_STOCK_DELETION);
      verify(interestStockRepository, never()).deleteByUserIdAndStockId(any(UUID.class), anyLong());
    }
  }

  @Nested
  @DisplayName("withdraw")
  class WithdrawTest {
    @Test
    @DisplayName("success")
    void withdraw_Success() {
      // given
      User user = createActiveUser(UUID.randomUUID());
      given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

      // when
      userService.withdraw(user.getId());

      // then
      assertThat(user.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("user not found")
    void withdraw_Fail_UserNotFound() {
      // given
      UUID userId = UUID.randomUUID();
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.withdraw(userId))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("getFavoriteStocks")
  class GetFavoriteStocksTest {
    @Test
    @DisplayName("success")
    void getFavoriteStocks_Success() {
      // given
      UUID userId = UUID.randomUUID();
      InterestStock first = InterestStock.create(userId, 1L, "ACME");
      InterestStock second = InterestStock.create(userId, 2L, "BETA");
      given(interestStockRepository.findAllByUserId(userId)).willReturn(List.of(first, second));

      // when
      List<UserDto.FavoriteStockResponse> response = userService.getFavoriteStocks(userId);

      // then
      assertThat(response).hasSize(2);
      assertThat(response.get(0).getStockId()).isEqualTo(1L);
      assertThat(response.get(1).getName()).isEqualTo("BETA");
    }
  }

  private User createActiveUser(UUID userId) {
    return User.builder()
        .id(userId)
        .email(new Email("test@example.com"))
        .loginId(new LoginId("user123"))
        .birthDate(LocalDate.of(1990, 1, 1))
        .phoneNumber(new PhoneNumber("010", "1234", "5678"))
        .role(UserRole.USER)
        .isDeleted(false)
        .build();
  }
}
