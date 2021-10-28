package rtx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import rfid.ConfigMsg;
import utils.PropertiesUtil;
import utils.SendUtil;
/**
 * 串口工具类
 * @author 52456
 */
public class SerialPortTools {
	private final static Logger logger = LoggerFactory.getLogger(SerialPortTools.class);
	/**
	 * 查找电脑上所有可用 com 端口
	 * @return 可用端口名称列表，没有时 列表为空
	 */
	@SuppressWarnings("unchecked")
	public static final ArrayList<String> findSystemAllComPort() {
		/**
		 * getPortIdentifiers：获得电脑主板当前所有可用串口
		 */
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
		ArrayList<String> portNameList = new ArrayList<>();
		/**
		 * 将可用串口名添加到 List 列表
		 */
		while (portList.hasMoreElements()) {
			String portName = portList.nextElement().getName();
			portNameList.add(portName);
		}
		return portNameList;
	}

	/**
	 * 打开指定的串口
	 * @param portName 端口名称，如 COM1，为 null 时，默认使用电脑中能用的端口中的第一个
	 * @param b        波特率(baudrate)，如 9600
	 * @param d        数据位（datebits），如 SerialPort.DATABITS_8 = 8
	 * @param s        停止位（stopbits），如 SerialPort.STOPBITS_1 = 1
	 * @param p        校验位 (parity)，如 SerialPort.PARITY_NONE = 0
	 * @return 打开的串口对象，打开失败时，返回 null
	 */
	public static final SerialPort openComPort(String portName, int b, int d, int s, int p)
			throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
		CommPort commPort = null;
		try {
			// 当没有传入可用的 com 口时，默认使用电脑中可用的 com 口中的第一个
			if (portName == null || "".equals(portName)) {
				List<String> comPortList = findSystemAllComPort();
				if (comPortList != null && comPortList.size() > 0) {
					portName = comPortList.get(0);
				}
			}
			// 通过端口名称识别指定 COM 端口
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
			/**
			 * open(String TheOwner, int i)：打开端口 TheOwner 自定义一个端口名称，随便自定义即可
			 * i：打开的端口的超时时间，单位毫秒，超时则抛出异常：PortInUseException if in use.
			 * 如果此时串口已经被占用，则抛出异常：gnu.io.PortInUseException: Unknown Application
			 */
			commPort = portIdentifier.open(portName, 5000);
			/**
			 * 判断端口是不是串口 public abstract class SerialPort extends CommPort
			 */
			if (commPort instanceof SerialPort) {
				SerialPort serialPort = (SerialPort) commPort;
				/**
				 * 设置串口参数：setSerialPortParams( int b, int d, int s, int p ) b：波特率（baudrate）
				 * d：数据位（datebits），SerialPort 支持 5,6,7,8 s：停止位（stopbits），SerialPort 支持 1,2,3
				 * p：校验位 (parity)，SerialPort 支持 0,1,2,3,4
				 * 如果参数设置错误，则抛出异常：gnu.io.UnsupportedCommOperationException: Invalid Parameter
				 * 此时必须关闭串口，否则下次 portIdentifier.open 时会打不开串口，因为已经被占用
				 */
				serialPort.setSerialPortParams(b, d, s, p);
				// 添加串口监听
				addListener(serialPort, new DataAvailableListener() {
					@Override
					public void dataAvailable() {
						// 读取串口中数据
//						byte[] data = getDataFromComPort(serialPort);
					}
				});
				return serialPort;
			} else {
				logger.error("当前端口 " + commPort.getName() + " 不是串口...");
			}
		} catch (NoSuchPortException e) {
			logger.error(e.getMessage(),e);
		} catch (PortInUseException e) {
			logger.error("串口 " + portName + " 已经被占用，请先解除占用...",e);
		} catch (UnsupportedCommOperationException e) {
			logger.error("串口参数设置错误，关闭串口，数据位[5-8]、停止位[1-3]、验证位[0-4]...",e);
			// 此时必须关闭串口，否则下次 portIdentifier.open 时会打不开串口，因为已经被占用
			if (commPort != null) {
				commPort.close();
			}
		}
		if(logger.isDebugEnabled()) {
        	logger.debug("打开串口 " + portName + " 失败...");
        }
		return null;
	}
	/**
	 * 从串口读取数据
	 *
	 * @param serialPort 串口对象
	 * @param
	 */
    @SuppressWarnings("unused")
	public static byte[] getDataFromComPort(SerialPort serialPort) { 
		InputStream is = null;
		byte[] bytes = null;
		try {
			 is = serialPort.getInputStream();
	         // 通过输入流对象的available方法获取数组字节长度
	         byte [] readBuffer = new byte[is.available()];
	         // 从线路上读取数据流
	         int len = 0;
	         while ((len = is.read(readBuffer)) != -1) { 
	             bytes=readBuffer;
	             is.close();
	             is = null;
	             break;
	         }
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bytes;
	}
	/**
	 * 往串口发送数据
	 *
	 * @param serialPort 串口对象
	 * @param orders      待发送数据
	 */
	public static void sendDataToComPort(SerialPort serialPort, byte[] orders) {
		OutputStream outputStream = null;
		try {
			if (serialPort != null) {
				outputStream = serialPort.getOutputStream();
				outputStream.write(orders);
//				outputStream.flush();
			} else {
				SendUtil.sendErr(ConfigMsg.DEFAULT_ERR_RTX_MSG);
				connectRtx();
			}
		} catch (IOException e) {
			logger.error("发送心跳失败",e);
			SendUtil.sendErr("发送串口心跳失败，请连接串口线");
			SerialPortTools.closeComPort(ConfigMsg.serialPort);
			ConfigMsg.serialPort=null;
			connectRtx();
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					logger.error(e.getMessage(),e);
				}
			}
		}
	}
	
	/**
	 * 连接串口
	 */
	public static void connectRtx() { 
		try {
			if(logger.isDebugEnabled()) {
	        	logger.debug("开始连接串口");
	        }
			if(ConfigMsg.serialPort==null) {
				String rtxPort=PropertiesUtil.getUrlValue(ConfigMsg.RTX_PORT);
				int baudrate=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RTX_BAUDRATE)); 
				int datebits=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RTX_DATEBITS)); 
				int stopbits=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RTX_STOPBITS)); 
				int parity=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RTX_PARITY)); 
				ConfigMsg.serialPort = openComPort(rtxPort, baudrate, datebits, stopbits, parity);
			} 
		} catch (Exception e) {
			logger.error("连接串口失败"+e.getMessage(),e);
			try {
    			int reconnect=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigMsg.RECONNECT));
        		if(logger.isDebugEnabled()) {
                	logger.debug("连接失败"+reconnect+"毫秒后重新连接串口");
                }
				Thread.sleep(reconnect);
				connectRtx();
			} catch (InterruptedException e1) { 
				logger.error("重新连接串口失败",e1);
			}
		} 
	}
	/**
	 * 关闭串口
	 * @param serialPort 待关闭的串口对象
	 */
	public static void closeComPort(SerialPort serialPort) {
		if (serialPort != null) {
			serialPort.close();
			if(logger.isDebugEnabled()) {
	        	logger.debug("关闭串口 " + serialPort.getName());
	        }
		}
	}
	 /**
     * 给串口设置监听
     * @param serialPort serialPort 要读取的串口
     * @param listener   SerialPortEventListener监听对象
     * @throws TooManyListenersException 监听对象太多
     */
    public static void setListenerToSerialPort(SerialPort serialPort, SerialPortEventListener listener) throws TooManyListenersException {
        //给串口添加事件监听
        serialPort.addEventListener(listener);
        //串口有数据监听
        serialPort.notifyOnDataAvailable(true);
        //中断事件监听
        serialPort.notifyOnBreakInterrupt(true);
    }
    /**
	 * 添加监听器
	 * @param serialPort 串口对象
	 * @param listener 串口存在有效数据监听
	 */
	public static void addListener(SerialPort serialPort, DataAvailableListener listener) {
		try {
			// 给串口添加监听器
			serialPort.addEventListener(new SerialPortListener(listener));
			// 设置当有数据到达时唤醒监听接收线程
			serialPort.notifyOnDataAvailable(true);
			// 设置当通信中断时唤醒中断线程
			serialPort.notifyOnBreakInterrupt(true);
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}
	}
    /**
	 * 串口监听
	 */
	public static class SerialPortListener implements SerialPortEventListener {
		private DataAvailableListener mDataAvailableListener;
		public SerialPortListener(DataAvailableListener mDataAvailableListener) {
			this.mDataAvailableListener = mDataAvailableListener;
		}
		@Override
		public void serialEvent(SerialPortEvent serialPortEvent) {
			switch (serialPortEvent.getEventType()) {
			case SerialPortEvent.DATA_AVAILABLE: // 1.串口存在有效数据
				if (mDataAvailableListener != null) {
					mDataAvailableListener.dataAvailable();
				}
				break;
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY: // 2.输出缓冲区已清空
				break;
			case SerialPortEvent.CTS: // 3.清除待发送数据
				break;
			case SerialPortEvent.DSR: // 4.待发送数据准备好了
				break;
			case SerialPortEvent.RI: // 5.振铃指示
				break;
			case SerialPortEvent.CD: // 6.载波检测
				break;
			case SerialPortEvent.OE: // 7.溢位（溢出）错误
				break;
			case SerialPortEvent.PE: // 8.奇偶校验错误
				break;
			case SerialPortEvent.FE: // 9.帧错误
				break;
			case SerialPortEvent.BI: // 10.通讯中断
//			    System.out.println("与串口设备通讯中断");
			    //ShowUtils.errorMessage("与串口设备通讯中断");
				break;
			default:
				break;
			}
		}
	}
	
    /**
	 * 串口存在有效数据监听
	 */
	public interface DataAvailableListener {
		/**
		 * 串口存在有效数据
		 */
		void dataAvailable();
	}
	
}
