package depth.finvibe.user.modules.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import depth.finvibe.user.modules.user.application.port.out.InterestStockRepository;
import depth.finvibe.user.modules.user.application.port.out.GamificationClient;
import depth.finvibe.user.modules.user.application.port.out.MarketClient;
import depth.finvibe.user.modules.user.application.port.out.UserEventPublisher;
import depth.finvibe.user.modules.user.application.port.out.UserRepository;
import depth.finvibe.user.modules.user.domain.InterestStock;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.infra.persistence.DailyLoginChecker;
import depth.finvibe.user.modules.user.domain.enums.UserRole;
import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.modules.user.domain.vo.Email;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import depth.finvibe.user.modules.user.domain.vo.PasswordHash;
import depth.finvibe.user.modules.user.domain.vo.PersonalDetails;
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
  private GamificationClient gamificationClient;

  @Mock
  private MarketClient marketClient;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private DailyLoginChecker dailyLoginChecker;

  @Mock
  private UserEventPublisher userEventPublisher;

  @Nested
  @DisplayName("update")
  class UpdateTest {
    @Test
    @DisplayName("success")
    void update_Success() {
      // given
      given(passwordEncoder.encode("old-password")).willReturn("encoded-old");
      User user = createActiveUser(UUID.randomUUID());
      Requester requester = new Requester(user.getId(), UserRole.USER);
      UserDto.UpdateUserRequest request = UserDto.UpdateUserRequest.builder()
          .loginId("user456")
          .oldPassword("old-password")
          .newPassword("new-password")
          .birthDate(LocalDate.of(1991, 2, 2))
          .phoneNumber("010-0000-0000")
          .build();

      given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
      given(userRepository.existsByLoginId(any(LoginId.class))).willReturn(false);
      given(passwordEncoder.matches("old-password", "encoded-old")).willReturn(true);
      given(passwordEncoder.encode("new-password")).willReturn("encoded-new");

      // when
      UserDto.UserResponse response = userService.update(request, requester);

      // then
      assertThat(response.getUserId()).isEqualTo(user.getId());
      assertThat(user.getLoginId().getValue()).isEqualTo("user456");
      assertThat(user.getPersonalDetails().getBirthDate()).isEqualTo(LocalDate.of(1991, 2, 2));
      assertThat(user.getPersonalDetails().getPhoneNumber().toString()).isEqualTo("010-0000-0000");
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
      assertThatThrownBy(() -> userService.update(request, requester))
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
      assertThatThrownBy(() -> userService.update(request, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.LOGIN_ID_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("nickname already exists")
    void update_Fail_NicknameExists() {
      // given
      User user = createActiveUser(UUID.randomUUID());
      Requester requester = new Requester(user.getId(), UserRole.USER);
      UserDto.UpdateUserRequest request = UserDto.UpdateUserRequest.builder()
          .nickname("duplicateNick")
          .build();

      given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
      given(userRepository.existsByNickname("duplicateNick")).willReturn(true);

      // when & then
      assertThatThrownBy(() -> userService.update(request, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.NICKNAME_ALREADY_EXISTS);
    }
  }

  @Nested
  @DisplayName("changeNickname")
  class ChangeNicknameTest {
    @Test
    @DisplayName("success")
    void changeNickname_Success() {
      // given
      User user = createActiveUser(UUID.randomUUID());
      Requester requester = new Requester(user.getId(), UserRole.USER);
      UserDto.ChangeNicknameRequest request = UserDto.ChangeNicknameRequest.builder()
          .nickname("newNick")
          .build();
      given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
      given(userRepository.existsByNickname("newNick")).willReturn(false);

      // when
      UserDto.UserResponse response = userService.changeNickname(request, requester);

      // then
      assertThat(response.getNickname()).isEqualTo("newNick");
      assertThat(user.getPersonalDetails().getNickname()).isEqualTo("newNick");
    }

    @Test
    @DisplayName("nickname already exists")
    void changeNickname_Fail_NicknameExists() {
      // given
      User user = createActiveUser(UUID.randomUUID());
      Requester requester = new Requester(user.getId(), UserRole.USER);
      UserDto.ChangeNicknameRequest request = UserDto.ChangeNicknameRequest.builder()
          .nickname("duplicateNick")
          .build();
      given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
      given(userRepository.existsByNickname("duplicateNick")).willReturn(true);

      // when & then
      assertThatThrownBy(() -> userService.changeNickname(request, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.NICKNAME_ALREADY_EXISTS);
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
      UserDto.FavoriteStockResponse response = userService.addFavoriteStock(stockId, requester);

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
      assertThatThrownBy(() -> userService.addFavoriteStock(stockId, requester))
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
      assertThatThrownBy(() -> userService.addFavoriteStock(stockId, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.MARKET_DATA_NOT_FOUND);
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
      UserDto.FavoriteStockResponse response = userService.removeFavoriteStock(stockId, requester);

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
      assertThatThrownBy(() -> userService.removeFavoriteStock(stockId, requester))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.INTEREST_STOCK_NOT_FOUND);
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

  @Nested
  @DisplayName("checkNicknameDuplicate")
  class CheckNicknameDuplicateTest {
    @Test
    @DisplayName("returns duplicate status")
    void checkNicknameDuplicate_Success() {
      // given
      given(userRepository.existsByNickname("tester")).willReturn(true);

      // when
      UserDto.DuplicateCheckResponse response = userService.checkNicknameDuplicate("tester");

      // then
      assertThat(response.isDuplicate()).isTrue();
    }
  }

  private User createActiveUser(UUID userId) {
    return User.builder()
        .id(userId)
        .loginId(new LoginId("user123"))
        .passwordHash(PasswordHash.create("old-password", passwordEncoder))
        .personalDetails(PersonalDetails.of(
            new PhoneNumber("010", "1234", "5678"),
            LocalDate.of(1990, 1, 1),
            "테스트",
            "tester",
            new Email("test@example.com")))
        .role(UserRole.USER)
        .isDeleted(false)
        .build();
  }
}
