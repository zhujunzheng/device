package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 动态获取配置
 * @author 52456
 *
 */
@Slf4j
public class PropertiesUtil {
	private final static String PROPERTIES=System.getProperty(SystemUtil.USER_DIR)+"/application.properties";
	public static String getUrlValue(String urlName) {
		Properties prop = new Properties();
        try {
			prop.load(new FileInputStream(PROPERTIES));
		} catch (IOException e) {
			log.error("获取配置失败",e);
		}
        return prop.getProperty(urlName);
    }
}
