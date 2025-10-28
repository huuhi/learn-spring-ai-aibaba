package alibaba.datafilter.exception;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/28
 * 说明: 权限不足错误
 */
public class ValidationException extends BusinessException {
    public ValidationException(String message) {
        super(message);
    }
}