package org.jxnu.stu.service.impl;

import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.common.ReturnCode;
import org.jxnu.stu.controller.vo.CartProductVoList;
import org.jxnu.stu.controller.vo.CartVo;
import org.jxnu.stu.dao.CartMapper;
import org.jxnu.stu.dao.pojo.Cart;
import org.jxnu.stu.service.CartService;
import org.jxnu.stu.util.BigDecimalHelper;
import org.jxnu.stu.util.PropertiesHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartMapper cartMapper;

    @Override
    public CartVo list(Integer userId) {
        CartVo cartVo = new CartVo();
        List<CartProductVoList> cartProductVoLists = cartMapper.listByUserIdOrProductId(userId,null);
        BigDecimal totalPrice = new BigDecimal("0");
        for(CartProductVoList cartProductVoList:cartProductVoLists){
            cartProductVoList.setProductMainImage(PropertiesHelper.getProperties("ftp.server.http.prefix")+cartProductVoList.getProductMainImage());
            cartProductVoList.setProductTotalPrice(BigDecimalHelper.mul(new BigDecimal(cartProductVoList.getQuantity()),cartProductVoList.getProductPrice()));
            if(cartProductVoList.getQuantity() > cartProductVoList.getProductStock()){
                //首先去更新数据库数据让其购买数量等于库存数
                Cart cart = new Cart();
                cart.setQuantity(cartProductVoList.getProductStock());
                cart.setId(cartProductVoList.getId());
                cartMapper.updateByPrimaryKeySelective(cart);
                //其次修改当前商品的购买数量
                cartProductVoList.setQuantity(cartProductVoList.getProductStock());
                cartProductVoList.setLimitQuantity(Constant.LIMIT_NUM_FAIL);
            }else{
                cartProductVoList.setLimitQuantity(Constant.LIMIT_NUM_SUCCESS);
            }
            if(cartProductVoList.getProductChecked() == 1){//当被勾选的时候用于计算总价
                totalPrice = BigDecimalHelper.add(totalPrice,cartProductVoList.getProductTotalPrice());
            }
        }
        cartVo.setCartProductVoList(cartProductVoLists);
        cartVo.setAllChecked(!(cartMapper.hasUnChecked(userId) > 0));
        cartVo.setCartTotalPrice(totalPrice);
        return cartVo;
    }

    @Override
    public CartVo add(Integer userId,Integer productId, Integer count) throws BusinessException {
            List<CartProductVoList> cartProductVoLists = cartMapper.listByUserIdOrProductId(userId, productId);
        if(cartProductVoLists.size() == 0){//说明不存在这样的商品购买记录所以要新增
            Cart cart = new Cart();
            cart.setQuantity(count);
            cart.setChecked(1);
            cart.setProductId(productId);
            cart.setUserId(userId);
            cartMapper.insert(cart);
        }else{
            int i = cartMapper.add(userId, productId, count);
            if(i < 1){
                throw new BusinessException(ReturnCode.CART_ADD_ERROR);
            }
        }
        CartVo cartVo = list(userId);
        return cartVo;
    }

    @Override
    public CartVo update(Integer userId, Integer productId, Integer count) throws BusinessException {
        int i = cartMapper.update(userId, productId, count);
        if(i < 1){
            throw new BusinessException(ReturnCode.CART_UPDATE_ERRIR);
        }
        CartVo cartVo = list(userId);
        return cartVo;
    }

    /**
     * 我们默认 productIds 的格式是 1,2,3,4,5 以逗号为分隔符的字符串
     * @param userId
     * @param productIds
     * @return
     */
    @Override
    public CartVo deleteProduct(Integer userId, String productIds) throws BusinessException {
        List<String> productIdList = Splitter.on(",").splitToList(productIds);
        int i = cartMapper.deleteProduct(userId, productIdList);
        if(i < 1){
            throw new BusinessException(ReturnCode.CART_DELETE_ERROR);
        }
        CartVo cartVo = list(userId);
        return cartVo;
    }

    @Override
    public CartVo selectOrUnSelect(Integer userId,Integer productId,Integer checkStatus){
        try {
            int i = cartMapper.selectOrUnSelect(userId, productId, checkStatus);
        }catch (Exception e){
            System.out.println("这里出现了异常");
            e.printStackTrace();
        }
        CartVo cartVo = list(userId);
        return cartVo;
    }

    @Override
    public Boolean clearCart(Integer userId){
        try {
            cartMapper.deleteAllCart(userId);
            return true;
        }catch (Exception e){
            log.error("清空购物车失败！",e);
            return false;
        }
    }

    @Override
    public Integer getCartProductCount(Integer userId) {
        int cartProductCount = cartMapper.getCartProductCount(userId);
        return cartProductCount;
    }
}
