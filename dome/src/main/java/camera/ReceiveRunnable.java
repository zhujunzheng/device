package camera;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rfid.ConfigMsg;
import rtx.RdfSendRunnable;
import utils.PropertiesUtil;
import utils.SendUtil;

/**
 * 连接相机线程
 * @author 52456
 *
 */
public class ReceiveRunnable implements Runnable{
	private final static Logger logger = LoggerFactory.getLogger(ReceiveRunnable.class);
	private String connectionErrMsg="Connection reset";
	private String softwareErrMsg="Software caused connection abort: send";

	private volatile boolean isStopped = false;
	public volatile Socket socket;
    ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1,
            new BasicThreadFactory.Builder().namingPattern("example-schedule-pool-%d").daemon(true).build());

    private TimerTask task;
    private InputStream inputStream;
    /**
     * 心跳发送
     */
  	public volatile RdfSendRunnable rdfSendRunnable;

  	public RdfSendRunnable getRdfSendRunnable() {
  		return rdfSendRunnable;
  	}

  	public void setRdfSendRunnable(RdfSendRunnable rdfSendRunnable) {
  		this.rdfSendRunnable = rdfSendRunnable;
  	}

    public ReceiveRunnable() {
		super();
	}
	public void stop(){
        isStopped = true;
    }
	@Override
    public void run() {
		//必要
		logger.info("连接相机");
		while(!isStopped) {
			connectCamera();
		}
    }
	/**
	 * 连接相机
	 */
	public void connectCamera() {
		try {
			String ip=PropertiesUtil.getUrlValue(ConfigMsg.CAMERA_IP);
			int port=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.CAMERA_PORT));
        	SocketAddress socketAddress = new InetSocketAddress(ip,port);
        	socket=new Socket();
        	socket.connect(socketAddress, 2000);
        	socket.setSoLinger(true, 0);
        	//发送心跳包
    		if (socket.isConnected()) {
				logger.info("连接相机成功");
    			sendBeatData();
    		}
    		inputStream = socket.getInputStream();
            byte[] buf = new byte[1024];
            int line = 0;
            while((line=inputStream.read(buf))!=-1){
                String str =new String(buf,0,line);
                String code=str.equals(ConfigMsg.NO_READ)?ConfigMsg.NO_READ:str.substring(str.lastIndexOf("/")+1);

				logger.info("识别到:"+str);
				ConfigMsg.CODE=code;
            }
        }catch (SocketException e) {
        	logger.error("连接相机失败:"+e.getMessage());
        	if(connectionErrMsg.equals(e.getMessage())) {
        		SendUtil.sendErr(ConfigMsg.DEFAULT_ERR_CAMERA_REST_MSG);
        		restartConnect();
        	}else {
        		SendUtil.sendErr(ConfigMsg.DEFAULT_ERR_CAMERA_MSG);
            	restartConnect();
        	}
        }catch (IOException e) {
        	logger.error("连接相机失败:"+e.getMessage());
        	SendUtil.sendErr(ConfigMsg.DEFAULT_ERR_CAMERA_MSG);
        	restartConnect();
        }
	}


	/**
	 * 释放资源
	 */
	 private void releaseSocket() {
		 if (task != null) {
             task.cancel();
             task = null;
         }
		 executorService.shutdown();
         if (inputStream != null) {
             try {
            	 inputStream.close();
             } catch (IOException e) {
                 logger.error("相机-socket-释放输入流失败",e);
             }
             inputStream = null;
         }
         if (socket != null) {
             try {
                 socket.close();
             } catch (IOException e) {
                 logger.error("相机-socket-释放socket失败",e);
             }
             socket = null;
         }
	 }

	/**
	  * 定时发送数据
     */
    private void sendBeatData() {
        if (task == null) {
            task = new TimerTask() {
                @Override
                public void run() {
                    try {
                    	socket.sendUrgentData(1);
                    }  catch (SocketException e) {
                    	logger.error("相机-socket-发送心跳包失败"+e.getMessage(),e.getMessage());
                    	if(softwareErrMsg.equals(e.getMessage())) {
                    		SendUtil.sendErr(ConfigMsg.DEFAULT_ERR_CAMERA_REST_MSG);
                    		restartConnect();
                    	}
                    }  catch (Exception e) {
                    	logger.error("相机-socket-发送心跳包失败",e.getMessage());
                    	restartConnect();
                    }
                }
            };
        }
        executorService.schedule(task,1000, TimeUnit.MILLISECONDS);
    }

    /**
     * 异常重新连接
     */
    public void  restartConnect() {
    	try {
        	if(rdfSendRunnable!=null) {
        		rdfSendRunnable.stop();
        	}
			int reconnect=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RECONNECT));
    		if(logger.isDebugEnabled()) {
            	logger.debug("连接断开"+reconnect+"毫秒后重新连接相机");
            }
    		releaseSocket();
			Thread.sleep(reconnect);
			connectCamera();
		} catch (InterruptedException e1) {
			logger.error("重新连接相机失败",e1);
		}
    }

}
