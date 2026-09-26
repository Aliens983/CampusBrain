package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.application.service.ServiceItemService;
import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import com.laoliu.cas.appointment.domain.repository.ServiceCategoryRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * getServiceById 缓存注解回归测试（真实 Spring 容器 + CacheInterceptor）。
 * <p>
 * Spring 6 缓存抽象对 Optional 一等支持：@Cacheable 方法返回 Optional 时，入库前自动
 * 拆包，unless SpEL 中的 #result 是内容实体（empty 对应 null），命中时再自动包回
 * Optional 返回。旧表达式 "#result.isPresent()" 在实体类型上求值必抛
 * SpelEvaluationException(EL1004E)，服务详情接口在缓存未命中时 100% 500；
 * 该问题在 Redis 有热点缓存时被命中路径掩盖，清空缓存后全部详情才暴露。
 *
 * @author forever-king
 */
@SpringJUnitConfig(ServiceItemCacheableTest.TestConfig.class)
@DisplayName("服务详情 @Cacheable(unless) 回归测试")
class ServiceItemCacheableTest {

    /** allowNullValues=false 对齐生产 RedisCacheConfiguration.disableCachingNullValues() */
    static final String CACHE_NAME = "services";

    @EnableCaching
    @Configuration
    @Import(ServiceItemServiceImpl.class)
    static class TestConfig {
        @Bean
        ServiceItemRepository serviceItemRepository() {
            return Mockito.mock(ServiceItemRepository.class);
        }

        @Bean
        ServiceCategoryRepository serviceCategoryRepository() {
            return Mockito.mock(ServiceCategoryRepository.class);
        }

        @Bean
        SimpleCacheManager cacheManager() {
            SimpleCacheManager manager = new SimpleCacheManager();
            manager.setCaches(List.of(new ConcurrentMapCache(CACHE_NAME, false)));
            return manager;
        }
    }

    @Autowired private ServiceItemService cachedService;
    @Autowired private ServiceItemRepository serviceRepository;
    @Autowired private SimpleCacheManager cacheManager;

    private Cache servicesCache;

    @BeforeEach
    void setUp() {
        servicesCache = cacheManager.getCache(CACHE_NAME);
        servicesCache.clear();
    }

    @Test
    @DisplayName("存在的服务：unless 可正常求值，实体拆包入缓存，第二次调用命中（仓储仅 1 次）")
    void presentEntityDoesNotTriggerSpelErrorAndCachesContent() {
        Long id = 7L;
        ServiceItem service = ServiceItem.builder().serviceId(id).serviceName("设备借用").build();
        when(serviceRepository.findById(id)).thenReturn(Optional.of(service));

        Optional<ServiceItem> first = cachedService.getServiceById(id);
        Optional<ServiceItem> second = cachedService.getServiceById(id);

        // 旧 unless 表达式下这一步整个接口 500（SpelEvaluationException: isPresent()）
        assertThat(first).containsSame(service);
        // 命中时 Spring 自动把缓存实体重新包回 Optional
        assertThat(second).containsSame(service);
        verify(serviceRepository, times(1)).findById(id);
        // 缓存中存的是拆包后的实体本身，而非 Optional 包装
        assertThat(servicesCache.get(id, ServiceItem.class)).isSameAs(service);
    }

    @Test
    @DisplayName("不存在的服务：empty 拆包为 null，unless+禁空缓存双重排除，不写缓存且不报错")
    void emptyOptionalIsNotCachedAndDoesNotError() {
        Long id = 999L;
        when(serviceRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(cachedService.getServiceById(id)).isEmpty();
        assertThat(cachedService.getServiceById(id)).isEmpty();

        // null 不缓存：每次穿透仓储，避免"新建同 ID 服务"在 TTL 内读不到
        verify(serviceRepository, times(2)).findById(id);
        assertThat(servicesCache.get(id)).isNull();
    }
}
