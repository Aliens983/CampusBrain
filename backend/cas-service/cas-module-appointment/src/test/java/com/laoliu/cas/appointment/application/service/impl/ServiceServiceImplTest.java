package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.ServiceService;
import com.laoliu.cas.appointment.domain.entity.Service;
import com.laoliu.cas.appointment.domain.repository.ServiceRepository;
import com.laoliu.cas.appointment.interfaces.dto.request.ServiceAddRequest;
import com.laoliu.cas.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ServiceServiceImpl 单元测试。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("服务管理服务单元测试")
class ServiceServiceImplTest {

    @Mock
    private ServiceRepository serviceRepository;

    private ServiceService serviceService;

    private static final Long SERVICE_ID_1 = 1L;
    private static final Long SERVICE_ID_2 = 2L;

    @BeforeEach
    void setUp() {
        serviceService = new ServiceServiceImpl(serviceRepository);
    }

    @Nested
    @DisplayName("获取所有服务 - getAllServices")
    class GetAllServicesTests {

        @Test
        @DisplayName("应当返回所有服务列表")
        void shouldReturnAllServices() {
            // Given
            Service service1 = buildService(SERVICE_ID_1, "自习室预约", "图书馆自习室", 1);
            Service service2 = buildService(SERVICE_ID_2, "心理咨询", "心理健康咨询", 1);
            when(serviceRepository.findAll()).thenReturn(List.of(service1, service2));

            // When
            List<Service> result = serviceService.getAllServices();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("自习室预约", result.get(0).getServiceName());
            assertEquals("心理咨询", result.get(1).getServiceName());
        }

        @Test
        @DisplayName("没有服务时应当返回空列表")
        void shouldReturnEmptyListWhenNoServices() {
            // Given
            when(serviceRepository.findAll()).thenReturn(List.of());

            // When
            List<Service> result = serviceService.getAllServices();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("获取可用服务 - getAvailableServices")
    class GetAvailableServicesTests {

        @Test
        @DisplayName("应当只返回启用状态的服务")
        void shouldReturnOnlyAvailableServices() {
            // Given
            Service available1 = buildService(SERVICE_ID_1, "自习室预约", "图书馆自习室", 1);
            Service disabled = buildService(SERVICE_ID_2, "已下架服务", "已下架", 0);
            when(serviceRepository.findAll()).thenReturn(List.of(available1, disabled));

            // When
            List<Service> result = serviceService.getAvailableServices();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("自习室预约", result.get(0).getServiceName());
            assertTrue(result.get(0).isAvailable());
        }

        @Test
        @DisplayName("所有服务都已禁用时应当返回空列表")
        void shouldReturnEmptyListWhenAllDisabled() {
            // Given
            Service disabled1 = buildService(SERVICE_ID_1, "已禁用1", "已禁用", 0);
            Service disabled2 = buildService(SERVICE_ID_2, "已禁用2", "已禁用", 0);
            when(serviceRepository.findAll()).thenReturn(List.of(disabled1, disabled2));

            // When
            List<Service> result = serviceService.getAvailableServices();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("根据 ID 获取服务 - getServiceById")
    class GetServiceByIdTests {

        @Test
        @DisplayName("应当返回指定 ID 的服务")
        void shouldReturnServiceById() {
            // Given
            Service service = buildService(SERVICE_ID_1, "自习室预约", "图书馆自习室", 1);
            when(serviceRepository.findById(SERVICE_ID_1)).thenReturn(Optional.of(service));

            // When
            Optional<Service> result = serviceService.getServiceById(SERVICE_ID_1);

            // Then
            assertTrue(result.isPresent());
            assertEquals(SERVICE_ID_1, result.get().getServiceId());
            assertEquals("自习室预约", result.get().getServiceName());
        }

        @Test
        @DisplayName("服务不存在时应当返回空 Optional")
        void shouldReturnEmptyOptionalWhenNotFound() {
            // Given
            when(serviceRepository.findById(SERVICE_ID_1)).thenReturn(Optional.empty());

            // When
            Optional<Service> result = serviceService.getServiceById(SERVICE_ID_1);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("添加服务 - addService")
    class AddServiceTests {

        @Test
        @DisplayName("应当成功添加服务并返回 true")
        void shouldAddServiceSuccessfully() {
            // Given
            ServiceAddRequest request = buildServiceAddRequest("新服务", "新服务描述", 1);
            when(serviceRepository.save(any(Service.class))).thenReturn(buildService(3L, "新服务", "新服务描述", 1));

            // When
            boolean result = serviceService.addService(request);

            // Then
            assertTrue(result);
            verify(serviceRepository).save(any(Service.class));
        }
    }

    // ======================== 辅助方法 ========================

    private Service buildService(Long id, String name, String describe, Integer state) {
        return Service.builder()
                .serviceId(id)
                .serviceName(name)
                .serviceDescribe(describe)
                .serviceState(state)
                .build();
    }

    private ServiceAddRequest buildServiceAddRequest(String name, String describe, Integer state) {
        ServiceAddRequest request = new ServiceAddRequest();
        request.setServiceName(name);
        request.setServiceDescribe(describe);
        request.setServiceState(state);
        return request;
    }
}
