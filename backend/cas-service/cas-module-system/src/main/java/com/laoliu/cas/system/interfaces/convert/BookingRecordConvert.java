package com.laoliu.cas.system.interfaces.convert;

import com.laoliu.cas.common.result.PageResult;
import com.laoliu.cas.system.infrastructure.persistence.dataobject.BookingRecordDO;
import com.laoliu.cas.system.interfaces.dto.response.BookingRecordRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * BookingRecordDO → BookingRecordRespVO 转换器（MapStruct）。
 *
 * @author forever-king
 */
@Mapper
public interface BookingRecordConvert {

    BookingRecordConvert INSTANCE = Mappers.getMapper(BookingRecordConvert.class);

    @Mapping(target = "createTime", source = "createTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updateTime", source = "updateTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    BookingRecordRespVO convert(BookingRecordDO bookingRecordDO);

    List<BookingRecordRespVO> convertList(List<BookingRecordDO> list);

    default PageResult<BookingRecordRespVO> convertPage(PageResult<BookingRecordDO> page) {
        if (page == null) {
            return null;
        }
        return new PageResult<>(
                convertList(page.getRecords()),
                page.getTotal()
        );
    }
}
