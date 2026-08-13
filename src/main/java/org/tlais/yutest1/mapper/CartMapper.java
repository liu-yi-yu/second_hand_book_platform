package org.tlais.yutest1.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.tlais.yutest1.domain.entity.CartItem;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface CartMapper {
    @Insert("insert into cart_items ( user_id, book_id, created_at) values ( #{userId}, #{bookId}, #{createdAt})")
    void insert(CartItem cartItem);

    @Select("select count(*) from cart_items where user_id = #{userId}")
    Integer count(String userId);

    @Select("select * from cart_items where user_id = #{currentId}")
    List<CartItem> get(String currentId);

    @Delete("delete from cart_items where book_id = #{bookId}")
    void deleteById(String bookId);


    void deleteBatch(ArrayList<String> bookIds);

}
