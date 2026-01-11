package depth.finvibe.user.modules.user.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import depth.finvibe.user.modules.user.domain.InterestStock;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.enums.AuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserDto {

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class SignUpRequest {
        @NotEmpty
        private String loginId;
        @NotEmpty
        private String password;
        @Email
        @NotEmpty
        private String email;
        @NotNull
        @Past
        private LocalDate birthDate;
        @Pattern(regexp = "^010-\\d{4}-\\d{4}$")
        private String phoneNumber;
        private String temporaryToken; //Nullable (OAuth)
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class SignUpResponse {
        private UserResponse user;
        private TokenResponse tokens;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class LoginRequest {
        private String loginId;
        private String password;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class OAuthLoginRequest {
        private AuthProvider provider;
        private String providerId;
        private String email;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class OAuthLoginResponse {
        private TokenResponse tokens;
        private String temporaryToken;
        private boolean registrationRequired;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class UpdateUserRequest {
        private String loginId;
        private String password;
        private LocalDate birthDate;
        private String phoneNumber;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private OffsetDateTime accessExpiresAt;
        private String refreshToken;
        private OffsetDateTime refreshExpiresAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class TokenRefreshRequest {
        private String refreshToken;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class TokenRefreshResponse {
        private String accessToken;
        private OffsetDateTime accessExpiresAt;
        private String refreshToken;
        private OffsetDateTime refreshExpiresAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class UserResponse {
        private UUID userId;
        private String email;
        private LocalDate birthDate;
        private String phoneNumber;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail().getValue())
                    .birthDate(user.getBirthDate())
                    .phoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber().toString() : null)
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class FavoriteStockResponse {
        private Long stockId;
        private String name;
        private UUID userId;

        public static FavoriteStockResponse from(InterestStock interestStock) {
            return FavoriteStockResponse.builder()
                    .stockId(interestStock.getStockId())
                    .name(interestStock.getStockName())
                    .userId(interestStock.getUserId())
                    .build();
        }
    }
}
