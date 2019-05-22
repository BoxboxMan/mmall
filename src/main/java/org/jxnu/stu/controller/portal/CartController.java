package org.jxnu.stu.controller.portal;

import com.alibaba.druid.util.StringUtils;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.common.ReturnCode;
import org.jxnu.stu.common.ServerResponse;
import org.jxnu.stu.controller.vo.CartVo;
import org.jxnu.stu.controller.vo.UserVo;
import org.jxnu.stu.service.CartService;
import org.jxnu.stu.util.CookieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "/list",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<CartVo> list(HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        CartVo cartVo = cartService.list(userVo.getId());
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartVo);
    }

    @RequestMapping(value = "/add",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<CartVo> add(Integer productId,Integer count,HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        if(productId == null || count == null){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR);
        }
        CartVo cartVo = cartService.add(userVo.getId(), productId, count);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartVo);
    }

    @RequestMapping(value = "/update",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<CartVo> update(Integer productId,Integer count,HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        if(productId == null || count == null){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR);
        }
        CartVo cartVo = cartService.update(userVo.getId(), productId, count);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartVo);
    }

    @RequestMapping(value = "/delete_product",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<CartVo> deleteProduct(String productIds,HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        if(StringUtils.isEmpty(productIds)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR);
        }
        CartVo cartVo = cartService.deleteProduct(userVo.getId(), productIds);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartVo);
    }

    @RequestMapping(value = "/clear_cart",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> clearCart(HttpServletRequest request){
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        Boolean res = cartService.clearCart(userVo.getId());
        return res ? ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),null,"清空购物车成功！")
                : ServerResponse.createServerResponse(ReturnCode.CART_CLEAR_FAILD.getCode(),"清空购物车失败");
    }

    @RequestMapping(value = "/select",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<CartVo> select(Integer productId,HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        if(productId == null){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR);
        }
        CartVo cartVo = cartService.selectOrUnSelect(userVo.getId(), productId, Constant.CHECKED);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartVo);
    }

    @RequestMapping(value = "/un_select",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<CartVo> unSelect(Integer productId,HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        if(productId == null){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR);
        }
        CartVo cartVo = cartService.selectOrUnSelect(userVo.getId(), productId, Constant.UNCHECKED);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartVo);
    }

    @RequestMapping(value = "/get_cart_product_count",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<Integer> getCartProductCount(HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        Integer cartProductCount = cartService.getCartProductCount(userVo.getId());
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartProductCount);
    }

    @RequestMapping(value = "/select_all",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<CartVo> selectAll(HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        CartVo cartVo = cartService.selectOrUnSelect(userVo.getId(), null, Constant.CHECKED);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartVo);
    }

    @RequestMapping(value = "/un_select_all",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<CartVo> unSelectAll(HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        CartVo cartVo = cartService.selectOrUnSelect(userVo.getId(), null, Constant.UNCHECKED);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartVo);
    }


}
