package org.jxnu.stu.schedule;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.dao.OrderMapper;
import org.jxnu.stu.dao.pojo.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ScheduleTask {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;


    @Scheduled(cron = "0 0/1 * * * ?")
    public void closeOrderTask(){
        log.info("---------------------定时关单开始-----------------------");
        boolean getLock = false;
        try {
            //说明分布式锁没有被获取，没有被创建
            if(getLock = redisTemplate.opsForValue().setIfAbsent(Constant.DistributedLock.LOCK_ORDER_TASK,System.currentTimeMillis() + Constant.Time.LOCK_ORDER_CLOSE)){
                redisTemplate.expire(Constant.DistributedLock.LOCK_ORDER_TASK,50, TimeUnit.SECONDS);
                log.info("---------------------获取锁成功-----------------------");
                this.closeOrder();
            }else {//如果被创建了则判断是否超时
                if(redisTemplate.opsForValue().get(Constant.DistributedLock.LOCK_ORDER_TASK) == null
                        || Long.valueOf(String.valueOf(redisTemplate.opsForValue().get(Constant.DistributedLock.LOCK_ORDER_TASK))) < System.currentTimeMillis()){
                    String oldValue = String.valueOf(redisTemplate.opsForValue().get(Constant.DistributedLock.LOCK_ORDER_TASK));
                    String checkOldValue = String.valueOf(redisTemplate.opsForValue().getAndSet(Constant.DistributedLock.LOCK_ORDER_TASK, System.currentTimeMillis() + Constant.Time.LOCK_ORDER_CLOSE));
                    if(StringUtils.equals(oldValue,checkOldValue)){
                        redisTemplate.expire(Constant.DistributedLock.LOCK_ORDER_TASK,50,TimeUnit.SECONDS);
                        log.info("---------------------获取锁成功-----------------------");
                        this.closeOrder();
                    }else{
                        log.info("---------------------获取锁失败-----------------------");
                    }
                }else {
                    log.info("---------------------获取锁失败-----------------------");
                }
            }
        }catch (Exception e){
            log.error("定时关单出现错误!",e);
        }finally {
            if(getLock){
                redisTemplate.delete(Constant.DistributedLock.LOCK_ORDER_TASK);
                log.info("---------------------释放锁-----------------------");
            }
        }
        log.info("---------------------定时关单结束-----------------------");
    }

    private void closeOrder(){
        //获取所有未付款的订单
        List<Order> orders = orderMapper.listAllByStatus(Constant.OrderStatus.ORDER_NOT_PAY.getStatusCode());
        //如果一小时未付款则关闭订单
        for (Order order:orders){
            Calendar outTime = DateUtils.toCalendar(DateUtils.addHours(order.getCreateTime(),1));
            Calendar currentTime = DateUtils.toCalendar(new Date());
            if(outTime.before(currentTime)){//如果超时则关闭订单
                orderMapper.updateStatusByOrderNo(order.getOrderNo(),Constant.OrderStatus.ORDER_CLOSE.getStatusCode());
                log.info("---------------------订单号:{}，被关闭-----------------------",order.getOrderNo());
            }
        }
    }

}
