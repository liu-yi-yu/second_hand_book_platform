package org.tlais.yutest1.service;



import org.tlais.yutest1.domain.dto.AdminDTO;
import org.tlais.yutest1.domain.dto.PageDTO;
import org.tlais.yutest1.domain.vo.*;

public interface AdminService {
    PageVO<UserProfileVO> getPage(PageDTO pageDTO, AdminDTO admin);

    void updateStatus(String id, AdminDTO adminDTO);

    PageVO<BookListVO> getBooks();

    void removeBook(String id);

    PageVO<OrderListVO> getOrders();

    void cancelOrder(String orderId,String reason);

    DashboardVO getDashboard();
}
