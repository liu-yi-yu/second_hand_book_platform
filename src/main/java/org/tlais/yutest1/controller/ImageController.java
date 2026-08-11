package org.tlais.yutest1.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.tlais.yutest1.domain.entity.Result;
import org.tlais.yutest1.domain.vo.ImageVO;
import org.tlais.yutest1.service.ImageService;
import org.tlais.yutest1.util.AliOssUtil;

@RestController
@RequestMapping("/api/upload")
@Slf4j
public class ImageController {
    @Autowired
    private AliOssUtil ossUtil;
    @Autowired
    private ImageService imageService;

    @PostMapping("/image")
    public Result uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("上传文件到OSS");
        // 上传文件到OSS
        String imageUrl = null;
        try {
            imageUrl = ossUtil.upload(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("上传文件到OSS成功，图片URL：{}",imageUrl);
        // 保存图片URL到数据库
        ImageVO imageVO = imageService.saveImage(imageUrl);
        return Result.success(imageVO);
    }
}
