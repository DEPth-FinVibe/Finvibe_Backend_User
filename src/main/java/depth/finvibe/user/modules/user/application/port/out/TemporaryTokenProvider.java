package depth.finvibe.user.modules.user.application.port.out;

import depth.finvibe.user.modules.user.domain.enums.AuthProvider;

public interface TemporaryTokenProvider {
    String generateTemporaryToken(AuthProvider provider, String providerId);
}
