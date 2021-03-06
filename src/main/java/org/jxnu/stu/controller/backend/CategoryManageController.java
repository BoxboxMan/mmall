package org.jxnu.stu.controller.backend;

import com.alibaba.druid.util.StringUtils;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.common.ReturnCode;
import org.jxnu.stu.common.ServerResponse;
import org.jxnu.stu.controller.vo.CategoryVo;
import org.jxnu.stu.controller.vo.UserVo;
import org.jxnu.stu.dao.CategoryMapper;
import org.jxnu.stu.dao.pojo.Category;
import org.jxnu.stu.service.CategoryService;
import org.jxnu.stu.service.bo.CategoryBo;
import org.jxnu.stu.util.CookieHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/manage/category")
//@CrossOrigin(allowCredentials = "true", origins = {"http://www.wannarich.com","http://admin.wannarich.com"})
public class CategoryManageController {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private CategoryMapper categoryMapper;


    /**
     * 根据产品类别ID获取分类信息
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
        List<List<Category>> list;
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(), categoryVoList);
    }

    /**
     * 添加分类信息
     * @param parentId
     * @param categoryName
     * @param session
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/add_category", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> addCategory(@RequestParam(defaultValue = "0") Integer parentId, String categoryName, HttpSession session) throws BusinessException {
        if (StringUtils.isEmpty(categoryName)) {
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR, "商品分类名称为空");
        }
        Category parentCategory = categoryMapper.selectByPrimaryKey(parentId);
        if(parentCategory == null && parentId != 0){
            throw new BusinessException(ReturnCode.CATEGORY_NOT_EXIST,"上层分类不存在");
        }
        categoryService.addCategory(parentId, categoryName);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(), "添加品类成功");
    }

    /**
     * 更新指定分类的名称
     * @param categoryId
     * @param categoryName
     * @param session
     * @return
     * @throws BusinessException
     */
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

    /**
     * 根据传入的 categoryId 按层遍历获取其下面所有子节点
     * @param categoryId
     * @param session
     * @return
     * @throws BusinessException
     */
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
