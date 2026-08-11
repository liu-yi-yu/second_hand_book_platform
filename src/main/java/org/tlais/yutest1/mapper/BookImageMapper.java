package org.tlais.yutest1.mapper;

import io.swagger.v3.oas.annotations.Operation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.tlais.yutest1.annotation.AutoFill;
import org.tlais.yutest1.domain.vo.ImageVO;
import org.tlais.yutest1.enumeration.OperationType;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookImageMapper {
//    @AutoFill(OperationType.INSERT)
    @Operation(summary = "插入图书图片关联")
    void insert(String bookId, List<String> imageIds, LocalDateTime createdAt);

    @Operation(summary = "根据图书ID查询图片关联")
    @Select("select image_id from book_image_relations where book_id = #{bookId}")
    List<String> selectList(String bookId);

    @Operation(summary = "根据图书ID删除图片关联")
    @Delete("delete from book_image_relations where book_id = #{bookId}")
    void delete(String bookId);
}
