package com.briup.cms.common.model.ext;

import com.briup.cms.common.model.entity.Role;
import com.briup.cms.common.model.entity.User;
import com.briup.cms.common.model.vo.UserVO;
import com.briup.cms.common.util.BeanUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author YuYan
 * @date 2023-11-30 15:09:05
 */
@Data
@EqualsAndHashCode(callSuper = true)
// @NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class RoleExt extends Role {


    public static List<RoleExt> toExt(List<Role> roles) {
        return roles.stream()
                .map(RoleExt::toExt)
                .collect(Collectors.toList());
    }

    public static RoleExt toExt(Role role) {
        return BeanUtil.copyProperties(role, RoleExt.class);
    }
}
