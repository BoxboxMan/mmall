package org.jxnu.stu.mq;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.controller.portal.OrderController;
import org.jxnu.stu.service.OrderService;
import org.jxnu.stu.util.JsonHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MqProducer {

    private DefaultMQProducer producer;

    private TransactionMQProducer transactionMQProducer;

    @Value("${mq.nameserver.add}")
    private String nameserverAddr;
    @Value("${mq.topicname}")
    private String topicName;
    @Autowired
    private OrderService orderService;

    @PostConstruct
    public void init() throws MQClientException {
        producer = new DefaultMQProducer("producer");
        producer.setNamesrvAddr(nameserverAddr);
        producer.start();
        //初始化事务型生产者
        transactionMQProducer = new TransactionMQProducer("transaction_producer");
        transactionMQProducer.setNamesrvAddr(nameserverAddr);
        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                Integer shippingId = (Integer) ((Map)arg).get("shippingId");
                Integer userId = (Integer) ((Map)arg).get("userId");
                Map<Integer,Integer> productIdWithAmount = (Map<Integer, Integer>) ((Map)arg).get("productIdWithAmount");
                try {
                    OrderController.temp.set(orderService.create(productIdWithAmount,shippingId,userId));
                } catch (BusinessException e) {
                    e.printStackTrace();
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                return null;
            }
        });
        transactionMQProducer.start();
    }


    /**
     * 发送事务型消息
     * @param productIdWithAmount 购买的商品ID对应的购买数量
     * @param shippingId
     * @param userId
     * @return
     * @throws UnsupportedEncodingException
     */
    public boolean transactionAsyncReduceStock(Map<Integer,Integer> productIdWithAmount,Integer shippingId, Integer userId) throws UnsupportedEncodingException {
        Map<Integer,Integer> bodyMap = productIdWithAmount;
        Map<String,Object> args = new HashMap<>();
        args.put("productIdWithAmount",productIdWithAmount);
        args.put("shippingId",shippingId);
        args.put("userId",userId);
        Message message = new Message(topicName,JSON.toJSON(bodyMap).toString().getBytes("UTF-8"));
        TransactionSendResult sendResult = null;
        try {
            sendResult = transactionMQProducer.sendMessageInTransaction(message, args);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }
        if(sendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE){
            return false;
        }else if(sendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE){
            return true;
        }else{
            return false;
        }
    }


    /**
     * 发送扣减库存消息
     * @param productId
     * @param amount
     * @return
     * @throws InterruptedException
     * @throws RemotingException
     * @throws MQClientException
     * @throws MQBrokerException
     */
    /*public boolean asyncReduceStock(Integer productId,Integer amount) {
        Map<String,Object> bodyMap = new HashMap<>();
        bodyMap.put("productId",productId);
        bodyMap.put("amount",amount);
        Message message = new Message(topicName, JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        try {
            producer.send(message);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        } catch (RemotingException e) {
            e.printStackTrace();
            return false;
        } catch (MQBrokerException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }*/



}
