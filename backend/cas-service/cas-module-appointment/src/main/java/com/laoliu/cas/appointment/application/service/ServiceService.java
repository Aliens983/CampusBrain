package com.laoliu.cas.appointment.application.service;

import com.laoliu.cas.appointment.domain.entity.Service;
import com.laoliu.cas.appointment.interfaces.dto.request.ServiceAddRequest;
import com.laoliu.cas.appointment.interfaces.dto.request.ServicePageReqVO;
import com.laoliu.cas.common.result.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * 服务管理应用服务接口
 *
 * @author forever-king
 */
public interface ServiceService {

    /** 获取所有服务 */
    List<Service> getAllServices();

    /**
     * 分页获取所有服务（支持筛选）。
     */
    PageResult<Service> getAllServices(ServicePageReqVO reqVO);

    /** 获取可用服务列表 */
    List<Service> getAvailableServices();

    /** 根据ID获取服务 */
    Optional<Service> getServiceById(Long id);

    /** 添加服务 */
    boolean addService(ServiceAddRequest request);

    /** 更新服务 */
    boolean updateService(Long id, ServiceAddRequest request);

    /** 获取用户已选服务 */
    List<Service> selectUserServices(Long userId);

    /**
     * 分页获取用户的服务。
     */
    PageResult<Service> selectUserServices(Long userId, int page, int pageSize);
}
