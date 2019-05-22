package org.jxnu.stu.service;

import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.controller.vo.CartVo;


public interface CartService {

    CartVo list(Integer userId);

    CartVo add(Integer userId,Integer productId,Integer count) throws BusinessException;

    CartVo update(Integer userId,Integer productId,Integer count) throws BusinessException;

    CartVo deleteProduct(Integer userId,String productIds) throws BusinessException;

    CartVo selectOrUnSelect(Integer userId,Integer productId,Integer checkStatus);

    Integer getCartProductCount(Integer userId);

    Boolean clearCart(Integer userId);
}
