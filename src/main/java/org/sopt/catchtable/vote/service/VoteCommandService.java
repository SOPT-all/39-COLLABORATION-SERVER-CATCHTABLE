package org.sopt.catchtable.vote.service;

import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.vote.domain.Ballot;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.dto.request.SubmitBallotRequest;
import org.sopt.catchtable.vote.repository.BallotRepository;
import org.sopt.catchtable.vote.repository.VoteCandidateRepository;
import org.sopt.catchtable.vote.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteCommandService {

    private final VoteRepository voteRepository;
    private final VoteCandidateRepository voteCandidateRepository;
    private final BallotRepository ballotRepository;

    /**
     * 동일 투표의 제출·종료 요청은 같은 Vote 행을 잠가 직렬화한다.
     * 투표별 사용자 유니크 제약은 DB에서도 중복 저장을 막는다.
     */
    @Transactional
    public void submitBallot(User user, Long voteId, SubmitBallotRequest request) {
        Vote vote = getVoteForUpdate(voteId);

        if (vote.isClosed()) {
            throw new BusinessException(ErrorCode.VOTE_CLOSED);
        }

        VoteCandidate candidate = voteCandidateRepository.findByIdAndVoteId(request.candidateId(), voteId)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VOTE_CANDIDATE));

        if (ballotRepository.existsByVoteIdAndUserId(voteId, user.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_VOTED);
        }

        ballotRepository.save(Ballot.create(vote, user, candidate, validateNickname(request.nickname())));
    }

    @Transactional
    public void closeVote(User user, Long voteId) {
        Vote vote = getVoteForUpdate(voteId);

        if (!vote.isHostedBy(user.getId())) {
            throw new BusinessException(ErrorCode.VOTE_CLOSE_FORBIDDEN);
        }

        if (vote.isClosed()) {
            throw new BusinessException(ErrorCode.VOTE_ALREADY_CLOSED);
        }

        vote.close();
    }

    private Vote getVoteForUpdate(Long voteId) {
        return voteRepository.findByIdForUpdate(voteId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));
    }

    private String validateNickname(String nickname) {
        if (nickname == null) {
            throw new BusinessException(ErrorCode.INVALID_NICKNAME);
        }

        String trimmedNickname = nickname.strip();

        if (trimmedNickname.isBlank() || trimmedNickname.length() > 15) {
            throw new BusinessException(ErrorCode.INVALID_NICKNAME);
        }

        return trimmedNickname;
    }
}
