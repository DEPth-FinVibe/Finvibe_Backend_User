package depth.finvibe.user.boot.security.resolver;


import depth.finvibe.user.boot.security.model.AuthenticatedUser;
import depth.finvibe.user.modules.user.domain.enums.UserRole;
import depth.finvibe.user.shared.dto.Requester;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_UUID_CLAIM = "id";
    private static final String ROLE_CLAIM = "role";

    private final ObjectMapper objectMapper;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedUser.class)
                && Requester.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public @Nullable Object resolveArgument(@NonNull MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        String authHeader = getAuthorizationHeader(webRequest);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        String payload = decodePayload(token);
        if (payload == null) {
            return null;
        }

        var claims = objectMapper.readValue(payload, Map.class);

        return new Requester(
                parseUuid(claims.get(USER_UUID_CLAIM)),
                parseRole(claims.get(ROLE_CLAIM))
        );
    }

    private @Nullable String getAuthorizationHeader(NativeWebRequest webRequest) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        return request.getHeader(AUTHORIZATION_HEADER);
    }

    private @Nullable String decodePayload(String token) {
        String[] chunks = token.split("\\.");
        if (chunks.length < 2) {
            return null;
        }
        Base64.Decoder decoder = Base64.getUrlDecoder();
        return new String(decoder.decode(chunks[1]));
    }

    private @Nullable UUID parseUuid(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        return UUID.fromString(value.toString());
    }

    private @Nullable UserRole parseRole(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        return Enum.valueOf(UserRole.class, value.toString());
    }
}
