package depth.finvibe.user.modules.user.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("비밀번호를 암호화하여 PasswordHash 객체를 생성한다")
    void create_success() {
        // given
        String rawPassword = "password123";

        // when
        PasswordHash passwordHash = PasswordHash.create(rawPassword, passwordEncoder);

        // then
        assertThat(passwordHash.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, passwordHash.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("입력받은 비밀번호와 암호화된 비밀번호가 일치하는지 확인한다")
    void matches_success() {
        // given
        String rawPassword = "password123";
        PasswordHash passwordHash = PasswordHash.create(rawPassword, passwordEncoder);

        // when
        boolean matches = passwordHash.matches(rawPassword, passwordEncoder);
        boolean notMatches = passwordHash.matches("wrongPassword", passwordEncoder);

        // then
        assertThat(matches).isTrue();
        assertThat(notMatches).isFalse();
    }
}
