package com.kb.infrastructure.persistence.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_tenant")
public class TenantDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private Long ownerId;
    private String status;
    private Integer maxMembers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
