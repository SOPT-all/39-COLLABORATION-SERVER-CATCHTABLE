package org.sopt.catchtable.user.service;

import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    // X-User-Id가 인증 수단이므로 사용자가 없으면 401
    public User getAuthorizedUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
