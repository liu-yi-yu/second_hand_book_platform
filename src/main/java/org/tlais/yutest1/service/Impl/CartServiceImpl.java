package org.tlais.yutest1.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tlais.yutest1.constant.BookStatu;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.domain.dto.CartAddDTO;
import org.tlais.yutest1.domain.entity.Book;
import org.tlais.yutest1.domain.entity.CartItem;
import org.tlais.yutest1.domain.entity.User;
import org.tlais.yutest1.domain.vo.BookSimpleVO;
import org.tlais.yutest1.domain.vo.CartItemVO;
import org.tlais.yutest1.domain.vo.CartOptOV;
import org.tlais.yutest1.domain.vo.UserSimpleVO;
import org.tlais.yutest1.mapper.BookMapper;
import org.tlais.yutest1.mapper.CartMapper;
import org.tlais.yutest1.mapper.UserMapper;
import org.tlais.yutest1.service.CartService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Integer addCart(CartAddDTO cartAddDTO) {
        String bookId = cartAddDTO.getBookId();

        // 检查书籍
        // 书籍不存在 |
        // 书籍已售出或已下架 |
        // 已在购物车中（重复添加） |
        // 不能将自己的书加入购物车 |
        Book book = bookMapper.selectById(bookId);
        String currentId = BaseContext.getCurrentId();
        if (book == null) {
            log.error("书籍不存在");
            return cartMapper.count(currentId);
        }
        if (!book.getStatus().equals("selling")) {
            log.error("书籍已售出或已下架");
            return cartMapper.count(currentId);
        }
        if (book.getSellerId().equals(currentId)) {
            log.error("不能将自己的书加入购物车");
            return cartMapper.count(currentId);
        }

        CartItem cartItem = new CartItem();
        cartItem.setUserId(currentId);
        cartItem.setBookId(bookId);
        cartItem.setCreatedAt(LocalDateTime.now());


        cartMapper.insert(cartItem);
        return cartMapper.count(currentId);

    }

    @Override
    public List<CartItemVO> getCart() {
        List<CartItem> cartItems = cartMapper.get(BaseContext.getCurrentId());
        if (cartItems == null) {
            log.error("购物车为空");
            return null;
        }
        List<CartItemVO> cartItemVOS = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            CartItemVO cartItemVO = new CartItemVO();
            BookSimpleVO bookSimpleVO = new BookSimpleVO();
            UserSimpleVO userSimpleVO = new UserSimpleVO();

            cartItemVO.setId(cartItem.getId());
            cartItemVO.setCreatedAt(cartItem.getCreatedAt().toString());
            String bookId = cartItem.getBookId();

            //通过书籍ID查询书籍信息
            Book book = bookMapper.selectById(bookId);

            if (bookReal(book, bookSimpleVO)) continue;
            cartItemVO.setBook(bookSimpleVO);

            String sellerId = book.getSellerId();
            if (sellIdReal(sellerId, userSimpleVO)) continue;
            cartItemVO.setSeller(userSimpleVO);

            cartItemVO.setSellingPrice(book.getSellingPrice().toString());
            cartItemVOS.add(cartItemVO);
        }
        return cartItemVOS;
    }

    private static boolean bookReal(Book book, BookSimpleVO bookSimpleVO) {
        if (book == null) {
            log.error("书籍不存在");
            return true;
        }
        if (!book.getStatus().equals(BookStatu.SELLING)) {
            log.error("书籍已售出或已下架");
            return true;
        }
        BeanUtils.copyProperties(book, bookSimpleVO);
        return false;
    }

    private boolean sellIdReal(String sellerId, UserSimpleVO userSimpleVO) {
        //通过卖家ID查询卖家信息
        User user = userMapper.selectById(sellerId);
        if (user == null) {
            log.error("卖家不存在");
            return true;
        }
        BeanUtils.copyProperties(user, userSimpleVO);
        return false;
    }

    @Override
    public void deleteCart(String bookId) {
        cartMapper.deleteById(bookId);
    }

    @Override
    public CartOptOV checkoutPreview(List<String> bookIds) {
        CartOptOV cartOptOV = new CartOptOV();
        List<CartItemVO> cartItemVOS = new ArrayList<>();
        List<Book> books = bookMapper.selectByIds(bookIds);
        BigDecimal totalPrice = BigDecimal.ZERO;
        if (books == null) {
            log.info("订单不存在");
            log.error("订单不存在");

            return null;
        }
        log.info("books: {}", books.toString());

        for (Book book : books) {
            log.info("book: {}", book.toString());
            CartItemVO cartItemVO = new CartItemVO();
            BookSimpleVO bookSimpleVO = new BookSimpleVO();
            UserSimpleVO userSimpleVO = new UserSimpleVO();

            //通过书籍ID查询书籍信息
            if (bookReal(book, bookSimpleVO)) continue;
            log.info("bookSimpleVO: {}", bookSimpleVO.toString());
            cartItemVO.setBook(bookSimpleVO);

            //通过卖家ID查询卖家信息
            if (sellIdReal(book.getSellerId(), userSimpleVO)) continue;
            log.info("userSimpleVO: {}", userSimpleVO.toString());
            cartItemVO.setSeller(userSimpleVO);

            BigDecimal sellingPrice = book.getSellingPrice();
            cartItemVO.setSellingPrice(sellingPrice.toString());

            totalPrice = totalPrice.add(sellingPrice);
            cartItemVOS.add(cartItemVO);
        }
        cartOptOV.setCartItemVOList(cartItemVOS);
        cartOptOV.setTotalPrice(totalPrice);
        cartOptOV.setCount(cartItemVOS.size());
        return cartOptOV;
        
    }
}
