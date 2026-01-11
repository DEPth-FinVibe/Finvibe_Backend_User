package depth.finvibe.user.modules.user.infra.messaging;

import depth.finvibe.user.modules.user.application.port.out.UserEventPublisher;
import depth.finvibe.user.shared.dto.SignInEvent;
import depth.finvibe.user.shared.dto.SignUpEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserKafkaProducer implements UserEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String USER_SIGNUP_TOPIC = "user.signup.v1";
    private static final String USER_SIGNIN_TOPIC = "user.signin.v1";

    @Override
    public void publishUserSignUpEvent(UUID userId) {
        log.info("Publishing user signup event for userId: {}", userId);

        SignUpEvent signUpEvent = createSignUpEvent(userId);
        kafkaTemplate.send(USER_SIGNUP_TOPIC, userId.toString(), signUpEvent);
    }

    @Override
    public void publishUserSignInEvent(UUID userId) {
        log.info("Publishing user signin event for userId: {}", userId);

        SignInEvent signInEvent = createSignInEvent(userId);
        kafkaTemplate.send(USER_SIGNIN_TOPIC, userId.toString(), signInEvent);
    }

    private SignUpEvent createSignUpEvent(UUID userId) {
        return SignUpEvent.builder()
                .userId(userId.toString())
                .build();
    }

    private SignInEvent createSignInEvent(UUID userId) {
        return SignInEvent.builder()
                .userId(userId.toString())
                .build();
    }
}
