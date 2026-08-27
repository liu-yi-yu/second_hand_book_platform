package org.tlais.yutest1.service;

import org.tlais.yutest1.domain.dto.PageDTO;
import org.tlais.yutest1.domain.dto.ReviewCreateDTO;
import org.tlais.yutest1.domain.entity.Review;
import org.tlais.yutest1.domain.vo.PageReviewVO;

import org.tlais.yutest1.domain.vo.ReviewVO;

public interface ReviewService {
    Review createReview(ReviewCreateDTO reviewCreateDTO);

    PageReviewVO<ReviewVO> getReviews(String targetUserId, PageDTO pageDTO);
}
