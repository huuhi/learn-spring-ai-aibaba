package alibaba.datafilter.model.em;

import lombok.Getter;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/31
 * 说明:
 */
public enum DebateRoles {
//  正方 proposition
    DEBATER_PRO("DEBATER_PRO"),
//  反方 opposition
    DEBATER_OPP("DEBATER_OPP"),
//    裁判发言
    DEBATER_JUDGE("DEBATER_JUDGE");
    @Getter
    private final String value;
    DebateRoles(String value) {
        this.value = value;
    }
}
