package org.sopt.catchtable.vote.repository;

import java.util.List;
import org.sopt.catchtable.vote.domain.Ballot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BallotRepository extends JpaRepository<Ballot, Long> {

    boolean existsByVoteIdAndUserId(Long voteId, Long userId);

    // 투표자 최신순. 생성 시각이 같으면 ID로 한 번 더 정렬
    List<Ballot> findAllByVoteIdOrderByCreatedAtDescIdDesc(Long voteId);
}
