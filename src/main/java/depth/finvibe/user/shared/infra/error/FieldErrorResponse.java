package depth.finvibe.user.shared.infra.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
@Schema(description = "필드 에러 정보")
public class FieldErrorResponse {
  @Schema(description = "에러가 발생한 필드명", example = "email")
  private final String field;
  @Schema(description = "필드 에러 메시지", example = "올바른 이메일 형식이 아닙니다.")
  private final String message;
}
