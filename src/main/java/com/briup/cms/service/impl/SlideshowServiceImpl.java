package com.briup.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.briup.cms.common.cache.Cached;
import com.briup.cms.common.exception.CmsException;
import com.briup.cms.common.model.entity.Slideshow;
import com.briup.cms.common.model.ext.SlideshowExt;
import com.briup.cms.common.util.*;
import com.briup.cms.dao.SlideshowMapper;
import com.briup.cms.service.ISlideshowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author YuYan
 * @date 2023-11-29 14:06:46
 */
@Service
@RequiredArgsConstructor
public class SlideshowServiceImpl implements ISlideshowService, BaseServiceInter {

    private final SlideshowMapper slideshowMapper;

    private final RedisUtil redisUtil;

    @Cached
    @Override
    public List<SlideshowExt> listByStatus(String status) {
        /* 创建一个条件对象 */
        LambdaQueryWrapper<Slideshow> lqw
                = new LambdaQueryWrapper<>();
        /* 设置查询条件，根据轮播图状态字段查询数据 */
        lqw.eq(Slideshow::getStatus, status);
        /* 执行查询 */
        List<Slideshow> slideshows = slideshowMapper.selectList(lqw);
        /* 将集合中的元素从Entity转换到Ext类型 */
        return SlideshowExt.toExt(slideshows);
    }

    @Override
    @Cached
    public IPage<SlideshowExt> pageQueryByClause(SlideshowExt slideshowExt,
                                                 IPage<Slideshow> page) {
        LambdaQueryWrapper<Slideshow> lqw = new LambdaQueryWrapper<>();

        /* 根据轮播图描述信息进行模糊匹配 */
        String description = slideshowExt.getDescription();
        if (ObjectUtil.hasText(description)) {
            lqw.like(Slideshow::getDescription, description);
        }

        /* 根据轮播图状态进行查询 */
        String status = slideshowExt.getStatus();
        if (ObjectUtil.hasText(status)) {
            lqw.eq(Slideshow::getStatus, status);
        }

        /* 调用Dao层方法执行查询，查询的结果数据会直接封装到page对象中  */
        slideshowMapper.selectPage(page, lqw);

        /* 使用工具类转换分页对象中包含的对象类型 */
        return PageUtil.convert(page, SlideshowExt::toExt);
    }

    @Cached
    @Override
    public SlideshowExt getById(Integer id) {
        return SlideshowExt.toExt(
                slideshowMapper.selectById(id));
    }

    @Override
    public void saveOrUpdate(SlideshowExt slideshowExt) {
        /* 取出参数 */
        Integer id = slideshowExt.getId();
        String url = slideshowExt.getUrl();
        String status = slideshowExt.getStatus();
        String description = slideshowExt.getDescription();

        /* id不存在则为新增操作 */
        if (ObjectUtil.isNull(id)) {

            /* 检查URL是否可用 */
            checkUrlUnique(url);

            /* 创建一个Entity对象，封装数据 */
            Slideshow slideshow = new Slideshow();
            slideshow.setUrl(url);
            slideshow.setDescription(description);

            /* 设置轮播图状态 */
            slideshow.setStatus(slideshowExt.getStatus());
            /* 逻辑删除状态默认为未删除 */
            slideshow.setDeleted(GlobalConstants.LOGIC_NOT_DELETED_FLAG_VALUE);
            /* 上传时间使用当前时间 */
            slideshow.setUploadTime(new Date());

            /* 调用Dao层实现插入 */
            checkResult(slideshowMapper.insert(slideshow));
        }
        /* id存在则为修改操作 */
        else {
            /* 先判断id是否存在 */
            Slideshow record = checkIdExist(id);

            /* 判断URL是否可用 */
            checkUrlUnique(url, id);

            /* 封装数据为Entity对象 */
            Slideshow slideshow = new Slideshow();
            slideshow.setId(id);
            slideshow.setUrl(url);
            slideshow.setStatus(status);
            slideshow.setDescription(description);
            /* 如果Url发生了修改，则说明用户重新上传了图片，更新图片的上传时间 */
            if (ObjectUtil.notEquals(url, record.getUrl())) {
                slideshow.setUploadTime(new Date());
            }

            /* 调用Dao层实现修改 */
            slideshowMapper.updateById(slideshow);
        }
    }

    @Override
    public void deleteById(Integer id) {
        checkIdExist(id);
        checkResult(slideshowMapper.deleteById(id));
    }

    @Override
    public void delete(List<Integer> ids) {
        /* 执行删除 */
        checkResult(slideshowMapper.deleteBatchIds(ids));
    }

    private Slideshow checkIdExist(Integer id) {
        Slideshow slideshow = slideshowMapper.selectById(id);
        if (ObjectUtil.isNull(slideshow)) {
            throw new CmsException(ResultCode.SLIDESHOW_NOT_EXISTED);
        }
        return slideshow;
    }

    private void checkUrlUnique(String url) {
        checkUrlUnique(url, null);
    }

    private void checkUrlUnique(String url, Integer id) {
        LambdaQueryWrapper<Slideshow> lqw =
                new LambdaQueryWrapper<>();
        lqw.eq(Slideshow::getUrl, url);
        if (ObjectUtil.nonNull(id)) {
            lqw.ne(Slideshow::getId, id);
        }
        if (ObjectUtil.nonNull(slideshowMapper.selectOne(lqw))) {
            throw new CmsException(ResultCode.SLIDESHOW_URL_EXISTED);
        }
    }

}
