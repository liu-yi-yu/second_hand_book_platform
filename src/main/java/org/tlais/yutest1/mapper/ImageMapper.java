package org.tlais.yutest1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.tlais.yutest1.domain.vo.ImageVO;

import java.util.List;

@Mapper
public interface ImageMapper {

    List<ImageVO> getByIds(List<String> strings);
}
