package com.kb.infrastructure.persistence.mysql;

import com.kb.domain.tenant.Tenant;
import com.kb.domain.tenant.TenantRepository;
import com.kb.infrastructure.persistence.mysql.dataobject.TenantDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TenantRepositoryImpl implements TenantRepository {

    private final TenantMapper mapper;

    @Override
    public Optional<Tenant> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Tenant> findByCode(String code) {
        return Optional.ofNullable(mapper.selectByCode(code)).map(this::toDomain);
    }

    @Override
    public List<Tenant> findByOwnerId(Long ownerId) {
        return mapper.selectByOwnerId(ownerId).stream().map(this::toDomain).toList();
    }

    @Override
    public Tenant save(Tenant tenant) {
        TenantDO tdo = new TenantDO();
        tdo.setId(tenant.getId());
        tdo.setName(tenant.getName());
        tdo.setCode(tenant.getCode());
        tdo.setOwnerId(tenant.getOwnerId());
        tdo.setStatus(tenant.getStatus());
        tdo.setMaxMembers(tenant.getMaxMembers());
        if (tdo.getId() == null) {
            mapper.insert(tdo);
        } else {
            mapper.updateById(tdo);
        }
        return toDomain(tdo);
    }

    @Override
    public long count() {
        return mapper.selectCount(null);
    }

    @Override
    public List<Tenant> findAll() {
        return mapper.selectList(null).stream().map(this::toDomain).toList();
    }

    private Tenant toDomain(TenantDO tdo) {
        return Tenant.builder()
                .id(tdo.getId())
                .name(tdo.getName())
                .code(tdo.getCode())
                .ownerId(tdo.getOwnerId())
                .status(tdo.getStatus())
                .maxMembers(tdo.getMaxMembers())
                .createdAt(tdo.getCreatedAt())
                .updatedAt(tdo.getUpdatedAt())
                .build();
    }
}
