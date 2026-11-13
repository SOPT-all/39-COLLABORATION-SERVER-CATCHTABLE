package org.sopt.catchtable.vote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.catchtable.global.domain.BaseEntity;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.user.domain.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Entity
@Table(
        name = "votes",
        indexes = {
                @Index(
                        name = "idx_votes_host_status_created_at",
                        columnList = "host_user_id, status, created_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote extends BaseEntity {

    public static final int CANDIDATE_COUNT = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "host_user_id",
            nullable = false,
            updatable = false
    )
    private User hostUser;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            columnDefinition = "VARCHAR(10) DEFAULT 'OPEN'"
    )
    private VoteStatus status = VoteStatus.OPEN;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    private Vote(User hostUser) {
        this.hostUser = hostUser;
    }

    public static Vote create(User hostUser) {
        return new Vote(hostUser);
    }

    // hostUser의 ID만 읽으므로 지연 로딩 프록시여도 추가 쿼리 없음
    public boolean isHostedBy(Long userId) {
        return Objects.equals(hostUser.getId(), userId);
    }

    // 후보 규칙: 서로 다른 매장 2개
    // 매장을 조회하기 전에 잘못된 요청을 걸러낼 수 있도록 ID만으로 검증한다
    public static void validateCandidateStoreIds(List<Long> storeIds) {
        if (storeIds == null
            || storeIds.size() != CANDIDATE_COUNT
            || storeIds.stream().anyMatch(Objects::isNull)
            || new HashSet<>(storeIds).size() != CANDIDATE_COUNT) {
            throw new BusinessException(ErrorCode.INVALID_VOTE_CANDIDATES);
        }
    }

    // 넘겨받은 순서대로 displayOrder 1, 2를 매겨 후보 생성
    public List<VoteCandidate> createCandidates(List<Store> stores) {
        validateCandidateStoreIds(stores == null ? null : stores.stream()
            .map(store -> store == null ? null : store.getId())
            .toList());

        return IntStream.range(0, stores.size())
            .mapToObj(i -> VoteCandidate.create(this, stores.get(i), i + 1))
            .toList();
    }

    public boolean isClosed() {
        return status == VoteStatus.CLOSED;
    }

    public void close() {
        if (isClosed()) {
            return;
        }

        status = VoteStatus.CLOSED;
        closedAt = LocalDateTime.now();
    }
}
