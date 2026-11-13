package org.sopt.catchtable.vote.controller;

import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.global.response.BaseResponse;
import org.sopt.catchtable.global.response.SuccessCode;
import org.sopt.catchtable.vote.dto.MyActiveVoteResponse;
import org.sopt.catchtable.vote.service.VoteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/votes")
public class MyVoteController {

    private final VoteService voteService;

    @GetMapping("/active")
    public BaseResponse<MyActiveVoteResponse> getMyActiveVote(
            @RequestHeader("X-User-Id") Long userId
    ) {
        MyActiveVoteResponse myActiveVoteResponse =
                voteService.getMyActiveVote(userId);
        return BaseResponse.success(
                SuccessCode.SUCCESS,
                myActiveVoteResponse
        );
    }
}



