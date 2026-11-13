package org.sopt.catchtable.vote.repository;

import java.util.List;
import org.sopt.catchtable.vote.domain.Ballot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BallotRepository extends JpaRepository<Ballot, Long> {

    // 투표자 목록은 최신순. 생성 시각이 초 단위로 같을 수 있어 ID로 한 번 더 정렬한다
    List<Ballot> findAllByVoteIdOrderByCreatedAtDescIdDesc(Long voteId);
}
