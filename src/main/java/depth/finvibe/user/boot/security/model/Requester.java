package depth.finvibe.user.boot.security.model;

import depth.finvibe.user.modules.user.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Requester {
    private UUID uuid;
    private UserRole role;
}
