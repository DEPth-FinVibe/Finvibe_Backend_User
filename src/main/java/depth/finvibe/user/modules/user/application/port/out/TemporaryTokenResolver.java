package depth.finvibe.user.modules.user.application.port.out;

import depth.finvibe.user.modules.user.domain.vo.OAuthInfo;

public interface TemporaryTokenResolver {
    boolean isTokenValid(String token);

    OAuthInfo getOAuthInfoFromTemporaryToken(String temporaryToken);

    String getEmailFromTemporaryToken(String temporaryToken);
}
