package com.briup.cms.common.model.vo;

import com.briup.cms.common.model.ext.ArticleExt;
import com.briup.cms.common.model.ext.LogEntityExt;
import com.briup.cms.common.util.ObjectUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 视图对象 - 日志
 *
 * @author YuYan
 * @date 2023-11-30 09:22:17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntityVO {


    @JsonProperty(value = "username")
    private String username;
    @JsonProperty(value = "businessName")
    private String businessName;
    @JsonProperty(value = "requestUrl")
    private String requestUrl;
    @JsonProperty(value = "ip")
    private String requestIp;
    @JsonProperty(value = "spendTime")
    private Long spendTime;
    @JsonProperty(value = "requestMethod")
    private String requestMethod;
    @JsonProperty(value = "createTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
    @JsonProperty(value = "startTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date startTime;
    @JsonProperty(value = "endTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date endTime;


    public static List<LogEntityVO> toVO(List<LogEntityExt> logEntityExts) {
        return ObjectUtil.isNull(logEntityExts) ? null :
                logEntityExts.stream()
                .map(LogEntityVO::toVO)
                .collect(Collectors.toList());
    }

    public static LogEntityVO toVO(LogEntityExt logEntityExt) {
        return ObjectUtil.isNull(logEntityExt) ? null :
                LogEntityVO.builder()
                        .username(logEntityExt.getUsername())
                        .requestUrl(logEntityExt.getRequestUrl())
                        .businessName(logEntityExt.getBusinessName())
                        .requestIp(logEntityExt.getRequestIp())
                        .spendTime(logEntityExt.getSpendTime())
                        .createTime(logEntityExt.getCreateTime())
                        .requestMethod(logEntityExt.getRequestMethod())
                        .startTime(logEntityExt.getStartTime())
                        .endTime(logEntityExt.getEndTime())
                        .build();
    }

}
