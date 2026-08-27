package org.tlais.yutest1.mapper;

import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.tlais.yutest1.domain.dto.AdminDTO;
import org.tlais.yutest1.domain.vo.UserProfileVO;

@Mapper
public interface AdminMapper {
    Page<UserProfileVO> getPage(AdminDTO admin);

    void updateStatus(String id, String status);
}
