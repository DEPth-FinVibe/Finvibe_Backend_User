package depth.finvibe.user.modules.user.infra.persistence;

import depth.finvibe.user.modules.user.application.port.out.UserRepository;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.vo.Email;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import depth.finvibe.user.modules.user.domain.vo.OAuthInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    @Override
    public User save(User user) {
        return jpaUserRepository.save(user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaUserRepository.findById(id);
    }

    @Override
    public Optional<User> findByLoginId(LoginId loginId) {
        return jpaUserRepository.findByLoginId(loginId);
    }

    @Override
    public Optional<User> findByOauthInfo(OAuthInfo oauthInfo) {
        return jpaUserRepository.findByOauthInfo(oauthInfo);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaUserRepository.existsByPersonalDetails_Email(email);
    }

    @Override
    public boolean existsByLoginId(LoginId loginId) {
        return jpaUserRepository.existsByLoginId(loginId);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return jpaUserRepository.existsByPersonalDetails_Nickname(nickname);
    }
}
