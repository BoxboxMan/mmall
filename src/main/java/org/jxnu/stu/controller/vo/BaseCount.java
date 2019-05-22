package org.jxnu.stu.controller.vo;

public class BaseCount {

    private Integer userCount;
    private Integer productCount;
    private Integer orderCount;
    private Integer OlinePeopleCount;

    public Integer getUserCount() {
        return userCount;
    }

    public void setUserCount(Integer userCount) {
        this.userCount = userCount;
    }

    public Integer getProductCount() {
        return productCount;
    }

    public void setProductCount(Integer productCount) {
        this.productCount = productCount;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Integer getOlinePeopleCount() {
        return OlinePeopleCount;
    }

    public void setOlinePeopleCount(Integer olinePeopleCount) {
        OlinePeopleCount = olinePeopleCount;
    }
}
