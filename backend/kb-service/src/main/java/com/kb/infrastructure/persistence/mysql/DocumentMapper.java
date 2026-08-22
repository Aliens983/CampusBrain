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
}
