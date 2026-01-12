package depth.finvibe.user.modules.user.domain;

import depth.finvibe.user.modules.user.domain.enums.AuthProvider;
import depth.finvibe.user.modules.user.domain.enums.UserRole;
import depth.finvibe.user.modules.user.domain.vo.Email;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import depth.finvibe.user.modules.user.domain.vo.OAuthInfo;
import depth.finvibe.user.modules.user.domain.vo.PasswordHash;
import depth.finvibe.user.modules.user.domain.vo.PersonalDetails;
import depth.finvibe.user.modules.user.domain.vo.PhoneNumber;
import depth.finvibe.user.shared.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("정적 팩토리 메서드 create를 통해 User 객체를 생성한다")
    void create_success() {
        // given
        String loginId = "user123";
        String password = "password123";
        String email = "test@example.com";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        String phoneNumber = "010-1234-5678";
        PersonalDetails personalDetails = PersonalDetails.of(
                PhoneNumber.parse(phoneNumber),
                birthDate,
                "홍길동",
                "길동이",
                new Email(email));
        LoginId loginIdVo = new LoginId(loginId);
        PasswordHash passwordHash = PasswordHash.create(password, passwordEncoder);

        // when
        User user = User.create(loginIdVo, passwordHash, personalDetails);

        // then
        assertThat(user.getId()).isNotNull();
        assertThat(user.getLoginId().getValue()).isEqualTo(loginId);
        assertThat(user.getPersonalDetails().getEmail().getValue()).isEqualTo(email);
        assertThat(user.getPersonalDetails().getBirthDate()).isEqualTo(birthDate);
        assertThat(user.getPersonalDetails().getPhoneNumber().toString()).isEqualTo(phoneNumber);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getPasswordHash().matches(password, passwordEncoder)).isTrue();
    }

    @Test
    @DisplayName("createSocial 메서드를 통해 소셜 로그인 전용 User 객체를 생성한다")
    void createSocial_success() {
        // given
        OAuthInfo oAuthInfo = OAuthInfo.ofSocial(AuthProvider.GOOGLE, "google-id");
        String email = "google@example.com";
        LocalDate birthDate = LocalDate.of(1995, 5, 5);
        String phoneNumber = "010-9999-9999";
        PersonalDetails personalDetails = PersonalDetails.of(
                PhoneNumber.parse(phoneNumber),
                birthDate,
                "김영희",
                "영희",
                new Email(email));

        // when
        User user = User.createSocial(oAuthInfo, personalDetails, passwordEncoder);

        // then
        assertThat(user.getId()).isNotNull();
        assertThat(user.getOauthInfo().getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.getOauthInfo().getProviderId()).isEqualTo("google-id");
        assertThat(user.getPersonalDetails().getEmail().getValue()).isEqualTo(email);
        assertThat(user.getPersonalDetails().getBirthDate()).isEqualTo(birthDate);
        assertThat(user.getPersonalDetails().getPhoneNumber().toString()).isEqualTo(phoneNumber);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getLoginId()).isNull();
        assertThat(user.getPasswordHash()).isNotNull();
    }

    @Test
    @DisplayName("withdraw 메서드 호출 시 사용자가 탈퇴 상태(isDeleted=true)가 된다")
    void withdraw_success() {
        // given
        User user = User.builder()
                .loginId(null) // loginId, passwordHash 등은 VO이므로 null이 아니어야 함 (실제 생성 시)
                .isDeleted(false)
                .build();

        // when
        user.withdraw();

        // then
        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("전화번호 형식이 잘못된 경우 예외가 발생한다")
    void create_with_invalid_phone_format() {
        // given
        String invalidPhoneNumber = "01012345678"; // 하이픈 없음

        // when & then
        assertThatThrownBy(() -> PhoneNumber.parse(invalidPhoneNumber))
                .isInstanceOf(DomainException.class);
    }
}
