package depth.finvibe.user.modules.user.domain;

import depth.finvibe.user.modules.user.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

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

        // when
        User user = User.create(loginId, password, email, birthDate, phoneNumber, passwordEncoder);

        // then
        assertThat(user.getId()).isNotNull();
        assertThat(user.getLoginId().getValue()).isEqualTo(loginId);
        assertThat(user.getEmail().getValue()).isEqualTo(email);
        assertThat(user.getBirthDate()).isEqualTo(birthDate);
        assertThat(user.getPhoneNumber().toString()).isEqualTo(phoneNumber);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getPasswordHash().matches(password, passwordEncoder)).isTrue();
    }

    @Test
    @DisplayName("withdraw 메서드 호출 시 사용자가 탈퇴 상태(isDeleted=true)가 된다")
    void withdraw_success() {
        // given
        User user = User.builder()
                .loginId(null) // loginId, passwordHash 등은 VO이므로 null이 아니어야 함 (실제 생성 시)
                .email(null)
                .isDeleted(false)
                .build();

        // when
        user.withdraw();

        // then
        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("전화번호 형식이 잘못된 경우 PhoneNumber는 null이 된다 (create 메서드 로직)")
    void create_with_invalid_phone_format() {
        // given
        String loginId = "user123";
        String password = "password123";
        String email = "test@example.com";
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        String invalidPhoneNumber = "01012345678"; // 하이픈 없음

        // when
        User user = User.create(loginId, password, email, birthDate, invalidPhoneNumber, passwordEncoder);

        // then
        assertThat(user.getPhoneNumber()).isNull();
    }
}
