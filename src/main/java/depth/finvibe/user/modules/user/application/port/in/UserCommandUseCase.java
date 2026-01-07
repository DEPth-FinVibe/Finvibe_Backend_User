package depth.finvibe.user.modules.user.application.port.in;

import depth.finvibe.user.modules.user.dto.UserDto;
import java.util.UUID;

public interface UserCommandUseCase {
    UserDto.UserResponse signUp(UserDto.SignUpRequest request);

    UserDto.TokenResponse login(UserDto.LoginRequest request);

    UserDto.FavoriteStockResponse addFavoriteStock(UUID userId, Long stockId);

    UserDto.FavoriteStockResponse removeFavoriteStock(UUID userId, Long stockId);
}
