package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ConsultantDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 咨询师 Mapper
 *
 * @author forever-king
 */
@Mapper
public interface ConsultantMapper extends BaseMapper<ConsultantDO> {

    @Select("SELECT * FROM consultant WHERE service_id = #{serviceId}")
    List<ConsultantDO> findByServiceId(@Param("serviceId") Long serviceId);

    @Select("SELECT c.* FROM consultant c " +
            "JOIN services s ON s.service_id = c.service_id " +
            "JOIN service_category sc ON sc.id = s.category_id " +
            "JOIN `user` u ON u.id = c.user_id " +
            "WHERE sc.code = 'teacher' AND u.role = 3")
    List<ConsultantDO> findActiveTeacherConsultants();

    @Select("SELECT * FROM consultant WHERE user_id = #{userId} AND service_id IN " +
            "(SELECT s.service_id FROM services s JOIN service_category sc ON sc.id = s.category_id WHERE sc.code = 'teacher')")
    List<ConsultantDO> findTeacherConsultantsByUserId(@Param("userId") Long userId);
}
