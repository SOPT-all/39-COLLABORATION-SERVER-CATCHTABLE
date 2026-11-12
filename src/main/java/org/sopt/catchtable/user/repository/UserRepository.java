package org.sopt.catchtable.user.repository;

import org.sopt.catchtable.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
