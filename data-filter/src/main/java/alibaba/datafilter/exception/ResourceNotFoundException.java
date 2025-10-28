package alibaba.datafilter.exception;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/28
 * 说明: 资源未找到异常
 */
public class ResourceNotFoundException extends BusinessException{
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
