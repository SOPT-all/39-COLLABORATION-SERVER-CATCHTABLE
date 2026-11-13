package org.sopt.catchtable.vote.repository;

import java.util.Optional;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long>, VoteRepositoryCustom {

    Optional<Vote> findFirstByHostUser_IdAndStatusOrderByCreatedAtDescIdDesc(
            Long userId,
            VoteStatus status
    );
}
