package com.kb.domain.tenant;

import java.util.List;
import java.util.Optional;

/**
 * 租户仓储接口。
 *
 * @author forever-king
 */
public interface TenantRepository {

    Optional<Tenant> findById(Long id);

    Optional<Tenant> findByCode(String code);

    List<Tenant> findByOwnerId(Long ownerId);

    Tenant save(Tenant tenant);

    long count();

    List<Tenant> findAll();
}
