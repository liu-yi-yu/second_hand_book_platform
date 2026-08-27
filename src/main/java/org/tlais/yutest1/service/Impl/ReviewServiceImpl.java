package org.tlais.yutest1.service.Impl;

import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tlais.yutest1.constant.OrderException;
import org.tlais.yutest1.constant.OrderStatu;
import org.tlais.yutest1.constant.ReviewException;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.domain.dto.PageDTO;
import org.tlais.yutest1.domain.dto.ReviewCreateDTO;
import org.tlais.yutest1.domain.entity.Order;
import org.tlais.yutest1.domain.entity.Review;
import org.tlais.yutest1.domain.entity.User;
import org.tlais.yutest1.domain.vo.PageReviewVO;
import org.tlais.yutest1.domain.vo.PageVO;
import org.tlais.yutest1.domain.vo.ReviewVO;
import org.tlais.yutest1.domain.vo.UserSimpleVO;
import org.tlais.yutest1.enumeration.OrderStatus;
import org.tlais.yutest1.mapper.OrdersMapper;
import org.tlais.yutest1.mapper.ReviewMapper;
import org.tlais.yutest1.mapper.UserMapper;
import org.tlais.yutest1.service.ReviewService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {
    @Autowired
    private ReviewMapper  reviewMapper;
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private UserMapper userMapper;

    private Integer reviewTotal = 0;

    @Override
    public Review createReview(ReviewCreateDTO reviewCreateDTO) {
        Order order = ordersMapper.selectById(reviewCreateDTO.getOrderId());
        if(order == null){
            throw new IllegalArgumentException(OrderException.ORDER_NOT_EXIST);
        }
        if(!order.getStatus().equals(OrderStatu.RECEIVED)){
            throw new IllegalArgumentException(OrderException.ORDER_STATUS_EXCEPTION);
        }
        String reviewerId = BaseContext.getCurrentId();
        Review review1 = reviewMapper.fillReview(order.getId(), reviewerId);
        if(review1 != null){
            throw new IllegalArgumentException(ReviewException.REVIEW_EXCEPTION);
        }

        String targetUserId ;
        if(order.getSellerId().equals(reviewerId)){
            targetUserId = order.getBuyerId();
        }else{
            targetUserId = order.getSellerId();
        }

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setReviewerId(reviewerId);
        review.setTargetUserId(targetUserId);
        review.setRating(reviewCreateDTO.getRating());
        review.setContent(reviewCreateDTO.getContent());
        LocalDateTime now = LocalDateTime.now();
        review.setCreatedAt(now);


        reviewMapper.insert(review);
        return review;
    }

    @Override
    public PageReviewVO<ReviewVO> getReviews(String targetUserId, PageDTO pageDTO) {
        PageHelper.startPage(pageDTO.getPage(), pageDTO.getPageSize());
        List<Review> reviewList = reviewMapper.fillReviewAll(targetUserId);

        Double avgRating = 0.0;
        Integer reviewCount = 0;
        HashMap<String,Integer> ratingMap = new HashMap<>();
        ratingMap.put("1",0);
        ratingMap.put("2",0);
        ratingMap.put("3",0);
        ratingMap.put("4",0);
        ratingMap.put("5",0);

        List<ReviewVO> reviewVOList = reviewList.stream().map(review -> {
            ReviewVO reviewVO = new ReviewVO();
            UserSimpleVO reviewer = new UserSimpleVO();

            BeanUtils.copyProperties(review, reviewVO);
            reviewVO.setCreatedAt(review.getCreatedAt().toString());

            reviewer.setId(review.getReviewerId());
            User user = userMapper.selectById(review.getReviewerId());
            reviewer.setUsername(user.getUsername());
            reviewer.setAvatarUrl(user.getAvatarUrl());
            reviewVO.setReviewer(reviewer);

            Integer rating1 = review.getRating();
            String rating = rating1.toString();
            ratingMap.put(rating, ratingMap.get(rating) + 1);

            reviewTotal += rating1;

            return reviewVO;
        }).collect(Collectors.toList());

        reviewCount = reviewVOList.size();
        avgRating = reviewTotal*1.0 / reviewCount;


        return new PageReviewVO<ReviewVO>(reviewVOList, (long) reviewVOList.size(), avgRating, reviewCount, ratingMap);
    }
}
