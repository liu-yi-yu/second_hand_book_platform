package org.tlais.yutest1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.tlais.yutest1.domain.entity.Review;


import java.util.List;

@Mapper
public interface ReviewMapper {
    @Select("insert into reviews (order_id,reviewer_id,target_user_id,rating,comment,created_at) values (#{order_id},#{reviewer_id},#{target_user_id},#{rating},#{comment},#{created_at})")
    void insert(Review review);

    @Select("select * from reviews where order_id = #{order_id} and reviewer_id = #{reviewer_id}")
    Review fillReview(Integer orderId, String reviewerId);

    @Select("select * from reviews where target_user_id = #{target_user_id}")
    List<Review> fillReviewAll(String targetUserId);
}
