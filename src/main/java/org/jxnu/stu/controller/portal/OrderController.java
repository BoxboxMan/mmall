package org.jxnu.stu.controller.portal;

import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.common.ReturnCode;
import org.jxnu.stu.common.ServerResponse;
import org.jxnu.stu.controller.vo.UserVo;
import org.jxnu.stu.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单，并生产二维码
     * @param orderNo
     * @param request
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/pay",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<Map> pay(String orderNo, HttpServletRequest request) throws Exception {
        UserVo userVo = (UserVo)request.getSession().getAttribute(Constant.CURRENT_USER);
        if(userVo == null){
            throw new BusinessException(ReturnCode.USER_NOT_LOGIN);
        }
        if(orderNo == null){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"请填写订单号");
        }
        String path = request.getSession().getServletContext().getRealPath("upload");
        Map map = orderService.pay(orderNo, userVo.getId(), path);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),map);
    }

    @RequestMapping(value = "/query_order_pay_status",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<Boolean> queryOrderPayStatus(String orderNo,HttpServletRequest request) throws BusinessException {
        UserVo userVo = (UserVo)request.getSession().getAttribute(Constant.CURRENT_USER);
        if(userVo == null){
            throw new BusinessException(ReturnCode.USER_NOT_LOGIN);
        }
        if(orderNo == null){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"请填写订单号");
        }
        Boolean isSuccess = orderService.queryOrderPayStatus(orderNo, userVo.getId());
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),isSuccess);
    }

    @RequestMapping(value = "/alipay_callback",method = RequestMethod.POST)
    @ResponseBody
    public String alipayCallback(HttpServletRequest request) throws BusinessException {
        String callback = orderService.alipayCallback(request);
        return callback;
    }

}
