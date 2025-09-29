package alibaba.datafilter.common.concurrent;

import alibaba.datafilter.model.dto.UserDTO;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/27
 * 说明:
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> tl=new ThreadLocal<>();
    public static void saveUser(UserDTO userDTO){
        tl.set(userDTO);
    }

    public static UserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
