package depth.finvibe.user.modules.user.api.external;

import depth.finvibe.user.boot.security.model.AuthenticatedUser;
import depth.finvibe.user.modules.user.application.port.in.UserCommandUseCase;
import depth.finvibe.user.modules.user.application.port.in.UserQueryUseCase;
import depth.finvibe.user.modules.user.dto.UserDto;
import depth.finvibe.user.shared.dto.Requester;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
@Tag(name = "회원", description = "회원 API")
public class MemberController {

    private final UserCommandUseCase userCommandUseCase;
    private final UserQueryUseCase userQueryUseCase;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보를 조회합니다.")
    public UserDto.UserResponse getMe(@Parameter(hidden = true) @AuthenticatedUser Requester requester) {
        return userQueryUseCase.getMe(requester.getUserId());
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "회원 정보 수정", description = "회원 정보를 수정합니다.")
    public UserDto.UserResponse update(@PathVariable UUID userId,
                                       @RequestBody UserDto.UpdateUserRequest request,
                                       @Parameter(hidden = true) @AuthenticatedUser Requester requester) {
        return userCommandUseCase.update(userId, request, requester);
    }

    @PostMapping("/{userId}/favorite-stocks/{stockId}")
    @Operation(summary = "관심 종목 추가", description = "사용자의 관심 종목을 추가합니다.")
    public UserDto.FavoriteStockResponse addFavoriteStock(@PathVariable UUID userId,
                                                          @PathVariable Long stockId,
                                                          @Parameter(hidden = true) @AuthenticatedUser Requester requester) {
        return userCommandUseCase.addFavoriteStock(userId, stockId, requester);
    }

    @DeleteMapping("/{userId}/favorite-stocks/{stockId}")
    @Operation(summary = "관심 종목 삭제", description = "사용자의 관심 종목을 삭제합니다.")
    public UserDto.FavoriteStockResponse removeFavoriteStock(@PathVariable UUID userId,
                                                             @PathVariable Long stockId,
                                                             @Parameter(hidden = true) @AuthenticatedUser Requester requester) {
        return userCommandUseCase.removeFavoriteStock(userId, stockId, requester);
    }

    @GetMapping("/{userId}/favorite-stocks")
    @Operation(summary = "관심 종목 조회", description = "사용자의 관심 종목 목록을 조회합니다.")
    public java.util.List<UserDto.FavoriteStockResponse> getFavoriteStocks(@PathVariable UUID userId) {
        return userQueryUseCase.getFavoriteStocks(userId);
    }

    @GetMapping("/check-login-id")
    @Operation(summary = "로그인 아이디 중복 확인", description = "로그인 아이디 중복 여부를 확인합니다.")
    public UserDto.DuplicateCheckResponse checkLoginIdDuplicate(@RequestParam String loginId) {
        return userQueryUseCase.checkLoginIdDuplicate(loginId);
    }

    @GetMapping("/check-email")
    @Operation(summary = "이메일 중복 확인", description = "이메일 중복 여부를 확인합니다.")
    public UserDto.DuplicateCheckResponse checkEmailDuplicate(@RequestParam String email) {
        return userQueryUseCase.checkEmailDuplicate(email);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "회원 탈퇴", description = "회원 계정을 탈퇴 처리합니다.")
    public void withdraw(@Parameter(hidden = true) @AuthenticatedUser Requester requester) {
        userCommandUseCase.withdraw(requester.getUserId());
    }
}
