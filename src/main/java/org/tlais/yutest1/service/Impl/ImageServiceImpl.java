package org.tlais.yutest1.service.Impl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.domain.entity.Image;
import org.tlais.yutest1.domain.vo.ImageVO;
import org.tlais.yutest1.mapper.BookMapper;
import org.tlais.yutest1.mapper.ImageMapper;
import org.tlais.yutest1.service.ImageService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ImageServiceImpl implements ImageService {
    @Autowired
    private ImageMapper imageMapper;
    @Autowired
    private BookMapper bookMapper;


    @Override
    public ImageVO saveImage(String imageUrl) {
        Image image = new Image();
        image.setId(UUID.randomUUID().toString().substring(0, 16));
        image.setUserId(BaseContext.getCurrentId());
        image.setUrl(imageUrl);
        image.setIsUsed(false);
        image.setCreatedAt(LocalDateTime.now());

        imageMapper.insert(image);

        ImageVO imageVO = new ImageVO();
        BeanUtils.copyProperties(image,imageVO);
        return imageVO;
    }
}
