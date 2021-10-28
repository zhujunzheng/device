package rfid;

import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;

import gnu.io.SerialPort;
import io.netty.channel.ChannelHandlerContext;
  
/**
 * 常量
 * @author 52456
 *
 */
public class ConfigMsg {
	public static String RFID=null;
	public static String CODE=null;
	/**
	 * 任务ID
	 */
	public static String TASK_ID=null;
	/**
	 * 模式ID
	 */
	public static String MODE_ID=null;
	/**
	 * socket客户端
	 */
	public static ChannelHandlerContext ctx=null; 
	public static SerialPort serialPort=null;
	public static String accessToken=null;
	/**
	 * 消息终止符
	 */
	public final static String DELIMITER = "$#";

	
	public final static String DEFAULT_DIS_CODE = "0003";

 

	public final static String REGISTER_SUCCESS_CODE = "0008";

	public final static String DEFAULT_UNI_SUCCESS_CODE = "7000";

	
	public final static String DEFAULT_ERR_CODE = "0010";

	public final static String DEFAULT_ERR_CAMERA_REST_MSG = "连接相机失败,请重启机器!";
	public final static String DEFAULT_ERR_CAMERA_MSG = "网线断开,请连接线后重新选择任务待黄灯熄灭，按绿色按钮";

	public final static String DEFAULT_ERR_RFID_MSG = "RFID读写器断开,请连接线后待黄灯熄灭，按绿色按钮";
	public final static String DEFAULT_ERR_RTX_MSG = "串口线断开,请连接线后重新选择任务待黄灯熄灭，按绿色按钮";
	

	/**
	 * 提交提交赋码结果接口
	 */
	public final static String SECUNITY_CODE_MSG="secunityCodeMsg"; 
	/**
	 * 注册设备接口
	 */
	public final static String REGISTER="register";
	/**
	 * webSocket传送
	 */
	public final static String WEB_SOCKET="webSocket"; 
	/**
	 * webSocket传送错误
	 */
	public final static String WEB_ERR="webErr"; 

	/**
	 * token令牌
	 */
	public final static String ACCESS_TOKEN="accessToken"; 
	/**
	 * 任务id
	 */
	public final static String PRODUCE_TASK_ID="produceTaskId"; 
	/**
	 * 运行模式
	 */
	public final static String PACK_MODE="mode"; 

	/**
	 * 扫码结果
	 */
	public final static String QR_CODE="qrCode";
	/**
	 * 暗码
	 */
	public final static String SECUNITY_CODE="secunityCode";

	/**
	 * 接口类型
	 */
	public final static String API_TYPE="apiType";
	 
	/**
	 * 入参接口参数
	 */
	public final static String ACTION="action";
	/**
	 * 入参数据参数 
	 */
	public final static String DATA="data";
	/**
	 * 本地IP
	 */
	public final static String IP="ip";

	/**
	 * 回参编码
	 */
	public final static String RESPONSE_CODE="responseCode"; 

	/**
	 * 服务端IP
	 */
	public final static String SERVICE_IP="service.socket.ip"; 
	/**
	 * 服务端端口
	 */
	public final static String SERVICE_PORT="service.socket.port"; 
	/**
	 * 相机IP
	 */
	public final static String CAMERA_IP="camera.ip"; 
	/**
	 * 相机端口
	 */
	public final static String CAMERA_PORT="camera.port"; 
	/**
	 * RFID-IP
	 */
	public final static String RFID_IP="rfid.ip"; 
	/**
	 * RFID-端口
	 */
	public final static String RFID_PORT="rfid.port"; 
	/**
	 * RFID-用户
	 */
	public final static String RFID_USER="rfid.user"; 
	/**
	 * RFID-密码
	 */
	public final static String RFID_PWD="rfid.pwd";
	/**
	 * 串口-端口
	 */
	public final static String RTX_PORT="rtx.port"; 
	/**
	 * 串口-波特率
	 */
	public final static String RTX_BAUDRATE="rtx.baudrate"; 
	/**
	 * 串口-数据位 
	 */
	public final static String RTX_DATEBITS="rtx.datebits"; 
	/**
	 * 串口-停止位
	 */
	public final static String RTX_STOPBITS="rtx.stopbits"; 
	/**
	 * 串口-校验位
	 */
	public final static String RTX_PARITY="rtx.parity"; 
	/**
	 * 本机Ip
	 */
	public final static String MY_IP="my.ip"; 
	/**
	 * 重连时间
	 */
	public final static String RECONNECT="my.reconnect.time";
	
	/**
	 * 是否开启PLC心跳
	 */
	public final static String IS_PLC="my.plc"; 
	
	

	/**
	 * 错误消息CODE
	 */
	public final static String ERR_CODE="errCode"; 
	/**
	 * 错误消息内容
	 */
	public final static String ERR_MSG="errMsg"; 


	
	

	/**
	 * 运行模式
	 * 仅二维码模式
	 */
	public final static String MODE_0="0";
	/**
	 * 二维码加标签模式
	 */
	public final static String MODE_1="1";

	
 
	/**
	 * 读码失败标识
	 */
	public final static String NO_READ="NoRead";
	/**
	 * 读码失败标识
	 */
	public final static String NO_RFID_READ="NoRFIDRead";
	

	public final static int VALUE_1=1;
	public final static int VALUE_2=2;
	public final static int VALUE_3=3;
}
