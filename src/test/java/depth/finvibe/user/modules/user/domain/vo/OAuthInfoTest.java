package depth.finvibe.user.modules.user.domain.vo;

import depth.finvibe.user.modules.user.domain.enums.AuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthInfoTest {

    @Test
    @DisplayName("로컬 로그인 정보(LOCAL)를 생성한다")
    void local_success() {
        // when
        OAuthInfo oAuthInfo = OAuthInfo.ofLocal();

        // then
        assertThat(oAuthInfo.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(oAuthInfo.getProviderId()).isNull();
        assertThat(oAuthInfo.isSocial()).isFalse();
    }

    @Test
    @DisplayName("소셜 로그인 정보(GOOGLE, NAVER 등)를 생성한다")
    void social_success() {
        // given
        AuthProvider provider = AuthProvider.GOOGLE;
        String providerId = "google-id-123";

        // when
        OAuthInfo oAuthInfo = OAuthInfo.ofSocial(provider, providerId);

        // then
        assertThat(oAuthInfo.getProvider()).isEqualTo(provider);
        assertThat(oAuthInfo.getProviderId()).isEqualTo(providerId);
        assertThat(oAuthInfo.isSocial()).isTrue();
    }
}
