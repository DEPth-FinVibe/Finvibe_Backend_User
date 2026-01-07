package depth.finvibe.user.modules.user.domain.error;

import depth.finvibe.user.shared.error.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 유저(User) 모듈 내에서 발생할 수 있는 비즈니스 에러 정의입니다.
 * 
 * <p>
 * {@link DomainErrorCode}를 구현하여 유저 도메인의 고유한 에러 상황을 명세합니다.
 * 각 상수는 에러 식별 코드와 다국어 메시지 키를 포함합니다.
 * </p>
 */
@AllArgsConstructor
@Getter
public enum UserErrorCode implements DomainErrorCode {
  INVALID_PHONE_NUMBER_PARAMS("INVALID_PHONE_NUMBER_PARAMS", "error.user.invalid_phone_number_params"),
  INVALID_EMAIL_FORMAT("INVALID_EMAIL_FORMAT", "error.user.invalid_email_format"),
  INVALID_LOGIN_ID_FORMAT("INVALID_LOGIN_ID_FORMAT", "error.user.invalid_login_id_format")
  ;

  private final String code;
  private final String messageKey;
}
