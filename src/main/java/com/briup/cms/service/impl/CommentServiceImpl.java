package com.briup.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.briup.cms.common.exception.CmsException;
import com.briup.cms.common.model.entity.Article;
import com.briup.cms.common.model.entity.Comment;
import com.briup.cms.common.model.entity.SubComment;
import com.briup.cms.common.model.ext.CommentExt;
import com.briup.cms.common.model.ext.SubCommentExt;
import com.briup.cms.common.model.ext.UserExt;
import com.briup.cms.common.model.param.CommentDeleteParam;
import com.briup.cms.common.util.*;
import com.briup.cms.dao.ArticleMapper;
import com.briup.cms.dao.CommentMapper;
import com.briup.cms.dao.SubCommentMapper;
import com.briup.cms.service.ICommentService;
import com.briup.cms.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author YuYan
 * @date 2023-12-06 14:58:35
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService, BaseServiceInter {

    /**
     * 一级评论模块Dao层对象
     */
    private final CommentMapper commentMapper;
    /**
     * 二级评论模块Dao层对象
     */
    private final SubCommentMapper subCommentMapper;
    /**
     * 资讯模块Dao层对象
     */
    private final ArticleMapper articleMapper;

    private final IUserService userService;

    @Override
    public void save(CommentExt commentExt) {
        /* 检查文章ID是否存在 */
        Long articleId = commentExt.getArticleId();
        checkArticleIdExist(articleId);
        /* 检查用户ID是否存在 */
        Long userId = commentExt.getUserId();
        UserExt userExt = checkUserIdExist(userId);
        /* 检查用户的状态是否为可用 */
        if (ObjectUtil.notEquals(userExt.getStatus(),
                GlobalConstants.DATA_STATUS_ENABLE)) {
            throw new CmsException(ResultCode.USER_ACCOUNT_FORBIDDEN);
        }
        /* 将数据封装为Entity对象 */
        Comment comment = Comment.builder()
                .content(commentExt.getContent())
                .userId(userId)
                .articleId(articleId)
                /* 自动生成发表时间 */
                .publishTime(new Date())
                .deleted(GlobalConstants.LOGIC_NOT_DELETED_FLAG_VALUE)
                .build();

        /* 调用Dao层执行插入 */
        commentMapper.insert(comment);
    }

    @Override
    public void save(SubCommentExt subCommentExt) {

        /* 检查用户ID是否存在 */
        Long userId = subCommentExt.getUserId();
        UserExt userExt = checkUserIdExist(userId);
        /* 检查用户的状态是否为可用 */
        if (ObjectUtil.notEquals(userExt.getStatus(),
                GlobalConstants.DATA_STATUS_ENABLE)) {
            throw new CmsException(ResultCode.USER_ACCOUNT_FORBIDDEN);
        }

        /* 检查父评论（一级评论）是否存在 */
        Long parentId = subCommentExt.getParentId();
        checkCommentIdExist(parentId);

        /* 如果参数提交了回复的二级评论ID，则检查该二级评论是否存在 */
        Long replyId = subCommentExt.getReplyId();
        if (ObjectUtil.nonNull(replyId)) {
            checkSubCommentIdExist(replyId);
        }

        /* 将数据封装为Entity对象 */
        SubComment subComment = SubComment.builder()
                .content(subCommentExt.getContent())
                .userId(userId)
                .parentId(parentId)
                .replyId(replyId)
                /* 自动生成发表时间 */
                .publishTime(new Date())
                .deleted(GlobalConstants.LOGIC_NOT_DELETED_FLAG_VALUE)
                .build();

        /* 调用Dao层执行插入 */
        subCommentMapper.insert(subComment);
    }

    @Override
    @Transactional
    public void delete(Long id, String type) {
       checkResult(internalDelete(id, type));
    }

    @Override
    public void deleteBatch(List<CommentDeleteParam> params) {
        int deleted = 0;
        for (CommentDeleteParam param : params) {
            deleted += internalDelete(param.getId(), param.getType());
        }
        checkResult(deleted);
    }

    private int internalDelete(Long id, String type) {
        if (ObjectUtil.equalsIgnoreCase(type, GlobalConstants.SUB_COMMENT_QUERY_TYPE)) {
            return deleteChild(id);
        } else if (ObjectUtil.equalsIgnoreCase(type, GlobalConstants.COMMENT_QUERY_TYPE)) {
            return deleteParent(id);
        } else {
            throw new CmsException(ResultCode.PARAM_IS_INVALID);
        }
    }

    /**
     * 删除二级评论
     * 只删这一条二级评论，回复该评论的评论不受影响
     * @param id
     */
    private int deleteChild(Long id) {
        return subCommentMapper.deleteById(id);
    }

    private int deleteParent(Long id) {
        /* 把二级评论表中所有parentId为该一级评论id的数据都删掉 */
        LambdaQueryWrapper<SubComment> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SubComment::getParentId, id);
        subCommentMapper.delete(lqw);
        /* 删除这条一级评论 */
        return commentMapper.deleteById(id);

        /* 测试事务管理，当一级评论删除不成功时观察数据库数据是否发生了变化 */
        // throw new RuntimeException("删除一级评论时出现了异常...");
    }

    @Override
    public List<SubCommentExt> list(SubCommentExt subCommentParam) {
        /* 根据一级评论ID查询一级评论信息 */
        Long parentId = subCommentParam.getParentId();
        Comment comment = checkCommentIdExist(parentId);
        /* 检查资讯ID是否存在 */
        Article article = checkArticleIdExist(comment.getArticleId());
        /* 判断文章的状态是否正确 */
        checkArticleStatusCorrect(article);
        /* 创建一个查询模型对象 */
        LambdaQueryWrapper<SubComment> lqw = new LambdaQueryWrapper<>();
        /* 查询条件 */
        lqw.eq(SubComment::getParentId, parentId);
        /* 排序规则 */
        lqw.orderByAsc(SubComment::getPublishTime);
        /* 查询评论信息 */
        List<SubComment> subComments = subCommentMapper.selectList(lqw);
        /* 转换对象格式为Ext */
        List<SubCommentExt> subCommentExts = SubCommentExt.toExt(subComments);
        /* 查询并封装发布人信息 */
        subCommentExts.forEach(subCommentExt -> {
            subCommentExt.setUserExt(userService.getById(
                    subCommentExt.getUserId()));
        });
        return subCommentExts;
    }

    @Override
    public IPage<CommentExt> pageQueryByClause(
            IPage<Comment> page, CommentExt commentParam) {

        LambdaQueryWrapper<Comment> lqw = new LambdaQueryWrapper<>();

        Long articleId = commentParam.getArticleId();
        /* 如果文章ID不为空，则判断是否存在 */
        if (ObjectUtil.nonNull(articleId)) {
            Article article = checkArticleIdExist(articleId);
            checkArticleStatusCorrect(article);
            lqw.eq(Comment::getArticleId, articleId);
        }

        /* 封装查询条件 */
        String content = commentParam.getContent();
        lqw.like(ObjectUtil.hasText(content), Comment::getContent, content);
        Long userId = commentParam.getUserId();
        lqw.eq(ObjectUtil.nonNull(userId), Comment::getUserId, userId);
        Date startTime = commentParam.getStartTime();
        lqw.gt(ObjectUtil.nonNull(startTime), Comment::getPublishTime, startTime);
        Date endTime = commentParam.getEndTime();
        lqw.lt(ObjectUtil.nonNull(endTime), Comment::getPublishTime, endTime);

        /* 调用Dao层执行查询 */
        commentMapper.selectPage(page, lqw);
        /* 转换分页对象中的对象类型 */
        IPage<CommentExt> newPage = PageUtil.convert(page, CommentExt::toExt);

        newPage.getRecords().forEach(record -> {
            /* 查询并封装发布者信息 */
            record.setUserExt(userService.getById(record.getUserId()));
            /* 查询并封装包含的二级评论信息 */
            List<SubCommentExt> subCommentExts = SubCommentExt.toExt(internalListSubCommentByParentId(record.getId()));
            record.setSubCommentExts(subCommentExts);
            /* 查询并封装二级评论中的发布者信息 */
            subCommentExts.forEach(subRecord -> {
                subRecord.setUserExt(userService.getById(subRecord.getUserId()));
            });
        });
        return newPage;
    }

    private List<SubComment> internalListSubCommentByParentId(Long parentId) {
        LambdaQueryWrapper<SubComment> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SubComment::getParentId, parentId);
        return subCommentMapper.selectList(lqw);
    }

    private Article internalGetArticleById(Long articleId) {
        return articleMapper.selectById(articleId);
    }

    private void checkArticleStatusCorrect(Article article) {
        if (ObjectUtil.notEquals(article.getStatus(),
                GlobalConstants.ARTICLE_STATUS_REVIEW_PASS)) {
            throw new CmsException(ResultCode.ARTICLE_IS_NOT_VISIBLE);
        }
    }
    private SubComment checkSubCommentIdExist(Long subCommentId) {
        SubComment subComment = subCommentMapper.selectById(subCommentId);
        if (ObjectUtil.isNull(subComment)) {
            throw new CmsException(ResultCode.SUB_COMMENT_NOT_EXIST);
        }
        return subComment;
    }
    private Comment checkCommentIdExist(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (ObjectUtil.isNull(comment)) {
            throw new CmsException(ResultCode.COMMENT_NOT_EXIST);
        }
        return comment;
    }
    private Article checkArticleIdExist(Long articleId) {
        Article article = internalGetArticleById(articleId);
        if (ObjectUtil.isNull(article)) {
            throw new CmsException(ResultCode.ARTICLE_NOT_EXIST);
        }
        return article;
    }
    private UserExt checkUserIdExist(Long userId) {
        UserExt userExt = userService.getById(userId);
        if (ObjectUtil.isNull(userExt)) {
            throw new CmsException(ResultCode.USER_NOT_EXIST);
        }
        return userExt;
    }

}
