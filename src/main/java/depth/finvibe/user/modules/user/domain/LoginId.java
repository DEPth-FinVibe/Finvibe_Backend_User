package depth.finvibe.user.modules.user.domain;

import depth.finvibe.user.modules.user.domain.error.WalletErrorCode;
import depth.finvibe.user.shared.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginId {
    private static final String LOGIN_ID_REGEX = "^[a-zA-Z0-9]{4,20}$";
    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile(LOGIN_ID_REGEX);

    @Column(name = "login_id")
    private String value;

    public LoginId(String value) {
        if (value == null || !LOGIN_ID_PATTERN.matcher(value).matches()) {
            throw new DomainException(WalletErrorCode.INVALID_LOGIN_ID_FORMAT);
        }
        this.value = value;
    }
}
