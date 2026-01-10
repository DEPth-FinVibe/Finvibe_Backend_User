package depth.finvibe.user.shared.dto;

import lombok.Builder;

@Builder
public record SignInEvent(
        String userId
) {
}
