package depth.finvibe.user.modules.user.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@SuperBuilder
public class User {
    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Embedded
    private Email email;

    @Embedded
    private LoginId loginId;

    @Embedded
    private PasswordHash passwordHash;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "firstPart", column = @Column(name = "phone_number_first_part")),
        @AttributeOverride(name = "secondPart", column = @Column(name = "phone_number_second_part")),
        @AttributeOverride(name = "thirdPart", column = @Column(name = "phone_number_third_part"))
    })
    private PhoneNumber phoneNumber;
}
