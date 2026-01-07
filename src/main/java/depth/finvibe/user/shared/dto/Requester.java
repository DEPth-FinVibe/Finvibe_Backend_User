package depth.finvibe.user.shared.dto;

import depth.finvibe.user.modules.user.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Requester {
  private UUID userId;
  private UserRole role;
}
