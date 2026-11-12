package org.sopt.catchtable.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.user.repository.UserRepository;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserService(userRepository);

    @Test
    void 등록된_사용자면_반환한다() {
        User user = mock(User.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        User result = userService.getAuthorizedUser("1");

        assertThat(result).isSameAs(user);
    }

    @Test
    void 헤더가_없으면_UNAUTHORIZED를_반환한다() {
        assertUnauthorized(() -> userService.getAuthorizedUser(null));
    }

    @Test
    void 숫자가_아닌_헤더면_UNAUTHORIZED를_반환한다() {
        assertUnauthorized(() -> userService.getAuthorizedUser("not-a-number"));
    }

    @Test
    void 등록되지_않은_사용자면_UNAUTHORIZED를_반환한다() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertUnauthorized(() -> userService.getAuthorizedUser("99"));
    }

    private void assertUnauthorized(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
