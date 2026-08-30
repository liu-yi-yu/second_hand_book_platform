package org.tlais.yutest1.service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tlais.yutest1.constant.BookException;
import org.tlais.yutest1.constant.BookStatu;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.context.BookViewCounter;
import org.tlais.yutest1.domain.dto.BookCreateDTO;
import org.tlais.yutest1.domain.dto.BookSearchDTO;
import org.tlais.yutest1.domain.dto.BookUpdateDTO;
import org.tlais.yutest1.domain.entity.Book;
import org.tlais.yutest1.domain.vo.*;
import org.tlais.yutest1.mapper.BookImageMapper;
import org.tlais.yutest1.mapper.BookMapper;
import org.tlais.yutest1.mapper.ImageMapper;
import org.tlais.yutest1.service.BookService;
import org.tlais.yutest1.service.ImageService;
import org.tlais.yutest1.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class BookServiceImpl implements BookService {
    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private BookImageMapper bookImageMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private ImageService imageService;
    @Autowired
    private ImageMapper imageMapper;
    @Autowired
    private BookViewCounter bookViewCounter;


    @Override
    @Transactional
    @Operation(summary = "添加图书")
    public BookVO addBook(BookCreateDTO bookCreateDTO) {
        log.info("bookCreateDTO:{}",bookCreateDTO);
        // 插入图书基本信息
        BookVO bookVO = new BookVO();
        BeanUtils.copyProperties(bookCreateDTO,bookVO);

        String currentId = BaseContext.getCurrentId();
        UserProfileVO byId = userService.getById(currentId);
        UserSimpleVO userSimpleVO = new UserSimpleVO();
        BeanUtils.copyProperties(byId,userSimpleVO);
        bookVO.setSeller(userSimpleVO);
        bookVO.setSellerId(currentId);

        String bookId = UUID.randomUUID().toString().substring(0, 20);
        bookVO.setId(bookId);
        bookVO.setOriginalPrice(bookCreateDTO.getOriginalPrice().toString());
        bookVO.setSellingPrice(bookCreateDTO.getSellingPrice().toString());

        String string = Arrays.toString(bookCreateDTO.getIsbn().split("-"));
        bookVO.setIsbn(string.substring(13));

        log.info("bookVO:{}",bookVO);
        bookMapper.insert(bookVO);
        if (bookCreateDTO.getImageIds() != null && !bookCreateDTO.getImageIds().isEmpty()) {
            // 插入图书图片关联
            bookImageMapper.insert(bookId, bookCreateDTO.getImageIds(),LocalDateTime.now());
        }

        //bookVO.setCreatedAt(now.toString());

        return bookVO;
    }

    @Override
    @Operation(summary = "根据ID查询图书详情")
    public BookVO getById(String bookId) {
        BookVO bookVO = new BookVO();
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new IllegalArgumentException(BookException.BOOK_NOT_FOUND);
        }
        BeanUtils.copyProperties(book,bookVO);
        bookVO.setCreatedAt(book.getCreatedAt().toString());
        bookVO.setUpdatedAt(book.getUpdatedAt().toString());

        List<String> strings = bookImageMapper.selectList(bookId);
        List<ImageVO> imageVOOS = imageMapper.getByIds(strings);
        bookVO.setImages(imageVOOS);

        // 设置卖家信息
        UserProfileVO byId = userService.getById(book.getSellerId());
        UserSimpleVO userSimpleVO = new UserSimpleVO();
        BeanUtils.copyProperties(byId,userSimpleVO);
        bookVO.setSeller(userSimpleVO);

        // 访问量 +1
        bookViewCounter.increment(bookId);

        return bookVO;
    }

    @Override
    public BookVO updateBook(String bookId, BookUpdateDTO bookUpdateDTO) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new IllegalArgumentException(BookException.BOOK_NOT_FOUND);
        }
        if(book.getStatus().equals(BookStatu.SOLD)){
            throw new IllegalArgumentException(BookException.BOOK_SOLD);
        }
        if(book.getStatus().equals(BookStatu.REMOVED)){
            book.setStatus(BookStatu.SELLING);
        }
        if(!book.getSellerId().equals(BaseContext.getCurrentId())){
            throw new IllegalArgumentException(BookException.BOOK_NOT_SOLD);
        }
        // 删除旧图片关联
        deleteImageBook(bookId);

        BeanUtils.copyProperties(bookUpdateDTO,book);
//        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);

        // 插入新图片关联
        if (bookUpdateDTO.getImageIds() != null && !bookUpdateDTO.getImageIds().isEmpty()) {
            bookImageMapper.insert(bookId, bookUpdateDTO.getImageIds(),LocalDateTime.now());
        }
        // 更新访问量
        bookViewCounter.increment(bookId);

        return getById(bookId);
    }

    @Override
    public void removeBook(String bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new IllegalArgumentException(BookException.BOOK_NOT_FOUND);
        }
        if(!book.getSellerId().equals(BaseContext.getCurrentId())){
            throw new IllegalArgumentException(BookException.BOOK_NOT_SOLD);
        }
        if(book.getStatus().equals(BookStatu.SOLD)){
            throw new IllegalArgumentException(BookException.BOOK_SOLD);
        }

        book.setStatus(BookStatu.REMOVED);
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);
    }

    @Override
    public PageVO<BookListVO> getPage(BookSearchDTO bookSearchDTO) {
        // 设置默认排序字段
        PageHelper.startPage
                (bookSearchDTO.getPageNum(), bookSearchDTO.getPageSize()
                        ,bookSearchDTO.getSortBy());
        Page<Book> books = bookMapper.selectList(bookSearchDTO);
        List<BookListVO> objects = new ArrayList<>();
        if(!books.isEmpty()) {
            books.forEach(book -> {
                BookListVO bookListVO = new BookListVO();
                BeanUtils.copyProperties(book, bookListVO);
                bookListVO.setCreatedAt(book.getCreatedAt().toString());
                List<String> strings = bookImageMapper.selectList(book.getId());
                List<ImageVO> imageVOOS = imageMapper.getByIds(strings);
                if(!imageVOOS.isEmpty()){
                    bookListVO.setCoverImage(imageVOOS.get(0).getThumbnailUrl());
                }
                objects.add(bookListVO);
            });
        }

        return new PageVO<BookListVO>(objects,books.getTotal());
    }

    public void deleteImageBook(String bookId) {
        bookImageMapper.delete(bookId);
    }


}
