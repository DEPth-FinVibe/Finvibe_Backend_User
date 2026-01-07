package depth.finvibe.user.modules.user.domain.vo;

import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.shared.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @Test
    @DisplayName("유효한 전화번호 부분들로 PhoneNumber 객체를 생성한다")
    void create_success() {
        // given
        String first = "010";
        String second = "1234";
        String third = "5678";

        // when
        PhoneNumber phoneNumber = new PhoneNumber(first, second, third);

        // then
        assertThat(phoneNumber.getFirstPart()).isEqualTo(first);
        assertThat(phoneNumber.getSecondPart()).isEqualTo(second);
        assertThat(phoneNumber.getThirdPart()).isEqualTo(third);
        assertThat(phoneNumber.toString()).isEqualTo("010-1234-5678");
    }

    @Test
    @DisplayName("전화번호 일부가 null인 경우 DomainException이 발생한다")
    void create_fail_null() {
        // when & then
        assertThatThrownBy(() -> new PhoneNumber("010", null, "5678"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.INVALID_PHONE_NUMBER_PARAMS);
    }
}
