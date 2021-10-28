package socket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import camera.ReceiveRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mot.rfid.api3.READER_TYPE;
import com.mot.rfid.api3.SECURE_MODE;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import rfid.ConfigMsg;
import rfid.Main;
import rfid.RFIDBase;
import rtx.RdfSendRunnable;
import rtx.SerialPortTools;
import utils.PropertiesUtil;
import utils.SendUtil;
/**
 * SOCKET接收
 * @author 52456
 *
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
	private final static Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);
	RFIDBase rfidBase;
	/**
	 * 扫码机接收线程
	 */
	ReceiveRunnable receiveRunnable;
	/**
	 * 串口发送线程
	 */
	RdfSendRunnable rdfSendRunnable; 
	/**
	 * 心跳开关
	 */
	private boolean isPing=true;
    ExecutorService executor = new ThreadPoolExecutor(5, 5, 60L, TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(),new DefaultThreadFactory("NettyClientHandler") );
	String isPlc=PropertiesUtil.getUrlValue(ConfigMsg.IS_PLC);
	@Override
	public void channelActive(ChannelHandlerContext ctx){
		logger.info("连接socket服务端:正在连接...");
		//发送设备码注册
	    Map<String, Object> map=new HashMap<String, Object>(2);
    	map.put(ConfigMsg.ACTION,ConfigMsg.REGISTER); 
    	Map<String, Object> data=new HashMap<String, Object>(1);
		String ip=PropertiesUtil.getUrlValue(ConfigMsg.MY_IP);
    	data.put(ConfigMsg.IP,ip);  
    	map.put(ConfigMsg.DATA, data);  
    	ctx.writeAndFlush(Unpooled.copiedBuffer(JSONUtil.toJsonStr(map)+ConfigMsg.DELIMITER,
                CharsetUtil.UTF_8));
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.error("socket服务端异常断开");
    	isPing=false;
		//服务器断开时关闭串口发送
    	if(rdfSendRunnable!=null) {
    		rdfSendRunnable.stop();
    	}
		if(ConfigMsg.serialPort!=null) {
			ConfigMsg.serialPort.close();
		}
		if(receiveRunnable!=null) {
			//服务器断开时关闭相机接收
			receiveRunnable.stop();
		}
		//断线重连
		Main.initclient();
	    super.channelInactive(ctx);
	}

	/**
	 * SOCKET接收消息
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object arg1) throws Exception {
	    //修改客户端后
        String str=JSONUtil.toJsonStr(arg1);
		logger.info(String.format("接收到消息：%s",str));
		JSONObject obj=JSONUtil.parseObj(str);
 		/*
		 * 调用设备注册接口成功后
		 * 注册连接RFID 与连接扫码机
		 */
		if(ConfigMsg.REGISTER_SUCCESS_CODE.equals(obj.getStr(ConfigMsg.RESPONSE_CODE))  &&  obj.getJSONObject(ConfigMsg.DATA)!=null && obj.getJSONObject(ConfigMsg.DATA).getStr(ConfigMsg.ACCESS_TOKEN)!=null) {
			String accessToken=obj.getJSONObject(ConfigMsg.DATA).getStr(ConfigMsg.ACCESS_TOKEN);
			ConfigMsg.ctx=ctx;
			ConfigMsg.accessToken=accessToken;
	        this.ping();
//			this.init(true);
		};
		//如果accessToken失效
		if(ConfigMsg.DEFAULT_DIS_CODE.equals(obj.getStr(ConfigMsg.RESPONSE_CODE)) &&  obj.getJSONObject(ConfigMsg.DATA)!=null && obj.getJSONObject(ConfigMsg.DATA).getStr(ConfigMsg.ACCESS_TOKEN)!=null  ) {
			String accessToken=obj.getJSONObject(ConfigMsg.DATA).getStr(ConfigMsg.ACCESS_TOKEN);
			//接口类型
			String apiType=obj.getJSONObject(ConfigMsg.DATA).getStr(ConfigMsg.API_TYPE);
			ConfigMsg.accessToken=accessToken;
			//重新提交赋码接口
			if(ConfigMsg.SECUNITY_CODE_MSG.equals(apiType)) {
				String secunityCode=obj.getJSONObject(ConfigMsg.DATA).getStr(ConfigMsg.SECUNITY_CODE);
				String qrCodeValue=obj.getJSONObject(ConfigMsg.DATA).getStr(ConfigMsg.QR_CODE);
		        SendUtil.secunityCodeMsg(qrCodeValue,secunityCode);
			}
		}
		//赋码如果成功不踢

		if(ConfigMsg.DEFAULT_UNI_SUCCESS_CODE.equals(obj.getStr(ConfigMsg.RESPONSE_CODE))  ) {
			//发送GPIO信号提升成功
			logger.info("赋码成功给信号了");
			rfidBase.setGpo(true,2);
			Thread.sleep(200);
			rfidBase.setGpo(false,2);
//			方式二
//			byte  [] in= new byte [] {0x02,0x31,0x31,0x30,0x30,0x30,0x30,0x34,0x30,0x41,0x30,0x30,0x03,0x32,0x41};
//			SerialPortTools.sendDataToComPort(ConfigMsg.serialPort,in);
		}
		/*
		 * 页面开启与关闭
		 */
		if(ConfigMsg.WEB_SOCKET.equals(obj.getStr(ConfigMsg.ACTION))) {
			if(obj.getJSONObject(ConfigMsg.DATA)==null) {
				if(rdfSendRunnable!=null) {
					logger.info("关闭启动心跳");
					rdfSendRunnable.stop();
				}
        	}else {
        		JSONObject data=obj.getJSONObject(ConfigMsg.DATA);
        		String porduceTaskId=data.getStr(ConfigMsg.PRODUCE_TASK_ID); 
        		String mode=data.getStr(ConfigMsg.PACK_MODE); 
				ConfigMsg.TASK_ID=porduceTaskId;
				ConfigMsg.MODE_ID=mode;
				if(!ConfigMsg.MODE_0.equals(isPlc)) {
					if(ConfigMsg.MODE_1.equals(ConfigMsg.MODE_ID)) { 
						//启动心跳
							if(rfidBase.isConnected && receiveRunnable.socket!=null && ConfigMsg.serialPort!=null) {
								logger.info("标签模式启动心跳");
								rdfSendRunnable=new RdfSendRunnable();
								executor.submit(rdfSendRunnable);
								rfidBase.setRdfSendRunnable(rdfSendRunnable);
								receiveRunnable.setRdfSendRunnable(rdfSendRunnable);
							}else {
								connectRfid(false); 
							} 
					}else { 
						//启动心跳 
						if( receiveRunnable.socket!=null && ConfigMsg.serialPort!=null) {
							logger.info("二维模式启动心跳");
							rdfSendRunnable=new RdfSendRunnable();
							executor.submit(rdfSendRunnable);
							receiveRunnable.setRdfSendRunnable(rdfSendRunnable);
						}else {
							connectRtx(); 
						} 
					}
				}
        	}
		}
    }
	/**
	 * 启动心跳
	 */
	private void ping() { 
		Runnable ping=new Runnable() {     
			 @Override
	         public void run(){                 
	        	 while (isPing) {
	 				try {
	 					ConfigMsg.ctx.writeAndFlush(Unpooled.copiedBuffer("{\"action\":\"ping\"}" + ConfigMsg.DELIMITER, CharsetUtil.UTF_8));
	 					Thread.sleep(1000*10);
	 				} catch (InterruptedException e) {
	 					e.printStackTrace();
	 				}
	 			}
	         }
	    };
	    executor.submit(ping);
    }
 
	/**
	 * 初始化设备
	 * @param isRtx
	 */
	public  void init(Boolean isRtx) {
		connectRfid(isRtx); 
	}

	/**
	 * 连接RFID
	 */
	public  void connectRfid(Boolean isRtx) { 
		//注册RFIDBase
		logger.info("开始连接RFIDBase");
		String ip=PropertiesUtil.getUrlValue(ConfigMsg.RFID_IP);
		int port=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RFID_PORT));
		String user=PropertiesUtil.getUrlValue(ConfigMsg.RFID_USER);
		String pwd=PropertiesUtil.getUrlValue(ConfigMsg.RFID_PWD);
    	
		RFIDBase rfidBase=new RFIDBase(user);
		Bootstrap b = new Bootstrap();
		b.group(new NioEventLoopGroup());
        b.channel(NioSocketChannel.class);
        b.handler(new NettyClientHandler()); 
        boolean connectToReader=rfidBase.connectToReader(ip, port);
    	if (connectToReader) {
			logger.info("RFIDBase连接成功");
			rfidBase.loginInfo.setHostName(ip);
			rfidBase.loginInfo.setUserName(user);
			rfidBase.loginInfo.setPassword(pwd);
			rfidBase.loginInfo.setSecureMode(SECURE_MODE.HTTP);
			rfidBase.loginInfo.setForceLogin(true);
			if (rfidBase.loginIn(READER_TYPE.FX))
			{
				logger.info("RFIDBase登录成功");
				this.rfidBase=rfidBase;
				if(isRtx) {
					connectRtx();
				}else {
					rdfSendRunnable=new RdfSendRunnable();
				    executor.submit(rdfSendRunnable);
					rfidBase.setRdfSendRunnable(rdfSendRunnable);
					receiveRunnable.setRdfSendRunnable(rdfSendRunnable);
				}
			}
		}else {
			try {
    			int reconnect=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RECONNECT));
    			logger.error("连接失败"+reconnect+"毫秒后重新连接RFID");
				Thread.sleep(reconnect);
				connectRfid(isRtx);
			} catch (InterruptedException e) { 
				logger.error("休息重连RFID异常",e.getMessage());
			}
		}
	}
	/**
	 * 连接串口
	 */
	public void connectRtx() {
		if(ConfigMsg.MODE_0.equals(isPlc)) {
			connectCamera(); 
		}else {
			try {
				logger.info("开始连接串口");
				if(ConfigMsg.serialPort==null) {
					String rtxPort=PropertiesUtil.getUrlValue(ConfigMsg.RTX_PORT);
					int baudrate=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RTX_BAUDRATE)); 
					int datebits=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RTX_DATEBITS)); 
					int stopbits=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RTX_STOPBITS)); 
					int parity=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RTX_PARITY)); 
					ConfigMsg.serialPort = SerialPortTools.openComPort(rtxPort, baudrate, datebits, stopbits, parity);
				}
				if(ConfigMsg.serialPort!=null) {
					logger.info("串口连接成功");
					connectCamera();
				}else {
					int reconnect=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RECONNECT));
					logger.error("串口连接失败"+reconnect+"毫秒后重新连接串口");
					Thread.sleep(reconnect);
					connectRtx();
				} 
			} catch (Exception e) {
				logger.error("连接串口失败"+e.getMessage());
			} 
		}
	}
	/**
	 * 连接相机
	 */
	public void connectCamera() { 
		receiveRunnable=new ReceiveRunnable();
	    executor.submit(receiveRunnable);
	}
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		 
		 
	}
}
