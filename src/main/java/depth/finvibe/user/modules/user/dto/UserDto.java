package depth.finvibe.user.modules.user.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import depth.finvibe.user.modules.user.domain.InterestStock;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.enums.AuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "사용자 관련 DTO")
public class UserDto {

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "일반 회원가입 요청")
    public static class SignUpRequest {
        @Email
        @Schema(description = "이메일", example = "user@example.com")
        private String email;

        @NotEmpty
        @Schema(description = "로그인 아이디", example = "user123")
        private String loginId;

        @NotEmpty
        @Schema(description = "비밀번호", example = "password123!")
        private String password;

        @NotEmpty
        @Schema(description = "닉네임", example = "핀바이브")
        private String nickname;

        @NotEmpty
        @Schema(description = "이름", example = "홍길동")
        private String name;

        @NotNull
        @Past
        @Schema(description = "생년월일", example = "1990-01-01")
        private LocalDate birthDate;

        @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "휴대폰 번호는 010-0000-0000 형식이어야 합니다.")
        @Schema(description = "휴대폰 번호", example = "010-1234-5678")
        private String phoneNumber;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "소셜 회원가입 요청")
    public static class OAuthSignUpRequest {
        @Email
        @Schema(description = "이메일", example = "user@example.com")
        private String email;

        @NotEmpty
        @Schema(description = "닉네임", example = "핀바이브")
        private String nickname;

        @NotEmpty
        @Schema(description = "이름", example = "홍길동")
        private String name;

        @NotNull
        @Past
        @Schema(description = "생년월일", example = "1990-01-01")
        private LocalDate birthDate;

        @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "휴대폰 번호는 010-0000-0000 형식이어야 합니다.")
        @Schema(description = "휴대폰 번호", example = "010-1234-5678")
        private String phoneNumber;

        @NotEmpty
        @Schema(description = "임시 토큰 (소셜 로그인 후 발급받은 토큰)", example = "temp-token-123")
        private String temporaryToken;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(staticName = "of")
    @Schema(description = "회원가입 응답")
    public static class SignUpResponse {
        @Schema(description = "회원 정보")
        private UserResponse user;

        @Schema(description = "토큰 정보")
        private TokenResponse tokens;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "로그인 요청")
    public static class LoginRequest {
        @Schema(description = "로그인 아이디", example = "user123")
        private String loginId;
        @Schema(description = "비밀번호", example = "password123!")
        private String password;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "소셜 로그인 요청")
    public static class OAuthLoginRequest {
        @Schema(description = "인증 제공자", example = "GOOGLE")
        private AuthProvider provider;
        @Schema(description = "제공자 고유 ID", example = "123456789")
        private String providerId;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(staticName = "of")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "소셜 로그인 응답")
    public static class OAuthLoginResponse {
        @Schema(description = "토큰 정보 (기존 회원인 경우)")
        private TokenResponse tokens;
        @Schema(description = "임시 토큰 (신규 회원인 경우 회원가입 시 필요)")
        private String temporaryToken;
        @Schema(description = "추가 정보 입력(회원가입) 필요 여부")
        private boolean registrationRequired;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "회원 정보 수정 요청")
    public static class UpdateUserRequest {
        @Schema(description = "로그인 아이디", example = "user123")
        private String loginId;
        @Schema(description = "기존 비밀번호", example = "oldPassword123!")
        private String oldPassword;
        @Schema(description = "새 비밀번호", example = "newPassword123!")
        private String newPassword;

        @Schema(description = "이메일", example = "user@example.com")
        private String email;
        @Schema(description = "이름", example = "홍길동")
        private String name;
        @Schema(description = "닉네임", example = "핀바이브수정")
        private String nickname;
        @Schema(description = "생년월일", example = "1990-01-01")
        private LocalDate birthDate;
        @Schema(description = "휴대폰 번호", example = "010-9876-5432")
        private String phoneNumber;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "토큰 정보")
    public static class TokenResponse {
        @Schema(description = "Access Token")
        private String accessToken;
        @Schema(description = "Access Token 만료 일시")
        private OffsetDateTime accessExpiresAt;
        @Schema(description = "Refresh Token")
        private String refreshToken;
        @Schema(description = "Refresh Token 만료 일시")
        private OffsetDateTime refreshExpiresAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "토큰 갱신 요청")
    public static class TokenRefreshRequest {
        @NotEmpty
        @Schema(description = "Refresh Token")
        private String refreshToken;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "토큰 갱신 응답")
    public static class TokenRefreshResponse {
        @Schema(description = "새로운 Access Token")
        private String accessToken;
        @Schema(description = "Access Token 만료 일시")
        private OffsetDateTime accessExpiresAt;
        @Schema(description = "새로운 Refresh Token")
        private String refreshToken;
        @Schema(description = "Refresh Token 만료 일시")
        private OffsetDateTime refreshExpiresAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "회원 정보 응답")
    public static class UserResponse {
        @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID userId;
        @Schema(description = "이메일", example = "user@example.com")
        private String email;
        @Schema(description = "닉네임", example = "핀바이브")
        private String nickname;
        @Schema(description = "이름", example = "홍길동")
        private String name;
        @Schema(description = "생년월일", example = "1990-01-01")
        private LocalDate birthDate;
        @Schema(description = "휴대폰 번호", example = "010-1234-5678")
        private String phoneNumber;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .userId(user.getId())
                    .email(user.getPersonalDetails().getEmail().getValue())
                    .nickname(user.getPersonalDetails().getNickname())
                    .name(user.getPersonalDetails().getName())
                    .birthDate(user.getPersonalDetails().getBirthDate())
                    .phoneNumber(user.getPersonalDetails().getPhoneNumber().toString())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "중복 체크 응답")
    public static class DuplicateCheckResponse {
        @Schema(description = "중복 여부 (true: 중복됨, false: 사용 가능)")
        private boolean isDuplicate;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "관심 종목 응답")
    public static class FavoriteStockResponse {
        @Schema(description = "종목 ID", example = "1")
        private Long stockId;
        @Schema(description = "종목명", example = "삼성전자")
        private String name;
        @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
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
