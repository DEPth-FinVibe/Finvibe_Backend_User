package depth.finvibe.user.modules.user.api.external;

import depth.finvibe.user.boot.security.model.AuthenticatedUser;
import depth.finvibe.user.modules.user.application.port.in.UserCommandUseCase;
import depth.finvibe.user.modules.user.application.port.in.UserQueryUseCase;
import depth.finvibe.user.modules.user.dto.UserDto;
import depth.finvibe.user.shared.dto.Requester;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final UserCommandUseCase userCommandUseCase;
    private final UserQueryUseCase userQueryUseCase;

    @GetMapping("/me")
    public UserDto.UserResponse getMe(@AuthenticatedUser Requester requester) {
        return userQueryUseCase.getMe(requester.getUserId());
    }

    @PatchMapping("/{userId}")
    public UserDto.UserResponse update(@PathVariable UUID userId,
                                       @RequestBody UserDto.UpdateUserRequest request,
                                       @AuthenticatedUser Requester requester) {
        return userCommandUseCase.update(userId, request, requester);
    }

    @PostMapping("/{userId}/favorite-stocks/{stockId}")
    public UserDto.FavoriteStockResponse addFavoriteStock(@PathVariable UUID userId,
                                                          @PathVariable Long stockId,
                                                          @AuthenticatedUser Requester requester) {
        return userCommandUseCase.addFavoriteStock(userId, stockId, requester);
    }

    @DeleteMapping("/{userId}/favorite-stocks/{stockId}")
    public UserDto.FavoriteStockResponse removeFavoriteStock(@PathVariable UUID userId,
                                                             @PathVariable Long stockId,
                                                             @AuthenticatedUser Requester requester) {
        return userCommandUseCase.removeFavoriteStock(userId, stockId, requester);
    }

    @GetMapping("/{userId}/favorite-stocks")
    public java.util.List<UserDto.FavoriteStockResponse> getFavoriteStocks(@PathVariable UUID userId) {
        return userQueryUseCase.getFavoriteStocks(userId);
    }

    @DeleteMapping("/{userId}")
    public void withdraw(@PathVariable UUID userId) {
        userCommandUseCase.withdraw(userId);
    }
}
