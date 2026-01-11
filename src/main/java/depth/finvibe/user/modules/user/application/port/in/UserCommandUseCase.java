package depth.finvibe.user.modules.user.application.port.in;

import java.util.UUID;

import depth.finvibe.user.modules.user.dto.UserDto;
import depth.finvibe.user.shared.dto.Requester;

public interface UserCommandUseCase {
    UserDto.UserResponse update(UUID userId, UserDto.UpdateUserRequest request, Requester requester);

    UserDto.FavoriteStockResponse addFavoriteStock(UUID userId, Long stockId, Requester requester);

    UserDto.FavoriteStockResponse removeFavoriteStock(UUID userId, Long stockId, Requester requester);

    void withdraw(UUID userId);
}
