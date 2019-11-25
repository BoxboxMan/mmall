package org.jxnu.stu.common;

public enum ReturnCode implements CommonReturn{
    //start from 10000 to 20000 be used to description Common Error
    ERROR(10000,"发生未知错误"),
    SUCCESS(10001,"成功！"),
    PARAMETER_VALUE_ERROR(10005,"参数异常"),
    COVER_ERROR(10006,"类型转换错误"),

    //start from 20000 to 30000 be used to description User Error
    USER_NOT_EXIST(20001,"用户不存在"),
    USER_ALREADY_EXIST(20002,"用户已经存在"),
    USER_LOGIN_FAILED(20003,"用户名或密码错误"),
    USER_LOGIN_SUCCESS(20004,"登陆成功！"),
    USER_REGISTE_FAILD(20005,"用户注册失败"),
    USER_NOT_LOGIN(20006,"用户没有登陆"),
    USER_NOT_HAS_QUESTION(20007,"用户不存在或用户没有设置密保问题"),
    USER_ANSWER_WRONG(20008,"密保问题或答案错误"),
    USER_FORGETTOKEN_ERROR(20009,"重置密码forgetToken不一致"),
    USER_RESET_PASSWORD_ERROR(20010,"用户重置密码失败"),
    USER_INFO_UPDATE_ERROR(20011,"用户信息更新失败"),
    USER_HAS_NO_PERMISSION(20012,"用户无权限"),

    //start from 30000 to 40000 be used to description Category Error
    CATEGORY_NOT_EXIST(30000,"该商品类别信息不存在"),
    CATEGORY_ADD_ERROR(30001,"添加商品类别失败"),
    CATEGORY_UPDATE_ERROR(30002,"更新商品类别信息失败"),



    //start from 40000 to 50000 be used to description product info
    PRODUCT_NOT_EXIST(40000,"商品不存在"),
    PRODUCT_UPDATE_ERROR(40001,"更新商品信息失败"),
    PRODUCT_STOCK_NOT_ENOUGH(40002,"商品库存不足"),
    PRODUCT_IMAGE_UPLOAD_FAILD(40003,"商品图片上传失败"),
    PRODUCT_STOCK_REDUCE_ERROR(40004,"扣减库存异常"),

    //start from 50000 to 60000 be used to description cart info
    CART_ADD_ERROR(50001,"购物车添加商品失败"),
    CART_UPDATE_ERRIR(50002,"更新购物车商品信息失败"),
    CART_DELETE_ERROR(50003,"购物车删除商品失败"),
    CART_CLEAR_FAILD(50004,"情况购物车失败"),
    CART_IS_EMPTY(50005,"购物车为空"),

    //start from 60000 to 70000 be used to description shipping info
    SHIPPING_ADD_ERROR(60000,"添加收获地址信息失败"),
    SHIPPING_DEL_ERROR(60001,"删除收货地址信息失败"),
    SHIPPING_UPDATE_ERROR(60002,"更新收货地址信息失败"),
    SHIPPING_NOT_EXIST(60003,"收货地址信息不存在"),


    //start from 70000 to 80000 be used to description order info
    ORDER_NOT_EXIST(70000,"订单信息不存在"),
    ORDER_STATUS_NOT_EXIST(70001,"该订单状态未被定义"),
    ORDER_CREATE_FAILD(70002,"创建订单失败"),
    ORDER_CANCEL_FAILD(70003,"取消订单失败"),
    ORDER_DELIVER_FAILD(70004,"订单发货失败"),
    ORDER_HAS_NO_PRODUCT(70005,"订单中不存在商品"),

    //start from 80000 to 90000 be used to description alipay error info
    ALIPAY_CALLBACK_REPETOR(80000,"阿里回调重复"),
    ALIPAY_CALLBACK_ORDER_NOT_EXIST(80001,"阿里回调订单不存在"),
    ALIPAY_CALLBACK_AMOUNT_NOT_EQUAL(80002,"阿里回调金额与订单金额不匹配"),


    //the encode string is null

    ;
    private int code;
    private String errMsg;
    private ReturnCode(int errCode,String errMsg){
        this.code = errCode;
        this.errMsg = errMsg;
    }
    @Override
    public int getCode() {
        return code;
    }
    @Override
    public String getMsg() {
        return errMsg;
    }
    @Override
    public ReturnCode setMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }

}
