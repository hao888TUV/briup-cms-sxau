package com.briup.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.briup.cms.common.cache.CacheEntity;
import com.briup.cms.common.cache.Cached;
import com.briup.cms.common.cache.FlushCache;
import com.briup.cms.common.exception.CmsException;
import com.briup.cms.common.model.entity.Article;
import com.briup.cms.common.model.entity.Category;
import com.briup.cms.common.model.excel.ExcelCategory;
import com.briup.cms.common.model.ext.CategoryExt;
import com.briup.cms.common.model.ext.UserExt;
import com.briup.cms.common.util.*;
import com.briup.cms.dao.ArticleMapper;
import com.briup.cms.dao.CategoryMapper;
import com.briup.cms.service.ICategoryService;
import com.briup.cms.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author YuYan
 * @date 2023-12-04 14:02:14
 */
@Service
@RequiredArgsConstructor
@CacheEntity(entityTypes = Category.class)
public class CategoryServiceImpl implements ICategoryService, BaseServiceInter {

    private final CategoryMapper categoryMapper;

    private final ArticleMapper articleMapper;

    private final IUserService userService;

    private final ExcelUtil excelUtil;

    @Override
    @FlushCache(flushEntityTypes = {Category.class, Article.class})
    public void save(CategoryExt categoryExt) {

        /* 检查名称是否可用 */
        String name = categoryExt.getName();
        checkNameUnique(name);

        /* 如果添加的是二级栏目，则检查一级栏目是否存在 */
        Integer parentId = categoryExt.getParentId();
        if (ObjectUtil.nonNull(parentId)) {
            checkParentIdExist(parentId);
        }

        /* 生成最新的orderNum */
        Integer orderNum = categoryMapper.selectMaxOrderNum();
        if (ObjectUtil.isNull(orderNum)) {
            orderNum = 0;
        }

        /* 最新的orderNum取当前最大值+1的值 */
        orderNum += 1;

        /* 将参数封装为Entity，调用Dao层执行插入 */
        Category category = new Category();
        category.setName(name);
        category.setDescription(categoryExt.getDescription());
        category.setOrderNum(orderNum);
        category.setParentId(parentId);
        category.setDeleted(GlobalConstants.LOGIC_NOT_DELETED_FLAG_VALUE);
        checkResult(categoryMapper.insert(category));
    }

    @Override
    public void delete(List<Integer> ids) {
        /* 累计实际删除掉的数据条数 */
        int deleteCount = 0;
        for (Integer id : ids) {
            /* 根据id查询出栏目信息 */
            Category category = categoryMapper.selectById(id);
            /* 如果parentId不存在，则要删除的是一级栏目 */
            if (ObjectUtil.isNull(category.getParentId())) {
                /* 查询该栏目下是否有子栏目 */
                LambdaQueryWrapper<Category> lqw = new LambdaQueryWrapper<>();
                lqw.eq(Category::getParentId, id);
                /* 如果有子栏目则不执行删除 */
                if (categoryMapper.selectCount(lqw) == 0) {
                    /* 如果没有子栏目，则执行删除 */
                    deleteCount += categoryMapper.deleteById(id);
                }
            }
            /* 如果存在，则要删除的是二级栏目 */
            else {
                /* 根据栏目ID查询该栏目下的文章信息 */
                List<Article> articles = internalListArticleByCategoryId(id);
                boolean deleteable = true;
                if (ObjectUtil.nonEmpty(articles)) {
                    /* 如果查出该栏目下存在文章信息，则需要逐一判断这些文章的作者用户数据状态 */
                    for (Article article : articles) {
                        /* 查询该文章的作者信息 */
                        UserExt userExt = userService.getByIdNullable(article.getUserId());
                        /* 如果查到的该对象有值，则禁止删除该栏目 */
                        if (ObjectUtil.nonNull(userExt)) {
                            deleteable = false;
                            break;
                        }
                    }
                }
                /* 如果经过判断允许被删除，则调用Dao层执行删除 */
                if (deleteable) {
                    deleteCount += categoryMapper.deleteById(id);
                }
            }
        }
        /* 如果没有任何一条数据删除成功，则抛出异常 */
        checkResult(deleteCount, ResultCode.CATEGORY_DELETE_FAILED);
    }


    @Override
    public void update(CategoryExt categoryExt) {
        Integer id = categoryExt.getId();
        String name = categoryExt.getName();
        Integer parentId = categoryExt.getParentId();
        /* 检查栏目名称是否重复 */
        checkNameUnique(name, id);

        /* 根据ID查出要修改的栏目信息 */
        Category category = categoryMapper.selectById(id);

        /* 如果该栏目是一个父栏目，而本次提交的参数中带有parentId，则抛出异常 */
        if (ObjectUtil.isNull(category.getParentId()) &&
                ObjectUtil.nonNull(categoryExt.getParentId())) {
            throw new CmsException(ResultCode.CATEGORY_LEVEL_SETTING_ERROR);
        }

        /* 如果要修改的是一个二级栏目，并且要修改所属的一级栏目，检查一级栏目是否存在 */
        if (ObjectUtil.nonNull(category.getParentId()) &&
                ObjectUtil.notEquals(category.getParentId(),
                        parentId)) {
            checkParentIdExist(parentId);
        }

        /* 封装参数为Entity，调用Dao层执行修改 */
        Category record = Category.builder()
                .id(id)
                .name(name)
                .description(categoryExt.getDescription())
                .parentId(parentId)
                .orderNum(categoryExt.getOrderNum())
                .build();
        checkResult(categoryMapper.updateById(record));
    }

    @Override
    public CategoryExt getById(Integer id, boolean cascadeChildren) {
        CategoryExt categoryExt = CategoryExt.toExt(categoryMapper.selectById(id));
        if (cascadeChildren && ObjectUtil.isNull(categoryExt.getParentId())) {
            /* 查询该栏目下的子栏目信息 */
            internalQueryChildren(categoryExt);
        }
        return categoryExt;
    }

    @Override
    @Cached
    public List<CategoryExt> list(String type, boolean cascadeChildren) {

        LambdaQueryWrapper<Category> lqw = new LambdaQueryWrapper<>();
        /* 根据查询类型设置查询条件 */
        if (ObjectUtil.equalsIgnoreCase(
                GlobalConstants.CATEGORY_QUERY_TYPE_PARENT, type)) {
            lqw.isNull(Category::getParentId);
        } else if (ObjectUtil.equalsIgnoreCase(
                GlobalConstants.CATEGORY_QUERY_TYPE_CHILD, type)) {
            lqw.isNotNull(Category::getParentId);
        }
        /* 调用Dao层查询 */
        List<Category> categories = categoryMapper.selectList(lqw);

        /* 转换数据类型 */
        List<CategoryExt> categoryExts = CategoryExt.toExt(categories);

        /* 判断是否需要查询一级栏目包含的二级栏目信息 */
        if (ObjectUtil.equalsIgnoreCase(
                GlobalConstants.CATEGORY_QUERY_TYPE_PARENT, type)
                && cascadeChildren) {
            categoryExts.forEach(this::internalQueryChildren);
        }
        return categoryExts;
    }

    @Override
    public IPage<CategoryExt> pageQueryByClause(IPage<Category> page, CategoryExt categoryExt) {

        Integer parentId = categoryExt.getParentId();
        LambdaQueryWrapper<Category> lqw = new LambdaQueryWrapper<>();
        if (ObjectUtil.nonNull(parentId)) {
            lqw.eq(Category::getParentId, parentId);
        }

        /* 执行查询 */
        categoryMapper.selectPage(page, lqw);

        /* 进行排序 */
        page.getRecords().sort((c1, c2) -> {

            Integer parentId1 = c1.getParentId();
            Integer parentId2 = c2.getParentId();
            /* 如果第一个parentId为null，则必然顺序不变 */
            if (ObjectUtil.isNull(parentId2)) {
                return 1;
            }
            /* 如果第一个是二级栏目，第二个是一级栏目，则向前插入 */
            if (ObjectUtil.isNull(parentId1)) {
                return -1;
            }
            return ObjectUtil.notEquals(parentId1, parentId2) ?
                    parentId1 - parentId2 :
                    c1.getOrderNum() - c2.getOrderNum();
        });

        return PageUtil.convert(page, CategoryExt::toExt);
    }

    @Override
    @SneakyThrows
    @Transactional
    public void upload(InputStream is) {
        /* 调用一个Excel工具类对MultipartFile中的输入流进行读取和解析 */
        List<ExcelCategory> excelCategories =
                excelUtil.read(is, ExcelCategory.class);

        /* 先对集合进行排序，父栏目在前，子栏目在后 */
        excelCategories.sort((o1, o2) -> {
            /* 当o1为父栏目o2为子栏目的时候才需要调整顺序 */
            if (ObjectUtil.notHasText(o1.getParentName())
                    && ObjectUtil.hasText(o2.getParentName())) {
                return -1;
            }
            return 0;
        });

        /* 遍历排好序的集合，处理每个要插入的栏目信息 */
        excelCategories.forEach(excelCategory -> {
            /* 检查名称是否可用 */
            String name = excelCategory.getName();
            checkNameUnique(name);

            /* 取出参数中的逻辑删除状态字符串 */
            String deletedStr = excelCategory.getDeleted();
            /* 定义一个变量，用来保存接下来要计算出来的逻辑删除标志位值 */
            Integer deleted;
            if (ObjectUtil.equals(deletedStr, GlobalConstants.LOGIC_NOT_DELETED_STATUS_VALUE)) {
                deleted = GlobalConstants.LOGIC_NOT_DELETED_FLAG_VALUE;
            } else if (ObjectUtil.equals(deletedStr, GlobalConstants.LOGIC_DELETED_STATUS_VALUE)) {
                deleted = GlobalConstants.LOGIC_DELETED_FLAG_VALUE;
            } else {
                throw new CmsException(ResultCode.CATEGORY_EXCEL_CONTENT_ERROR);
            }
            /* 如果本条数据为子栏目，则根据填写的栏目名称查询父栏目id，赋值给parentId属性 */
            Integer parentId = null;
            String parentName = excelCategory.getParentName();
            if (ObjectUtil.hasText(parentName)) {
                Category parent = checkParentNameExist(parentName);
                parentId = parent.getId();
            }

            /* 创建Entity对象并封装属性 */
            Category category = Category.builder()
                    .name(name)
                    .description(excelCategory.getDescription())
                    .orderNum(excelCategory.getOrderNum())
                    .deleted(deleted)
                    .parentId(parentId)
                    .build();

            /* 插入该元素 */
            categoryMapper.insert(category);
        });
    }

    /**
     * 导出数据为Excel表格，输出到指定的流上
     *
     * @param os
     */
    @Override
    public void download(OutputStream os) {
        /* 查出所有的栏目信息 */
        List<Category> categories = categoryMapper.selectList(null);
        /* 将集合转换成Map<Integer, Category> */
        Map<Integer, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, o -> o));

        List<ExcelCategory> excelCategories = categories.stream()
                .map(category -> ExcelCategory.builder()
                        .name(category.getName())
                        .description(category.getDescription())
                        .orderNum(category.getOrderNum())
                        .deleted(Optional.ofNullable(category.getDeleted())
                                .filter(deleted -> ObjectUtil.equals(
                                        deleted,
                                        GlobalConstants.LOGIC_NOT_DELETED_FLAG_VALUE))
                                .map(integer -> GlobalConstants.LOGIC_NOT_DELETED_STATUS_VALUE)
                                .orElse(GlobalConstants.LOGIC_DELETED_STATUS_VALUE))
                        .parentName(Optional.ofNullable(category.getParentId())
                                .map(categoryMap::get)
                                .map(Category::getName)
                                .orElse(""))
                        .build())
                .collect(Collectors.toList());

        excelUtil.write(os, ExcelCategory.class, excelCategories);
    }

    /**
     * 为传入的栏目对象查询并封装子栏目信息
     * @param categoryExt
     */
    private void internalQueryChildren(CategoryExt categoryExt) {
        /* 判断传入的是否是一个父栏目 */
        if (ObjectUtil.nonNull(categoryExt.getParentId())) {
            return;
        }
        LambdaQueryWrapper<Category> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Category::getParentId, categoryExt.getId());
        List<Category> categories = categoryMapper.selectList(lqw);
        categoryExt.setChildren(CategoryExt.toExt(categories));
    }

    private List<Article> internalListArticleByCategoryId(Integer categoryId) {
        LambdaQueryWrapper<Article> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Article::getCategoryId, categoryId);
        return articleMapper.selectList(lqw);
    }

    private void checkIdExist(Integer id) {
        if (ObjectUtil.isNull(categoryMapper.selectById(id))) {
            throw new CmsException(ResultCode.CATEGORY_NOT_EXIST);
        }
    }

    private void checkParentIdExist(Integer id) {
        if (ObjectUtil.isNull(categoryMapper.selectById(id))) {
            throw new CmsException(ResultCode.PARENT_CATEGORY_IS_INVALID);
        }
    }

    private Category checkParentNameExist(String name) {
        LambdaQueryWrapper<Category> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Category::getName, name);
        Category parent = categoryMapper.selectOne(lqw);
        if (ObjectUtil.isNull(parent)) {
            throw new CmsException(ResultCode.PARENT_CATEGORY_IS_INVALID);
        }
        return parent;
    }

    private void checkNameUnique(String name) {
        checkNameUnique(name, null);
    }

    private void checkNameUnique(String name, Integer id) {
        LambdaQueryWrapper<Category> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Category::getName, name);
        if (ObjectUtil.nonNull(id)) {
            lqw.ne(Category::getId, id);
        }
        if (ObjectUtil.nonNull(categoryMapper.selectOne(lqw))) {
            throw new CmsException(ResultCode.CATEGORY_NAME_HAS_EXISTED);
        }
    }

}
