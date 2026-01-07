package depth.finvibe.user.modules.user.application.port.out;

import java.util.UUID;

public interface UserEventPublisher {
    void publishUserSignUpEvent(UUID userId);
    void publishUserSignInEvent(UUID userId);
}
