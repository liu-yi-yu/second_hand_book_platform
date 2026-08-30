package org.tlais.yutest1.mapper;

import jakarta.validation.constraints.NotBlank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.tlais.yutest1.annotation.AutoFill;
import org.tlais.yutest1.domain.entity.Order;
import org.tlais.yutest1.domain.entity.UserCredit;
import org.tlais.yutest1.enumeration.OperationType;
import org.tlais.yutest1.domain.entity.User;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {
    int insert(User user);

    @Select("select * from users where username = #{username}")
    User selectByUsername(String username);

    @Select("select * from users where email = #{email}")
    User selectByEmail(@NotBlank(message = "邮箱不能为空") String email);

    @Select("select * from users where id = #{id}")
    User selectById(String id);

    @AutoFill(OperationType.UPDATE)
    void update(User user);

    @Select("select count(*) from users")
    Integer getCount();

    @Select("select count(*) from users where status = 'active' and last_login_at >= #{now}")
    Integer getCount7d(LocalDateTime now);
    
    //减少积分，一方减少10分
    void minusCredit(List<Order> p1, int minusCredit, LocalDateTime now);

    @Update("update users set status = 'disabled' , updated_at = #{now} where credit_score < #{credit}")
    void updateByCredit(int credit, LocalDateTime now);

    //添加积分，双方
    void addCredit(List<Order> p3, int addCredit, LocalDateTime now);

    void decCredit(List<UserCredit> userCredits, LocalDateTime now);
}
