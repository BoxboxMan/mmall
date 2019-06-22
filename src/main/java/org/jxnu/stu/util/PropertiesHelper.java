package org.jxnu.stu.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ResourceBundle;

@Component
public class PropertiesHelper {

    private static String profile;

    private static ResourceBundle resourceBundle = null;

    @Value("${spring.profiles.active}")
    public void setProfile(String profile){
        PropertiesHelper.profile = profile;
    }

    @PostConstruct
    private void initResourceBundle(){
        resourceBundle = ResourceBundle.getBundle("application-" + profile);
    }

    public static String getProperties(String key){
        return resourceBundle.getString(key);
    }
}
