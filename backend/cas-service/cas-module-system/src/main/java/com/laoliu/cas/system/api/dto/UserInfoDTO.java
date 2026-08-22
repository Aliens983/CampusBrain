package com.laoliu.cas.system.api.dto;

import lombok.Data;
import com.laoliu.cas.system.domain.entity.User;

import java.io.Serializable;

/**
 * 用户信息 DTO - 用于跨模块传输
 *
 * @author forever-king
 */
@Data
public class UserInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String email;
    private Integer role;
    private Integer age;
    private String sex;
    private String grade;

    /**
     * 从领域实体创建 DTO
     */
    public static UserInfoDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setAge(user.getAge());
        dto.setSex(user.getSex());
        dto.setGrade(user.getGrade());
        return dto;
    }
}
