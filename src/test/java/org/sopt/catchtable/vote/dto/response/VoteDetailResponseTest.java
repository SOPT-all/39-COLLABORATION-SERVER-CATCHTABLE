package org.sopt.catchtable.vote.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sopt.catchtable.fixture.EntityFixture.ballotWithId;
import static org.sopt.catchtable.fixture.EntityFixture.candidateWithId;
import static org.sopt.catchtable.fixture.EntityFixture.closedVote;
import static org.sopt.catchtable.fixture.EntityFixture.savedVote;
import static org.sopt.catchtable.fixture.EntityFixture.storeDetail;
import static org.sopt.catchtable.fixture.EntityFixture.storeWithId;
import static org.sopt.catchtable.fixture.EntityFixture.userWithId;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.vote.domain.Ballot;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse.CandidateDetailResponse;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse.VoterResponse;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 투표 현황 응답을 만드는 규칙(득표 집계, 투표자 목록, 요청자 정보, 시각 형식)을 검증한다.
 */
class VoteDetailResponseTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 11, 6, 19, 30, 0, 123_000_000);

    private TimeZone originalTimeZone;

    private User host;
    private Vote vote;
    private VoteCandidate first;
    private VoteCandidate second;
    private Map<Long, StoreDetailResponse> storeDetails;

    @BeforeEach
    void setUp() {
        // 시각 변환 결과를 고정하기 위해 서버 시간대를 서울로 맞춘다
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        host = user(1L, "생성자");
        vote = savedVote(10L, host, CREATED_AT);
        first = candidateWithId(21L, vote, storeWithId(3L), 1);
        second = candidateWithId(22L, vote, storeWithId(5L), 2);
        storeDetails = Map.of(3L, storeDetail(3L), 5L, storeDetail(5L));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @DisplayName("아무도 투표하지 않았으면 참여자 수와 후보별 득표 수는 0이고, 투표자 목록은 빈 배열이다")
    @Test
    void noBallots() {
        // given
        List<Ballot> noBallots = List.of();

        // when
        VoteDetailResponse response = VoteDetailResponse.of(vote, host, List.of(first, second), noBallots, storeDetails);

        // then
        assertThat(response.participantCount()).isZero();
        assertThat(response.candidates())
            .extracting(CandidateDetailResponse::voteCount, CandidateDetailResponse::voters)
            .containsExactly(tuple(0, List.of()), tuple(0, List.of()));
    }

    @DisplayName("후보별로 득표 수와 투표자 닉네임을 모으고, 투표자는 넘겨받은 표 순서(최신순)를 유지한다")
    @Test
    void countBallotsByCandidate() {
        // given
        User voterA = user(2L, "계정A");
        User voterB = user(3L, "계정B");
        User voterC = user(4L, "계정C");
        List<Ballot> latestFirst = List.of(
            ballotWithId(103L, vote, voterC, first, "세번째"),
            ballotWithId(102L, vote, voterB, second, "두번째"),
            ballotWithId(101L, vote, voterA, first, "첫번째")
        );

        // when
        VoteDetailResponse response = VoteDetailResponse.of(vote, host, List.of(first, second), latestFirst, storeDetails);

        // then
        assertThat(response.participantCount()).isEqualTo(3);
        assertThat(response.candidates())
            .extracting(CandidateDetailResponse::candidateId, CandidateDetailResponse::voteCount, CandidateDetailResponse::voters)
            .containsExactly(
                tuple(21L, 2, List.of(new VoterResponse("세번째"), new VoterResponse("첫번째"))),
                tuple(22L, 1, List.of(new VoterResponse("두번째")))
            );
    }

    @DisplayName("후보는 넘겨받은 노출 순서대로, 각 후보의 매장 비교 정보와 함께 돌려준다")
    @Test
    void candidatesWithStore() {
        // given
        List<VoteCandidate> candidatesInDisplayOrder = List.of(first, second);

        // when
        VoteDetailResponse response = VoteDetailResponse.of(vote, host, candidatesInDisplayOrder, List.of(), storeDetails);

        // then
        assertThat(response.candidates())
            .extracting(
                CandidateDetailResponse::displayOrder,
                candidate -> candidate.store().storeId(),
                candidate -> candidate.store().name()
            )
            .containsExactly(tuple(1, 3L, "매장3"), tuple(2, 5L, "매장5"));
    }

    @DisplayName("투표하지 않은 요청자는 선택한 후보와 투표 닉네임이 없고, 계정 닉네임만 기본값으로 받는다")
    @Test
    void viewerBeforeVoting() {
        // given
        User viewer = user(2L, "계정닉네임");

        // when
        VoteDetailResponse response = VoteDetailResponse.of(vote, viewer, List.of(first, second), List.of(), storeDetails);

        // then
        assertThat(response.viewer().userId()).isEqualTo(2L);
        assertThat(response.viewer().isHost()).isFalse();
        assertThat(response.viewer().hasVoted()).isFalse();
        assertThat(response.viewer().selectedCandidateId()).isNull();
        assertThat(response.viewer().displayName()).isNull();
        assertThat(response.viewer().defaultNickname()).isEqualTo("계정닉네임");
    }

    @DisplayName("투표한 요청자는 자신이 선택한 후보와, 계정 닉네임과 별개로 투표할 때 쓴 닉네임을 받는다")
    @Test
    void viewerAfterVoting() {
        // given
        User viewer = user(2L, "계정닉네임");
        User other = user(3L, "다른사람");
        List<Ballot> ballots = List.of(
            ballotWithId(102L, vote, other, first, "다른사람"),
            ballotWithId(101L, vote, viewer, second, "투표닉네임")
        );

        // when
        VoteDetailResponse response = VoteDetailResponse.of(vote, viewer, List.of(first, second), ballots, storeDetails);

        // then
        assertThat(response.viewer().hasVoted()).isTrue();
        assertThat(response.viewer().selectedCandidateId()).isEqualTo(22L);
        assertThat(response.viewer().displayName()).isEqualTo("투표닉네임");
        assertThat(response.viewer().defaultNickname()).isEqualTo("계정닉네임");
    }

    @DisplayName("투표를 만든 사람이 조회하면 생성자로 표시된다")
    @Test
    void viewerIsHost() {
        // given
        User viewer = host;

        // when
        VoteDetailResponse response = VoteDetailResponse.of(vote, viewer, List.of(first, second), List.of(), storeDetails);

        // then
        assertThat(response.viewer().isHost()).isTrue();
    }

    @DisplayName("진행 중인 투표는 종료 시각이 null이고, 생성 시각은 서울 기준 초 단위로 나간다")
    @Test
    void openVoteTimes() {
        // given
        Vote openVote = savedVote(10L, host, CREATED_AT);

        // when
        VoteDetailResponse response = VoteDetailResponse.of(openVote, host, List.of(first, second), List.of(), storeDetails);

        // then
        assertThat(response.status()).isEqualTo(VoteStatus.OPEN);
        assertThat(response.createdAt()).isEqualTo(OffsetDateTime.of(2026, 11, 6, 19, 30, 0, 0, ZoneOffset.ofHours(9)));
        assertThat(response.closedAt()).isNull();
    }

    @DisplayName("종료된 투표는 종료 시각을 서울 기준 초 단위로 돌려준다")
    @Test
    void closedVoteTimes() {
        // given
        Vote closed = closedVote(10L, host, CREATED_AT, LocalDateTime.of(2026, 11, 6, 21, 0, 5, 999_000_000));

        // when
        VoteDetailResponse response = VoteDetailResponse.of(closed, host, List.of(first, second), List.of(), storeDetails);

        // then
        assertThat(response.status()).isEqualTo(VoteStatus.CLOSED);
        assertThat(response.closedAt()).isEqualTo(OffsetDateTime.of(2026, 11, 6, 21, 0, 5, 0, ZoneOffset.ofHours(9)));
    }

    @DisplayName("후보 매장 정보가 빠져 있으면, 어느 후보의 어느 매장이 없는지 알려주며 응답을 만들지 않는다")
    @Test
    void candidateStoreMissing() {
        // given
        Map<Long, StoreDetailResponse> onlyFirstStore = Map.of(3L, storeDetail(3L));

        // when & then
        assertThatThrownBy(() -> VoteDetailResponse.of(vote, host, List.of(first, second), List.of(), onlyFirstStore))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("candidateId=22")
            .hasMessageContaining("storeId=5");
    }

    @DisplayName("후보가 2개가 아닌 투표는 응답을 만들지 않고, 투표 ID와 실제 후보 수를 알려준다")
    @ParameterizedTest(name = "후보 {0}개")
    @MethodSource("wrongCandidateCounts")
    void wrongCandidateCount(int candidateCount) {
        // given
        List<VoteCandidate> candidates = Stream.of(first, second, candidateWithId(23L, vote, storeWithId(7L), 3))
            .limit(candidateCount)
            .toList();

        // when & then
        assertThatThrownBy(() -> VoteDetailResponse.of(vote, host, candidates, List.of(), storeDetails))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("voteId=10")
            .hasMessageContaining("candidateCount=" + candidateCount);
    }

    private static Stream<Integer> wrongCandidateCounts() {
        return Stream.of(0, 1, 3);
    }

    private User user(Long id, String nickname) {
        User user = userWithId(id);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        return user;
    }
}
