package depth.finvibe.user.modules.user.domain;

import java.time.LocalDate;
import java.util.UUID;

import depth.finvibe.user.modules.user.domain.vo.*;
import jakarta.persistence.*;
import lombok.Builder;
import org.springframework.security.crypto.password.PasswordEncoder;

import depth.finvibe.user.modules.user.domain.enums.UserRole;
import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
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
    @AttributeOverride(name = "value", column = @Column(name = "login_id", nullable = false))
    private LoginId loginId;

    @Embedded
    @AttributeOverride(name = "passwordHash", column = @Column(name = "password_hash", nullable = false))
    private PasswordHash passwordHash;

    @Embedded
    private OAuthInfo oauthInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Embedded
    private PersonalDetails personalDetails;

    @Builder.Default
    @Column(nullable = false)
    private boolean isDeleted = false;

    public static User create(String loginId, String password, String email, PersonalDetails personalDetails, PasswordEncoder passwordEncoder) {
        return User.builder()
                .id(UUID.randomUUID())
                .loginId(new LoginId(loginId))
                .passwordHash(PasswordHash.create(password, passwordEncoder))
                .email(new Email(email))
                .personalDetails(personalDetails)
                .role(UserRole.USER)
                .build();
    }

    public static User createSocial(OAuthInfo oAuthInfo, String email, PersonalDetails personalDetails, PasswordEncoder passwordEncoder) {
        String randomPassword = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12);

        return User.builder()
                .id(UUID.randomUUID())
                .oauthInfo(oAuthInfo)
                .email(new Email(email))
                .passwordHash(PasswordHash.create(randomPassword, passwordEncoder))
                .personalDetails(personalDetails)
                .role(UserRole.USER)
                .build();
    }

    public void withdraw() {
        validateActive();

        this.isDeleted = true;
    }

    public void update(
            String loginId,
            String password,
            LocalDate birthDate,
            String phoneNumber,
            PasswordEncoder passwordEncoder,
            UUID requesterId,
            UserRole requesterRole) {
        validateActive();
        validateUpdatable(requesterId, requesterRole);

        if (loginId != null) {
            this.loginId = new LoginId(loginId);
        }

        if (password != null) {
            this.passwordHash = PasswordHash.create(password, passwordEncoder);
        }

        if (birthDate != null || phoneNumber != null) {
            PhoneNumber newPhoneNumber = phoneNumber != null
                    ? PhoneNumber.parse(phoneNumber)
                    : this.personalDetails.getPhoneNumber();
            LocalDate newBirthDate = birthDate != null
                    ? birthDate
                    : this.personalDetails.getBirthDate();

            this.personalDetails = PersonalDetails.of(
                    newPhoneNumber,
                    newBirthDate,
                    this.personalDetails.getName(),
                    this.personalDetails.getNickname());
        }
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

    private void validateUpdatable(UUID requesterId, UserRole requesterRole) {
        if (!this.id.equals(requesterId) && requesterRole != UserRole.ADMIN) {
            throw new DomainException(UserErrorCode.UNAUTHORIZED_USER_UPDATE);
        }
    }
}
