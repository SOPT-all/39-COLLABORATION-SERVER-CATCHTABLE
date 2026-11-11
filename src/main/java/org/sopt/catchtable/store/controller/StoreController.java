package org.sopt.catchtable.store.controller;

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

    @GetMapping("/{storeId}")
    public BaseResponse<StoreDetailResponse> getStoreDetail(
            @PathVariable("storeId") Long storeId
    ) {
        StoreDetailResponse storeDetailResponse = storeService.getStoreDetail(storeId);

        return BaseResponse.success(
                SuccessCode.SUCCESS,
                storeDetailResponse
        );
    }
}
