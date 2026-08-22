package com.laoliu.cas.system.interfaces.convert;

import com.laoliu.cas.common.result.PageResult;
import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.system.interfaces.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * User 实体 ↔ UserResponse VO 转换器（MapStruct）。
 *
 * @author forever-king
 */
@Mapper
public interface UserConvert {

    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    UserResponse convert(User user);

    List<UserResponse> convertList(List<User> list);

    default PageResult<UserResponse> convertPage(PageResult<User> page) {
        if (page == null) {
            return null;
        }
        return new PageResult<>(
                convertList(page.getRecords()),
                page.getTotal()
        );
    }
}
