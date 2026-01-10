package depth.finvibe.user.modules.user.infra.security;

import depth.finvibe.user.modules.user.application.port.out.TemporaryTokenProvider;
import depth.finvibe.user.modules.user.application.port.out.TemporaryTokenResolver;
import depth.finvibe.user.modules.user.domain.enums.AuthProvider;
import depth.finvibe.user.modules.user.domain.vo.OAuthInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTemporaryTokenProvider implements TemporaryTokenProvider, TemporaryTokenResolver {
    private static final String CLAIM_PROVIDER = "provider";
    private static final String CLAIM_PROVIDER_ID = "provider_id";
    private static final String CLAIM_EMAIL = "email";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long temporaryTokenExpiration;

    private SecretKey key;

    @PostConstruct
    protected void init() {
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateTemporaryToken(AuthProvider provider, String providerId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + temporaryTokenExpiration);

        return Jwts.builder()
                .claim(CLAIM_PROVIDER, provider.name())
                .claim(CLAIM_PROVIDER_ID, providerId)
                .claim(CLAIM_EMAIL, email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    @Override
    public OAuthInfo getOAuthInfoFromTemporaryToken(String temporaryToken) {
        Claims claims = parseClaims(temporaryToken);
        AuthProvider provider = AuthProvider.valueOf(claims.get(CLAIM_PROVIDER).toString());
        String providerId = claims.get(CLAIM_PROVIDER_ID).toString();
        return OAuthInfo.ofSocial(provider, providerId);
    }

    @Override
    public String getEmailFromTemporaryToken(String temporaryToken) {
        Claims claims = parseClaims(temporaryToken);
        Object email = claims.get(CLAIM_EMAIL);
        return email != null ? email.toString() : null;
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
