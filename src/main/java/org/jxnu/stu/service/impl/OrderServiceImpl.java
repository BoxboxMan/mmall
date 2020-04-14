package org.jxnu.stu.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayResponse;
import com.alipay.api.domain.Car;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.common.ReturnCode;
import org.jxnu.stu.common.ServerResponse;
import org.jxnu.stu.controller.vo.OrderItemVo;
import org.jxnu.stu.controller.vo.OrderVo;
import org.jxnu.stu.controller.vo.ShippingVo;
import org.jxnu.stu.dao.*;
import org.jxnu.stu.dao.pojo.*;
import org.jxnu.stu.mq.MqProducer;
import org.jxnu.stu.service.OrderService;
import org.jxnu.stu.service.ShippingService;
import org.jxnu.stu.util.BigDecimalHelper;
import org.jxnu.stu.util.DateTimeHelper;
import org.jxnu.stu.util.FTPHelper;
import org.jxnu.stu.util.PropertiesHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {


    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ShippingService shippingService;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShippingMapper shippingMapper;
    @Autowired
    private PayInfoMapper payInfoMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private MqProducer mqProducer;

    private static AlipayTradeService tradeService;

    public Map pay(Long orderNo, Integer userId, String path) throws Exception {
        Order order = orderMapper.selectByOrderNoAndUserId(orderNo, userId);
        if (order == null) {
            throw new BusinessException(ReturnCode.ORDER_NOT_EXIST);
        }
        List<OrderItem> orderItems = orderItemMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if (orderItems.size() < 1) {
            throw new BusinessException(ReturnCode.ERROR, "订单详情不存在");
        }
        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = String.valueOf(orderNo);

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = "mmall当面付扫码消费";

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
        // 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
        int quantity = 0;
        for (OrderItem orderItem : orderItems) {
            quantity += orderItem.getQuantity();
            goodsDetailList.add(GoodsDetail.newInstance(orderItem.getProductId().toString(), orderItem.getProductName(),
                    BigDecimalHelper.mul(orderItem.getCurrentUnitPrice(), new BigDecimal("100")).longValue(),
                    orderItem.getQuantity()));
        }

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = "购买" + quantity + "件商品共" + totalAmount + "元";

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(PropertiesHelper.getProperties("zfbinfo.callback.http.prefix"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);

        Configs.init(PropertiesHelper.getProperties("zfbinfo.properties.location"));
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();

        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                log.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                File dirFile = new File(path);
                if (!dirFile.exists()) {
                    dirFile.setWritable(true);
                    dirFile.mkdirs();
                }
                // 需要修改为运行机器上的路径
                String filePath = String.format(path + "/qr-%s.png",
                        response.getOutTradeNo());
                log.info("filePath:" + filePath);
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, filePath);
                File targetFile = new File(filePath);
                try {
                    FTPHelper.uploadFile(Lists.newArrayList(targetFile));
                } catch (Exception e) {
                    log.error("上传二维码失败", e);
                    throw new Exception(e);
                }
                //上传完成后删除图片
                targetFile.delete();
                Map<String, String> map = new HashMap<>();
                map.put("orderNo", outTradeNo);
                map.put("qrPath", PropertiesHelper.getProperties("ftp.server.http.prefix") + targetFile.getName());
                return map;
            case FAILED:
                log.error("支付宝预下单失败!!!");
                throw new BusinessException(ReturnCode.ERROR, "支付宝预下单失败!!!");

            case UNKNOWN:
                log.error("系统异常，预下单状态未知!!!");
                throw new BusinessException(ReturnCode.ERROR, "系统异常，预下单状态未知!!!");

            default:
                log.error("不支持的交易状态，交易返回异常!!!");
                throw new BusinessException(ReturnCode.ERROR, "不支持的交易状态，交易返回异常!!!");
        }

    }

    @Override
    public Boolean queryOrderPayStatus(Long orderNo, Integer userId) throws BusinessException {
        Order order = orderMapper.selectByOrderNoAndUserId(orderNo, userId);
        if (order == null) {
            throw new BusinessException(ReturnCode.ORDER_NOT_EXIST);
        }
        if (order.getStatus() > Constant.OrderStatus.ORDER_NOT_PAY.getStatusCode()) {
            return true;
        }
        return false;
    }

    @Override
    public String alipayCallback(HttpServletRequest request) throws BusinessException {
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, String> map = Maps.newHashMap();
        Iterator<String> iterator = parameterMap.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String[] values = parameterMap.get(key);
            String valueStr = new String();
            for (int i = 0; i < values.length; i++) {
                valueStr = i == values.length - 1 ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            map.put(key, valueStr);
        }
        map.remove("sign_type");
        //开始验签
        try {
            boolean rsaCheckV2 = AlipaySignature.rsaCheckV2(map, Configs.getAlipayPublicKey(), "UTF-8", Configs.getSignType());
            if (!rsaCheckV2) {
                log.warn("遭到恶意请求!");
                return "恶意请求将提交网警处理!";
            }
        } catch (AlipayApiException e) {
            log.error("支付宝验签失败", e);
            throw new BusinessException(ReturnCode.ERROR, "支付宝验签失败");
        }
        //判断是否为重复通知
        String out_trade_no = map.get("out_trade_no");
        Order callBackOrder = orderMapper.selectByOrderNo(Long.valueOf(out_trade_no));
        if (callBackOrder == null) {
            log.info(ReturnCode.ALIPAY_CALLBACK_ORDER_NOT_EXIST.getMsg());
            return "false";
        }
        if (callBackOrder.getStatus() > Constant.OrderStatus.ORDER_PAYED.getStatusCode()) {
            log.info(ReturnCode.ALIPAY_CALLBACK_REPETOR.getMsg());
            return "false";
        }
        //判断total_amount是否确实为该订单的实际金额
        if (new BigDecimal(map.get("total_amount")).compareTo(callBackOrder.getPayment()) != 0) {
            log.info(ReturnCode.ALIPAY_CALLBACK_AMOUNT_NOT_EQUAL.getMsg());
            return "false";
        }
        String tradeStatus = map.get("trade_status");
        String tradeNo = map.get("trade_no");
        if (StringUtils.equals(Constant.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS, tradeStatus)) {
            //更新Order表
            Order order = new Order();
            order.setId(callBackOrder.getId());
            order.setPaymentTime(DateTimeHelper.strToDate(DateTimeHelper.dateToString(new Date())));
            order.setStatus(Constant.OrderStatus.ORDER_PAYED.getStatusCode());
            orderMapper.updateByPrimaryKeySelective(order);
            //更新pay_info表
            PayInfo payInfo = new PayInfo();
            payInfo.setUserId(callBackOrder.getUserId());
            payInfo.setOrderNo(callBackOrder.getOrderNo());
            payInfo.setPayPlatform(Constant.PayPlatform.ALI_ZFB.getPlatformCode());
            payInfo.setPlatformNumber(tradeNo);
            payInfo.setPlatformStatus(tradeStatus);
            payInfoMapper.insert(payInfo);
            log.info("AlipayCallback回调成功！");
            return Constant.AlipayCallback.RESPONSE_SUCCESS;
        }
        //这里我们就不判断seller_id了，因为没有签约
        return Constant.AlipayCallback.RESPONSE_FAILED;
    }

    /**
     * 创建订单
     *
     * @param shippingId
     * @param userId
     * @return
     * @throws BusinessException
     */
    @Transactional
    @Override
    public OrderVo create(Map<Integer, Integer> productIdWithAmount, Integer shippingId, Integer userId) throws BusinessException {
        //订单入库---由cart表得到对应商品，添加至order和orderItem表中，同时清空购物车和和product减库存
        ShippingVo shippingVo = shippingService.select(shippingId, userId);
        //预减库存
        boolean reduceResult = this.reduceProductStock(productIdWithAmount);
        if (reduceResult == false) {
            throw new BusinessException(ReturnCode.PRODUCT_STOCK_REDUCE_ERROR);
        }
        List<OrderItem> orderItemList = this.assembleOrderItem(userId, productIdWithAmount);
        Order order = this.assembleOrder(userId, shippingId, getPayment(orderItemList));
        //orderItem装填OrderNo
        for (OrderItem orderItem : orderItemList) {
            orderItem.setOrderNo(order.getOrderNo());
        }
        orderMapper.insert(order);
        orderItemMapper.batchInsert(orderItemList);
        this.emptyCart(userId, productIdWithAmount);
        OrderVo orderVo = this.assembleOrderVo(order, shippingId, orderItemList);
        return orderVo;
    }

    /**
     * 获取当前购物车中已勾选商品详情视图
     *
     * @param userId
     * @return
     * @throws BusinessException
     */
    @Override
    public OrderVo getOrderCartProduct(Integer userId) throws BusinessException {
        OrderVo orderVo = new OrderVo();
        List<Cart> carts = cartMapper.selectCheckedByUserId(userId);
        List<OrderItemVo> orderItemVoList = new ArrayList<>();
        for (Cart cartItem : carts) {
            Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
            OrderItemVo orderItemVo = this.coverOrderItemVoFromCartAndProduct(cartItem, product);
            orderItemVoList.add(orderItemVo);
        }
        orderVo.setOrderItemVoList(orderItemVoList);
        orderVo.setImageHost(PropertiesHelper.getProperties("ftp.server.http.prefix"));
        orderVo.setProductTotalPrice(this.getTotalPriceFromOrderItemVoList(orderItemVoList));
        return orderVo;
    }

    /**
     * 用户：获取当前用户所有的订单信息（不包含订单内产品详情等）
     *
     * @param userId
     * @param pageSize
     * @param pageNum
     * @return
     * @throws BusinessException
     */
    @Override
    public PageInfo<OrderVo> list(Integer userId, @RequestParam(defaultValue = "10") Integer pageSize,
                                  @RequestParam(defaultValue = "1") Integer pageNum) throws BusinessException {
        PageHelper.startPage(pageNum, pageSize);
        List<Order> orderList = orderMapper.selectByUserId(userId);
        if(orderList.size() <= 0){
            throw new BusinessException(ReturnCode.ORDER_LIST_EMPTY);
        }
        List<OrderVo> orderVoList = new ArrayList<>();
        for (Order orderItem : orderList) {
            List<OrderItem> orderItemList = orderItemMapper.selectByUserIdAndOrderNo(orderItem.getUserId(), orderItem.getOrderNo());
            OrderVo orderVo = this.assembleOrderVo(orderItem, orderItem.getShippingId(), orderItemList);
            orderVoList.add(orderVo);
        }
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return pageResult;
    }

    /**
     * 管理员：分页列出所有订单列表
     *
     * @param pageSize
     * @param pageNum
     * @return
     * @throws BusinessException
     */
    @Override
    public PageInfo<OrderVo> listAll(Integer pageSize, Integer pageNum) throws BusinessException {
        PageHelper.startPage(pageNum, pageSize);
        List<Order> orderList = orderMapper.listAll();
        List<OrderVo> orderVoList = new ArrayList<>();
        for (Order orderItem : orderList) {
            List<OrderItem> orderItemList = orderItemMapper.selectByUserIdAndOrderNo(orderItem.getUserId(), orderItem.getOrderNo());
            OrderVo orderVo = this.assembleOrderVo(orderItem, orderItem.getShippingId(), orderItemList);
            orderVoList.add(orderVo);
        }
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return pageResult;
    }

    /**
     * 用户：根据订单号查询自身某个订单详情
     *
     * @param userId
     * @param orderNo
     * @return
     * @throws BusinessException
     */
    @Override
    public OrderVo detail(Integer userId, Long orderNo) throws BusinessException {
        List<OrderItem> orderItemList = orderItemMapper.selectByUserIdAndOrderNo(userId, orderNo);
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order != null && order.getUserId().intValue() != userId) {//越权问题
            throw new BusinessException(ReturnCode.USER_HAS_NO_PERMISSION);
        }
        OrderVo orderVo = this.assembleOrderVo(order, order.getShippingId(), orderItemList);
        return orderVo;
    }

    /**
     * 管理员：根据订单号获取订单详情
     *
     * @param orderNo
     * @return
     * @throws BusinessException
     */
    @Override
    public OrderVo detail(Long orderNo) throws BusinessException {
        List<OrderItem> orderItemList = orderItemMapper.selectByOrderNo(orderNo);
        Order order = orderMapper.selectByOrderNo(orderNo);
        OrderVo orderVo = this.assembleOrderVo(order, order.getShippingId(), orderItemList);
        return orderVo;
    }


    /**
     * 取消未付款的订单
     *
     * @param userId
     * @param orderNo
     * @return
     * @throws BusinessException
     */
    @Override
    public boolean cancel(Integer userId, Long orderNo) throws BusinessException {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order.getUserId().intValue() != userId.intValue()) {
            throw new BusinessException(ReturnCode.USER_HAS_NO_PERMISSION);
        }
        if (order.getStatus() >= Constant.OrderStatus.ORDER_PAYED.getStatusCode()) {
            return false;
        }
        orderMapper.updateStatusByOrderNo(orderNo, Constant.OrderStatus.ORDER_CANCLE.getStatusCode());
        log.info("---------------------订单号:{}，被取消-----------------------",order.getOrderNo());
        List<OrderItem> orderItemList = orderItemMapper.selectByOrderNo(order.getOrderNo());
        for(OrderItem orderItem:orderItemList){
            productMapper.updateStockById(orderItem.getProductId(),orderItem.getQuantity());//数据库补回
            if(null == redisTemplate.opsForValue().get("product_stock_id_" + orderItem.getProductId())){//缓存库存
                redisTemplate.opsForValue().set("product_stock_id_" + orderItem.getProductId(),(productMapper.selectByPrimaryKey(orderItem.getProductId())).getStock());//缓存库存
            }else {
                redisTemplate.opsForValue().increment("product_stock_id_" + orderItem.getProductId(),orderItem.getQuantity().intValue());//更新缓存
            }
        }
        return true;
    }

    /**
     * 通过OrderItemVoList计算商品总价
     *
     * @param orderItemVoList
     * @return
     */
    private BigDecimal getTotalPriceFromOrderItemVoList(List<OrderItemVo> orderItemVoList) {
        BigDecimal totalPrice = new BigDecimal("0");
        for (OrderItemVo orderItemVo : orderItemVoList) {
            totalPrice = BigDecimalHelper.add(totalPrice, orderItemVo.getTotalPrice());
        }
        return totalPrice;
    }

    /**
     * 结合Cart和Product组装orderItemVo
     *
     * @param cart
     * @param product
     * @return
     * @throws BusinessException
     */
    private OrderItemVo coverOrderItemVoFromCartAndProduct(Cart cart, Product product) throws BusinessException {
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setProductId(product.getId());
        orderItemVo.setProductName(product.getName());
        orderItemVo.setProductImage(PropertiesHelper.getProperties("ftp.server.http.prefix") + product.getMainImage());
        orderItemVo.setCurrentUnitPrice(product.getPrice());
        orderItemVo.setQuantity(cart.getQuantity());
        orderItemVo.setTotalPrice(BigDecimalHelper.mul(product.getPrice(), new BigDecimal(cart.getQuantity())));
        orderItemVo.setCreateTime(DateTimeHelper.dateToString(cart.getCreateTime()));
        return orderItemVo;
    }

    /**
     * 由order、shipping、orderItem组装OrderVo
     *
     * @param order
     * @param shippingId
     * @param orderItemList
     * @return
     * @throws BusinessException
     */
    private OrderVo assembleOrderVo(Order order, Integer shippingId, List<OrderItem> orderItemList) throws BusinessException {
        Shipping shipping = shippingMapper.selectByPrimaryKey(shippingId);
        OrderVo orderVo = new OrderVo();
        orderVo.setOrderNo(order.getOrderNo());
        orderVo.setPayment(order.getPayment());
        orderVo.setPaymentType(order.getPaymentType());
        Constant.PaymentType[] paymentTypes = Constant.PaymentType.values();
        for(int i=0; i<paymentTypes.length; i++){
            if(paymentTypes[i].getStatusCode() == order.getPaymentType()){
                orderVo.setPaymentTypeMsg(paymentTypes[i].getStatusMsg());
            }
        }
        orderVo.setPostage(order.getPostage());
        orderVo.setStatus(order.getStatus());
        Constant.OrderStatus[] orderStatuses = Constant.OrderStatus.values();
        for(int i=0; i<orderStatuses.length; i++){
            if(orderStatuses[i].getStatusCode() == order.getStatus()){
                orderVo.setStatusMsg(orderStatuses[i].getStatusMsg());
            }
        }
        orderVo.setPaymentTime(DateTimeHelper.dateToString(order.getPaymentTime()));
        orderVo.setSendTime(DateTimeHelper.dateToString(order.getSendTime()));
        orderVo.setCloseTime(DateTimeHelper.dateToString(order.getCloseTime()));
        orderVo.setCreateTime(DateTimeHelper.dateToString(order.getCreateTime()));
        orderVo.setOrderItemVoList(this.assembleOrderItemVo(orderItemList));
        orderVo.setShippingId(shipping.getId());
        orderVo.setImageHost(PropertiesHelper.getProperties("ftp.server.http.prefix"));
        orderVo.setReceiverName(shipping.getReceiverName());
        orderVo.setShippingVo(this.assembleShippingVo(shipping));
        orderVo.setProductTotalPrice(order.getPayment());
        return orderVo;
    }

    /**
     * coverShippingVoFromShi
     *
     * @param shipping
     * @return
     * @throws BusinessException
     */
    private ShippingVo assembleShippingVo(Shipping shipping) throws BusinessException {
        ShippingVo shippingVo = new ShippingVo();
        shippingVo.setId(shipping.getId());
        shippingVo.setUserId(shipping.getUserId());
        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverPhone(shipping.getReceiverPhone());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverZip(shipping.getReceiverZip());
        shippingVo.setCreateTime(DateTimeHelper.dateToString(shipping.getCreateTime()));
        shippingVo.setUpdateTime(DateTimeHelper.dateToString(shipping.getUpdateTime()));
        return shippingVo;
    }

    /**
     * coverOrderItemVoFromOrderItem
     *
     * @param orderItemList
     * @return
     * @throws BusinessException
     */
    private List<OrderItemVo> assembleOrderItemVo(List<OrderItem> orderItemList) throws BusinessException {
        List<OrderItemVo> orderItemVoList = new ArrayList<>();
        for (OrderItem orderItem : orderItemList) {
            OrderItemVo orderItemVo = new OrderItemVo();
            orderItemVo.setOrderNo(orderItem.getOrderNo());
            orderItemVo.setProductId(orderItem.getProductId());
            orderItemVo.setProductName(orderItem.getProductName());
            orderItemVo.setProductImage(PropertiesHelper.getProperties("ftp.server.http.prefix") + orderItem.getProductImage());
            orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
            orderItemVo.setQuantity(orderItem.getQuantity());
            orderItemVo.setTotalPrice(orderItem.getTotalPrice());
            orderItemVo.setCreateTime(DateTimeHelper.dateToString(orderItem.getCreateTime()));
            orderItemVoList.add(orderItemVo);
        }
        return orderItemVoList;
    }


    /**
     * 根据下单商品扣库存
     *
     * @param productIdWithAmount
     * @return
     * @throws BusinessException
     */
    @Transactional
    public boolean reduceProductStock(Map<Integer, Integer> productIdWithAmount) throws BusinessException {
        for (Integer productId : productIdWithAmount.keySet()) {
            if (redisTemplate.opsForValue().get("product_stock_sellOut_id_" + productId.intValue()) != null) {//如果购买商品中只要有一件商品被标识了售罄，则直接返回下单失败
                log.info("商品号为：{}，库存已售罄,下单失败", productId);
                return false;
            }
            Integer amount = productIdWithAmount.get(productId.intValue());
            Integer productCount = (Integer) redisTemplate.opsForValue().get("product_stock_id_" + productId.intValue());
            if (productCount == null) {//如果redis中不存在，就去数据库中进行查找
                Product product = productMapper.selectByPrimaryKey(productId.intValue());
                productCount = product.getStock();
                redisTemplate.opsForValue().setIfAbsent("product_stock_id_" + productId.intValue(), productCount.intValue(), 30, TimeUnit.MINUTES);
            }
            //Product product = productMapper.selectByPrimaryKey(cart.getProductId());
            if (amount.intValue() > productCount.intValue()) {
                throw new BusinessException(ReturnCode.PRODUCT_STOCK_NOT_ENOUGH);
            }
            //redis中预减库存
            Long result = redisTemplate.opsForValue().increment("product_stock_id_" + productId.intValue(), amount.intValue() * -1);
            if (result.longValue() == 0) {
                log.info("此时商品号为：{}，库存已售罄", productId);
                redisTemplate.opsForValue().setIfAbsent("product_stock_sellOut_id_" + productId.intValue(), -1);
            } else if (result.longValue() < 0) {
                log.info("商品号为：{}，redis预扣库存失败，开始回补库存", productId.intValue());
                redisTemplate.opsForValue().increment("product_stock_id_" + productId.intValue(), amount.intValue());
                return false;
            }
        }
        return true;
    }

    /**
     * 清空购物车中已勾选的商品
     *
     * @param userId
     * @param productIdWithAmount
     */
    private void emptyCart(Integer userId, Map<Integer, Integer> productIdWithAmount) {
        List<Integer> productIdList = new ArrayList<>();
        for (Integer productId : productIdWithAmount.keySet()) {
            productIdList.add(productId);
        }
        cartMapper.deleteProduct(userId, productIdList);
    }

    /**
     * 封装Order
     *
     * @param userId
     * @param shippingId
     * @param payment
     * @return
     */
    private Order assembleOrder(Integer userId, Integer shippingId, BigDecimal payment) {
        Order order = new Order();
        order.setOrderNo(productOrderNo());
        order.setUserId(userId);
        order.setShippingId(shippingId);
        order.setPayment(payment);
        order.setPaymentType(Constant.PaymentType.OLINE_PAY_ALI.getStatusCode());
        order.setPostage(0);
        order.setStatus(Constant.OrderStatus.ORDER_NOT_PAY.getStatusCode());
        //支付时间、发货时间、交易完成和交易关闭时间在其他接口中会完善。
        return order;
    }

    /**
     * 遍历orderItem得到payment
     *
     * @param orderItemList
     * @return
     */
    private BigDecimal getPayment(List<OrderItem> orderItemList) {
        BigDecimal payment = new BigDecimal("0");
        for (OrderItem orderItem : orderItemList) {
            payment = BigDecimalHelper.add(payment, orderItem.getTotalPrice());
        }
        return payment;
    }

    private long productOrderNo() {
        return System.currentTimeMillis() + System.currentTimeMillis() % 9;
    }

    /**
     * @param userId
     * @param productIdWithAmount
     * @return
     */
    private List<OrderItem> assembleOrderItem(Integer userId, Map<Integer, Integer> productIdWithAmount) {
        List<OrderItem> orderItemList = new ArrayList<>();
        for (Integer productId : productIdWithAmount.keySet()) {
            Product product = productMapper.selectByPrimaryKey(productId);
            Integer amount = productIdWithAmount.get(productId);
            OrderItem orderItem = new OrderItem();
            orderItem.setUserId(userId);
            orderItem.setQuantity(amount);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setTotalPrice(BigDecimalHelper.mul(product.getPrice(), new BigDecimal(amount)));
            orderItemList.add(orderItem);
        }
        return orderItemList;
    }

    /**
     * 阿里支付简单打印应答
     *
     * @param response
     */
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            log.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                log.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            log.info("body:" + response.getBody());
        }
    }
}
