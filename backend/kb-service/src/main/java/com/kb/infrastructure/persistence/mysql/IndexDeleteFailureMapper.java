package com.kb.infrastructure.persistence.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.infrastructure.persistence.mysql.dataobject.IndexDeleteFailureDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 外部索引删除失败对账 Mapper（A-04）。
 *
 * @author forever-king
 */
@Mapper
public interface IndexDeleteFailureMapper extends BaseMapper<IndexDeleteFailureDO> {

    /**
     * 记录失败：PENDING 已存在则刷新原因并累加重试次数；RESOLVED/GIVE_UP 行不复活。
     * 唯一键 (document_id, target) 保证并发下只产生一行。
     */
    @Insert("""
            INSERT INTO index_delete_failure (document_id, target, fail_reason, retry_count, status)
            VALUES (#{documentId}, #{target}, #{failReason}, 0, 'PENDING')
            ON DUPLICATE KEY UPDATE
                fail_reason = IF(status = 'PENDING', VALUES(fail_reason), fail_reason),
                retry_count = IF(status = 'PENDING', retry_count + 1, retry_count),
                updated_at = NOW()
            """)
    int insertOrIncrement(@Param("documentId") Long documentId,
                          @Param("target") String target,
                          @Param("failReason") String failReason);

    @Select("""
            SELECT * FROM index_delete_failure
            WHERE status = 'PENDING' AND retry_count < #{maxRetryCount}
            ORDER BY updated_at ASC
            LIMIT #{limit}
            """)
    List<IndexDeleteFailureDO> selectPending(@Param("maxRetryCount") int maxRetryCount,
                                             @Param("limit") int limit);

    @Update("UPDATE index_delete_failure SET status = 'RESOLVED', updated_at = NOW() WHERE id = #{id}")
    int updateResolved(@Param("id") Long id);

    @Update("UPDATE index_delete_failure SET status = 'GIVE_UP', updated_at = NOW() WHERE id = #{id}")
    int updateGiveUp(@Param("id") Long id);
}
