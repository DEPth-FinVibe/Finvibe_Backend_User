package depth.finvibe.user.shared.dto;

public record UserSignInEvent(
        String id,
        String loginId,
        String email
) {
}
