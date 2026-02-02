package depth.finvibe.user.modules.user.application.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import depth.finvibe.user.modules.user.application.port.out.InterestStockRepository;
import depth.finvibe.user.modules.user.application.port.out.MarketClient;
import depth.finvibe.user.modules.user.application.port.out.UserRepository;
import depth.finvibe.user.modules.user.domain.vo.*;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.user.modules.user.application.port.in.UserCommandUseCase;
import depth.finvibe.user.modules.user.application.port.in.UserQueryUseCase;
import depth.finvibe.user.modules.user.domain.InterestStock;
import depth.finvibe.user.modules.user.domain.User;
import depth.finvibe.user.modules.user.domain.error.UserErrorCode;
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
    private final MarketClient marketClient;
    private final PasswordEncoder passwordEncoder;

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

    @Override
    @Transactional(readOnly = true)
    public UserDto.DuplicateCheckResponse checkLoginIdDuplicate(String loginId) {
        boolean isDuplicate = userRepository.existsByLoginId(new LoginId(loginId));
        return new UserDto.DuplicateCheckResponse(isDuplicate);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto.DuplicateCheckResponse checkEmailDuplicate(String email) {
        boolean isDuplicate = userRepository.existsByEmail(new Email(email));
        return new UserDto.DuplicateCheckResponse(isDuplicate);
    }

    @Override
    @Transactional(readOnly = true)
    public String getNickname(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(UserErrorCode.USER_NOT_FOUND));
        return user.getPersonalDetails().getNickname();
    }

    private void updateUserAttributes(UserDto.UpdateUserRequest request, User user, Requester requester) {
        user.validateUpdatable(requester.getUserId(), requester.getRole());

        LoginId updateLoginId = null;
        PasswordHash updatePasswordHash = null;
        PersonalDetails updatePersonalDetails;

        updateLoginId = createUpdateLoginId(request, updateLoginId);
        updatePasswordHash = createUpdatePasswordHash(request, user, updatePasswordHash);
        updatePersonalDetails = createUpdatedPersonalDetails(request, user);

        user.update(
            updateLoginId,
            updatePasswordHash,
            updatePersonalDetails
        );
    }

    private static LoginId createUpdateLoginId(UserDto.UpdateUserRequest request, LoginId updateLoginId) {
        if(Objects.nonNull(request.getLoginId())) {
            updateLoginId = new LoginId(request.getLoginId());
        }
        return updateLoginId;
    }

    private PasswordHash createUpdatePasswordHash(UserDto.UpdateUserRequest request, User user, PasswordHash updatePasswordHash) {
        if(Objects.nonNull(request.getNewPassword())) {
            boolean passwordMatch = user.getPasswordHash().matches(request.getOldPassword(), passwordEncoder);
            if (!passwordMatch) {
                throw new DomainException(UserErrorCode.INVALID_PASSWORD);
            }
            updatePasswordHash = PasswordHash.create(request.getNewPassword(), passwordEncoder);
        }
        return updatePasswordHash;
    }

    private static @NonNull PersonalDetails createUpdatedPersonalDetails(UserDto.UpdateUserRequest request, User user) {
        PersonalDetails updatePersonalDetails;
        updatePersonalDetails = PersonalDetails.of(
            Objects.nonNull(request.getPhoneNumber()) ? PhoneNumber.parse(request.getPhoneNumber()) : user.getPersonalDetails().getPhoneNumber(),
            Objects.nonNull(request.getBirthDate()) ? request.getBirthDate() : user.getPersonalDetails().getBirthDate(),
            Objects.nonNull(request.getNickname()) ? request.getNickname() : user.getPersonalDetails().getNickname(),
            Objects.nonNull(request.getName()) ? request.getName() : user.getPersonalDetails().getName(),
            Objects.nonNull(request.getEmail()) ? new Email(request.getEmail()) : user.getPersonalDetails().getEmail()
        );
        return updatePersonalDetails;
    }


    private void checkLoginIdAlreadyExist(User user, String newLoginId) {
        if (newLoginId != null && !user.getLoginId().getValue().equals(newLoginId)) {
            if (userRepository.existsByLoginId(new LoginId(newLoginId))) {
                throw new DomainException(UserErrorCode.LOGIN_ID_ALREADY_EXISTS);
            }
        }
    }

    private void checkStockIsAlreadyAdded(UUID userId, Long stockId) {
        if (interestStockRepository.findByUserIdAndStockId(userId, stockId).isPresent()) {
            throw new DomainException(UserErrorCode.INTEREST_STOCK_ALREADY_EXISTS);
        }
    }
}
