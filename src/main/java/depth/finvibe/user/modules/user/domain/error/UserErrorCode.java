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
  INVALID_LOGIN_ID_FORMAT("INVALID_LOGIN_ID_FORMAT", "error.user.invalid_login_id_format"),
  EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "error.user.email_already_exists"),
  LOGIN_ID_ALREADY_EXISTS("LOGIN_ID_ALREADY_EXISTS", "error.user.login_id_already_exists"),
  USER_NOT_FOUND("USER_NOT_FOUND", "error.user.user_not_found"),
  INVALID_PASSWORD("INVALID_PASSWORD", "error.user.invalid_password"),
  USER_DELETED("USER_DELETED", "error.user.user_deleted"),
  INTEREST_STOCK_NOT_FOUND("INTEREST_STOCK_NOT_FOUND", "error.user.interest_stock_not_found"),
  INTEREST_STOCK_ALREADY_EXISTS("INTEREST_STOCK_ALREADY_EXISTS", "error.user.interest_stock_already_exists"),
  MARKET_DATA_NOT_FOUND("MARKET_DATA_NOT_FOUND", "error.user.market_data_not_found"),
  UNAUTHORIZED_USER_UPDATE("UNAUTHORIZED_USER_UPDATE", "error.user.unauthorized_user_update"),
  UNAUTHORIZED_INTEREST_STOCK_DELETION("UNAUTHORIZED_INTEREST_STOCK_DELETION", "error.user.unauthorized_interest_stock_deletion"),
  UNAUTHORIZED_INTEREST_STOCK_CREATION("UNAUTHORIZED_INTEREST_STOCK_CREATION", "error.user.unauthorized_interest_stock_creation"),
  INVALID_REFRESH_TOKEN("INVALID_REFRESH_TOKEN", "error.user.invalid_refresh_token");

  private final String code;
  private final String messageKey;
}
