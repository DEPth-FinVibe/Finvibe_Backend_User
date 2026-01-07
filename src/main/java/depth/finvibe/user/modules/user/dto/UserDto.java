package depth.finvibe.user.modules.user.dto;

import depth.finvibe.user.modules.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserDto {

    @Getter
    @Builder
    public static class SignUpRequest {
        private final UUID userId;
        private final String loginId;
        private final String password;
        private final String email;
        private final LocalDate birthDate;
        private final String phoneNumber;
        private final boolean isDeleted;
        private final String temporaryToken;
    }

    @Getter
    @Builder
    public static class LoginRequest {
        private final String loginId;
        private final String password;
    }

    @Getter
    @Builder
    public static class TokenResponse {
        private final String accessToken;
        private final OffsetDateTime accessExpiresAt;
        private final String refreshToken;
        private final OffsetDateTime refreshExpiresAt;
    }

    @Getter
    @Builder
    public static class UserResponse {
        private final UUID userId;
        private final String email;
        private final LocalDate birthDate;
        private final String phoneNumber;
        private final boolean isDeleted;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail().getValue())
                    .birthDate(user.getBirthDate())
                    .phoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber().toString() : null)
                    .isDeleted(user.isDeleted())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FavoriteStockResponse {
        private final Long interestStockId;
        private final String name;
        private final UUID userId;
    }
}
