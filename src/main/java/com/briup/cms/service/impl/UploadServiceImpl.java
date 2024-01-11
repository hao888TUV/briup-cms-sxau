package com.briup.cms.service.impl;

import com.briup.cms.common.util.QiniuUtil;
import com.briup.cms.service.IUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author YuYan
 * @date 2023-11-30 14:36:21
 */
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements IUploadService {

    private final QiniuUtil qiniuUtil;

    @Override
    public String upload(MultipartFile multipartFile) {
        return qiniuUtil.upload(multipartFile);
    }

}
