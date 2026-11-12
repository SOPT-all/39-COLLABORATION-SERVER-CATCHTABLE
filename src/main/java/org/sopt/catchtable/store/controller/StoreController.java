package org.sopt.catchtable.store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.global.response.BaseResponse;
import org.sopt.catchtable.global.response.SuccessCode;
import org.sopt.catchtable.store.dto.StoreDetailResponse;
import org.sopt.catchtable.store.service.StoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;

    @Operation(
            summary = "가게 상세 조회",
            description = "가게 기본 정보, 이미지, 대표 메뉴, AI 요약, 편의 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 매장 ID"),
            @ApiResponse(responseCode = "404", description = "매장을 찾을 수 없음")
    })
    @GetMapping("/{storeId}")
    public BaseResponse<StoreDetailResponse> getStoreDetail(
            @Parameter(description = "조회할 매장 ID", example = "1")
            @PathVariable("storeId")
            @Positive(message = "매장 ID는 1 이상이어야 합니다.")
            Long storeId
    ) {
        StoreDetailResponse storeDetailResponse =
                storeService.getStoreDetail(storeId);

        return BaseResponse.success(
                SuccessCode.SUCCESS,
                storeDetailResponse
        );
    }
}
