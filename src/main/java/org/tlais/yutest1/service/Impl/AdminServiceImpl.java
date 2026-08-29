package org.tlais.yutest1.service.Impl;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tlais.yutest1.constant.*;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.domain.dto.AdminDTO;
import org.tlais.yutest1.domain.dto.PageDTO;
import org.tlais.yutest1.domain.entity.Book;
import org.tlais.yutest1.domain.entity.Order;
import org.tlais.yutest1.domain.entity.OrderCountByStatus;
import org.tlais.yutest1.domain.entity.User;
import org.tlais.yutest1.domain.vo.*;
import org.tlais.yutest1.exception.BusinessException;
import org.tlais.yutest1.mapper.*;
import org.tlais.yutest1.service.AdminService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BookImageMapper bookImageMapper;

    @Override
    public PageVO<UserProfileVO> getPage(PageDTO pageDTO, AdminDTO admin) {
        PageHelper.startPage(pageDTO.getPage(), pageDTO.getPageSize());
        Page<UserProfileVO> page = adminMapper.getPage(admin);
        return new PageVO<>(page.getResult(),page.getTotal());
    }

    @Override
    @Transactional
    public void updateStatus(String id, AdminDTO adminDTO) {
        String currentId = BaseContext.getCurrentId();
        if (currentId.equals(id)) {
            throw new IllegalArgumentException(UserException.CANNOT_UPDATE_OWN_STATUS);
        }
        User user = userMapper.selectById(id);
        if(user == null){
            throw new BusinessException(UserException.USER_NOT_FOUND);
        }

        if(adminDTO.getStatus().equals(Status.USER_ACTIVE)){
            bookMapper.updateStatus(id, BookStatu.REMOVED,BookStatu.SELLING, LocalDateTime.now());
        }
        else {
            bookMapper.updateStatus(id, BookStatu.SELLING,BookStatu.REMOVED, LocalDateTime.now());
        }
    }

    @Override
    public PageVO<BookListVO> getBooks() {
        PageHelper.startPage(1, 15);
        Page<BookListVO> page = bookMapper.getBookList();
        return new PageVO<>(page.getResult(),page.getTotal());
    }

    @Override
    @Transactional
    public void removeBook(String id) {
        Book book = bookMapper.selectById(id);
        if(book == null){
            throw new IllegalArgumentException(BookException.BOOK_NOT_FOUND);
        }

        bookMapper.updateStatusByBookId(id, BookStatu.REMOVED, LocalDateTime.now());

    }

    @Override
    public PageVO<OrderListVO> getOrders() {
        PageHelper.startPage(1, 15);
        Page<OrderListVO> page = ordersMapper.getOrderList();
        return new PageVO<>(page.getResult(),page.getTotal());
    }

    @Override
    @Transactional
    public void cancelOrder(String orderId,String reason) {
        Order order = ordersMapper.selectById(Integer.parseInt(orderId));
        if(order == null){
            throw new IllegalArgumentException(OrderException.ORDER_NOT_EXIST);
        }
        order.setStatus(OrderStatu.CANCELLED);
        order.setCancelReason(reason);
        ordersMapper.updateById(order);
        bookMapper.updateStatusByBookId(order.getBookId(), BookStatu.SELLING, LocalDateTime.now());
    }

    @Override
    public DashboardVO getDashboard() {
        DashboardVO dashboardVO = new DashboardVO();

        dashboardVO.setTotalUsers(userMapper.getCount());
        LocalDateTime now = LocalDateTime.now();
        dashboardVO.setActiveUsers7Days(userMapper.getCount7d(now));
        dashboardVO.setTotalBooksSelling(bookMapper.getCountSelling());
        dashboardVO.setTotalOrders(ordersMapper.getCount());
        dashboardVO.setCompletedOrders(ordersMapper.getCountByStatus(OrderStatu.COMPLETED,null));
        dashboardVO.setTotalSales(ordersMapper.getTotalAmount());
        ArrayList<OrderCountByStatus> orderCountByStatusList = ordersMapper.getCountTotalStatus();
        dashboardVO.setOrderStatusCount(orderCountByStatusList);
        dashboardVO.setNewOrders7Days(ordersMapper.getCountByStatus7d(OrderStatu.PENDING,now));

        return dashboardVO;
    }

}
