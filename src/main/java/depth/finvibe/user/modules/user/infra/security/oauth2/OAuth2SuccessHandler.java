package depth.finvibe.user.modules.user.infra.security.oauth2;

import depth.finvibe.user.modules.user.application.port.in.AuthCommandUseCase;
import depth.finvibe.user.modules.user.domain.enums.AuthProvider;
import depth.finvibe.user.modules.user.dto.UserDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthCommandUseCase authCommandUseCase;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        AuthProvider provider = (AuthProvider) attributes.get("provider");
        String providerId = (String) attributes.get("providerId");
        String email = (String) attributes.get("email");

        UserDto.OAuthLoginRequest loginRequest = UserDto.OAuthLoginRequest.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .build();

        UserDto.OAuthLoginResponse loginResponse = authCommandUseCase.oauthLogin(loginRequest);

        String targetUrl = determineTargetUrl(loginResponse);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String determineTargetUrl(UserDto.OAuthLoginResponse response) {
        String baseUrl = "http://localhost:3000/oauth/callback";

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("registration_required", response.isRegistrationRequired());

        if (response.isRegistrationRequired()) {
            builder.queryParam("temporary_token", response.getTemporaryToken());
        } else {
            builder.queryParam("access_token", response.getTokens().getAccessToken())
                    .queryParam("refresh_token", response.getTokens().getRefreshToken());
        }

        return builder.build().toUriString();
    }
}
