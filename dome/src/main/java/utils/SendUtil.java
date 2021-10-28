package utils;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.hutool.json.JSONUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import rfid.ConfigMsg;

/**
 * socket 发送util
 * @author 52456
 *
 */
public class SendUtil {
	private final static Logger logger = LoggerFactory.getLogger(SendUtil.class);
	/**
	 * 发送页面异常
	 * @param msg
	 */
	public static void sendErr(String msg) {
		Map<String, Object> map=new HashMap<String, Object>(3);
    	map.put(ConfigMsg.ACTION, ConfigMsg.WEB_ERR);
    	map.put(ConfigMsg.ACCESS_TOKEN, ConfigMsg.accessToken);
    	Map<String, Object> data=new HashMap<String, Object>(2);
    	data.put(ConfigMsg.ERR_CODE,ConfigMsg.DEFAULT_ERR_CODE); 
    	data.put(ConfigMsg.ERR_MSG,msg); 
    	map.put(ConfigMsg.DATA, data);
    	ConfigMsg.ctx.writeAndFlush(Unpooled.copiedBuffer(JSONUtil.toJsonStr(map)+ConfigMsg.DELIMITER,
                CharsetUtil.UTF_8)); 
	}
	/**
	 * 提交服务端修改赋码
	 * @param qrCode
	 * @param strWriteData
	 */
	public static void secunityCodeMsg(String qrCode,String strWriteData) {
		Map<String, Object> map=new HashMap<String, Object>(3);
    	map.put(ConfigMsg.ACTION, ConfigMsg.SECUNITY_CODE_MSG);
    	map.put(ConfigMsg.ACCESS_TOKEN, ConfigMsg.accessToken);
    	Map<String, Object> data=new HashMap<String, Object>(4);
    	data.put(ConfigMsg.SECUNITY_CODE, strWriteData);
    	data.put(ConfigMsg.QR_CODE, qrCode);
    	data.put(ConfigMsg.PRODUCE_TASK_ID, ConfigMsg.TASK_ID); 
    	data.put(ConfigMsg.PACK_MODE, ConfigMsg.MODE_ID);
    	map.put(ConfigMsg.DATA, data); 
    	ConfigMsg.ctx.writeAndFlush(Unpooled.copiedBuffer(JSONUtil.toJsonStr(map)+ConfigMsg.DELIMITER,
                CharsetUtil.UTF_8));
		logger.info("客户端发送数据:"+JSONUtil.toJsonStr(map));
	} 
}
