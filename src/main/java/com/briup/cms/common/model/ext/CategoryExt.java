package com.briup.cms.common.model.ext;

import com.briup.cms.common.model.entity.Category;
import com.briup.cms.common.model.entity.Role;
import com.briup.cms.common.model.vo.CategoryVO;
import com.briup.cms.common.model.vo.UserVO;
import com.briup.cms.common.util.BeanUtil;
import com.briup.cms.common.util.ObjectUtil;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author YuYan
 * @date 2023-11-30 15:09:05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class CategoryExt extends Category {

    /* 包含的所有二级栏目信息 */
    private List<CategoryExt> children;

    public static CategoryExt toExt(CategoryVO categoryVO) {

        return ObjectUtil.isNull(categoryVO) ? null :
                CategoryExt.builder()
                .id(categoryVO.getId())
                .name(categoryVO.getName())
                .description(categoryVO.getDescription())
                .parentId(categoryVO.getParentId())
                .build();
    }

    public static List<CategoryExt> toExt(List<Category> categories) {
        return categories.stream()
                .map(CategoryExt::toExt)
                .collect(Collectors.toList());
    }


    public static CategoryExt toExt(Category category) {
        return BeanUtil.copyProperties(category, CategoryExt.class);
    }
}
