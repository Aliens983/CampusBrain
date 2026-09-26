package com.laoliu.cas.db;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.laoliu.cas.appointment.domain.view.BookingRef;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ConsultChatMessageDO;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ItemDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ConsultChatConversationMapper;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ConsultChatMessageMapper;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ItemMapper;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.TimeSlotMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 预约防重/防超卖唯一约束的真实 MySQL 集成验证（深度审查报告 3.1 / 5.1 / 5.2，D-A1）。
 *
 * <p>ItemMapper.xml 的 SQL 不在 Mockito 单测中执行，本类用 Testcontainers MySQL 8.0
 * （或 IT_MYSQL_URL 指定的外部库）验证：
 * <ol>
 *   <li>Flyway 冒烟：开发期全量基线 V1（含生成列与 uk_item_active_general）迁移成功，
 *       不存在历史增量脚本版本；</li>
 *   <li>终态共存 / 资源类单共存场景在真实 SQL 上回归；</li>
 *   <li>多线程并发验证：通用下单 DB 层收敛 1 行、容量不超卖、时段占用单赢家。</li>
 * </ol>
 *
 * <p>不启动 Spring 上下文（避免 Redis/RabbitMQ 依赖），手工装配
 * MyBatis-Plus 的 SqlSessionFactory（BaseMapper 支持）+ Hikari 连接池，
 * 隔离级别保持 MySQL 默认 REPEATABLE READ，真实复现快照读并发窗口。
 */
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class BookingGuardDbTest {

    private static final int PENDING = 0;
    private static final int APPROVED = 1;
    private static final int REJECTED = 2;
    private static final int CANCELLED = 3;
    private static final int COMPLETED = 4;

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("cas_it")
            .withUsername("it")
            .withPassword("it");

    /**
     * 外部 MySQL 开关：设置 IT_MYSQL_URL（如 jdbc:mysql://127.0.0.1:3399/cas_it）后，
     * 不再启动 Testcontainers，直连该库（每轮先 Flyway clean 保证可重复执行）。
     * 用于 Docker 端口转发异常（容器内正常但宿主机访问发布端口失败）的环境；
     * 默认缺省仍走 Testcontainers MySQL 8.0。
     */
    private static final String EXTERNAL_JDBC_URL = System.getenv("IT_MYSQL_URL");
    private static final String EXTERNAL_USER =
            System.getenv().getOrDefault("IT_MYSQL_USER", "it");
    private static final String EXTERNAL_PASSWORD =
            System.getenv().getOrDefault("IT_MYSQL_PASSWORD", "it");

    private static HikariDataSource dataSource;
    private static JdbcTemplate jdbc;
    private static SqlSessionFactory factory;

    @BeforeAll
    static void setUp() throws Exception {
        boolean external = EXTERNAL_JDBC_URL != null && !EXTERNAL_JDBC_URL.isBlank();
        if (external) {
            dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(EXTERNAL_JDBC_URL);
            dataSource.setUsername(EXTERNAL_USER);
            dataSource.setPassword(EXTERNAL_PASSWORD);
        } else {
            MYSQL.start();
            dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(MYSQL.getJdbcUrl());
            dataSource.setUsername(MYSQL.getUsername());
            dataSource.setPassword(MYSQL.getPassword());
        }
        dataSource.setMaximumPoolSize(20);

        // 5.1 冒烟本身：全量基线 V1 一次迁移完成，任一 SQL 有误会在此抛出。
        // 外部库可能残留上一轮对象，clean 后重放；全新容器库 clean 为空操作。
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();

        jdbc = new JdbcTemplate(dataSource);

        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/*.xml"));
        factory = factoryBean.getObject();
        factory.getConfiguration().addMappers(
                "com.laoliu.cas.appointment.infrastructure.persistence.mapper");
    }

    @AfterAll
    static void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
        if (EXTERNAL_JDBC_URL == null || EXTERNAL_JDBC_URL.isBlank()) {
            MYSQL.stop();
        }
    }

    // ==================== 5.1 Flyway 冒烟 ====================

    @Test
    @DisplayName("5.1 冒烟：全量基线 V1 就位（生成列+新唯一约束），历史增量版本不存在")
    void baselineV1ContainsGeneratedColumnAndUniqueConstraint() {
        Integer newIndex = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints "
                        + "WHERE constraint_schema = DATABASE() AND table_name = 'item' "
                        + "AND constraint_name = 'uk_item_active_general'",
                Integer.class);
        Integer oldIndex = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'item' "
                        + "AND index_name = 'uk_user_service_status'",
                Integer.class);
        Integer generatedColumn = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'item' "
                        + "AND column_name = 'active_dedup' AND extra LIKE '%GENERATED%'",
                Integer.class);
        Integer v1Applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1",
                Integer.class);
        Integer incrementalVersions = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history "
                        + "WHERE version IN ('4','5','6','7','8','9')",
                Integer.class);
        Integer endDateColumn = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'services' "
                        + "AND column_name = 'end_date'",
                Integer.class);
        Integer uniqueEmail = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'user' "
                        + "AND index_name = 'uk_user_email' AND non_unique = 0",
                Integer.class);

        assertThat(newIndex).isEqualTo(1);
        assertThat(oldIndex).isZero();
        assertThat(generatedColumn).isEqualTo(1);
        assertThat(v1Applied).isEqualTo(1);
        assertThat(incrementalVersions).isZero();
        assertThat(endDateColumn).isEqualTo(1);
        assertThat(uniqueEmail).isEqualTo(1);
    }

    // ==================== 故障 A：二次取消 / 终态共存 ====================

    @Test
    @DisplayName("3.1-A 同一用户同服务反复约/取消/完结/拒绝不再撞唯一键")
    void repeatedCancelAndTerminalStatusesNeverConflict() {
        long userId = 9201L;
        int serviceId = 9201;
        seedUser(userId);
        seedService(serviceId, 5);

        // 每个阶段独立事务提交，再用新连接核对状态（贴近生产每请求一事务）。
        // 注意 cancelByIdsAndRelease 是 item JOIN services 的多表 UPDATE，
        // 其返回行数为各表匹配行之和，不能当作"取消订单数"断言。
        Long first;
        Long second;
        Long third;
        Long fourth;
        try (SqlSession session = factory.openSession()) {
            ItemMapper mapper = session.getMapper(ItemMapper.class);
            first = insertGeneral(mapper, userId, serviceId);
            session.commit();
        }
        cancelAndExpectStatus(first, userId, CANCELLED);

        try (SqlSession session = factory.openSession()) {
            ItemMapper mapper = session.getMapper(ItemMapper.class);
            // 第二轮：再约 → 再取消（旧 UNIQUE(u,s,status) 设计下这里必撞 (u,s,3) 报 1062）
            second = insertGeneral(mapper, userId, serviceId);
            session.commit();
        }
        cancelAndExpectStatus(second, userId, CANCELLED);

        try (SqlSession session = factory.openSession()) {
            ItemMapper mapper = session.getMapper(ItemMapper.class);
            // 第三轮：待审核 → 已完结（4）
            third = insertGeneral(mapper, userId, serviceId);
            assertThat(mapper.auditService(third, COMPLETED, null, List.of(PENDING)))
                    .isEqualTo(1);
            // 第四轮：新待审单与已完结单共存；随后拒绝（2）
            fourth = insertGeneral(mapper, userId, serviceId);
            assertThat(mapper.auditService(fourth, REJECTED, "不符合", List.of(PENDING)))
                    .isEqualTo(1);
            session.commit();
        }
        assertThat(statusOf(third)).isEqualTo(COMPLETED);
        assertThat(statusOf(fourth)).isEqualTo(REJECTED);

        // 直接证据：同一 (u,s) 的终态行可无限共存（active_dedup 全为 NULL）
        jdbc.update("INSERT INTO item(user_id, service_id, manage_status) VALUES "
                + "(9201, 9201, 3), (9201, 9201, 3), (9201, 9201, 4)");

        Integer cancelledRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM item WHERE user_id = 9201 AND service_id = 9201 "
                        + "AND manage_status = 3", Integer.class);
        assertThat(cancelledRows).isGreaterThanOrEqualTo(3);
    }

    // ==================== 故障 B：同服务多资源单 ====================

    @Test
    @DisplayName("3.1-B 同一服务的两间教室/两位咨询师/两台设备待审单共存（旧约束下第二笔 500）")
    void multipleResourceBookingsOfSameServiceCoexist() {
        long userId = 9202L;
        int serviceId = 9202;
        seedUser(userId);
        seedService(serviceId, -1);
        seedConsultant(9202L, serviceId);
        LocalDate date = LocalDate.now().plusDays(7);

        try (SqlSession session = factory.openSession()) {
            ItemMapper mapper = session.getMapper(ItemMapper.class);

            Long room1 = insertRoom(mapper, userId, serviceId, 7101L, date, "08:00", "09:00");
            Long room2 = insertRoom(mapper, userId, serviceId, 7102L, date, "08:00", "09:00");
            Long consult1 = insertConsultation(mapper, userId, serviceId, 9202L, 8101L,
                    date, "09:00", "10:00");
            Long consult2 = insertConsultation(mapper, userId, serviceId, 9202L, 8102L,
                    date, "10:00", "11:00");
            Long equip1 = insertEquipment(mapper, userId, serviceId, 9101L, date, "13:00", "14:00");
            Long equip2 = insertEquipment(mapper, userId, serviceId, 9102L, date, "14:00", "15:00");
            session.commit();

            assertThat(List.of(room1, room2, consult1, consult2, equip1, equip2))
                    .as("六笔资源单（同服务不同资源）必须全部落库")
                    .doesNotContainNull();
        }

        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM item WHERE user_id = 9202 AND service_id = 9202 "
                        + "AND manage_status = 0", Integer.class);
        assertThat(rows).isEqualTo(6);
    }

    @Test
    @DisplayName("通用单活跃态唯一：第二笔待审被幂等拦截，终态后可重新预约")
    void activeGeneralBookingIsDeduplicatedUntilTerminal() {
        long userId = 9203L;
        int serviceId = 9203;
        seedUser(userId);
        seedService(serviceId, 5);

        try (SqlSession session = factory.openSession()) {
            ItemMapper mapper = session.getMapper(ItemMapper.class);
            assertThat(insertGeneral(mapper, userId, serviceId)).isNotNull();
            ItemDO duplicate = ItemDO.builder().userId(userId).serviceId(serviceId).build();
            assertThat(mapper.insertSingleService(duplicate, PENDING, APPROVED)).isZero();
            session.commit();
        }

        // 置终态后，新一笔待审单允许创建（NULL 不参与唯一）
        jdbc.update("UPDATE item SET manage_status = 3 WHERE user_id = 9203 AND service_id = 9203");
        try (SqlSession session = factory.openSession()) {
            ItemMapper mapper = session.getMapper(ItemMapper.class);
            assertThat(insertGeneral(mapper, userId, serviceId)).isNotNull();
            session.commit();
        }

        Integer active = jdbc.queryForObject(
                "SELECT COUNT(*) FROM item WHERE user_id = 9203 AND service_id = 9203 "
                        + "AND manage_status IN (0, 1)", Integer.class);
        assertThat(active).isEqualTo(1);
    }

    // ==================== 5.2 并发场景 ====================

    @Test
    @DisplayName("5.2 并发通用下单：10 线程同刻点击，最终恰好一笔有效单（死锁方整体回滚，绝不产生重复）")
    void concurrentGeneralBookingConvergesToOneRow() throws Exception {
        long userId = 9204L;
        int serviceId = 9204;
        seedUser(userId);
        seedService(serviceId, 100);

        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch fire = new CountDownLatch(1);
        AtomicInteger inserted = new AtomicInteger();
        AtomicInteger deadlocked = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        fire.await();
                        try (SqlSession session = factory.openSession()) {
                            ItemMapper mapper = session.getMapper(ItemMapper.class);
                            ItemDO item = ItemDO.builder().userId(userId).serviceId(serviceId).build();
                            int rows = mapper.insertSingleService(item, PENDING, APPROVED);
                            session.commit();
                            if (rows > 0) {
                                inserted.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        // REPEATABLE READ 下 INSERT...SELECT 的间隙锁会让同时起跑的并发事务
                        // 互相死锁（1213）：InnoDB 回滚落败方整个事务。它不会产生重复行，
                        // 落败方语义上等同于"未抢到"，仅 UX 上表现为需重试（已在看板第 7 节
                        // 登记：是否加应用层有限重试/改 RC 由用户裁定，不在 DB 约束范围内）。
                        if (isDeadlock(e)) {
                            deadlocked.incrementAndGet();
                        } else {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            fire.countDown();
        } finally {
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }

        // 不变量：成功提交恰好 1 笔；其余要么 INSERT IGNORE 0 行，要么死锁整体回滚
        assertThat(inserted.get()).isEqualTo(1);
        assertThat(inserted.get() + deadlocked.get()).isLessThanOrEqualTo(threads);
        Integer activeRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM item WHERE user_id = 9204 AND service_id = 9204 "
                        + "AND manage_status IN (0, 1)", Integer.class);
        assertThat(activeRows).isEqualTo(1);
    }

    private static boolean isDeadlock(Throwable e) {
        for (Throwable c = e; c != null; c = c.getCause()) {
            String name = c.getClass().getSimpleName();
            String msg = c.getMessage();
            if (name.contains("Deadlock") || (msg != null && msg.contains("Deadlock found"))) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("5.2 并发容量扣减：capacity=2 时 10 线程竞争恰好 2 笔成功，绝不超卖")
    void concurrentDecrementStockNeverOversells() throws Exception {
        seedService(9205, 2);

        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch fire = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        fire.await();
                        try (SqlSession session = factory.openSession()) {
                            if (session.getMapper(ItemMapper.class).decrementStock(9205L) > 0) {
                                success.incrementAndGet();
                            }
                            session.commit();
                        }
                    } catch (Exception ignored) {
                        // 死锁等错误不计成功，符合"绝不超卖"语义
                    }
                });
            }
            ready.await(10, TimeUnit.SECONDS);
            fire.countDown();
        } finally {
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }

        assertThat(success.get()).isEqualTo(2);
        Integer booked = jdbc.queryForObject(
                "SELECT booked_count FROM services WHERE service_id = 9205", Integer.class);
        assertThat(booked).isEqualTo(2);
    }

    @Test
    @DisplayName("5.2 并发时段占用：10 线程抢占同一时段，原子 UPDATE 保证单赢家")
    void concurrentSlotOccupyHasSingleWinner() throws Exception {
        seedService(9206, -1);
        seedConsultant(9206L, 9206);
        jdbc.update("INSERT INTO time_slot(id, consultant_id, slot_date, start_time, end_time, available) "
                + "VALUES (9206, 9206, ?, '08:00', '09:00', 1)", LocalDate.now().plusDays(7));

        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch fire = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        fire.await();
                        try (SqlSession session = factory.openSession()) {
                            if (session.getMapper(TimeSlotMapper.class).occupy(9206L) > 0) {
                                success.incrementAndGet();
                            }
                            session.commit();
                        }
                    } catch (Exception ignored) {
                        // 锁等待异常不计成功
                    }
                });
            }
            ready.await(10, TimeUnit.SECONDS);
            fire.countDown();
        } finally {
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }

        assertThat(success.get()).isEqualTo(1);
        Integer available = jdbc.queryForObject(
                "SELECT available FROM time_slot WHERE id = 9206", Integer.class);
        assertThat(available).isZero();
    }

    // ==================== 3.4 管理员强制取消/完结（僵尸单兜底） ====================

    @Test
    @DisplayName("3.4 已通过的咨询单用户无法取消，管理员强制取消后状态置 3 且时段释放")
    void adminCancelApprovedConsultationReleasesSlot() {
        seedUser(9301);
        seedService(9301, -1);
        seedConsultant(9301L, 9301);
        LocalDate future = LocalDate.now().plusDays(7);
        jdbc.update("INSERT INTO time_slot(id, consultant_id, slot_date, start_time, end_time, available) "
                + "VALUES (9301, 9301, ?, '10:00', '11:00', 1)", future);

        Long orderId;
        try (SqlSession session = factory.openSession()) {
            orderId = insertConsultation(session.getMapper(ItemMapper.class),
                    9301L, 9301, 9301L, 9301L, future, "10:00", "11:00");
            session.commit();
        }
        assertThat(orderId).isNotNull();
        // 模拟审核通过 + 时段已被占用
        jdbc.update("UPDATE item SET manage_status = 1 WHERE order_id = ?", orderId);
        jdbc.update("UPDATE time_slot SET available = 0 WHERE id = 9301");

        // 用户侧取消：已通过的非活动单 0 行（3.4 僵尸成因之一）
        try (SqlSession session = factory.openSession()) {
            int rows = session.getMapper(ItemMapper.class).cancelByIdsAndRelease(
                    9301L, List.of(orderId), PENDING, APPROVED, CANCELLED);
            assertThat(rows).isZero();
            session.commit();
        }
        assertThat(statusOf(orderId)).isEqualTo(APPROVED);

        // 管理员强制取消：命中、状态置 3、时段释放
        try (SqlSession session = factory.openSession()) {
            int rows = session.getMapper(ItemMapper.class).adminCancelAndRelease(
                    orderId, "管理员清理僵尸单", PENDING, APPROVED, CANCELLED);
            assertThat(rows).isGreaterThanOrEqualTo(1);
            session.commit();
        }
        assertThat(statusOf(orderId)).isEqualTo(CANCELLED);
        Integer available = jdbc.queryForObject(
                "SELECT available FROM time_slot WHERE id = 9301", Integer.class);
        assertThat(available).isEqualTo(1);
    }

    @Test
    @DisplayName("3.4 无 end_time 且服务无 end_date 的僵尸通用单：定时任务无法完结，管理员强制完结并回补名额")
    void adminCompleteZombieGeneralBookingReturnsCapacity() {
        seedUser(9302);
        seedService(9302, 2);
        jdbc.update("UPDATE services SET booked_count = 1 WHERE service_id = 9302");
        Long orderId;
        try (SqlSession session = factory.openSession()) {
            orderId = insertGeneral(session.getMapper(ItemMapper.class), 9302L, 9302);
            session.commit();
        }
        jdbc.update("UPDATE item SET manage_status = 1 WHERE order_id = ?", orderId);

        // 自动完结对该单无效（end_time NULL + end_date NULL）——僵尸单成立
        try (SqlSession session = factory.openSession()) {
            assertThat(session.getMapper(ItemMapper.class).autoCompleteExpired(APPROVED, COMPLETED)).isZero();
            session.commit();
        }
        assertThat(statusOf(orderId)).isEqualTo(APPROVED);

        // 管理员强制完结：状态 4，booked_count 1→0
        try (SqlSession session = factory.openSession()) {
            int rows = session.getMapper(ItemMapper.class).adminCompleteAndRelease(
                    orderId, null, PENDING, APPROVED, COMPLETED);
            assertThat(rows).isGreaterThanOrEqualTo(1);
            session.commit();
        }
        assertThat(statusOf(orderId)).isEqualTo(COMPLETED);
        Integer booked = jdbc.queryForObject(
                "SELECT booked_count FROM services WHERE service_id = 9302", Integer.class);
        assertThat(booked).isZero();

        // 终态幂等：再次完结 0 行，名额不二次回补
        try (SqlSession session = factory.openSession()) {
            assertThat(session.getMapper(ItemMapper.class).adminCompleteAndRelease(
                    orderId, null, PENDING, APPROVED, COMPLETED)).isZero();
            session.commit();
        }
        assertThat(jdbc.queryForObject(
                "SELECT booked_count FROM services WHERE service_id = 9302", Integer.class)).isZero();
    }

    @Test
    @DisplayName("3.4 终态单管理员取消/完结均 0 行：状态与名额保持不变")
    void adminActionsOnTerminalBookingAreIdempotent() {
        seedUser(9303);
        seedService(9303, 2);
        Long orderId;
        try (SqlSession session = factory.openSession()) {
            orderId = insertGeneral(session.getMapper(ItemMapper.class), 9303L, 9303);
            session.commit();
        }
        jdbc.update("UPDATE item SET manage_status = 3 WHERE order_id = ?", orderId);
        jdbc.update("UPDATE services SET booked_count = 1 WHERE service_id = 9303");

        try (SqlSession session = factory.openSession()) {
            ItemMapper mapper = session.getMapper(ItemMapper.class);
            assertThat(mapper.adminCancelAndRelease(orderId, null, PENDING, APPROVED, CANCELLED)).isZero();
            assertThat(mapper.adminCompleteAndRelease(orderId, null, PENDING, APPROVED, COMPLETED)).isZero();
            session.commit();
        }
        assertThat(statusOf(orderId)).isEqualTo(CANCELLED);
        assertThat(jdbc.queryForObject(
                "SELECT booked_count FROM services WHERE service_id = 9303", Integer.class)).isEqualTo(1);
    }

    // ==================== 3.5 取消事件：锁定读引用 + 真实 serviceId ====================

    @Test
    @DisplayName("3.5 selectCancellableBookingRefs FOR UPDATE 锁定读返回真实引用，取消后终态单不再命中")
    void cancellableBookingRefsReturnRealServiceIdAndDisappearAfterCancel() {
        seedUser(9304);
        seedService(9304, 5);
        final Long orderId;
        try (SqlSession session = factory.openSession()) {
            orderId = insertGeneral(session.getMapper(ItemMapper.class), 9304L, 9304);
            session.commit();
        }

        try (SqlSession session = factory.openSession()) {
            ItemMapper mapper = session.getMapper(ItemMapper.class);
            // 入参混入一个不存在的订单：锁定读只返回真实命中的 1 单，且携带 orderId/userId/serviceId
            List<BookingRef> refs = mapper.selectCancellableBookingRefs(
                    9304L, List.of(orderId, 999999L), PENDING, APPROVED);
            assertThat(refs).hasSize(1);
            BookingRef ref = refs.get(0);
            assertThat(ref.orderId()).isEqualTo(orderId);
            assertThat(ref.userId()).isEqualTo(9304L);
            assertThat(ref.serviceId()).isEqualTo(9304L);

            // 同事务内按锁定集合执行取消：多表行数 ≥2（item + services），但取消订单数以 refs 为准
            int rows = mapper.cancelByIdsAndRelease(
                    9304L, List.of(orderId), PENDING, APPROVED, CANCELLED);
            assertThat(rows).isGreaterThanOrEqualTo(2);
            session.commit();
        }
        assertThat(statusOf(orderId)).isEqualTo(CANCELLED);

        // 终态单再次锁定读为空：事件不会二次发送
        try (SqlSession session = factory.openSession()) {
            assertThat(session.getMapper(ItemMapper.class).selectCancellableBookingRefs(
                    9304L, List.of(orderId), PENDING, APPROVED)).isEmpty();
        }
    }

    // ==================== 3.6 autoCompleteExpired 回补条件与取消链路一致（补 slot_id） ====================

    @Test
    @DisplayName("3.6 自动完结：仅 slot_id 非空（其余三外键空）的单不回补名额；四项全空容量单正常回补")
    void autoCompleteReleasesOnlyFourNullCapacityBookings() {
        // 行 A：异常形态——只有 slot_id（无 consultant/room/equipment），时段已过，已通过
        seedUser(9305);
        seedService(9305, 2);
        jdbc.update("UPDATE services SET booked_count = 1 WHERE service_id = 9305");
        final Long slotOnlyOrder;
        try (SqlSession session = factory.openSession()) {
            slotOnlyOrder = insertGeneral(session.getMapper(ItemMapper.class), 9305L, 9305);
            session.commit();
        }
        jdbc.update("UPDATE item SET manage_status = 1, slot_id = 930500, "
                + "slot_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY), start_time = '08:00', end_time = '09:00' "
                + "WHERE order_id = ?", slotOnlyOrder);

        // 行 B：容量型单（四外键全空），服务 end_date 已过，已通过
        seedUser(9306);
        seedService(9306, 2);
        jdbc.update("UPDATE services SET booked_count = 1, end_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY) "
                + "WHERE service_id = 9306");
        final Long capacityOrder;
        try (SqlSession session = factory.openSession()) {
            capacityOrder = insertGeneral(session.getMapper(ItemMapper.class), 9306L, 9306);
            session.commit();
        }
        jdbc.update("UPDATE item SET manage_status = 1 WHERE order_id = ?", capacityOrder);

        // 全局自动完结：两行都应置 COMPLETED
        try (SqlSession session = factory.openSession()) {
            int rows = session.getMapper(ItemMapper.class).autoCompleteExpired(APPROVED, COMPLETED);
            // 多表 UPDATE，两行 × 两表 = 4
            assertThat(rows).isGreaterThanOrEqualTo(4);
            session.commit();
        }
        assertThat(statusOf(slotOnlyOrder)).isEqualTo(COMPLETED);
        assertThat(statusOf(capacityOrder)).isEqualTo(COMPLETED);

        // 行 A 从未扣过 booked_count（slot 型），完结不得回补：保持 1（旧 SQL 漏判 slot_id 会变成 0）
        assertThat(jdbc.queryForObject(
                "SELECT booked_count FROM services WHERE service_id = 9305", Integer.class)).isEqualTo(1);
        // 行 B 容量型真实扣过，完结回补 1→0
        assertThat(jdbc.queryForObject(
                "SELECT booked_count FROM services WHERE service_id = 9306", Integer.class)).isZero();
    }

    // ==================== 4.7 批量聚合 SQL（N+1 收敛）真实 MySQL 验证 ====================

    @Test
    @DisplayName("4.7 教室/设备/咨询师批量聚合：GROUP BY 计数与逐条版语义一致，无占用者缺省")
    void batchOverlapAndSlotCountQueriesMatchPerRowSemantics() {
        LocalDate date = LocalDate.now().plusDays(3);
        seedUser(9401);
        seedService(9401, -1);
        // 教室 9401 窗口 10:00-12:00 内两笔（待审+已通过），另有一笔已取消不计；9402 仅窗口外一笔
        jdbc.update("INSERT INTO item(user_id, service_id, room_id, slot_date, start_time, end_time, manage_status) "
                + "VALUES (9401, 9401, 9401, ?, '09:00', '11:00', 0),"
                + "       (9401, 9401, 9401, ?, '11:00', '13:00', 1),"
                + "       (9401, 9401, 9401, ?, '10:30', '11:30', 3),"
                + "       (9401, 9401, 9402, ?, '08:00', '09:00', 0)",
                date, date, date, date);
        // 设备 9501 窗口内 2+3 台（待审+已通过），另有已拒绝的 100 台不计
        jdbc.update("INSERT INTO item(user_id, service_id, equipment_id, quantity, slot_date, "
                        + "start_time, end_time, manage_status) "
                        + "VALUES (9401, 9401, 9501, 2, ?, '09:30', '11:30', 0),"
                        + "       (9401, 9401, 9501, 3, ?, '11:00', '12:30', 1),"
                        + "       (9401, 9401, 9501, 100, ?, '10:00', '12:00', 2)",
                date, date, date);

        try (SqlSession session = factory.openSession()) {
            ItemMapper mapper = session.getMapper(ItemMapper.class);
            // 9403 完全无单：结果集中应缺席（调用方按 0 兜底）
            Map<Long, Integer> roomCounts = mapper.countRoomOverlapBatch(
                            List.of(9401L, 9402L, 9403L), date, "10:00", "12:00", PENDING, APPROVED).stream()
                    .collect(Collectors.toMap(ItemMapper.ResourceOverlapRow::resourceId,
                            ItemMapper.ResourceOverlapRow::cnt));
            assertThat(roomCounts).containsEntry(9401L, 2);
            assertThat(roomCounts).doesNotContainKeys(9402L, 9403L);

            Map<Long, Integer> equipCounts = mapper.sumEquipmentOverlapBatch(
                            List.of(9501L, 9502L), date, "10:00", "12:00", PENDING, APPROVED).stream()
                    .collect(Collectors.toMap(ItemMapper.ResourceOverlapRow::resourceId,
                            ItemMapper.ResourceOverlapRow::cnt));
            assertThat(equipCounts).containsEntry(9501L, 5);
            assertThat(equipCounts).doesNotContainKey(9502L);
        }

        // 咨询师时段：9402 当日 2 个可用 + 1 个停用；9403 的可用时段在另一天
        seedService(9402, -1);
        seedConsultant(9402L, 9402);
        seedConsultant(9403L, 9402);
        jdbc.update("INSERT INTO time_slot(id, consultant_id, slot_date, start_time, end_time, available) "
                + "VALUES (940201, 9402, ?, '08:00', '09:00', 1),"
                + "       (940202, 9402, ?, '09:00', '10:00', 1),"
                + "       (940203, 9402, ?, '10:00', '11:00', 0),"
                + "       (940301, 9403, ?, '08:00', '09:00', 1)",
                date, date, date, date.plusDays(1));
        try (SqlSession session = factory.openSession()) {
            Map<Long, Integer> slotCounts = session.getMapper(TimeSlotMapper.class)
                            .countAvailableByConsultants(List.of(9402L, 9403L), date).stream()
                    .collect(Collectors.toMap(TimeSlotMapper.SlotCountRow::consultantId,
                            TimeSlotMapper.SlotCountRow::cnt));
            assertThat(slotCounts).containsEntry(9402L, 2);
            assertThat(slotCounts).doesNotContainKey(9403L);
        }
    }

    @Test
    @DisplayName("4.7 会话列表批量查询：最后消息按 MAX(id)、未读 GROUP BY、用户名 IN 批量取回")
    void chatBatchQueriesReturnLastMessageUnreadAndNames() {
        jdbc.update("INSERT INTO `user`(id, name, email, password) VALUES "
                + "(9601, '学生甲', '9601@e.com', 'x'), (9602, '教师乙', '9602@e.com', 'x'),"
                + "(9603, '学生丙', '9603@e.com', 'x'), (9604, '教师丁', '9604@e.com', 'x')");
        jdbc.update("INSERT INTO consult_chat_conversation(id, student_id, teacher_id, created_at) VALUES "
                + "(9601, 9601, 9602, NOW()), (9602, 9603, 9604, NOW())");
        // 会话 9601：学生 9601 视角有 2 条教师未读；最后一条是学生自己发的
        jdbc.update("INSERT INTO consult_chat_message(id, conversation_id, sender_id, content, read_flag, created_at) "
                + "VALUES (96001, 9601, 9602, '第一条', 0, NOW()),"
                + "       (96002, 9601, 9602, '第二条', 0, NOW()),"
                + "       (96003, 9601, 9601, '我的回复', 0, NOW()),"
                // 会话 9602：唯一一条已读 → 未读聚合缺席，但最后消息仍应返回
                + "       (96004, 9602, 9603, '已读消息', 1, NOW())");

        try (SqlSession session = factory.openSession()) {
            ConsultChatMessageMapper messageMapper = session.getMapper(ConsultChatMessageMapper.class);
            ConsultChatConversationMapper conversationMapper =
                    session.getMapper(ConsultChatConversationMapper.class);

            Map<Long, ConsultChatMessageDO> lastMessages =
                    messageMapper.selectLastMessages(List.of(9601L, 9602L, 999999L));
            assertThat(lastMessages).containsOnlyKeys(9601L, 9602L);
            assertThat(lastMessages.get(9601L).getId()).isEqualTo(96003L);
            assertThat(lastMessages.get(9601L).getContent()).isEqualTo("我的回复");
            assertThat(lastMessages.get(9602L).getId()).isEqualTo(96004L);

            Map<Long, Long> unread = messageMapper.countUnreadGrouped(List.of(9601L, 9602L), 9601L)
                    .stream()
                    .collect(Collectors.toMap(ConsultChatMessageMapper.UnreadCountRow::conversationId,
                            ConsultChatMessageMapper.UnreadCountRow::cnt));
            assertThat(unread).containsExactly(Map.entry(9601L, 2L));

            Map<Long, String> names = conversationMapper.selectUserNames(
                            List.of(9601L, 9602L, 9603L, 9604L, 999999L)).stream()
                    .collect(Collectors.toMap(ConsultChatConversationMapper.UserNameRow::userId,
                            ConsultChatConversationMapper.UserNameRow::userName));
            assertThat(names).hasSize(4)
                    .containsEntry(9601L, "学生甲")
                    .containsEntry(9602L, "教师乙")
                    .doesNotContainKey(999999L);
        }
    }

    /** 执行取消并提交，随后用新连接核对订单状态（多表 UPDATE 行数不可用于取消数断言） */
    private static void cancelAndExpectStatus(Long orderId, long userId, int expectedStatus) {
        try (SqlSession session = factory.openSession()) {
            int rows = session.getMapper(ItemMapper.class).cancelByIdsAndRelease(
                    userId, List.of(orderId), PENDING, APPROVED, CANCELLED);
            assertThat(rows).isGreaterThanOrEqualTo(1);
            session.commit();
        }
        assertThat(statusOf(orderId)).isEqualTo(expectedStatus);
    }

    private static Integer statusOf(Long orderId) {
        return jdbc.queryForObject(
                "SELECT manage_status FROM item WHERE order_id = ?", Integer.class, orderId);
    }

    // ==================== 数据准备 / 插入辅助 ====================

    private static void seedUser(long id) {
        jdbc.update("INSERT INTO `user`(id, name, email, password) VALUES (?, ?, ?, 'x')",
                id, "it-user-" + id, "it-" + id + "@example.com");
    }

    private static void seedService(int id, int capacity) {
        jdbc.update("INSERT INTO services(service_id, service_name, service_state, campus, "
                        + "image_url, capacity, booked_count) VALUES (?, ?, 1, 'cq', '', ?, 0)",
                id, "it-service-" + id, capacity);
    }

    private static void seedConsultant(long id, int serviceId) {
        jdbc.update("INSERT INTO consultant(id, name, department, title, service_id) "
                        + "VALUES (?, 'it-consultant', 'it-dept', 'it-title', ?)",
                id, serviceId);
    }

    /** 通用/活动单插入并返回回填 orderId（失败断言，测试前置条件） */
    private static Long insertGeneral(ItemMapper mapper, long userId, int serviceId) {
        ItemDO item = ItemDO.builder().userId(userId).serviceId(serviceId).build();
        int rows = mapper.insertSingleService(item, PENDING, APPROVED);
        assertThat(rows).isEqualTo(1);
        return item.getOrderId().longValue();
    }

    private static Long insertRoom(ItemMapper mapper, long userId, int serviceId, long roomId,
                                   LocalDate date, String start, String end) {
        ItemDO item = ItemDO.builder().userId(userId).serviceId(serviceId).roomId(roomId)
                .slotDate(date).startTime(start).endTime(end).build();
        int rows = mapper.insertRoomBooking(item, PENDING, 3600);
        return rows > 0 ? item.getOrderId().longValue() : null;
    }

    private static Long insertConsultation(ItemMapper mapper, long userId, int serviceId,
                                           long consultantId, long slotId,
                                           LocalDate date, String start, String end) {
        ItemDO item = ItemDO.builder().userId(userId).serviceId(serviceId)
                .consultantId(consultantId).slotId(slotId)
                .slotDate(date).startTime(start).endTime(end).build();
        int rows = mapper.insertConsultationBooking(item, PENDING, 3600);
        return rows > 0 ? item.getOrderId().longValue() : null;
    }

    private static Long insertEquipment(ItemMapper mapper, long userId, int serviceId,
                                        long equipmentId, LocalDate date, String start, String end) {
        ItemDO item = ItemDO.builder().userId(userId).serviceId(serviceId)
                .equipmentId(equipmentId).quantity(1)
                .slotDate(date).startTime(start).endTime(end).build();
        int rows = mapper.insertEquipmentBooking(item, PENDING, 3600);
        return rows > 0 ? item.getOrderId().longValue() : null;
    }
}
