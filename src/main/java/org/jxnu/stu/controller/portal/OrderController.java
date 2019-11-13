package org.jxnu.stu.controller.portal;

import com.alibaba.druid.util.StringUtils;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.common.ReturnCode;
import org.jxnu.stu.common.ServerResponse;
import org.jxnu.stu.controller.vo.OrderVo;
import org.jxnu.stu.controller.vo.ShippingVo;
import org.jxnu.stu.controller.vo.UserVo;
import org.jxnu.stu.dao.CartMapper;
import org.jxnu.stu.dao.pojo.Cart;
import org.jxnu.stu.mq.MqProducer;
import org.jxnu.stu.service.CartService;
import org.jxnu.stu.service.OrderService;
import org.jxnu.stu.service.ShippingService;
import org.jxnu.stu.util.CookieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true")
public class OrderController {

    public static final ThreadLocal<OrderVo> temp = new ThreadLocal<>();
    @Autowired
    private OrderService orderService;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private MqProducer mqProducer;
    @Autowired
    private CartMapper cartMapper;



    /**
     * 用户：支付订单，并生产二维码
     * @param orderNo
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/pay",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<Map> pay(Long orderNo, HttpServletRequest request) throws Exception {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        if(orderNo == null){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"请填写订单号");
        }
        String path = request.getSession().getServletContext().getRealPath("upload");
        Map map = orderService.pay(orderNo, userVo.getId(), path);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),map);
    }

    /**
     * 查询订单支付状态
     * @param orderNo
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/query_order_pay_status",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<Boolean> queryOrderPayStatus(Long orderNo,HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        if(orderNo == null){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"请填写订单号");
        }
        Boolean isSuccess = orderService.queryOrderPayStatus(orderNo, userVo.getId());
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),isSuccess);
    }

    /**
     * 支付宝回调接口
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/alipay_callback",method = RequestMethod.POST)
    @ResponseBody
    public String alipayCallback(HttpServletRequest request) throws BusinessException {
        log.info("支付宝回调开始");
        String callback = orderService.alipayCallback(request);
        return callback;
    }

    /**
     * 用户：购物车中选中的商品进行创建订单,如果为null则默认为购物车中选中商品进行下单
     * @param productIdWithAmountMap
     * @param shippingId
     * @param request
     * @return
     * @throws BusinessException
     * @throws UnsupportedEncodingException
     */
    @RequestMapping(value = "/create",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<OrderVo> create(Map<String,String> productIdWithAmountMap,Integer shippingId, HttpServletRequest request) throws BusinessException, UnsupportedEncodingException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        if(shippingId == null) {
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR, "请输入地址id");
        }
        Map<Integer,Integer> productIdWithAmount = new HashMap<>();//productId对应其被购买的count
        for(String key:productIdWithAmountMap.keySet()){
            productIdWithAmount.put(Integer.valueOf(key),Integer.valueOf(productIdWithAmountMap.get(key)));
        }
        if(productIdWithAmount == null || productIdWithAmount.size() < 1){//如果productIdWithAmount为空则用购物车中选中的商品下单。
            List<Cart> cartList = cartMapper.selectCheckedByUserId(userVo.getId());
            if (cartList.size() < 1 || cartList == null) {
                throw new BusinessException(ReturnCode.CART_IS_EMPTY);
            }
            for(Cart cart:cartList){
                productIdWithAmount.put(cart.getProductId(),cart.getQuantity());
            }
        }
        if(productIdWithAmount.keySet().size() < 1){
            throw new BusinessException(ReturnCode.ORDER_HAS_NO_PRODUCT);
        }
        boolean result = mqProducer.transactionAsyncReduceStock(productIdWithAmount, shippingId, userVo.getId());
        OrderVo orderVo = temp.get();
        if(orderVo == null || !result){
            return ServerResponse.createServerResponse(ReturnCode.ORDER_CREATE_FAILD.getCode(),ReturnCode.ORDER_CREATE_FAILD.getMsg());
        }
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),null,orderVo);
    }

    /**
     * 获取购物车中已勾选的商品的详情
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/get_order_cart_product",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<OrderVo> getOrderCartProduct(HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        OrderVo orderVo = orderService.getOrderCartProduct(userVo.getId());
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),orderVo);
    }

    /**
     * 获取个人订单列表
     * @param pageSize
     * @param pageNum
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/list",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> list(@RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        PageInfo<OrderVo> pageInfo = orderService.list(userVo.getId(), pageSize, pageNum);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),pageInfo);
    }

    /**
     * 根据 OrderNo 获取订单内部商品等详情信息
     * @param orderNo
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/detail",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<OrderVo> detail(Long orderNo,HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        if(orderNo == null){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"请输入订单号");
        }
        OrderVo orderVo = orderService.detail(userVo.getId(), orderNo);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),orderVo);
    }

    /**
     * 允许用户取消未支付的订单
     * @param orderNo
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/cancel",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<OrderVo> cancel(Long orderNo,HttpServletRequest request) throws BusinessException{
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        if(orderNo == null){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"请输入订单号");
        }
        boolean result = orderService.cancel(userVo.getId(), orderNo);
        return result == true ? ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),"取消订单成功") :
                    ServerResponse.createServerResponse(ReturnCode.ORDER_CREATE_FAILD.getCode(),ReturnCode.ORDER_CREATE_FAILD.getMsg());
    }

}
