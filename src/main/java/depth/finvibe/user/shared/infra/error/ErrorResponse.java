package depth.finvibe.user.shared.infra.error;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "에러 응답")
public class ErrorResponse {
  @Schema(description = "HTTP 상태 코드", example = "400")
  private int status;
  @Schema(description = "에러 코드", example = "U001")
  private String code;
  @Schema(description = "에러 메시지", example = "잘못된 요청입니다.")
  private String message;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "필드별 에러 상세")
  private List<FieldErrorResponse> fieldErrors;

  public static ErrorResponse of(int status, String code, String message) {
    return ErrorResponse.builder()
        .status(status)
        .code(code)
        .message(message)
        .build();
  }

  public static ErrorResponse of(int status, String code, String message, List<FieldErrorResponse> fieldErrors) {
    return ErrorResponse.builder()
        .status(status)
        .code(code)
        .message(message)
        .fieldErrors(fieldErrors)
        .build();
  }
}
