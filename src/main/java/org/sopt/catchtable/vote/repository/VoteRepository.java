package org.sopt.catchtable.vote.repository;

import org.sopt.catchtable.vote.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {
}
