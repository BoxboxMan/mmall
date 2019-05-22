package org.jxnu.stu.controller.backend;

import com.alibaba.druid.util.StringUtils;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.common.ReturnCode;
import org.jxnu.stu.common.ServerResponse;
import org.jxnu.stu.controller.vo.CategoryVo;
import org.jxnu.stu.controller.vo.UserVo;
import org.jxnu.stu.dao.pojo.Category;
import org.jxnu.stu.service.CategoryService;
import org.jxnu.stu.service.bo.CategoryBo;
import org.jxnu.stu.util.CookieHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/manage/category")
public class CategoryManageController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    /**
     * get category info ,find category info parallel
     *
     * @param categoryId
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/get_category", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<List<CategoryVo>> getCategory(@RequestParam(defaultValue = "0") Integer categoryId, HttpServletRequest request) throws BusinessException {
        List<CategoryBo> categoryBoList = categoryService.getChildrenParallelCategory(categoryId);
        List<CategoryVo> categoryVoList = new ArrayList<>();
        for (CategoryBo categoryBo : categoryBoList) {
            CategoryVo categoryVo = coverCategoryVoFromCategoryBo(categoryBo);
            categoryVoList.add(categoryVo);
        }
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(), categoryVoList);
    }

    @RequestMapping(value = "/add_category", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> addCategory(@RequestParam(defaultValue = "0") Integer parentId, String categoryName, HttpSession session) throws BusinessException {
        if (StringUtils.isEmpty(categoryName)) {
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR, "商品分类名称为空");
        }
        categoryService.addCategory(parentId, categoryName);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(), "添加品类成功");
    }

    @RequestMapping(value = "/set_category_name", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> setCategoryName(Integer categoryId, String categoryName, HttpSession session) throws BusinessException {
        if (categoryId == null) {
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR, "商品类别ID为空");
        }
        if (StringUtils.isEmpty(categoryName)) {
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR, "商品类别名称为空");
        }
        categoryService.setCategoryName(categoryId, categoryName);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(), "更新品类信息成功");
    }

    @RequestMapping(value = "/get_deep_category", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<List<CategoryVo>> getDeepCategory(Integer categoryId, HttpSession session) throws BusinessException {
        if (categoryId == null) {
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR, "商品类别ID为空");
        }
        List<CategoryBo> deepCategoryList = categoryService.getDeepCategory(categoryId);
        List<CategoryVo> categoryVoList = new ArrayList<>();
        for (CategoryBo categoryBo : deepCategoryList) {
            categoryVoList.add(coverCategoryVoFromCategoryBo(categoryBo));
        }
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(), categoryVoList);
    }


    public CategoryVo coverCategoryVoFromCategoryBo(CategoryBo categoryBo) throws BusinessException {
        if (categoryBo == null) {
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR, "categoryBo不能为空");
        }
        CategoryVo categoryVo = new CategoryVo();
        BeanUtils.copyProperties(categoryBo, categoryVo);
        return categoryVo;
    }
}
