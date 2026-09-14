package com.laoliu.cas.appointment.interfaces.convert;

import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceResponse;
import com.laoliu.cas.common.result.PageResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * ServiceItem 实体 ↔ ServiceResponse 转换器（MapStruct）
 *
 * @author forever-king
 */
@Mapper
public interface ServiceConvert {

    ServiceConvert INSTANCE = Mappers.getMapper(ServiceConvert.class);

    ServiceResponse convert(ServiceItem service);

    List<ServiceResponse> convertList(List<ServiceItem> list);

    default PageResult<ServiceResponse> convertPage(PageResult<ServiceItem> page) {
        if (page == null) {
            return null;
        }
        return new PageResult<>(
                convertList(page.getRecords()),
                page.getTotal()
        );
    }
}
