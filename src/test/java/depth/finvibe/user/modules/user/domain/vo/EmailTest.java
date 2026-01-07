package depth.finvibe.user.modules.user.domain.vo;

import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.shared.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    @DisplayName("유효한 이메일 형식으로 Email 객체를 생성한다")
    void create_success() {
        // given
        String emailValue = "test@example.com";

        // when
        Email email = new Email(emailValue);

        // then
        assertThat(email.getValue()).isEqualTo(emailValue);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "test@", "@example.com", ""})
    @DisplayName("유효하지 않은 이메일 형식인 경우 DomainException이 발생한다")
    void create_fail_invalidFormat(String invalidEmail) {
        // when & then
        assertThatThrownBy(() -> new Email(invalidEmail))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("이메일 값이 null인 경우 DomainException이 발생한다")
    void create_fail_null() {
        // when & then
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.INVALID_EMAIL_FORMAT);
    }
}
