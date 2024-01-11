package com.briup.cms.common.model.ext;

import com.briup.cms.common.model.entity.Slideshow;
import com.briup.cms.common.model.vo.SlideshowVO;
import com.briup.cms.common.util.BeanUtil;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author YuYan
 * @date 2023-11-29 11:47:27
 */
@Data
@EqualsAndHashCode(callSuper = true)
// @NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // 带有继承关系的建造者模式可以使用SuperBuilder注解
              // 就可以为继承父类的属性赋值（子父类都需要加该注解）
// @ToString(callSuper = true)
public class SlideshowExt extends Slideshow {

    public static SlideshowExt toExt(SlideshowVO slideshowVO) {
        return SlideshowExt.builder()
                .id(slideshowVO.getId())
                .description(slideshowVO.getDescription())
                .url(slideshowVO.getUrl())
                .status(slideshowVO.getStatus())
                .build();
    }

    public static List<SlideshowExt> toExt(List<Slideshow> slideshows) {
        if (slideshows == null) {
            return null;
        }
        return slideshows.stream()
                .map(SlideshowExt::toExt)
                .collect(Collectors.toList());
    }

    public static SlideshowExt toExt(Slideshow slideshow) {
        return BeanUtil.copyProperties(slideshow, SlideshowExt.class);
    }

}
