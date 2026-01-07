package depth.finvibe.user.modules.user.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Embeddable
@Getter
public class PasswordHash {
    private String passwordHash;

    protected PasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public static PasswordHash create(String rawPassword, PasswordEncoder passwordEncoder) {
        return new PasswordHash(passwordEncoder.encode(rawPassword));
    }

    public boolean matches(String rawPassword, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(rawPassword, this.passwordHash);
    }
}
