package org.jxnu.stu.service;

import com.github.pagehelper.PageInfo;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.controller.vo.ProductVo;
import org.jxnu.stu.dao.pojo.Product;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;


public interface ProductService {

    PageInfo list(Integer categoryId, String keyword, Integer pageNum, Integer pageSize, String orderBy) throws BusinessException;

    ProductVo detail(Integer productId) throws BusinessException;

    PageInfo list(Integer pageNum, Integer pageSize) throws BusinessException;

    PageInfo search(String productName, Integer productId, Integer pageNum, Integer pageSize) throws BusinessException;

    void setSaleStatus(Integer productId, Integer status) throws BusinessException;

    void save(ProductVo product) throws BusinessException;

}
