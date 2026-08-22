package com.laoliu.cas.appointment.interfaces.convert;

import com.laoliu.cas.appointment.domain.entity.Service;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceRespVO;
import com.laoliu.cas.common.result.PageResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Service 实体 ↔ ServiceRespVO 转换器（MapStruct）。
 *
 * @author forever-king
 */
@Mapper
public interface ServiceConvert {

    ServiceConvert INSTANCE = Mappers.getMapper(ServiceConvert.class);

    ServiceRespVO convert(Service service);

    List<ServiceRespVO> convertList(List<Service> list);

    default PageResult<ServiceRespVO> convertPage(PageResult<Service> page) {
        if (page == null) {
            return null;
        }
        return new PageResult<>(
                convertList(page.getRecords()),
                page.getTotal()
        );
    }
}
