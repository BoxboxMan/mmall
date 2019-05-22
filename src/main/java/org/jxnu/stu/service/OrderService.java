package org.jxnu.stu.service;

import com.github.pagehelper.PageInfo;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.ServerResponse;
import org.jxnu.stu.controller.vo.OrderVo;
import org.jxnu.stu.dao.pojo.Order;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface OrderService {

    Map pay(Long orderNo, Integer userId, String path) throws Exception;

    Boolean queryOrderPayStatus(Long orderNo,Integer userId) throws BusinessException;

    String alipayCallback(HttpServletRequest request) throws BusinessException;

    OrderVo create(Integer shippingId, Integer userId) throws BusinessException;

    OrderVo getOrderCartProduct(Integer userId)throws BusinessException;

    PageInfo<OrderVo> list(Integer userId, Integer pageSize, Integer pageNum) throws BusinessException;

    OrderVo detail(Integer userId,Long orderNo) throws BusinessException;

    boolean cancel(Integer userId,Long orderNo) throws BusinessException;

    PageInfo<OrderVo> listAll(Integer pageSize,Integer pageNum) throws BusinessException;

    OrderVo detail(Long orderNo) throws BusinessException;
}
