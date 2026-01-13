package depth.finvibe.user.shared.dto;

import depth.finvibe.user.modules.user.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "인증된 요청자 정보 (토큰에서 추출)")
public class Requester {
  @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID userId;
  @Schema(description = "사용자 권한", example = "USER")
  private UserRole role;
}
