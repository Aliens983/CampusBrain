package com.laoliu.cas.system.interfaces.assembler;

import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.system.interfaces.dto.response.UserResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author forever-king
 */
@Component
public class UserAssembler {

    public UserResponse convertToUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId()).name(user.getName())
                .grade(user.getGrade()).sex(user.getSex())
                .age(user.getAge()).email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public List<UserResponse> convertToUserResponseList(List<User> users) {
        return users.stream().map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
}
