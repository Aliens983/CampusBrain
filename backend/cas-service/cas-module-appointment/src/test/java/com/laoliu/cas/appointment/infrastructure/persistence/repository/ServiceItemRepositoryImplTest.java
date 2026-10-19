package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import com.laoliu.cas.appointment.domain.repository.ServiceCategoryRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ServiceItemDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ServiceItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * ServiceItemRepositoryImpl 单元测试——聚焦 save 更新路径的 booked_count 不回写契约.
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("服务仓库实现单元测试")
class ServiceItemRepositoryImplTest {

    @Mock
    private ServiceItemMapper serviceMapper;

    @Mock
    private ServiceCategoryRepository serviceCategoryRepository;

    private ServiceItemRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ServiceItemRepositoryImpl(serviceMapper, serviceCategoryRepository);
    }

    @Nested
    @DisplayName("保存服务 - save")
    class SaveTests {

        @Test
        @DisplayName("更新已存在的服务时必须剔除 booked_count，禁止全量 updateById 回写")
        void shouldNotWriteBookedCountWhenUpdating() {
            // Given：管理端加载到的实体带有读取时的 booked_count=5，
            // 而并发下单在此刻已将其推进到 6——本次更新不得覆盖该增量
            ServiceItem existing = ServiceItem.builder()
                    .serviceId(1L)
                    .serviceName("活动报名")
                    .serviceDescribe("校园活动报名")
                    .serviceState(1)
                    .capacity(10)
                    .bookedCount(5)
                    .categoryId(4L)
                    .campus("cq")
                    .imageUrl("/uploads/campus/cq.jpg")
                    .endDate(LocalDate.of(2026, 12, 31))
                    .build();

            // When
            repository.save(existing);

            // Then：走选择性更新而非全量 updateById，且写入对象不含 booked_count
            ArgumentCaptor<ServiceItemDO> captor = ArgumentCaptor.forClass(ServiceItemDO.class);
            verify(serviceMapper).updateByPrimaryKeySelective(captor.capture());
            verify(serviceMapper, never()).updateById(any(ServiceItemDO.class));
            assertNull(captor.getValue().getBookedCount(),
                    "booked_count 只允许由下单/取消的条件更新原子维护，不得随管理更新回写");
        }

        @Test
        @DisplayName("更新已存在的服务时其余可编辑字段应完整传入")
        void shouldPassAllEditableFieldsWhenUpdating() {
            ServiceItem existing = ServiceItem.builder()
                    .serviceId(2L)
                    .serviceName("设备借用")
                    .serviceDescribe("教学设备借用")
                    .serviceState(0)
                    .capacity(20)
                    .bookedCount(3)
                    .categoryId(2L)
                    .campus("xs")
                    .imageUrl("/uploads/equipment.jpg")
                    .endDate(LocalDate.of(2027, 1, 31))
                    .build();

            repository.save(existing);

            ArgumentCaptor<ServiceItemDO> captor = ArgumentCaptor.forClass(ServiceItemDO.class);
            verify(serviceMapper).updateByPrimaryKeySelective(captor.capture());
            ServiceItemDO written = captor.getValue();
            org.junit.jupiter.api.Assertions.assertEquals(2L, written.getServiceId());
            org.junit.jupiter.api.Assertions.assertEquals("设备借用", written.getServiceName());
            org.junit.jupiter.api.Assertions.assertEquals("教学设备借用", written.getServiceDescribe());
            org.junit.jupiter.api.Assertions.assertEquals(0, written.getServiceState());
            org.junit.jupiter.api.Assertions.assertEquals(20, written.getCapacity());
            org.junit.jupiter.api.Assertions.assertEquals("/uploads/equipment.jpg", written.getImageUrl());
            org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2027, 1, 31), written.getEndDate());
        }

        @Test
        @DisplayName("保存新服务（serviceId 为空）时走插入路径")
        void shouldInsertWhenServiceIdIsNull() {
            ServiceItem fresh = ServiceItem.builder()
                    .serviceName("新服务")
                    .serviceDescribe("描述")
                    .serviceState(1)
                    .capacity(-1)
                    .categoryId(4L)
                    .campus("cq")
                    .build();

            repository.save(fresh);

            verify(serviceMapper).insert(any(ServiceItemDO.class));
            verify(serviceMapper, never()).updateByPrimaryKeySelective(any(ServiceItemDO.class));
            verify(serviceMapper, never()).updateById(any(ServiceItemDO.class));
        }
    }
}
