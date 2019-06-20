package org.jxnu.stu.service;

import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.service.bo.CategoryBo;

import java.util.List;

public interface CategoryService {
    List<CategoryBo> getChildrenParallelCategory(Integer categoryId) throws BusinessException;

    void addCategory(Integer parentId,String categoryName) throws BusinessException;

    void setCategoryName(Integer categoryId,String categoryName) throws BusinessException;

    List<CategoryBo> getDeepCategory(Integer categoryId) throws BusinessException;
}
