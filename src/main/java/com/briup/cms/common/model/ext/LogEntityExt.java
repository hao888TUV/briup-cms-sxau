package com.briup.cms.common.model.ext;

import com.briup.cms.common.model.entity.LogEntity;
import com.briup.cms.common.model.vo.LogEntityVO;
import com.briup.cms.common.util.BeanUtil;
import com.briup.cms.common.util.ObjectUtil;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;
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
public class LogEntityExt extends LogEntity {

    /**
     * 查询的开始时间和结束时间
     */
    private Date startTime;
    private Date endTime;
    private String username;
    private String requestUrl;
    private int limit;

    public static LogEntityExt toExt(LogEntityVO logEntityVO) {
        return ObjectUtil.isNull(logEntityVO) ? null :
                LogEntityExt.builder()
                        .requestUrl(logEntityVO.getRequestUrl())
                        .username(logEntityVO.getUsername())
                        .startTime(logEntityVO.getStartTime())
                        .endTime(logEntityVO.getEndTime())
                        .build();
    }

    public static List<LogEntityExt> toExt(List<LogEntity> logEntities) {
        return logEntities.stream()
                .map(LogEntityExt::toExt)
                .collect(Collectors.toList());
    }

    public static LogEntityExt toExt(LogEntity logEntity) {
        return BeanUtil.copyProperties(logEntity, LogEntityExt.class);
    }
}
