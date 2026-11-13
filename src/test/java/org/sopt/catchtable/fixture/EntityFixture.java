package org.sopt.catchtable.fixture;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.vote.domain.Ballot;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 테스트용 엔티티 생성기.
 * User, Store는 시연용 데이터라 생성 메서드가 없으므로 protected 기본 생성자 + 필드 주입으로 만든다.
 * - withId 계열: DB 없이 쓰는 단위 테스트용 (id가 채워진 상태)
 * - new 계열: DB에 저장하는 통합 테스트용 (id는 저장 시 생성)
 */
public final class EntityFixture {

    private EntityFixture() {
    }

    public static User userWithId(Long id) {
        User user = newUser("테스터");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static User newUser(String nickname) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        return user;
    }

    public static Store storeWithId(Long id) {
        Store store = newStore("매장" + id);
        ReflectionTestUtils.setField(store, "id", id);
        return store;
    }

    public static Store newStore(String name) {
        Store store = BeanUtils.instantiateClass(Store.class);
        ReflectionTestUtils.setField(store, "name", name);
        ReflectionTestUtils.setField(store, "category", "한식");
        ReflectionTestUtils.setField(store, "region", "강남");
        ReflectionTestUtils.setField(store, "intro", "테스트 매장");
        ReflectionTestUtils.setField(store, "rating", new BigDecimal("4.5"));
        ReflectionTestUtils.setField(store, "stationName", "강남역");
        ReflectionTestUtils.setField(store, "stationLines", "2");
        ReflectionTestUtils.setField(store, "stationDistanceM", 100);
        ReflectionTestUtils.setField(store, "priceRange", "1~2만원");
        ReflectionTestUtils.setField(store, "businessHours", "11:00~22:00");
        return store;
    }

    // 저장된 것처럼 id와 생성 시각이 채워진 투표
    public static Vote savedVote(Long id, User hostUser, LocalDateTime createdAt) {
        Vote vote = Vote.create(hostUser);
        ReflectionTestUtils.setField(vote, "id", id);
        ReflectionTestUtils.setField(vote, "createdAt", createdAt);
        return vote;
    }

    // 종료된 것처럼 상태와 종료 시각이 채워진 투표
    public static Vote closedVote(Long id, User hostUser, LocalDateTime createdAt, LocalDateTime closedAt) {
        Vote vote = savedVote(id, hostUser, createdAt);
        ReflectionTestUtils.setField(vote, "status", VoteStatus.CLOSED);
        ReflectionTestUtils.setField(vote, "closedAt", closedAt);
        return vote;
    }

    public static VoteCandidate candidateWithId(Long id, Vote vote, Store store, int displayOrder) {
        VoteCandidate candidate = VoteCandidate.create(vote, store, displayOrder);
        ReflectionTestUtils.setField(candidate, "id", id);
        return candidate;
    }

    public static Ballot ballotWithId(Long id, Vote vote, User user, VoteCandidate candidate, String displayName) {
        Ballot ballot = Ballot.create(vote, user, candidate, displayName);
        ReflectionTestUtils.setField(ballot, "id", id);
        return ballot;
    }

    // 매장 상세 조회(StoreService) 결과
    public static StoreDetailResponse storeDetail(Long storeId) {
        return StoreDetailResponse.builder()
            .storeId(storeId)
            .name("매장" + storeId)
            .rating(new BigDecimal("4.5"))
            .reviewCount(100)
            .priceRange("1~2만원")
            .imageUrls(List.of())
            .representativeMenus(List.of())
            .amenities(List.of())
            .build();
    }
}
