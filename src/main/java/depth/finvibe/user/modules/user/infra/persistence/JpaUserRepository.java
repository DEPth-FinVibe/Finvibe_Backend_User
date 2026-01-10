package depth.finvibe.user.modules.user.infra.persistence;

import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.vo.Email;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByLoginId(LoginId loginId);
    boolean existsByEmail(Email email);
    boolean existsByLoginId(LoginId loginId);
}
