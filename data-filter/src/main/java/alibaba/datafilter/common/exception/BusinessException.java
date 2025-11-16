package alibaba.datafilter.common.exception;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/28
 * 说明: 基础异常错误
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}