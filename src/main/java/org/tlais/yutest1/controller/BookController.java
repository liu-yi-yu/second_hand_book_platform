package org.tlais.yutest1.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.tlais.yutest1.domain.dto.BookCreateDTO;
import org.tlais.yutest1.domain.dto.BookSearchDTO;
import org.tlais.yutest1.domain.dto.BookUpdateDTO;
import org.tlais.yutest1.domain.dto.PageDTO;
import org.tlais.yutest1.domain.entity.Result;
import org.tlais.yutest1.service.BookService;

@RestController
@RequestMapping("/api/books")
public class BookController {
    @Autowired
    private BookService bookService;

    @PostMapping()
    @Operation(summary = "添加书籍")
    public Result addBook(@RequestBody BookCreateDTO bookCreateDTO) {
        bookService.addBook(bookCreateDTO);
        return Result.success();
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "获取书籍详情")
    public Result getBook(@PathVariable String bookId) {
        return Result.success(bookService.getById(bookId));
    }

    @PutMapping("/{bookId}")
    @Operation(summary = "更新书籍")
    public Result updateBook(@PathVariable String bookId, @RequestBody BookUpdateDTO bookUpdateDTO) {
        return Result.success(bookService.updateBook(bookId,bookUpdateDTO));
    }

    @DeleteMapping("/{bookId}")
    @Operation(summary = "下架书籍")
    public Result removeBook(@PathVariable String bookId) {
        bookService.removeBook(bookId);
        return Result.success();
    }

    @GetMapping()
    @Operation(summary = "获取分页书籍列表")
    public Result getBooksPage(@RequestBody BookSearchDTO bookSearchDTO) {
        return Result.success(bookService.getPage(bookSearchDTO));
    }

    @GetMapping("/search")
    public Result searchBook(@RequestBody BookSearchDTO bookSearchDTO) {
        return Result.success(bookService.getPage(bookSearchDTO));
    }

    @GetMapping("/search/suggestions")
    public Result searchBookSuggestions(@RequestBody BookSearchDTO bookSearchDTO) {
        bookSearchDTO.setPageSize(10);
        return Result.success(bookService.getPage(bookSearchDTO));
    }

}
