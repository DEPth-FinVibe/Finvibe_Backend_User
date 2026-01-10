package depth.finvibe.user.modules.user.adapter.in.web;

import depth.finvibe.user.modules.user.application.port.in.UserCommandUseCase;
import depth.finvibe.user.modules.user.application.port.in.UserQueryUseCase;
import depth.finvibe.user.modules.user.dto.UserDto;
import depth.finvibe.user.shared.dto.Requester;
import depth.finvibe.user.boot.security.model.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCommandUseCase userCommandUseCase;
    private final UserQueryUseCase userQueryUseCase;

    @PostMapping("/signup")
    public ResponseEntity<UserDto.SignUpResponse> signUp(@RequestBody UserDto.SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userCommandUseCase.signUp(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto.UserResponse> getMe(@RequestParam UUID userId) {
        return ResponseEntity.ok(userQueryUseCase.getMe(userId));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserDto.UserResponse> update(
            @PathVariable UUID userId,
            @RequestBody UserDto.UpdateUserRequest request,
            @AuthenticatedUser Requester requester) {
        return ResponseEntity.ok(userCommandUseCase.update(userId, request, requester));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> withdraw(@PathVariable UUID userId) {
        userCommandUseCase.withdraw(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/favorite-stocks/{stockId}")
    public ResponseEntity<UserDto.FavoriteStockResponse> addFavoriteStock(
            @PathVariable UUID userId,
            @PathVariable Long stockId,
            @AuthenticatedUser Requester requester) {
        return ResponseEntity.ok(userCommandUseCase.addFavoriteStock(userId, stockId, requester));
    }

    @DeleteMapping("/{userId}/favorite-stocks/{stockId}")
    public ResponseEntity<UserDto.FavoriteStockResponse> removeFavoriteStock(
            @PathVariable UUID userId,
            @PathVariable Long stockId,
            @AuthenticatedUser Requester requester) {
        return ResponseEntity.ok(userCommandUseCase.removeFavoriteStock(userId, stockId, requester));
    }

    @GetMapping("/{userId}/favorite-stocks")
    public ResponseEntity<List<UserDto.FavoriteStockResponse>> getFavoriteStocks(@PathVariable UUID userId) {
        return ResponseEntity.ok(userQueryUseCase.getFavoriteStocks(userId));
    }
}
