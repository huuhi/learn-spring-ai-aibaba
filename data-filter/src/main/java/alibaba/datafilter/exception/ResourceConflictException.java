package alibaba.datafilter.exception;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/28
 * 说明: 资源冲突错误
 */
public class ResourceConflictException extends BusinessException {
    public ResourceConflictException(String message) {
        super(message);
    }
}