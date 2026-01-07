package depth.finvibe.user.modules.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InterestStockTest {

    @Test
    @DisplayName("정적 팩토리 메서드 create를 통해 InterestStock 객체를 생성한다")
    void create_success() {
        // given
        UUID userId = UUID.randomUUID();
        Long stockId = 100L;
        String stockName = "삼성전자";

        // when
        InterestStock interestStock = InterestStock.create(userId, stockId, stockName);

        // then
        assertThat(interestStock.getUserId()).isEqualTo(userId);
        assertThat(interestStock.getStockId()).isEqualTo(stockId);
        assertThat(interestStock.getStockName()).isEqualTo(stockName);
    }
}
