package org.tlais.yutest1.mapper;

import jakarta.validation.constraints.NotBlank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.tlais.yutest1.annotation.AutoFill;
import org.tlais.yutest1.enumeration.OperationType;
import org.tlais.yutest1.domain.entity.User;

@Mapper
public interface UserMapper {
    @AutoFill(OperationType.INSERT)
    int insert(User user);

    @Select("select * from users where username = #{username}")
    User selectByUsername(String username);

    @Select("select * from users where email = #{email}")
    User selectByEmail(@NotBlank(message = "邮箱不能为空") String email);

    @Select("select * from users where id = #{id}")
    User selectById(String id);

    @AutoFill(OperationType.UPDATE)
    void update(User user);
}
