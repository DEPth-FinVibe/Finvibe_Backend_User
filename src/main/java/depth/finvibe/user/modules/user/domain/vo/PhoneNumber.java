package depth.finvibe.user.modules.user.domain.vo;

import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.shared.error.DomainException;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneNumber {
    private String firstPart;
    private String secondPart;
    private String thirdPart;

    public PhoneNumber(String firstPart, String secondPart, String thirdPart) {
        if(firstPart == null || secondPart == null || thirdPart == null) {
            throw new DomainException(UserErrorCode.INVALID_PHONE_NUMBER_PARAMS);
        }

        this.firstPart = firstPart;
        this.secondPart = secondPart;
        this.thirdPart = thirdPart;
    }

    @Override
    public String toString() {
        return String.format("%s-%s-%s", firstPart, secondPart, thirdPart);
    }
}
