package org.jxnu.stu.service.impl;

import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.common.ReturnCode;
import org.jxnu.stu.dao.CategoryMapper;
import org.jxnu.stu.dao.pojo.Category;
import org.jxnu.stu.service.CategoryService;
import org.jxnu.stu.service.bo.CategoryBo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * @param categoryId
     * @return the categoryId contains childrenNode with parentNode
     * @throws BusinessException
     */
    public List<CategoryBo> getChildrenParallelCategory(Integer categoryId) throws BusinessException {
        List<Category> categoryList = categoryMapper.findChildrenParallelCategory(categoryId);
        List<CategoryBo> resList = new ArrayList<>();
        for (Category category : categoryList) {
            CategoryBo categoryBo = coverCategoryBoFromCategoryDo(category);
            resList.add(categoryBo);
        }
        return resList;
    }

    public void addCategory(Integer parentId, String categoryName) throws BusinessException {
        Category category = new Category();
        category.setParentId(parentId);
        category.setName(categoryName);
        category.setStatus(Constant.CATEGORY_NORMAL);
        int i = categoryMapper.insert(category);
        if (i == 0) {
            throw new BusinessException(ReturnCode.CATEGORY_ADD_ERROR);
        }
    }

    public List<CategoryBo> getDeepCategory(Integer categoryId) throws BusinessException {
        LinkedList<CategoryBo> linkedList = new LinkedList<>();
        List<CategoryBo> list = new ArrayList<>();
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if (category == null) {
            throw new BusinessException(ReturnCode.CATEGORY_NOT_EXIST);
        }
        linkedList.add(coverCategoryBoFromCategoryDo(category));
        while (linkedList.size() > 0) {
            CategoryBo categoryBo = linkedList.poll();
            list.add(categoryBo);
            List<CategoryBo> childrenParallelCategory = getChildrenParallelCategory(categoryBo.getId());
            for (CategoryBo c : childrenParallelCategory) {
                if(!c.getId().equals(categoryBo.getId())){//这里有一个操蛋的自动装箱问题
                    linkedList.offer(c);
                }
            }
        }
        return list;
    }

    public void setCategoryName(Integer categoryId, String categoryName) throws BusinessException {
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);
        int i = categoryMapper.updateByPrimaryKeySelective(category);
        if (i == 0) {
            throw new BusinessException(ReturnCode.CATEGORY_UPDATE_ERROR);
        }
    }

    public CategoryBo coverCategoryBoFromCategoryDo(Category category) throws BusinessException {
        if (category == null) {
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR, "该商品分类信息不存在");
        }
        CategoryBo categoryBo = new CategoryBo();
        BeanUtils.copyProperties(category, categoryBo);
        return categoryBo;
    }
}
