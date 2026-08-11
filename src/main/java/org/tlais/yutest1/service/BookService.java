package org.tlais.yutest1.service;

import org.tlais.yutest1.domain.dto.BookCreateDTO;
import org.tlais.yutest1.domain.dto.BookSearchDTO;
import org.tlais.yutest1.domain.dto.BookUpdateDTO;
import org.tlais.yutest1.domain.vo.BookListVO;
import org.tlais.yutest1.domain.vo.BookVO;
import org.tlais.yutest1.domain.vo.PageVO;

public interface BookService {
    BookVO addBook(BookCreateDTO bookCreateDTO);

    BookVO getById(String bookId);

    BookVO updateBook(String bookId, BookUpdateDTO bookUpdateDTO);

    void removeBook(String bookId);

    PageVO<BookListVO> getPage(BookSearchDTO bookSearchDTO);

}
