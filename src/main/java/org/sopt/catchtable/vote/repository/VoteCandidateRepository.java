package org.sopt.catchtable.vote.repository;

import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteCandidateRepository extends JpaRepository<VoteCandidate, Long> {
}
