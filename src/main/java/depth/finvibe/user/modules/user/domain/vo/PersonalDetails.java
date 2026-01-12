package depth.finvibe.user.modules.user.domain.vo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Getter
public class PersonalDetails {
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "firstPart", column = @Column(name = "phone_number_first_part")),
        @AttributeOverride(name = "secondPart", column = @Column(name = "phone_number_second_part")),
        @AttributeOverride(name = "thirdPart", column = @Column(name = "phone_number_third_part"))
    })
    private PhoneNumber phoneNumber;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String nickname;
}
