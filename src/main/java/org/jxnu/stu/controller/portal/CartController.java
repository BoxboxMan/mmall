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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/cart")
//@CrossOrigin(allowCredentials = "true")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取当前用户的购物车信息
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/list",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<CartVo> list(HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        CartVo cartVo = cartService.list(userVo.getId());
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartVo);
    }

    /**
     * 当前登陆用户添加商品和对应数量到自己的购物车中
     * @param productId
     * @param count
     * @param request
     * @return
     * @throws BusinessException
     */
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

    /**
     * 当前登陆用户修改购物车中指定商品的数量，不允许修改数量到0，想要删除必须调delete_product
     * @param productId
     * @param count
     * @param request
     * @return
     * @throws BusinessException
     */
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

    /**
     * 删除指定的多个商品种类。
     * @param productIds
     * @param request
     * @return
     * @throws BusinessException
     */
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

    /**
     * 清空当前登陆用户的购物车
     * @param request
     * @return
     */
    @RequestMapping(value = "/clear_cart",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> clearCart(HttpServletRequest request){
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        Boolean res = cartService.clearCart(userVo.getId());
        return res ? ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),null,"清空购物车成功！")
                : ServerResponse.createServerResponse(ReturnCode.CART_CLEAR_FAILD.getCode(),"清空购物车失败");
    }

    /**
     * 根据 productId 选中指定的商品
     * @param productId
     * @param request
     * @return
     * @throws BusinessException
     */
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

    /**
     * 购物车中取消选中指定的单个商品
     * @param productId
     * @param request
     * @return
     * @throws BusinessException
     */
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

    /**
     * 获取当前购物车中商品的总数量
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/get_cart_product_count",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<Integer> getCartProductCount(HttpServletRequest request) throws BusinessException {
        UserVo userVo = null;
        String loggingToken = CookieHelper.readLoggingToken(request);
        if(!StringUtils.isEmpty(loggingToken)) {
            userVo = (UserVo) redisTemplate.opsForValue().get(loggingToken);
        }
        if(userVo == null){
            return ServerResponse.createServerResponse(ReturnCode.ERROR.getCode(),"用户未登录，但是不能返回未登录状态码（前台会强行登陆）");
        }
        Integer cartProductCount = cartService.getCartProductCount(userVo.getId());
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartProductCount);
    }

    /**
     * 选中购物车中所有的商品
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/select_all",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<CartVo> selectAll(HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        CartVo cartVo = cartService.selectOrUnSelect(userVo.getId(), null, Constant.CHECKED);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartVo);
    }

    /**
     * 取消勾选购物车中所有的商品
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/un_select_all",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<CartVo> unSelectAll(HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        CartVo cartVo = cartService.selectOrUnSelect(userVo.getId(), null, Constant.UNCHECKED);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),cartVo);
    }


}
