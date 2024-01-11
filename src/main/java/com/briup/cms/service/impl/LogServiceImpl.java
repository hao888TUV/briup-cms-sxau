package com.briup.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.briup.cms.common.model.entity.LogEntity;
import com.briup.cms.common.model.excel.ExcelLogEntity;
import com.briup.cms.common.model.ext.LogEntityExt;
import com.briup.cms.common.util.*;
import com.briup.cms.dao.LogMapper;
import com.briup.cms.service.ILogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author YuYan
 * @date 2023-12-08 14:11:59
 */
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements ILogService {

    private final LogMapper logMapper;

    private final JsonUtil jsonUtil;

    private final ExcelUtil excelUtil;

    @Override
    @Async
    public void save(LogEntity logEntity) {
        System.out.println("Service：" + Thread.currentThread().getName());
        // 假设插入日志的时候，出现了卡顿
        // try {
        //     Thread.sleep(10000);
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }
        logMapper.insert(logEntity);
    }

    @Override
    public IPage<LogEntityExt> pageQueryByClause(IPage<LogEntity> page,
                                              LogEntityExt logEntityExt) {

        LambdaQueryWrapper<LogEntity> lqw = new LambdaQueryWrapper<>();
        String username = logEntityExt.getUsername();
        lqw.like(ObjectUtil.hasText(username), LogEntity::getUsername, username);
        String requestUrl = logEntityExt.getRequestUrl();
        lqw.like(ObjectUtil.hasText(requestUrl), LogEntity::getRequestUrl, requestUrl);
        Date startTime = logEntityExt.getStartTime();
        lqw.gt(ObjectUtil.nonNull(startTime), LogEntity::getCreateTime, startTime);
        Date endTime = logEntityExt.getEndTime();
        lqw.lt(ObjectUtil.nonNull(endTime), LogEntity::getCreateTime, endTime);
        IPage<LogEntity> logEntityIPage = logMapper.selectPage(page, lqw);
        return PageUtil.convert(logEntityIPage, LogEntityExt::toExt);
    }

    @Override
    public void download(OutputStream os,
                         LogEntityExt logEntityParam) {

        /* 创建一个操作对象 */
        LambdaQueryWrapper<LogEntity> lqw = new LambdaQueryWrapper<>();
        String username = logEntityParam.getUsername();
        lqw.like(ObjectUtil.hasText(username), LogEntity::getUsername, username);
        String requestUrl = logEntityParam.getRequestUrl();
        lqw.like(ObjectUtil.hasText(requestUrl), LogEntity::getRequestUrl, requestUrl);
        Date startTime = logEntityParam.getStartTime();
        lqw.gt(ObjectUtil.nonNull(startTime), LogEntity::getCreateTime, startTime);
        Date endTime = logEntityParam.getEndTime();
        lqw.lt(ObjectUtil.nonNull(endTime), LogEntity::getCreateTime, endTime);
        int limit = logEntityParam.getLimit();
        if (limit != -1) {
            lqw.last("limit " + limit);
        }

        /* 执行查询 */
        List<LogEntity> logEntities = logMapper.selectList(lqw);

        List<ExcelLogEntity> excelLogEntities = logEntities.stream()
                .map(logEntity -> {
                    /* 一个基于JSON格式的字符串值 */
                    String responseResult = logEntity.getResponseResult();
                    Map<String, Object> map = jsonUtil.parseForMap(responseResult);
                    return ExcelLogEntity.builder()
                            .username(logEntity.getUsername())
                            .businessName(logEntity.getBusinessName())
                            .requestUrl(logEntity.getRequestUrl())
                            .requestMethod(logEntity.getRequestMethod())
                            .requestIp(logEntity.getRequestIp())
                            .spendTime(logEntity.getSpendTime())
                            .createTime(logEntity.getCreateTime())
                            .code(Optional.ofNullable(map.get("code"))
                                        .map(String::valueOf)
                                        .orElse("无数据"))
                            .message(Optional.ofNullable(map.get("message"))
                                    .map(String::valueOf)
                                    .orElse("无数据"))
                            .build();
                })
                .collect(Collectors.toList());
        excelUtil.write(os, ExcelLogEntity.class, excelLogEntities);
    }
}
