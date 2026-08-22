package com.laoliu.cas.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.system.infrastructure.persistence.dataobject.BookingRecordDO;
import com.laoliu.cas.system.infrastructure.persistence.dataobject.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author forever-king
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    @Select("SELECT * FROM user WHERE id = #{id}")
    UserDO selectByPrimaryKey(@Param("id") Long id);

    @Select("SELECT * FROM user WHERE email = #{email} LIMIT 1")
    Long getUserIdByEmail(@Param("email") String email);

    @Select("SELECT password FROM user WHERE email = #{email}")
    String getEncodePasswordByEmail(@Param("email") String email);

    @Update("UPDATE user SET password = #{password} WHERE email = #{email}")
    void updatePasswordByEmail(@Param("email") String email, @Param("password") String password);

    @Select("SELECT password FROM user WHERE id = #{id}")
    String getEncodePasswordById(@Param("id") Long id);

    @Update("UPDATE user SET password = #{password} WHERE id = #{id}")
    void updatePasswordById(@Param("id") Long id, @Param("password") String password);

    @Select("SELECT role FROM user WHERE id = #{userId}")
    String getRoleByUserId(@Param("userId") Long userId);

    @Update("UPDATE user SET role = 0 WHERE id = #{userId}")
    void updateRoleToCommonUser(@Param("userId") Long userId);

    @Update("UPDATE user SET role = 1 WHERE id = #{userId}")
    void updateRoleToAdmin(@Param("userId") Long userId);

    @Select("SELECT * FROM user")
    List<UserDO> getAllUsers();

    /**
     * 分页查询所有用户。MyBatis-Plus 自动拦截 Page 参数添加 LIMIT/OFFSET。
     */
    @Select("SELECT * FROM user ORDER BY id DESC")
    IPage<UserDO> getAllUsersWithPage(Page<UserDO> page);

    @Override
    int insert(UserDO userDO);

    @Select("SELECT i.order_id AS orderId, s.service_name AS serviceName, s.service_describe AS serviceDescribe, " +
            "u.name AS username, i.create_time AS createTime, i.update_time AS updateTime, " +
            "i.manage_status AS manageStatus, " +
            "CASE i.manage_status WHEN 0 THEN '待审核' WHEN 1 THEN '已通过' WHEN 2 THEN '已拒绝' WHEN 3 THEN '已取消' ELSE '未知' END AS statusDescription " +
            "FROM item i " +
            "JOIN services s ON i.service_id = s.service_id " +
            "JOIN user u ON i.user_id = u.id " +
            "WHERE i.user_id = #{userId}")
    List<BookingRecordDO> getAllBookings(@Param("userId") Long userId);

    /**
     * 分页查询用户的预约记录。
     */
    @Select("SELECT i.order_id AS orderId, s.service_name AS serviceName, s.service_describe AS serviceDescribe, " +
            "u.name AS username, i.create_time AS createTime, i.update_time AS updateTime, " +
            "i.manage_status AS manageStatus, " +
            "CASE i.manage_status WHEN 0 THEN '待审核' WHEN 1 THEN '已通过' WHEN 2 THEN '已拒绝' WHEN 3 THEN '已取消' ELSE '未知' END AS statusDescription " +
            "FROM item i " +
            "JOIN services s ON i.service_id = s.service_id " +
            "JOIN user u ON i.user_id = u.id " +
            "WHERE i.user_id = #{userId} " +
            "ORDER BY i.create_time DESC")
    IPage<BookingRecordDO> getAllBookingsWithPage(@Param("userId") Long userId, Page<?> page);
}
