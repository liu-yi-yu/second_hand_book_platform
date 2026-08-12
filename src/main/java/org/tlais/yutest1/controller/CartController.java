package org.tlais.yutest1.controller;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;

import org.tlais.yutest1.domain.dto.CartAddDTO;
import org.tlais.yutest1.domain.dto.CartPayDTO;
import org.tlais.yutest1.domain.entity.Result;
import org.tlais.yutest1.service.CartService;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    @Autowired
    private CartService cartService;

    @GetMapping()
    public Result getCart() {
        return Result.success(cartService.getCart());
    }

    @PostMapping()
    public Result addCart(@RequestBody CartAddDTO cartAddDTO) {
        return Result.success(cartService.addCart(cartAddDTO));
    }

    @DeleteMapping("/{book_id}")
    public Result deleteCart(@PathVariable("book_id") String bookId) {
        cartService.deleteCart(bookId);
        return Result.success();
    }

    @PostMapping("/checkout-preview")
    public Result checkoutPreview(@RequestBody CartPayDTO cartPayDTO) {
        return Result.success(cartService.checkoutPreview(cartPayDTO.getBookIds()));
    }



}
