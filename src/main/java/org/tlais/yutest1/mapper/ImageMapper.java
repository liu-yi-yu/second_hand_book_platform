package org.tlais.yutest1.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.tlais.yutest1.domain.entity.Image;
import org.tlais.yutest1.domain.vo.ImageVO;

import java.util.List;

@Mapper
public interface ImageMapper {

    List<ImageVO> getByIds(List<String> strings);

    /** 插入图片 */
    @Insert("insert into images (id, user_id, url, is_used, created_at) values (#{id}, #{userId}, #{url}, #{isUsed}, #{createdAt})")
    void insert(Image image);
}
