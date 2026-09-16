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
     * 按归属用户分页查询。
     */
    @Select("SELECT * FROM document WHERE owner_id = #{ownerId} " +
            "ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    List<DocumentDO> selectPageByOwnerId(@Param("ownerId") Long ownerId,
                                         @Param("offset") long offset,
                                         @Param("size") int size);

    /**
     * 全量分页查询（管理员列表）。
     */
    @Select("SELECT * FROM document ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    List<DocumentDO> selectPageAll(@Param("offset") long offset, @Param("size") int size);

    /**
     * 归属 + 标题关键词分页搜索。
     */
    @Select("SELECT * FROM document WHERE owner_id = #{ownerId} " +
            "AND LOWER(title) LIKE CONCAT('%', LOWER(#{keyword}), '%') " +
            "ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    List<DocumentDO> selectPageByOwnerIdAndTitle(@Param("ownerId") Long ownerId,
                                                 @Param("keyword") String keyword,
                                                 @Param("offset") long offset,
                                                 @Param("size") int size);

    /**
     * 全量标题关键词分页搜索（管理员）。
     */
    @Select("SELECT * FROM document WHERE LOWER(title) LIKE CONCAT('%', LOWER(#{keyword}), '%') " +
            "ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    List<DocumentDO> selectPageByTitle(@Param("keyword") String keyword,
                                       @Param("offset") long offset,
                                       @Param("size") int size);
}
