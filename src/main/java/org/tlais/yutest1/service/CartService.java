package org.tlais.yutest1.service;

import org.tlais.yutest1.domain.dto.CartAddDTO;
import org.tlais.yutest1.domain.vo.CartItemVO;
import org.tlais.yutest1.domain.vo.CartOptOV;

import java.util.List;

public interface CartService {
    Integer addCart(CartAddDTO cartAddDTO);

    List<CartItemVO> getCart();

    void deleteCart(String bookId);

    CartOptOV checkoutPreview(List<String> bookIds);
}
