package org.sopt.catchtable.vote.service;

import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.sopt.catchtable.vote.dto.MyActiveVoteResponse;
import org.sopt.catchtable.vote.repository.VoteRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final VoteRepository voteRepository;

    public MyActiveVoteResponse getMyActiveVote(Long userId){
        Optional<Vote> voteOptional =
                voteRepository.findFirstByHostUser_IdAndStatusOrderByCreatedAtDesc(
                userId,
                VoteStatus.OPEN
        );

        if(voteOptional.isEmpty()) {
            return MyActiveVoteResponse.builder()
                    .vote(null)
                    .build();
        } else {
            Vote vote = voteOptional.get();

            OffsetDateTime createdAt = vote.getCreatedAt()
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toOffsetDateTime();

            MyActiveVoteResponse.VoteResponse voteResponse =
                MyActiveVoteResponse.VoteResponse.builder()
                        .voteId(vote.getId())
                        .candidateCount(2)
                        .createdAt(createdAt)
                        .build();

            return MyActiveVoteResponse.builder()
                    .vote(voteResponse)
                    .build();
        }
    }
}
