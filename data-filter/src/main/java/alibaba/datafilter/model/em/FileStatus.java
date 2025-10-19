package alibaba.datafilter.model.em;

import lombok.Getter;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/19
 * 说明:
 */
public enum FileStatus {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");
    @Getter
    private final String value;
    FileStatus(String value) {
        this.value = value;
    }
}
