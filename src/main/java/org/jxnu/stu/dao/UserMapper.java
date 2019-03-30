package org.jxnu.stu.dao;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.jxnu.stu.dao.pojo.User;
import org.jxnu.stu.dao.pojo.UserExample;

public interface UserMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_user
     *
     * @mbg.generated Sat Mar 30 17:46:04 CST 2019
     */
    long countByExample(UserExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_user
     *
     * @mbg.generated Sat Mar 30 17:46:04 CST 2019
     */
    int deleteByExample(UserExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_user
     *
     * @mbg.generated Sat Mar 30 17:46:04 CST 2019
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_user
     *
     * @mbg.generated Sat Mar 30 17:46:04 CST 2019
     */
    int insert(User record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_user
     *
     * @mbg.generated Sat Mar 30 17:46:04 CST 2019
     */
    int insertSelective(User record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_user
     *
     * @mbg.generated Sat Mar 30 17:46:04 CST 2019
     */
    List<User> selectByExample(UserExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_user
     *
     * @mbg.generated Sat Mar 30 17:46:04 CST 2019
     */
    User selectByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_user
     *
     * @mbg.generated Sat Mar 30 17:46:04 CST 2019
     */
    int updateByExampleSelective(@Param("record") User record, @Param("example") UserExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_user
     *
     * @mbg.generated Sat Mar 30 17:46:04 CST 2019
     */
    int updateByExample(@Param("record") User record, @Param("example") UserExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_user
     *
     * @mbg.generated Sat Mar 30 17:46:04 CST 2019
     */
    int updateByPrimaryKeySelective(User record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_user
     *
     * @mbg.generated Sat Mar 30 17:46:04 CST 2019
     */
    int updateByPrimaryKey(User record);
}