package com.briup.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.briup.cms.common.exception.CmsException;
import com.briup.cms.common.model.entity.User;
import com.briup.cms.common.model.ext.UserExt;
import com.briup.cms.common.util.*;
import com.briup.cms.dao.UserMapper;
import com.briup.cms.service.IRoleService;
import com.briup.cms.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author YuYan
 * @date 2023-11-30 15:18:31
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService, BaseServiceInter {

    /**
     * 用户模块Dao层对象
     */
    private final UserMapper userMapper;
    /**
     * 角色模块Service层对象
     */
    private final IRoleService roleService;
    /**
     * 加密工具
     */
    private final SecurityUtil securityUtil;


    @Override
    public void save(UserExt userExt) {
        /* 参数校验 */
        String username = userExt.getUsername();
        String password = userExt.getPassword();
        if (ObjectUtil.anyNotHasText(username, password)) {
            throw new CmsException(ResultCode.PARAM_IS_BLANK);
        }
        /* 检查用户名是否可用 */
        checkUsernameUnique(username);

        /* 将密码进行加密 */
        password = securityUtil.bcryptEncode(password);

        /* 如果没有提交性别字段值，则使用配置的默认性别 */
        String gender = userExt.getGender();
        if (ObjectUtil.notHasText(gender)) {
            gender = GlobalConstants.USER_DEFAULT_GENDER;
        }

        /* 创建并封装Entity */
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setAvatar(userExt.getAvatar());
        user.setGender(gender);
        user.setEmail(userExt.getEmail());
        user.setPhone(userExt.getPhone());
        user.setBirthday(userExt.getBirthday());

        /* 用户注册时间设置为当前时间 */
        user.setRegisterTime(new Date());
        /* 设置用户默认状态 */
        user.setStatus(GlobalConstants.USER_DEFAULT_STATUS);
        /* 设置用户默认角色ID */
        user.setRoleId(GlobalConstants.USER_DEFAULT_ROLE_ID);
        /* 设置用户默认的VIP状态 */
        user.setVip(GlobalConstants.USER_DEFAULT_VIP);
        /* 设置逻辑删除默认状态值 */
        user.setDeleted(GlobalConstants.LOGIC_NOT_DELETED_FLAG_VALUE);

        /* 调用Dao层实现插入 */
        checkResult(userMapper.insert(user));
    }


    @Override
    public UserExt getById(Long id) {
        return UserExt.toExt(checkIdExist(id));
    }

    @Override
    public UserExt getByIdNullable(Long id) {
        return UserExt.toExt(userMapper.selectById(id));
    }

    @Override
    public void update(UserExt userExt) {
        Long id = userExt.getId();
        /* 校验id是否为空 */
        if (ObjectUtil.isNull(id)) {
            throw new CmsException(ResultCode.PARAM_IS_BLANK);
        }
        /* 校验id在数据库中是否存在 */
        checkIdExist(id);

        /* 判断用户是否需要修改用户名 */
        String username = userExt.getUsername();
        if (ObjectUtil.hasText(username)) {
            checkUsernameUnique(username, id);
        }
        /* 判断用户是否需要修改密码 */
        String password = userExt.getPassword();
        if (ObjectUtil.hasText(password)) {
            password = securityUtil.bcryptEncode(password);
        }

        /* 把参数封装为Entity对象（也可以通过复制属性实现） */
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setAvatar(userExt.getAvatar());
        user.setGender(userExt.getGender());
        user.setEmail(userExt.getEmail());
        user.setPhone(userExt.getPhone());
        user.setBirthday(userExt.getBirthday());

        /* 调用Dao层执行修改 */
        checkResult(userMapper.updateById(user)); // 带有selective机制
    }

    @Override
    public void delete(List<Long> ids) {
        checkResult(userMapper.deleteBatchIds(ids));
    }


    @Override
    public IPage<UserExt> pageQueryByClause(UserExt userExt, IPage<User> page) {
        /* 创建一个查询模型 */
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        Integer vip = userExt.getVip();
        lqw.eq(ObjectUtil.nonNull(vip), User::getVip, vip);
        Integer roleId = userExt.getRoleId();
        lqw.eq(ObjectUtil.nonNull(roleId), User::getRoleId, roleId);
        String status = userExt.getStatus();
        lqw.eq(ObjectUtil.hasText(status), User::getStatus, status);
        String username = userExt.getUsername();
        lqw.like(ObjectUtil.hasText(username), User::getUsername, username);
        /* 调用Dao层进行查询 */
        userMapper.selectPage(page, lqw);
        /* 转换分页对象内部的数据清单中的对象类型 */
        IPage<UserExt> newPage = PageUtil.convert(page, UserExt::toExt);
        /* 为每个用户对象都查询并封装对应的角色信息 */
        newPage.getRecords().forEach(record -> record.setRoleExt(
                roleService.getById(record.getRoleId())));
        /* 返回结果 */
        return newPage;
    }

    @Override
    public UserExt getByUsername(String username) {
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getUsername, username);
        return UserExt.toExt(userMapper.selectOne(lqw));
    }

    @Override
    public List<UserExt> list() {
        return UserExt.toExt(userMapper.selectList(null));
    }


    private void checkUsernameUnique(String username) {
        checkUsernameUnique(username, null);
    }
    private void checkUsernameUnique(String username, Long id) {
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getUsername, username);
        if (ObjectUtil.nonNull(id)) {
            lqw.ne(User::getId, id);
        }
        if (ObjectUtil.nonNull(userMapper.selectOne(lqw))) {
            throw new CmsException(ResultCode.USERNAME_HAS_EXISTED);
        }
    }

    private User checkIdExist(Long id) {
        User user = userMapper.selectById(id);
        if (ObjectUtil.isNull(user)) {
            throw new CmsException(ResultCode.USER_NOT_EXIST);
        }
        return user;
    }
}
