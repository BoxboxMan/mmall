package org.jxnu.stu.common;


import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Set;

/**
 * Constant class
 */
public class Constant {

    public static final String CURRENT_USER = "currentUser";
    public static final String USER_FORGET_TOKEN = "forgetToken";

    public static final int USER_ADMIN = 0;
    public static final int USER_ORDINARY = 1;

    public static final String LIMIT_NUM_SUCCESS = "LIMIT_NUM_SUCCESS";
    public static final String LIMIT_NUM_FAIL = "LIMIT_NUM_FAIL";

    public static final int CHECKED = 1;
    public static final int UNCHECKED = 0;

    public interface ProductListOrderBy{
        Set<String> PRICE_ASC_DESC = Sets.newHashSet("price_asc","price_desc");
    }


    public static final boolean CATEGORY_NORMAL = true;
    public static final boolean CATEGORY_ABANDON = false;

}