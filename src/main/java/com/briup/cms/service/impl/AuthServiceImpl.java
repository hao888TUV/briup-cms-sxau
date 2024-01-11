package com.briup.cms.service.impl;

import com.briup.cms.common.exception.CmsException;
import com.briup.cms.common.model.ext.UserExt;
import com.briup.cms.common.util.*;
import com.briup.cms.service.IAuthService;
import com.briup.cms.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author YuYan
 * @date 2023-12-01 14:23:58
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    /**
     * 用户模块Service层对象
     */
    private final IUserService userService;
    /**
     * 加密工具
     */
    private final SecurityUtil securityUtil;
    /**
     * Token工具
     */
    private final JwtUtil jwtUtil;

    @Override
    public String login(String username, String password) {
        /* 调用用户Service组件查询用户信息 */
        UserExt userExt = userService.getByUsername(username);
        /* 判断账号是否存在 */
        if (ObjectUtil.isNull(userExt)) {
            throw new CmsException(ResultCode.USER_LOGIN_ERROR);
        }
        /* 判断密码是否正确 */
        if (!securityUtil.bcryptMatches(password, userExt.getPassword())) {
            throw new CmsException(ResultCode.USER_LOGIN_ERROR);
        }
        /* 判断用户的状态是否允许登录 */
        if (ObjectUtil.notEquals(userExt.getStatus(),
                GlobalConstants.DATA_STATUS_ENABLE)) {
            throw new CmsException(ResultCode.USER_ACCOUNT_FORBIDDEN);
        }
        /* 将用户ID封装为Map，作为Token的载荷 */
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userExt.getId());
        claims.put("username", userExt.getUsername());
        /* 登录成功，生成Token令牌 */
        return jwtUtil.generate(claims);
    }

    @Override
    public UserExt getUserinfo(String token) {
        Map<String, Object> claims = jwtUtil.getClaims(token);
        Long id = (Long) claims.get("userId");
        return userService.getById(id);
    }
}
