package com.briup.cms.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.briup.cms.common.download.Download;
import com.briup.cms.common.model.entity.LogEntity;
import com.briup.cms.common.model.ext.LogEntityExt;
import com.briup.cms.common.model.vo.LogEntityVO;
import com.briup.cms.common.util.PageUtil;
import com.briup.cms.common.util.Result;
import com.briup.cms.service.ILogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * @author YuYan
 * @date 2023-12-11 09:52:06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/log")
public class LogController {

    private final ILogService logService;

    @GetMapping(params = "page=true")
    public Result pageQuery(@RequestParam(value = "pageSize", required = true) int pageSize,
                            @RequestParam(value = "pageNum", required = true) int pageNum,
                            @RequestParam(value = "username", required = false) String username,
                            @RequestParam(value = "requestUrl", required = false) String requestUrl,
                            @RequestParam(value = "startTime", required = false) Date startTime,
                            @RequestParam(value = "endTime", required = false) Date endTime) {

        IPage<LogEntity> page = new Page<>(pageNum, pageSize);

        LogEntityExt logEntityExt = LogEntityExt.builder()
                .username(username)
                .requestUrl(requestUrl)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        return Result.ok(PageUtil.convert(logService.pageQueryByClause(page, logEntityExt), LogEntityVO::toVO));
    }

    @GetMapping(params = {"action=download", "type=data"})
    @Download(fileName = "日志.xlsx")
    public void downloadData(HttpServletResponse response,
                             @RequestParam(value = "username", required = false) String username,
                             @RequestParam(value = "requestUrl", required = false) String requestUrl,
                             @RequestParam(value = "startTime", required = false) Date startTime,
                             @RequestParam(value = "endTime", required = false) Date endTime,
                             @RequestParam(value = "count", required = false, defaultValue = "-1") int limit) throws Exception {
        /* 封装请求参数 */
        logService.download(response.getOutputStream(),
                LogEntityExt.builder()
                        .username(username)
                        .requestUrl(requestUrl)
                        .startTime(startTime)
                        .endTime(endTime)
                        .limit(limit)
                        .build());
    }

}
