package com.briup.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.briup.cms.common.exception.CmsException;
import com.briup.cms.common.model.entity.Article;
import com.briup.cms.common.model.entity.Comment;
import com.briup.cms.common.model.ext.ArticleExt;
import com.briup.cms.common.model.ext.CommentExt;
import com.briup.cms.common.util.*;
import com.briup.cms.dao.ArticleMapper;
import com.briup.cms.dao.CategoryMapper;
import com.briup.cms.dao.CommentMapper;
import com.briup.cms.service.IArticleService;
import com.briup.cms.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author YuYan
 * @date 2023-12-05 09:50:37
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements IArticleService, BaseServiceInter {

    /**
     * 文章模块Dao层对象
     */
    private final ArticleMapper articleMapper;
    /**
     * 一级评论模块Dao层对象
     */
    private final CommentMapper commentMapper;
    /**
     * 栏目模块Dao层对象
     */
    private final CategoryMapper categoryMapper;
    /**
     * 用户模块Service层对象
     */
    private final IUserService userService;


    @Override
    public void saveOrUpdate(ArticleExt articleExt) {
        /* 如果id为空，则对应新增操作 */
        if (ObjectUtil.isNull(articleExt.getId())) {
            save(articleExt);
        }
        /* 如果id不为空，则对应修改操作 */
        else {
            update(articleExt);
        }
    }

    private void save(ArticleExt articleExt) {

        /* 从ThreadLocal中取出当前用户ID */
        Long userId = RequestInfoHolder.getLong("userId");
        /* 判断用户是否存在 */
        checkUserIdExist(userId);

        /* 判断栏目是否存在 */
        Integer categoryId = articleExt.getCategoryId();
        checkCategoryIdExist(categoryId);

        /* 将参数封装为Entity对象 */
        Article article = Article.builder()
                .title(articleExt.getTitle())
                .content(articleExt.getContent())
                .userId(userId)
                .categoryId(categoryId)
                /* 生成文章的默认状态 */
                .status(GlobalConstants.ARTICLE_DEFAULT_STATUS)
                /* 初始阅读量、点赞量、点踩量设为0 */
                .readNum(0)
                .likeNum(0)
                .dislikeNum(0)
                /* 设置文章的收费状态 */
                .charged(articleExt.getCharged())
                /* 设置文章的发布时间 */
                .publishTime(new Date())
                /* 设置逻辑删除标志位 */
                .deleted(GlobalConstants.LOGIC_NOT_DELETED_FLAG_VALUE)
                .build();

        /* 调用Dao层执行插入 */
        checkResult(articleMapper.insert(article));
    }

    private void update(ArticleExt articleExt) {
        Long id = articleExt.getId();
        /* 检查ID是否存在并获取资讯对象 */
        Article article = checkIdExist(id);

        // /* 检查用户ID是否存在 */
        // Long userId = articleExt.getUserId();
        // checkUserIdExist(userId);

        /* 检查栏目ID是否存在 */
        Integer categoryId = articleExt.getCategoryId();
        checkCategoryIdExist(categoryId);

        /* 如果文章已经发表（审核通过），用户提交的内容、标题
        与数据库中查出的不一致则抛出异常 */
        String title = articleExt.getTitle();
        String content = articleExt.getContent();
        if (ObjectUtil.equals(article.getStatus(), GlobalConstants.ARTICLE_STATUS_REVIEW_PASS)
                && (ObjectUtil.notEquals(article.getTitle(), title)
                || ObjectUtil.notEquals(article.getContent(), content))) {
            throw new CmsException(ResultCode.ARTICLE_TITLE_CONTENT_UPDATE_FORBIDDEN);
        }

        /* 将要修改的字段值封装成Entity对象 */
        article = Article.builder()
                .id(id)
                .categoryId(categoryId)
                .content(content)
                .title(title)
                .charged(articleExt.getCharged())
                /* 更新文章的发布时间 */
                .publishTime(new Date())
                .build();
        /* 调用Dao层执行写入 */
        checkResult(articleMapper.updateById(article));
    }

    @Override
    public void review(Long id, String status) {
        /* 检查ID是否存在 */
        checkIdExist(id);
        /* 调用Dao层执行修改 */
        checkResult(articleMapper.updateById(
                Article.builder()
                .id(id)
                .status(status)
                .build()));
    }

    @Override
    public void delete(List<Long> ids) {
        checkResult(articleMapper.deleteBatchIds(ids));
    }
    // 模拟在开发分支中所做的一些修改…………
    // 提交了第一次开发分支之后，再次进行了修改

    @Override
    public ArticleExt getById(Long id, int commentsNum) {
        /* 调用Dao层查询 */
        Article article = articleMapper.selectById(id);
        /* 转换Entity为Ext类型 */
        ArticleExt articleExt = ArticleExt.toExt(article);
        /* 查询该文章的三条评论，封装到Ext对象中 */
        List<Comment> comments = internalListComments(id, GlobalConstants.DEFAULT_ARTICLE_QUERY_INCLUDE_COMMENT_NUMBER);
        articleExt.setCommentExts(CommentExt.toExt(comments));
        /* 查询并封装文章的作者信息 */
        articleExt.setUserExt(userService.getById(article.getUserId()));
        return articleExt;
    }

    @Override
    public IPage<ArticleExt> pageQueryByClause(IPage<Article> page, ArticleExt articleParam) {
        LambdaQueryWrapper<Article> lqw = new LambdaQueryWrapper<>();
        /* 封装查询条件 */
        /* 文章标题 - 模拟匹配 */
        String title = articleParam.getTitle();
        lqw.like(ObjectUtil.hasText(title), Article::getTitle, title);
        /* 所属栏目 - 等值判断 */
        Integer categoryId = articleParam.getCategoryId();
        lqw.eq(ObjectUtil.nonNull(categoryId), Article::getCategoryId, categoryId);
        /* 文章作者 - 等值判断 */
        Long userId = articleParam.getUserId();
        lqw.eq(ObjectUtil.nonNull(userId), Article::getUserId, userId);
        /* 审核状态 - 等值判断 */
        String status = articleParam.getStatus();
        lqw.eq(ObjectUtil.hasText(status), Article::getStatus, status);
        /* 收费类型 - 等值判断 */
        Integer charged = articleParam.getCharged();
        lqw.eq(ObjectUtil.nonNull(charged), Article::getCharged, charged);
        /* 查询范围开始时间 - 早于文章发布时间 */
        Date beginTime = articleParam.getStartTime();
        lqw.gt(ObjectUtil.nonNull(beginTime), Article::getPublishTime, beginTime);
        /* 查询范围结束时间 - 晚于文章发布时间 */
        Date endTime = articleParam.getEndTime();
        lqw.lt(ObjectUtil.nonNull(endTime), Article::getPublishTime, endTime);

        /* 调用Dao层执行分页查询 */
        articleMapper.selectPage(page, lqw);

        /* 将Page中的对象类型转为Ext类型 */
        IPage<ArticleExt> newPage = PageUtil.convert(page, ArticleExt::toExt);
        /* 为每个ArticleExt对象查询并封装用户信息 */
        newPage.getRecords().forEach(articleExt -> {
            articleExt.setUserExt(userService.getById(articleExt.getUserId()));
        });
        return newPage;
    }

    @Override
    public List<ArticleExt> list() {
        return ArticleExt.toExt(articleMapper.selectList(null));
    }

    private List<Comment> internalListComments(Long articleId, int limit) {
        LambdaQueryWrapper<Comment> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Comment::getArticleId, articleId);
        lqw.last("limit " + limit);
        return commentMapper.selectList(lqw);
    }

    private void checkUserIdExist(Long userId) {
        if (ObjectUtil.isNull(userService.getById(userId))) {
            throw new CmsException(ResultCode.USER_NOT_EXIST);
        }
    }

    private void checkCategoryIdExist(Integer categoryId) {
        if (ObjectUtil.isNull(categoryMapper.selectById(categoryId))) {
            throw new CmsException(ResultCode.CATEGORY_NOT_EXIST);
        }
    }

    private Article checkIdExist(Long id) {
        Article article = articleMapper.selectById(id);
        if (ObjectUtil.isNull(article)) {
            throw new CmsException(ResultCode.ARTICLE_NOT_EXIST);
        }
        return article;
    }

}
