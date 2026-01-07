package depth.finvibe.user.modules.user.domain;

import depth.finvibe.user.modules.user.domain.enums.UserRole;
import depth.finvibe.user.modules.user.domain.vo.Email;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import depth.finvibe.user.modules.user.domain.vo.OAuthInfo;
import depth.finvibe.user.modules.user.domain.vo.PasswordHash;
import depth.finvibe.user.modules.user.domain.vo.PhoneNumber;
import depth.finvibe.user.shared.domain.TimeStampedBaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@SuperBuilder
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

    public static User create(String loginId, String password, String email, LocalDate birthDate, String phoneNumber, PasswordEncoder passwordEncoder) {
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

    public void withdraw() {
        this.isDeleted = true;
    }
}
