package com.briup.cms.common.model.ext;

import com.briup.cms.common.model.entity.User;
import com.briup.cms.common.model.vo.UserVO;
import com.briup.cms.common.util.BeanUtil;
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
public class UserExt extends User {
    /**
     * 用户拥有的角色
     */
    private RoleExt roleExt;

    public static UserExt toExt(UserVO userVO) {
        return UserExt.builder().
                id(userVO.getId())
                .username(userVO.getUsername())
                .password(userVO.getPassword())
                .avatar(userVO.getAvatar())
                .phone(userVO.getPhone())
                .email(userVO.getEmail())
                .gender(userVO.getGender())
                .birthday(userVO.getBirthday())
                .build();
    }

    public static List<UserExt> toExt(List<User> users) {
        return users.stream()
                .map(UserExt::toExt)
                .collect(Collectors.toList());
    }

    public static UserExt toExt(User user) {
        return BeanUtil.copyProperties(user, UserExt.class);
    }
}
