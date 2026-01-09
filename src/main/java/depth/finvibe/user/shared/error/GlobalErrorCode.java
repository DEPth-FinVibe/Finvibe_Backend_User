package depth.finvibe.user.shared.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements DomainErrorCode {
  INVALID_REQUEST("INVALID_REQUEST", "error.invalid_request"),
  METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "error.method_not_allowed"),
  UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", "error.unsupported_media_type"),
  NOT_ACCEPTABLE("NOT_ACCEPTABLE", "error.not_acceptable"),
  NOT_FOUND("NOT_FOUND", "error.not_found"),
  INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "error.internal_server_error");

  private final String code;
  private final String messageKey;

  public static GlobalErrorCode fromStatus(HttpStatusCode status) {
    int value = status.value();
    if (value == HttpStatus.BAD_REQUEST.value()) {
      return INVALID_REQUEST;
    }
    if (value == HttpStatus.NOT_FOUND.value()) {
      return NOT_FOUND;
    }
    if (value == HttpStatus.METHOD_NOT_ALLOWED.value()) {
      return METHOD_NOT_ALLOWED;
    }
    if (value == HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()) {
      return UNSUPPORTED_MEDIA_TYPE;
    }
    if (value == HttpStatus.NOT_ACCEPTABLE.value()) {
      return NOT_ACCEPTABLE;
    }
    return INTERNAL_SERVER_ERROR;
  }
}
