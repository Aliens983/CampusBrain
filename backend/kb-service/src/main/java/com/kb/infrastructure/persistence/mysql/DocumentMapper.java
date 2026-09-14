package com.kb.infrastructure.persistence.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.infrastructure.persistence.mysql.dataobject.DocumentDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * MyBatis-Plus mapper for document table.
 *
 * @author forever-king
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentDO> {

    @Select("SELECT * FROM document WHERE status = #{status} ORDER BY created_at DESC")
    List<DocumentDO> selectByStatus(@Param("status") String status);

    @Update("UPDATE document SET status = #{status}, error_msg = #{errorMsg} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("errorMsg") String errorMsg);

    @Update("UPDATE document SET status = 'READY', chunk_count = #{chunkCount} WHERE id = #{id}")
    int markReady(@Param("id") Long id, @Param("chunkCount") int chunkCount);

    @Select("SELECT COUNT(*) FROM document WHERE status = #{status}")
    long countByStatus(@Param("status") String status);

    /**
     * 按归属用户查询。
     * <p>
     * 用 {@code owner_id = #{ownerId}} 而非 {@code <=>}：历史数据 owner_id 可能为 NULL，
     * 这类无主文档对任何用户都不可见（fail-closed），避免被枚举。
     */
    @Select("SELECT * FROM document WHERE owner_id = #{ownerId} ORDER BY created_at DESC")
    List<DocumentDO> selectByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * 归属 + 标题模糊搜索，下推 SQL。
     * <p>
     * 此前是全表加载后在 JVM 里 {@code toLowerCase().contains()}，文档量上来后每次搜索都是全表扫描
     * 加全量 DTO 构造；下推后只返回命中的行。
     */
    @Select("SELECT * FROM document WHERE owner_id = #{ownerId} " +
            "AND LOWER(title) LIKE CONCAT('%', LOWER(#{keyword}), '%') ORDER BY created_at DESC")
    List<DocumentDO> selectByOwnerIdAndTitle(@Param("ownerId") Long ownerId,
                                             @Param("keyword") String keyword);
}
