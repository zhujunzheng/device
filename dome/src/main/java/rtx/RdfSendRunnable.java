package rtx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rfid.ConfigMsg;
/**
 * PLC心跳线程
 * @author 52456
 *
 */
public class RdfSendRunnable implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(RdfSendRunnable.class);
 
	private volatile boolean isStopped = false;
	private final byte  [] in= new byte [] {0x02,0x45,0x31,0x30,0x30,0x41,0x30,0x30,0x30,0x32,0x30,0x30,0x30,0x30,0x03,0x39,0x43};

	public RdfSendRunnable() {
		super(); 
	}
	public void stop() {
		isStopped = true;
	}
	@Override
	public void run() {
		while (!isStopped) {
			try {
				SerialPortTools.sendDataToComPort(ConfigMsg.serialPort,in);
				Thread.sleep(500);
			} catch (InterruptedException e) {
				logger.error("发送串口心跳失败"+e.getMessage(), e);
			} 
		}
	}
	 
}
