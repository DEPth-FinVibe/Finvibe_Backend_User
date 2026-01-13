package depth.finvibe.user.modules.user.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Schema(description = "인증 제공자")
public enum AuthProvider {
    @Schema(description = "자체 로그인")
    LOCAL("LOCAL"),
    @Schema(description = "구글")
    GOOGLE("GOOGLE"),
    @Schema(description = "카카오")
    KAKAO("KAKAO"),
    @Schema(description = "네이버")
    NAVER("NAVER");

    private final String value;
}
