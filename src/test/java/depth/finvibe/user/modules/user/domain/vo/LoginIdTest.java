package depth.finvibe.user.modules.user.domain.vo;

import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.shared.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginIdTest {

    @Test
    @DisplayName("유효한 로그인 ID 형식(4~20자 영문/숫자)으로 LoginId 객체를 생성한다")
    void create_success() {
        // given
        String loginIdValue = "user123";

        // when
        LoginId loginId = new LoginId(loginIdValue);

        // then
        assertThat(loginId.getValue()).isEqualTo(loginIdValue);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "too_long_login_id_that_exceeds_twenty_characters", "user!", ""})
    @DisplayName("유효하지 않은 로그인 ID 형식인 경우 DomainException이 발생한다")
    void create_fail_invalidFormat(String invalidLoginId) {
        // when & then
        assertThatThrownBy(() -> new LoginId(invalidLoginId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.INVALID_LOGIN_ID_FORMAT);
    }

    @Test
    @DisplayName("로그인 ID 값이 null인 경우 DomainException이 발생한다")
    void create_fail_null() {
        // when & then
        assertThatThrownBy(() -> new LoginId(null))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.INVALID_LOGIN_ID_FORMAT);
    }
}
