package depth.finvibe.user.modules.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InterestStockIdTest {

    @Test
    @DisplayName("InterestStockId 객체를 생성하고 equals 및 hashCode가 정상 작동하는지 확인한다")
    void equals_and_hashCode_success() {
        // given
        UUID userId = UUID.randomUUID();
        Long stockId = 100L;

        InterestStockId id1 = new InterestStockId(userId, stockId);
        InterestStockId id2 = new InterestStockId(userId, stockId);
        InterestStockId id3 = new InterestStockId(UUID.randomUUID(), stockId);

        // then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        assertThat(id1).isNotEqualTo(id3);
    }
}
