package org.sopt.catchtable.vote.repository;

import java.util.Optional;
import org.sopt.catchtable.vote.domain.Vote;

public interface VoteRepositoryCustom {

    Optional<Vote> findByIdForUpdate(Long voteId);
}
