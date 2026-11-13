package org.sopt.catchtable.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.vote.dto.request.CreateVoteRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

/**
 * application.yaml의 Jackson 설정이 실제 요청 변환에 적용되는지 확인한다.
 * 컨트롤러 테스트(standaloneSetup)는 이 설정을 읽지 않아서 스프링 컨텍스트에서 따로 검증한다.
 */
@SpringBootTest
class JacksonConfigTest {

    @Autowired
    private JsonMapper jsonMapper;

    @DisplayName("매장 ID에 실수를 보내면 정수로 잘라 받지 않고 요청 변환에 실패한다")
    @Test
    void rejectFloatForIntegerField() {
        // given
        String body = "{\"storeIds\":[1.5,2]}";

        // when & then
        assertThatThrownBy(() -> jsonMapper.readValue(body, CreateVoteRequest.class))
            .isInstanceOf(DatabindException.class);
    }

    @DisplayName("매장 ID를 정수로 보내면 그대로 받는다")
    @Test
    void acceptIntegerField() {
        // given
        String body = "{\"storeIds\":[1,2]}";

        // when
        CreateVoteRequest request = jsonMapper.readValue(body, CreateVoteRequest.class);

        // then
        assertThat(request.storeIds()).containsExactly(1L, 2L);
    }
}
