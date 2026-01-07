package depth.finvibe.user.modules.user.application.port.in;

import depth.finvibe.user.modules.user.dto.UserDto;

import java.util.List;
import java.util.UUID;

public interface UserQueryUseCase {
    UserDto.UserResponse getMe(UUID userId);

    List<UserDto.FavoriteStockResponse> getFavoriteStocks(UUID userId);
}
