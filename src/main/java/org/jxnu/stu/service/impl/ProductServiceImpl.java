package org.jxnu.stu.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
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
import org.jxnu.stu.util.PropertiesHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {


    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private CategoryServiceImpl categoryService;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * 根据分类或者名称进行模糊查询
     *
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
        List<Integer> categoryIdList = new ArrayList<>();
        List<Product> products = new ArrayList<>();
        List<ProductListVo> productListVoList = new ArrayList<>();
        if (categoryId != null) {
            Category category = null;
            if (categoryId.intValue() == 0) {//如果分类为祖先节点，直接获取所有商品
                return this.list(pageNum, pageSize);
            } else {
                category = categoryMapper.selectByPrimaryKey(categoryId);
            }
            if (category == null && StringUtils.isEmpty(keyword)) {//分类为空，且名称也为空直接返回空list
                PageHelper.startPage(pageNum, pageSize);
                List<ProductListVo> list = new ArrayList<>();
                PageInfo pageInfo = new PageInfo(list);
                return pageInfo;
            }
            List<CategoryBo> deepCategory = categoryService.getDeepCategory(category.getId());
            for (CategoryBo categoryBo : deepCategory) {
                categoryIdList.add(categoryBo.getId());
            }
        }
        if (!StringUtils.isEmpty(keyword)) {//说明此时categoryId不为空
            keyword = new StringBuilder().append("%").append(keyword).append("%").toString();
        }
        PageHelper.startPage(pageNum, pageSize);
        if (!StringUtils.isEmpty(orderBy)) {
            if (Constant.ProductListOrderBy.PRICE_ASC_DESC.contains(orderBy)) {
                String[] strings = orderBy.split("_");
                PageHelper.orderBy(strings[0] + " " + strings[1]);
            }
        }
        products = productMapper.listProductByCategoryIdListOrProductName(keyword, categoryIdList);
        for (Product product : products) {
            productListVoList.add(coverProductVoFromProductDo(product));
        }
        PageInfo pageInfo = new PageInfo(products);
        pageInfo.setList(productListVoList);
        return pageInfo;
    }

    @Override
    public ProductVo detail(Integer productId) throws BusinessException {
        Product product = productMapper.selectByPrimaryKey(productId);
        if (product == null) {
            throw new BusinessException(ReturnCode.PRODUCT_NOT_EXIST);
        }
        ProductVo productVo = assemebleProductVoFromProductDo(product);
        if (productVo == null) {
            throw new BusinessException(ReturnCode.COVER_ERROR);
        }
        return productVo;
    }

    @Override
    public PageInfo list(Integer pageNum, Integer pageSize) throws BusinessException {
        PageHelper.startPage(pageNum, pageSize);
        List<Product> products = productMapper.listAll();
        List<ProductVo> productVoList = new ArrayList<>();
        for (Product productItem : products) {
            productVoList.add(this.assemebleProductVoFromProductDo(productItem));
        }
        PageInfo pageInfo = new PageInfo(productVoList);
        return pageInfo;
    }

    /**
     * 存在productId 直接用 productId搜索，否则用productName搜索
     *
     * @param productName
     * @param productId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageInfo search(String productName, Integer productId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Product> products = new ArrayList<>();
        if (productId != null) {
            Product product = productMapper.selectByPrimaryKey(productId);
            products.add(product);
            PageInfo pageInfo = new PageInfo(products);
            return pageInfo;
        }
        List<Product> productList = new ArrayList<>();
        if (!StringUtils.isEmpty(productName)) {
            productName = new StringBuilder().append("%").append(productName).append("%").toString();
            productList = productMapper.listProductByProductName(productName);
        }
        List<ProductListVo> productVoList = new ArrayList<>();
        for (Product product : productList) {
            productVoList.add(coverProductVoFromProductDo(product));
        }
        PageInfo pageInfo = new PageInfo(productVoList);
        return pageInfo;
    }

    @Override
    @Transactional
    public void setSaleStatus(Integer productId, Integer status) throws BusinessException {
        //如果对应商品下架则需要删除redis库存，如果商品上架需要更新redis库存
        if (status.equals(Constant.ProductConstant.STATUS_SOLD_OUT)) {//设置下架
            redisTemplate.delete("product_stock_id_" + productId);
        } else if (status.equals(Constant.ProductConstant.STATUS_ON_SALE)) {//设置上架
            Product product = productMapper.selectByPrimaryKey(productId);
            redisTemplate.opsForValue().set("product_stock_id_" + productId, product.getStock());
            //如果上架商品的库存大于0，则确保redis中没有该商品的库存售罄标识
            if (product.getStock() > 0) {
                redisTemplate.delete("product_stock_sellOut_id_" + product.getId());
            }
        }
        int i = productMapper.setSaleStatus(productId, status);
        if (i < 1) {
            throw new BusinessException(ReturnCode.PRODUCT_UPDATE_ERROR);
        }
    }

    @Override
    @Transactional
    public void save(ProductVo product) throws BusinessException {
        Product productDo = new Product();
        BeanUtils.copyProperties(product, productDo);
        //传入数据库的应该为图片的原名，不能带http://
        productDo.setMainImage(productDo.getMainImage().replaceAll(PropertiesHelper.getProperties("ftp.server.http.prefix"), ""));
        List<String> subImages = product.getSubImages();
        StringBuffer subImagesString = new StringBuffer();
        for (String subImage : subImages) {
            //删除图片带的http://xxx，以及双引号
            subImage = subImage.replaceAll(PropertiesHelper.getProperties("ftp.server.http.prefix"), "").replace("\"", "");
            String str = subImagesString.length() == 0 ? subImage : "," + subImage;
            subImagesString.append(str);
        }
        //subImagesString.deleteCharAt(0).deleteCharAt(subImagesString.length() - 1);//删除前后的数组括号（因为前段传值问题引起的)
        productDo.setSubImages(new String(subImagesString));
        Integer result = null;
        if (product.getId() == null) {
            productDo.setStatus(2);//新增的商品统一为下架状态
            result = productMapper.insert(productDo);
            log.info("新增商品{} 成功", productDo.getName());
        } else {
            result = productMapper.updateByPrimaryKeySelective(productDo);
            //若更新的产品为上架状态则修改redis对应商品库存,同时删除redis中对应商品售罄标识
            if (productDo.getStatus().equals(Constant.ProductConstant.STATUS_ON_SALE)) {
                redisTemplate.opsForValue().set("product_stock_id_" + productDo.getId(), productDo.getStock());//更新redis库存
                redisTemplate.delete("product_stock_sellOut_id_" + productDo.getId());//删除redis库存售罄标识
            }
            log.info("更新id为:{}  的商品成功", productDo.getId());
        }
    }


    /**
     * 用于list 展示
     *
     * @param product
     * @return
     */
    private ProductListVo coverProductVoFromProductDo(Product product) {
        if (product == null) {
            return null;
        }
        ProductListVo productListVo = new ProductListVo();
        BeanUtils.copyProperties(product, productListVo);
        if (!StringUtils.isEmpty(productListVo.getMainImage())) {
            productListVo.setMainImage(PropertiesHelper.getProperties("ftp.server.http.prefix") + product.getMainImage());
        }
        return productListVo;
    }

    /**
     * 用于商品详情展示
     *
     * @return
     */
    private ProductVo assemebleProductVoFromProductDo(Product product) throws BusinessException {
        if (product == null) {
            return null;
        }
        ProductVo productVo = new ProductVo();
        BeanUtils.copyProperties(product, productVo);
        productVo.setCreateTime(DateTimeHelper.dateToString(product.getCreateTime()));
        productVo.setUpdateTime(DateTimeHelper.dateToString(product.getUpdateTime()));
        //获取图片服务器前缀
        String imagePrefix = PropertiesHelper.getProperties("ftp.server.http.prefix");
        if (!StringUtils.isEmpty(product.getMainImage())) {//图片不为空才能加前缀返回
            productVo.setMainImage(imagePrefix + product.getMainImage());
        }
        String subImages = product.getSubImages();
        if (!StringUtils.isEmpty(subImages)) {
            String[] subImagesArray = subImages.split(",");
            List<String> subImagesList = new ArrayList<>();
            for (int i = 0; i < subImagesArray.length; i++) {
                subImagesList.add(imagePrefix + subImagesArray[i]);
            }
            productVo.setSubImages(subImagesList);
        }
        return productVo;
    }
}
