package com.kb.infrastructure.persistence.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.infrastructure.persistence.mysql.dataobject.DocumentChunkDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus mapper for document_chunk table.
 *
 * @author forever-king
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunkDO> {

    @Select("SELECT * FROM document_chunk WHERE document_id = #{documentId} ORDER BY chunk_index ASC")
    List<DocumentChunkDO> selectByDocumentId(@Param("documentId") Long documentId);

    @Delete("DELETE FROM document_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") Long documentId);

    @Select("SELECT COUNT(*) FROM document_chunk WHERE document_id = #{documentId}")
    int countByDocumentId(@Param("documentId") Long documentId);
}
