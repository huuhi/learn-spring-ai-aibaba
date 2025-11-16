package alibaba.datafilter.common.exception;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/28
 * 说明: 权限不足
 */
public class PermissionDeniedException extends BusinessException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}