package depth.finvibe.user.boot.config;

import depth.finvibe.user.modules.user.infra.client.HttpMarketClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ExternalApiConfig {

    private <T> T createClient(String baseUrl, Class<T> serviceType) {
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultStatusHandler(
                        HttpStatusCode::is4xxClientError,
                        (req, res) -> {

                        }
                )
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(serviceType);
    }

    @Bean
    HttpMarketClient httpMarketClient() {
        return createClient("http://market:80", HttpMarketClient.class);
    }
}
