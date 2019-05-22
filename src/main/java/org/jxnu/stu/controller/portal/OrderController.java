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
import org.jxnu.stu.service.CartService;
import org.jxnu.stu.service.OrderService;
import org.jxnu.stu.service.ShippingService;
import org.jxnu.stu.util.CookieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;


    /**
     * 创建订单，并生产二维码
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
    @RequestMapping(value = "/alipay_callback")
    @ResponseBody
    public String alipayCallback(HttpServletRequest request) throws BusinessException {
        log.info("支付宝回调开始");
        String callback = orderService.alipayCallback(request);
        return callback;
    }


    @RequestMapping(value = "/create",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<OrderVo> create(Integer shippingId, HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        if(shippingId == null){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"请输入地址id");
        }
        OrderVo orderVo = orderService.create(shippingId, userVo.getId());
        if(orderVo == null){
            return ServerResponse.createServerResponse(ReturnCode.ORDER_CREATE_FAILD.getCode(),ReturnCode.ORDER_CREATE_FAILD.getMsg());
        }
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),null,orderVo);
    }

    @RequestMapping(value = "/get_order_cart_product",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<OrderVo> getOrderCartProduct(HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        OrderVo orderVo = orderService.getOrderCartProduct(userVo.getId());
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),orderVo);
    }

    @RequestMapping(value = "/list",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> list(@RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        PageInfo<OrderVo> pageInfo = orderService.list(userVo.getId(), pageSize, pageNum);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),pageInfo);
    }

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
