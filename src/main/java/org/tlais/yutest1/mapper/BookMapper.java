package org.tlais.yutest1.mapper;

import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.tlais.yutest1.annotation.AutoFill;
import org.tlais.yutest1.domain.dto.BookSearchDTO;
import org.tlais.yutest1.domain.entity.Book;
import org.tlais.yutest1.domain.vo.BookVO;
import org.tlais.yutest1.enumeration.OperationType;

import java.util.List;

@Mapper
public interface BookMapper {
    @AutoFill(OperationType.INSERT)
    void insert(BookVO bookVO);

    @Select("select id, seller_id, title, author, isbn, original_price, selling_price, `condition`, category, description, status, version, created_at, updated_at from books where id = #{bookId}")
    Book selectById(String bookId);

    @AutoFill(OperationType.UPDATE)
    void updateById(Book book);

    Page<Book> selectList(BookSearchDTO bookSearchDTO);

    List<Book> selectByIds(List<String> bookIds);
}
