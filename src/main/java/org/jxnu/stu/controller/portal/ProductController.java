package org.jxnu.stu.controller.portal;

import com.alibaba.druid.util.StringUtils;
import com.github.pagehelper.PageInfo;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.ReturnCode;
import org.jxnu.stu.common.ServerResponse;
import org.jxnu.stu.controller.vo.ProductVo;
import org.jxnu.stu.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/product")
@CrossOrigin(allowCredentials = "true", origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 根据分类、或者关键词搜索商品列表，若传入的categoryId = 0，默认返回所有商品
     * @param categoryId
     * @param keyword 关键词
     * @param pageNum
     * @param pageSize
     * @param orderBy 例如 price_desc 属性加下划线
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/list",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> list(Integer categoryId, String keyword,
                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  @RequestParam(defaultValue = "") String orderBy) throws BusinessException {

        if(categoryId == null && StringUtils.isEmpty(keyword)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR);
        }
        PageInfo pageInfo = productService.list(categoryId, keyword, pageNum, pageSize, orderBy);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),pageInfo);
    }

    /**
     * 根据商品Id获取商品详情
     * @param productId
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/detail",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<ProductVo> detail(Integer productId) throws BusinessException {
        if(productId == null){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR);
        }
        ProductVo detail = productService.detail(productId);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),detail);
    }

}
