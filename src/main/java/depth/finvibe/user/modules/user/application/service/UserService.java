package depth.finvibe.user.modules.user.application.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import depth.finvibe.user.modules.user.application.port.out.*;
import depth.finvibe.user.modules.user.domain.RefreshToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.user.modules.user.application.port.in.UserCommandUseCase;
import depth.finvibe.user.modules.user.application.port.in.UserQueryUseCase;
import depth.finvibe.user.modules.user.domain.InterestStock;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.modules.user.domain.vo.Email;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
import depth.finvibe.user.modules.user.domain.vo.OAuthInfo;
import depth.finvibe.user.modules.user.dto.UserDto;
import depth.finvibe.user.shared.dto.Requester;
import depth.finvibe.user.shared.error.DomainException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserCommandUseCase, UserQueryUseCase {

    private final UserRepository userRepository;
    private final InterestStockRepository interestStockRepository;
    private final UserEventPublisher userEventPublisher;
    private final MarketClient marketClient;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryTokenResolver temporaryTokenResolver;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public UserDto.SignUpResponse signUp(UserDto.SignUpRequest request) {
        User savedUser = (request.getTemporaryToken() != null && !request.getTemporaryToken().isBlank())
                ? signUpWithOAuth(request)
                : signUpWithLocal(request);

        userEventPublisher.publishUserSignUpEvent(savedUser.getId());
        
        UserDto.TokenResponse tokens = completeLogin(savedUser);

        return UserDto.SignUpResponse.builder()
                .user(UserDto.UserResponse.from(savedUser))
                .tokens(tokens)
                .build();
    }

    private User signUpWithLocal(UserDto.SignUpRequest request) {
        checkUserAlreadyExist(request);
        User user = createUserFromSignUpRequest(request);
        return userRepository.save(user);
    }

    private User signUpWithOAuth(UserDto.SignUpRequest request) {
        if (!temporaryTokenResolver.isTokenValid(request.getTemporaryToken())) {
            throw new DomainException(UserErrorCode.INVALID_TEMPORARY_TOKEN);
        }

        OAuthInfo oAuthInfo = temporaryTokenResolver.getOAuthInfoFromTemporaryToken(request.getTemporaryToken());
        String email = temporaryTokenResolver.getEmailFromTemporaryToken(request.getTemporaryToken());

        if (userRepository.existsByEmail(new Email(email))) {
            throw new DomainException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.createSocial(oAuthInfo, email, request.getBirthDate(), request.getPhoneNumber());
        return userRepository.save(user);
    }

    private UserDto.TokenResponse completeLogin(User user) {
        UserDto.TokenResponse tokenResponse = tokenProvider.generateToken(user.getId(), user.getRole());
        storeRefreshToken(user.getId(), tokenResponse.getRefreshToken());
        return tokenResponse;
    }

    private void storeRefreshToken(UUID userId, String refreshToken) {
        if (refreshToken == null) {
            return;
        }
        refreshTokenRepository.deleteByUserId(userId);
        refreshTokenRepository.save(RefreshToken.create(userId, refreshToken));
    }

    @Override
    @Transactional
    public UserDto.UserResponse update(UUID userId, UserDto.UpdateUserRequest request, Requester requester) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(UserErrorCode.USER_NOT_FOUND));

        checkLoginIdAlreadyExist(user, request.getLoginId());

        updateUserAttributes(request, user, requester);

        return UserDto.UserResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto.UserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(UserErrorCode.USER_NOT_FOUND));

        user.validateActive();

        return UserDto.UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserDto.FavoriteStockResponse addFavoriteStock(UUID userId, Long stockId, Requester requester) {
        checkStockIsAlreadyAdded(userId, stockId);

        String stockName = marketClient.getStockNameByStockId(stockId)
                .orElseThrow(() -> new DomainException(UserErrorCode.MARKET_DATA_NOT_FOUND));

        InterestStock interestStock = InterestStock.create(userId, stockId, stockName);
        interestStock.validateCreatable(requester.getUserId(), requester.getRole());

        InterestStock saved = interestStockRepository.save(interestStock);

        return UserDto.FavoriteStockResponse.from(saved);
    }

    @Override
    @Transactional
    public UserDto.FavoriteStockResponse removeFavoriteStock(UUID userId, Long stockId, Requester requester) {
        InterestStock interestStock = interestStockRepository.findByUserIdAndStockId(userId, stockId)
                .orElseThrow(() -> new DomainException(UserErrorCode.INTEREST_STOCK_NOT_FOUND));

        interestStock.validateDeletable(requester.getUserId(), requester.getRole());

        interestStockRepository.deleteByUserIdAndStockId(userId, stockId);
        return UserDto.FavoriteStockResponse.from(interestStock);
    }

    @Override
    @Transactional
    public void withdraw(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(UserErrorCode.USER_NOT_FOUND));

        user.withdraw();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto.FavoriteStockResponse> getFavoriteStocks(UUID userId) {
        return interestStockRepository.findAllByUserId(userId).stream()
                .map(UserDto.FavoriteStockResponse::from)
                .collect(Collectors.toList());
    }

    private void updateUserAttributes(UserDto.UpdateUserRequest request, User user, Requester requester) {
        user.update(
                request.getLoginId(),
                request.getPassword(),
                request.getBirthDate(),
                request.getPhoneNumber(),
                passwordEncoder,
                requester.getUserId(),
                requester.getRole());
    }


    private void checkUserAlreadyExist(UserDto.SignUpRequest request) {
        if (userRepository.existsByEmail(new Email(request.getEmail()))) {
            throw new DomainException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByLoginId(new LoginId(request.getLoginId()))) {
            throw new DomainException(UserErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }
    }

    private void checkLoginIdAlreadyExist(User user, String newLoginId) {
        if (newLoginId != null && !user.getLoginId().getValue().equals(newLoginId)) {
            if (userRepository.existsByLoginId(new LoginId(newLoginId))) {
                throw new DomainException(UserErrorCode.LOGIN_ID_ALREADY_EXISTS);
            }
        }
    }

    private User createUserFromSignUpRequest(UserDto.SignUpRequest request) {
        return User.create(request.getLoginId(), request.getPassword(), request.getEmail(), request.getBirthDate(),
                request.getPhoneNumber(), passwordEncoder);
    }

    private void checkStockIsAlreadyAdded(UUID userId, Long stockId) {
        if (interestStockRepository.findByUserIdAndStockId(userId, stockId).isPresent()) {
            throw new DomainException(UserErrorCode.INTEREST_STOCK_ALREADY_EXISTS);
        }
    }
}
