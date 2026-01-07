package depth.finvibe.user.shared.dto;

import java.util.UUID;

public record UserSignUpEvent(
        String id,
        String loginId,
        String email
) {
}
