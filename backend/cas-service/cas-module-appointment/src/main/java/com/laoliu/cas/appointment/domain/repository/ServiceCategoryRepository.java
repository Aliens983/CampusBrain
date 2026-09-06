package com.laoliu.cas.appointment.domain.repository;

import com.laoliu.cas.appointment.domain.entity.ServiceCategory;

import java.util.List;

/**
 * 服务业务分类仓库
 * <p>
 * 分类表为只读种子（固定 4 类），仓库仅需提供全量列表，
 * 供 services.category_id 的代码级外键解析与展示名回填。
 *
 * @author forever-king
 */
public interface ServiceCategoryRepository {

    /**
     * 查询全部分类（按 sort 升序）
     */
    List<ServiceCategory> findAll();
}
