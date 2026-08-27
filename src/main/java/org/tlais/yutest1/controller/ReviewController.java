package org.tlais.yutest1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.tlais.yutest1.domain.dto.PageDTO;
import org.tlais.yutest1.domain.dto.ReviewCreateDTO;
import org.tlais.yutest1.domain.entity.Result;

import org.tlais.yutest1.service.ReviewService;


@RestController
@RequestMapping("/api")
public class ReviewController {
    @Autowired
    private ReviewService reviewService;

    @PostMapping("/reviews")
    public Result review(@RequestBody ReviewCreateDTO reviewCreateDTO) {
        return Result.success(reviewService.createReview(reviewCreateDTO));
    }

    @GetMapping("/users/{user_id}/reviews")
    public Result getReviews(@PathVariable String userId, PageDTO pageDTO) {
        return Result.success(reviewService.getReviews(userId, pageDTO));
    }
}
