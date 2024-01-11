package com.briup.cms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.briup.cms.common.model.entity.LogEntity;
import com.briup.cms.common.model.ext.LogEntityExt;

import javax.servlet.ServletOutputStream;
import java.io.OutputStream;

/**
 * @author YuYan
 * @date 2023-12-08 14:10:53
 */
public interface ILogService {

    void save(LogEntity logEntity);

    IPage<LogEntityExt> pageQueryByClause(IPage<LogEntity> page,
                                       LogEntityExt logEntityExt);

    void download(OutputStream os,
                  LogEntityExt logEntityParam);
}
