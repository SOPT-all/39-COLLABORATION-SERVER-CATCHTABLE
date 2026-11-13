package org.sopt.catchtable.vote.repository;

import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findFirstByHostUser_IdAndStatusOrderByCreatedAtDescIdDesc(
            Long userId,
            VoteStatus status
    );
}
