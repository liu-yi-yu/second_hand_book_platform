package org.tlais.yutest1.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.tlais.yutest1.domain.dto.BookCreateDTO;
import org.tlais.yutest1.service.BookService;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(BookControllerText.class)// 只加载 Controller 层
public class BookControllerText {

    @Autowired
    private MockMvc mockMvc;       // 模拟浏览器发请求

    @MockBean
    private BookService bookService;

    // 构造一个带登录信息的 session，通过拦截器
    private MockHttpSession loginSession() {
        MockHttpSession session = new MockHttpSession();
        //"username": "原奕辰",
        //    "email": "izh36@tom.com",
        //    "password": "boY1QJMR2syrRGU"
        session.setAttribute("username", "原奕辰");
        session.setAttribute("email", "izh36@tom.com");
        session.setAttribute("password", "admin");      // ← 拦截器就看这个

        return session;
    }

    @Test
    public void addBook() throws Exception {
        BookCreateDTO bookCreateDTO = new BookCreateDTO();
        bookCreateDTO.setTitle("测试书");
        bookCreateDTO.setAuthor("测试作者");
        bookCreateDTO.setOriginalPrice(BigDecimal.valueOf(100));
        bookCreateDTO.setSellingPrice(BigDecimal.valueOf(80));
        bookCreateDTO.setCondition("brand_new");
        bookCreateDTO.setCategory("literature");
        bookCreateDTO.setDescription("测试书");
        bookCreateDTO.setImageIds(List.of("123", "456"));

        mockMvc.perform(get("/book/add")
                        .session(loginSession())
                        .param("bookCreateDTO", bookCreateDTO.toString())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("book/add"));



    }
}
