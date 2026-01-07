package depth.finvibe.user.modules.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import depth.finvibe.user.modules.user.application.port.out.InterestStockRepository;
import depth.finvibe.user.modules.user.application.port.out.MarketClient;
import depth.finvibe.user.modules.user.application.port.out.TokenProvider;
import depth.finvibe.user.modules.user.application.port.out.UserEventPublisher;
import depth.finvibe.user.modules.user.application.port.out.UserRepository;
import depth.finvibe.user.modules.user.domain.InterestStock;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.enums.UserRole;
import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.modules.user.domain.vo.Email;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import depth.finvibe.user.modules.user.domain.vo.PasswordHash;
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
  private TokenProvider tokenProvider;

  @Mock
  private MarketClient marketClient;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Nested
  @DisplayName("signUp 테스트")
  class SignUpTest {
    @Test
    @DisplayName("회원가입 성공")
    void signUp_Success() {
      // given
      UserDto.SignUpRequest request = UserDto.SignUpRequest.builder()
          .loginId("testuser")
          .password("password")
          .email("test@example.com")
          .birthDate(LocalDate.of(1990, 1, 1))
          .phoneNumber("010-1234-5678")
          .build();

      given(userRepository.existsByEmail(any(Email.class))).willReturn(false);
      given(userRepository.existsByLoginId(any(LoginId.class))).willReturn(false);
      given(passwordEncoder.encode(any())).willReturn("encodedPassword");

      User user = User.create(request.getLoginId(), request.getPassword(), request.getEmail(),
          request.getBirthDate(), request.getPhoneNumber(), passwordEncoder);
      given(userRepository.save(any(User.class))).willReturn(user);

      // when
      UserDto.UserResponse response = userService.signUp(request);

      // then
      assertThat(response.getEmail()).isEqualTo(request.getEmail());
      verify(userRepository, times(1)).save(any(User.class));
      verify(userEventPublisher, times(1)).publishUserSignUpEvent(any(UUID.class));
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 가입 시도 시 예외 발생")
    void signUp_Fail_EmailExists() {
      // given
      UserDto.SignUpRequest request = UserDto.SignUpRequest.builder()
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
    @DisplayName("이미 존재하는 로그인 아이디로 가입 시도 시 예외 발생")
    void signUp_Fail_LoginIdExists() {
      // given
      UserDto.SignUpRequest request = UserDto.SignUpRequest.builder()
          .email("test@example.com")
          .loginId("testuser")
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
  @DisplayName("login 테스트")
  class LoginTest {
    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
      // given
      UserDto.LoginRequest request = UserDto.LoginRequest.builder()
          .loginId("testuser")
          .password("password")
          .build();

      User user = User.builder()
          .id(UUID.randomUUID())
          .loginId(new LoginId("testuser"))
          .isDeleted(false)
          .build();
      given(passwordEncoder.encode("password")).willReturn("encodedPassword");
      PasswordHash passwordHash = PasswordHash.create("password", passwordEncoder);
      ReflectionTestUtils.setField(user, "passwordHash", passwordHash);

      given(userRepository.findByLoginId(any(LoginId.class))).willReturn(Optional.of(user));
      given(passwordEncoder.matches(request.getPassword(), "encodedPassword")).willReturn(true);
      given(tokenProvider.generateToken(user.getId()))
          .willReturn(UserDto.TokenResponse.builder().accessToken("token").build());

      // when
      UserDto.TokenResponse response = userService.login(request);

      // then
      assertThat(response.getAccessToken()).isEqualTo("token");
      verify(userEventPublisher, times(1)).publishUserSignInEvent(user.getId());
    }

    @Test
    @DisplayName("존재하지 않는 유저 로그인 시도 시 예외 발생")
    void login_Fail_UserNotFound() {
      // given
      UserDto.LoginRequest request = UserDto.LoginRequest.builder()
          .loginId("unknown")
          .build();
      given(userRepository.findByLoginId(any(LoginId.class))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.login(request))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("update 테스트")
  class UpdateTest {
    @Test
    @DisplayName("회원 정보 수정 성공")
    void update_Success() {
      // given
      UUID userId = UUID.randomUUID();
      UserDto.UpdateUserRequest request = UserDto.UpdateUserRequest.builder()
          .loginId("newLoginId")
          .build();
      Requester requester = new Requester(userId, UserRole.USER);

      User user = User.builder()
          .id(userId)
          .loginId(new LoginId("oldLoginId"))
          .email(new Email("test@example.com"))
          .birthDate(LocalDate.of(1990, 1, 1))
          .isDeleted(false)
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(userRepository.existsByLoginId(any(LoginId.class))).willReturn(false);

      // when
      UserDto.UserResponse response = userService.update(userId, request, requester);

      // then
      assertThat(user.getLoginId().getValue()).isEqualTo("newLoginId");
      assertThat(response.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("이미 존재하는 로그인 아이디로 수정 시도 시 예외 발생")
    void update_Fail_LoginIdExists() {
      // given
      UUID userId = UUID.randomUUID();
      UserDto.UpdateUserRequest request = UserDto.UpdateUserRequest.builder()
          .loginId("existingLoginId")
          .build();
      Requester requester = new Requester(userId, UserRole.USER);

      User user = User.builder()
          .id(userId)
          .loginId(new LoginId("oldLoginId"))
          .isDeleted(false)
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(userRepository.existsByLoginId(any(LoginId.class))).willReturn(true);

      // when & then
      assertThatThrownBy(() -> userService.update(userId, request, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.LOGIN_ID_ALREADY_EXISTS);
    }
  }

  @Nested
  @DisplayName("getMe 테스트")
  class GetMeTest {
    @Test
    @DisplayName("내 정보 조회 성공")
    void getMe_Success() {
      // given
      UUID userId = UUID.randomUUID();
      User user = User.builder()
          .id(userId)
          .email(new Email("test@example.com"))
          .birthDate(LocalDate.of(1990, 1, 1))
          .isDeleted(false)
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(user));

      // when
      UserDto.UserResponse response = userService.getMe(userId);

      // then
      assertThat(response.getUserId()).isEqualTo(userId);
      assertThat(response.getEmail()).isEqualTo("test@example.com");
    }
  }

  @Nested
  @DisplayName("FavoriteStock 테스트")
  class FavoriteStockTest {
    @Test
    @DisplayName("관심 종목 추가 성공")
    void addFavoriteStock_Success() {
      // given
      UUID userId = UUID.randomUUID();
      Long stockId = 1L;
      String stockName = "Samsung";

      given(interestStockRepository.findByUserIdAndStockId(userId, stockId)).willReturn(Optional.empty());
      given(marketClient.getStockNameByStockId(stockId)).willReturn(Optional.of(stockName));

      InterestStock interestStock = InterestStock.create(userId, stockId, stockName);
      given(interestStockRepository.save(any(InterestStock.class))).willReturn(interestStock);

      Requester requester = new Requester(userId, UserRole.USER);

      // when
      UserDto.FavoriteStockResponse response = userService.addFavoriteStock(userId, stockId, requester);

      // then
      assertThat(response.getStockId()).isEqualTo(stockId);
      assertThat(response.getName()).isEqualTo(stockName);
      verify(interestStockRepository, times(1)).save(any(InterestStock.class));
    }

    @Test
    @DisplayName("이미 추가된 관심 종목 추가 시도 시 예외 발생")
    void addFavoriteStock_Fail_AlreadyExists() {
      // given
      UUID userId = UUID.randomUUID();
      Long stockId = 1L;
      given(interestStockRepository.findByUserIdAndStockId(userId, stockId))
          .willReturn(Optional.of(InterestStock.create(userId, stockId, "Samsung")));

      Requester requester = new Requester(userId, UserRole.USER);

      // when & then
      assertThatThrownBy(() -> userService.addFavoriteStock(userId, stockId, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.INTEREST_STOCK_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("존재하지 않는 주식 종목 추가 시도 시 예외 발생")
    void addFavoriteStock_Fail_MarketDataNotFound() {
      // given
      UUID userId = UUID.randomUUID();
      Long stockId = 999L;
      given(interestStockRepository.findByUserIdAndStockId(userId, stockId)).willReturn(Optional.empty());
      given(marketClient.getStockNameByStockId(stockId)).willReturn(Optional.empty());

      Requester requester = new Requester(userId, UserRole.USER);

      // when & then
      assertThatThrownBy(() -> userService.addFavoriteStock(userId, stockId, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.MARKET_DATA_NOT_FOUND);
    }

    @Test
    @DisplayName("관심 종목 삭제 성공")
    void removeFavoriteStock_Success() {
      // given
      UUID userId = UUID.randomUUID();
      Long stockId = 1L;
      InterestStock interestStock = InterestStock.create(userId, stockId, "Samsung");

      given(interestStockRepository.findByUserIdAndStockId(userId, stockId)).willReturn(Optional.of(interestStock));

      Requester requester = new Requester(userId, UserRole.USER);

      // when
      UserDto.FavoriteStockResponse response = userService.removeFavoriteStock(userId, stockId, requester);

      // then
      assertThat(response.getStockId()).isEqualTo(stockId);
      verify(interestStockRepository, times(1)).deleteByUserIdAndStockId(userId, stockId);
    }

    @Test
    @DisplayName("권한 없는 관심 종목 삭제 시도 시 예외 발생")
    void removeFavoriteStock_Fail_Unauthorized() {
      // given
      UUID ownerId = UUID.randomUUID();
      UUID otherId = UUID.randomUUID();
      Long stockId = 1L;
      InterestStock interestStock = InterestStock.create(ownerId, stockId, "Samsung");

      given(interestStockRepository.findByUserIdAndStockId(ownerId, stockId)).willReturn(Optional.of(interestStock));

      Requester requester = new Requester(otherId, UserRole.USER);

      // when & then
      assertThatThrownBy(() -> userService.removeFavoriteStock(ownerId, stockId, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.UNAUTHORIZED_INTEREST_STOCK_DELETION);
    }

    @Test
    @DisplayName("존재하지 않는 관심 종목 삭제 시도 시 예외 발생")
    void removeFavoriteStock_Fail_NotFound() {
      // given
      UUID userId = UUID.randomUUID();
      Long stockId = 1L;
      given(interestStockRepository.findByUserIdAndStockId(userId, stockId)).willReturn(Optional.empty());

      Requester requester = new Requester(userId, UserRole.USER);

      // when & then
      assertThatThrownBy(() -> userService.removeFavoriteStock(userId, stockId, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.INTEREST_STOCK_NOT_FOUND);
    }

    @Test
    @DisplayName("관심 종목 목록 조회 성공")
    void getFavoriteStocks_Success() {
      // given
      UUID userId = UUID.randomUUID();
      InterestStock stock1 = InterestStock.create(userId, 1L, "Samsung");
      InterestStock stock2 = InterestStock.create(userId, 2L, "SK Hynix");
      given(interestStockRepository.findAllByUserId(userId)).willReturn(List.of(stock1, stock2));

      // when
      List<UserDto.FavoriteStockResponse> responses = userService.getFavoriteStocks(userId);

      // then
      assertThat(responses).hasSize(2);
      assertThat(responses.get(0).getName()).isEqualTo("Samsung");
      assertThat(responses.get(1).getName()).isEqualTo("SK Hynix");
    }
  }

  @Nested
  @DisplayName("withdraw 테스트")
  class WithdrawTest {
    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdraw_Success() {
      // given
      UUID userId = UUID.randomUUID();
      User user = User.builder()
          .id(userId)
          .isDeleted(false)
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(user));

      // when
      userService.withdraw(userId);

      // then
      assertThat(user.isDeleted()).isTrue();
    }
  }
}
