package alibaba.datafilter.common.exception;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/28
 * 说明: 未授权
 */
public class UnauthorizedException extends BusinessException{
    public UnauthorizedException(String message) {
        super(message);
    }
}
