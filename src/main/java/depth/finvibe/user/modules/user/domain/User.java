package depth.finvibe.user.modules.user.domain;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Builder;
import org.springframework.security.crypto.password.PasswordEncoder;

import depth.finvibe.user.modules.user.domain.enums.UserRole;
import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.modules.user.domain.vo.Email;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import depth.finvibe.user.modules.user.domain.vo.OAuthInfo;
import depth.finvibe.user.modules.user.domain.vo.PasswordHash;
import depth.finvibe.user.modules.user.domain.vo.PhoneNumber;
import depth.finvibe.user.shared.domain.TimeStampedBaseEntity;
import depth.finvibe.user.shared.error.DomainException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@SuperBuilder
@Table(name = "users")
public class User extends TimeStampedBaseEntity {
    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Embedded
    private Email email;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "login_id", nullable = true))
    private LoginId loginId;

    @Embedded
    @AttributeOverride(name = "passwordHash", column = @Column(name = "password_hash", nullable = true))
    private PasswordHash passwordHash;

    @Embedded
    private OAuthInfo oAuthInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "firstPart", column = @Column(name = "phone_number_first_part")),
            @AttributeOverride(name = "secondPart", column = @Column(name = "phone_number_second_part")),
            @AttributeOverride(name = "thirdPart", column = @Column(name = "phone_number_third_part"))
    })
    private PhoneNumber phoneNumber;

    @Builder.Default
    @Column(nullable = false)
    private boolean isDeleted = false;

    public static User create(String loginId, String password, String email, LocalDate birthDate, String phoneNumber,
            PasswordEncoder passwordEncoder) {
        String[] phoneParts = phoneNumber.split("-");
        PhoneNumber phone = (phoneParts.length == 3)
                ? new PhoneNumber(phoneParts[0], phoneParts[1], phoneParts[2])
                : null;

        return User.builder()
                .id(UUID.randomUUID())
                .loginId(new LoginId(loginId))
                .passwordHash(PasswordHash.create(password, passwordEncoder))
                .email(new Email(email))
                .birthDate(birthDate)
                .phoneNumber(phone)
                .role(UserRole.USER)
                .build();
    }

    public static User createSocial(OAuthInfo oAuthInfo, String email, LocalDate birthDate, String phoneNumber) {
        String[] phoneParts = phoneNumber.split("-");
        PhoneNumber phone = (phoneParts.length == 3)
                ? new PhoneNumber(phoneParts[0], phoneParts[1], phoneParts[2])
                : null;

        return User.builder()
                .id(UUID.randomUUID())
                .oAuthInfo(oAuthInfo)
                .email(new Email(email))
                .birthDate(birthDate)
                .phoneNumber(phone)
                .role(UserRole.USER)
                .build();
    }

    public void update(String loginId,
            String password,
            LocalDate birthDate,
            String phoneNumber,
            PasswordEncoder passwordEncoder,
            UUID requesterId,
            UserRole requesterRole) {
        validateActive();

        if (requesterId != this.id && requesterRole != UserRole.ADMIN) {
            throw new DomainException(UserErrorCode.UNAUTHORIZED_USER_UPDATE);
        }

        if (loginId != null) {
            this.loginId = new LoginId(loginId);
        }
        if (password != null) {
            this.passwordHash = PasswordHash.create(password, passwordEncoder);
        }
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
        if (phoneNumber != null) {
            String[] phoneParts = phoneNumber.split("-");
            if (phoneParts.length == 3) {
                this.phoneNumber = new PhoneNumber(phoneParts[0], phoneParts[1], phoneParts[2]);
            }
        }
    }

    public void withdraw() {
        validateActive();

        this.isDeleted = true;
    }

    public void validateLogin(String rawPassword, PasswordEncoder passwordEncoder) {
        validateActive();
        if (this.passwordHash == null || !this.passwordHash.matches(rawPassword, passwordEncoder)) {
            throw new DomainException(UserErrorCode.INVALID_PASSWORD);
        }
    }

    public void validateActive() {
        if (this.isDeleted) {
            throw new DomainException(UserErrorCode.USER_DELETED);
        }
    }
}
