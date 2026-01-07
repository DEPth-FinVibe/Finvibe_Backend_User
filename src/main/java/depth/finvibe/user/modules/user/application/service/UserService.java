package depth.finvibe.user.modules.user.application.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.user.modules.user.application.port.in.UserCommandUseCase;
import depth.finvibe.user.modules.user.application.port.in.UserQueryUseCase;
import depth.finvibe.user.modules.user.application.port.out.InterestStockRepository;
import depth.finvibe.user.modules.user.application.port.out.MarketClient;
import depth.finvibe.user.modules.user.application.port.out.TokenProvider;
import depth.finvibe.user.modules.user.application.port.out.UserEventPublisher;
import depth.finvibe.user.modules.user.application.port.out.UserRepository;
import depth.finvibe.user.modules.user.domain.InterestStock;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
import depth.finvibe.user.modules.user.domain.vo.Email;
import depth.finvibe.user.modules.user.domain.vo.LoginId;
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
    private final TokenProvider tokenProvider;
    private final MarketClient marketClient;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserDto.UserResponse signUp(UserDto.SignUpRequest request) {
        checkUserAlreadyExist(request);

        User user = createUserFromSignUpRequest(request);

        User savedUser = userRepository.save(user);
        userEventPublisher.publishUserSignUpEvent(savedUser.getId());

        return UserDto.UserResponse.from(savedUser);
    }

    @Override
    @Transactional
    public UserDto.TokenResponse login(UserDto.LoginRequest request) {
        User user = userRepository.findByLoginId(new LoginId(request.getLoginId()))
                .orElseThrow(() -> new DomainException(UserErrorCode.USER_NOT_FOUND));

        user.validateLogin(request.getPassword(), passwordEncoder);

        userEventPublisher.publishUserSignInEvent(user.getId());
        return tokenProvider.generateToken(user.getId());
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
