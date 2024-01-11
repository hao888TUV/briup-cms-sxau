package com.briup.cms.service.impl;

import com.briup.cms.common.model.ext.RoleExt;
import com.briup.cms.dao.RoleMapper;
import com.briup.cms.service.IRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author YuYan
 * @date 2023-12-01 11:48:16
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements IRoleService {

    private final RoleMapper roleMapper;

    @Override
    public RoleExt getById(Integer id) {
        return RoleExt.toExt(roleMapper.selectById(id));
    }

    @Override
    public List<RoleExt> list() {
        return roleMapper.selectList(null)
                    .stream()
                    .map(RoleExt::toExt)
                    .collect(Collectors.toList());
    }
}
