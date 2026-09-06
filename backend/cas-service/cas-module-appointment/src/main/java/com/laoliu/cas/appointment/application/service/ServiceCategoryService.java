package com.laoliu.cas.appointment.application.service;

import com.laoliu.cas.appointment.domain.entity.ServiceCategory;

import java.util.List;

/**
 * 服务业务分类应用服务（只读字典，供前端下拉与展示名使用）
 *
 * @author forever-king
 */
public interface ServiceCategoryService {

    /** 获取全部业务分类（按 sort 升序） */
    List<ServiceCategory> listCategories();
}
