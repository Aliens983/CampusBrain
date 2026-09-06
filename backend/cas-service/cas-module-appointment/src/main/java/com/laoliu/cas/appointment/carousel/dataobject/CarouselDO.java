package com.laoliu.cas.appointment.carousel.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 首页轮播图数据对象
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("carousel")
public class CarouselDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 图片URL */
    private String imageUrl;

    /** 排序（小在前） */
    private Integer sort;

    /** 是否启用：1是 0否 */
    private Integer enabled;
}
