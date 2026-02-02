package depth.finvibe.user.modules.user.api.internal;

import depth.finvibe.user.modules.user.application.port.in.UserQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/members")
@RequiredArgsConstructor
public class InternalMemberController {

    private final UserQueryUseCase userQueryUseCase;

    @GetMapping("/{userId}/nickname")
    public String getNickname(
        @PathVariable String userId
    ) {
        return userQueryUseCase.getNickname(UUID.fromString(userId));
    }
}
