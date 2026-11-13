package org.sopt.catchtable.vote.repository;

import java.util.List;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteCandidateRepository extends JpaRepository<VoteCandidate, Long> {

    List<VoteCandidate> findAllByVoteIdOrderByDisplayOrderAsc(Long voteId);
}
