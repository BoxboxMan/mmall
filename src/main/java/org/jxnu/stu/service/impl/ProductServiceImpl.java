package org.jxnu.stu.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.common.ReturnCode;
import org.jxnu.stu.controller.vo.ProductListVo;
import org.jxnu.stu.controller.vo.ProductVo;
import org.jxnu.stu.dao.CategoryMapper;
import org.jxnu.stu.dao.ProductMapper;
import org.jxnu.stu.dao.pojo.Category;
import org.jxnu.stu.dao.pojo.Product;
import org.jxnu.stu.service.ProductService;
import org.jxnu.stu.service.bo.CategoryBo;
import org.jxnu.stu.util.DateTimeHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {


    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CategoryServiceImpl categoryService;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 根据分类或者名称进行模糊查询
     * @param categoryId
     * @param keyword
     * @param pageNum
     * @param pageSize
     * @param orderBy
     * @return
     * @throws BusinessException
     */
    @Override
    public PageInfo list(Integer categoryId, String keyword, Integer pageNum, Integer pageSize, String orderBy) throws BusinessException {
        PageHelper.startPage(pageNum,pageSize);
        List<Integer> categoryIdList = new ArrayList<>();
        if(categoryId != null){
            Category category = categoryMapper.selectByPrimaryKey(categoryId);
            if(category == null && StringUtils.isEmpty(keyword)){//分类为空，且名称也为空直接返回空list
                List<ProductListVo> list = new ArrayList<>();
                PageInfo pageInfo = new PageInfo(list);
                return pageInfo;
            }
            List<CategoryBo> deepCategory = categoryService.getDeepCategory(category.getId());
            for(CategoryBo categoryBo:deepCategory){
                categoryIdList.add(categoryBo.getId());
            }
        }
        if(!StringUtils.isEmpty(keyword)){//说明此时categoryId不为空
            keyword = new StringBuilder().append("%").append(keyword).append("%").toString();
        }
        if(!StringUtils.isEmpty(orderBy)){
            if(Constant.ProductListOrderBy.PRICE_ASC_DESC.contains(orderBy)){
                String[] strings = orderBy.split("_");
                PageHelper.orderBy(strings[0]+" "+strings[1]);
            }
        }
        List<Product> products = productMapper.listProductByCategoryIdListOrProductName(keyword, categoryIdList);
        List<ProductListVo> productListVoList = new ArrayList<>();
        for (Product product:products){
            productListVoList.add(coverProductVoFromProductDo(product));
        }
        PageInfo pageInfo = new PageInfo(productListVoList);
        return pageInfo;
    }

    @Override
    public ProductVo detail(Integer productId) throws BusinessException {
        Product product = productMapper.selectByPrimaryKey(productId);
        ProductVo productVo = coverProductVoFromProductDoDetail(product);
        if(productVo == null){
            throw new BusinessException(ReturnCode.COVER_ERROR);
        }
        return productVo;
    }

    @Override
    public PageInfo list(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Product> products = productMapper.listAll();
        PageInfo pageInfo = new PageInfo(products);
        return pageInfo;
    }

    /**
     * 存在productId 直接用 productId搜索，否则用productName搜索
     * @param productName
     * @param productId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageInfo search(String productName, Integer productId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Product> products = new ArrayList<>();
        if(productId != null){
            Product product = productMapper.selectByPrimaryKey(productId);
            products.add(product);
            PageInfo pageInfo = new PageInfo(products);
            return pageInfo;
        }
        List<Product> productList = new ArrayList<>();
        if(!StringUtils.isEmpty(productName)){
            productName = new StringBuilder().append("%").append(productName).append("%").toString();
            productList = productMapper.listProductByProductName(productName);
        }
        List<ProductListVo> productVoList = new ArrayList<>();
        for(Product product:productList){
            productVoList.add(coverProductVoFromProductDo(product));
        }
        PageInfo pageInfo = new PageInfo(productVoList);
        return pageInfo;
    }


    /**
     * 用于list 展示
     * @param product
     * @return
     */
    public ProductListVo coverProductVoFromProductDo(Product product){
        if(product == null){
            return null;
        }
        ProductListVo productListVo = new ProductListVo();
        BeanUtils.copyProperties(product, productListVo);
        return productListVo;
    }

    /**
     * 用于商品详情展示
     * @return
     */
    public ProductVo coverProductVoFromProductDoDetail(Product product) throws BusinessException {
        if(product == null){
            return null;
        }
        ProductVo productVo = new ProductVo();
        BeanUtils.copyProperties(product, productVo);
        productVo.setCreateTime(DateTimeHelper.dateToString(product.getCreateTime()));
        productVo.setUpdateTime(DateTimeHelper.dateToString(product.getUpdateTime()));
        return productVo;
    }
}
