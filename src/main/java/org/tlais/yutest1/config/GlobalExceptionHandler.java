package org.tlais.yutest1.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.tlais.yutest1.domain.entity.Result;
import org.tlais.yutest1.exception.BusinessException;
import org.tlais.yutest1.util.ResultGenerator;

/**
 * 全局异常处理器
 * <p>
 * 知识点：AOP 思想——把散落在各 Controller 的 try-catch/genFailResult 集中到一处。
 * 因为所有 Controller 都是 @Controller（非 @RestController），所以用 @ControllerAdvice。
 * 方法上加 @ResponseBody 才能返回 JSON。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 参数校验失败（@Valid 验证不通过时 Spring 抛出 BindException）→ 返回给前端显示
     */
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public Result handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResultGenerator.genFailResult(msg);
    }

    /**
     * 业务异常 → 返回给前端显示
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public Result handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResultGenerator.genFailResult(e.getMessage());
    }

    /**
     * 兜底：意料之外的异常 → 记日志，返回通用错误信息
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return ResultGenerator.genFailResult("系统异常，请联系管理员");
    }
}
