package depth.finvibe.user.modules.user.infra.client;

import depth.finvibe.user.modules.user.application.port.out.MarketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MarketClientImpl implements MarketClient {

    private final HttpMarketClient httpMarketClient;

    @Override
    public Optional<String> getStockNameByStockId(Long stockId) {
        return Optional.ofNullable(
                httpMarketClient.getStockNameById(stockId)
        );
    }
}
