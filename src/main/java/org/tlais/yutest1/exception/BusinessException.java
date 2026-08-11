package org.tlais.yutest1.exception;

/**
 * 业务异常
 * <p>
 * Service 层遇到业务规则不满足时抛出此异常，由 GlobalExceptionHandler 统一捕获并返回错误 JSON。
 * <p>
 * 使用方式：throw new BusinessException("标签数量限制为6");
 * <p>
 * 对比旧写法：return "标签数量限制为6";  // 然后 Controller 检查字符串再 genFailResult
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
