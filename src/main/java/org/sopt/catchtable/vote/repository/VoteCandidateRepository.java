package org.sopt.catchtable.vote.repository;

import java.util.List;
import java.util.Optional;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteCandidateRepository extends JpaRepository<VoteCandidate, Long> {

    Optional<VoteCandidate> findByIdAndVoteId(Long candidateId, Long voteId);

    List<VoteCandidate> findAllByVoteIdOrderByDisplayOrderAsc(Long voteId);
}
