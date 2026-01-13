package depth.finvibe.user.modules.user.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Schema(description = "사용자 권한")
public enum UserRole {
    @Schema(description = "일반 사용자")
    USER("ROLE_USER"),
    @Schema(description = "관리자")
    ADMIN("ROLE_ADMIN");

    private final String value;
}
