package rtx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

/**
 * 测试类
 * @author 52456
 *
 */
public class Test {

	private final static Logger logger = LoggerFactory.getLogger(Test.class);
	public static void main(String[] args) {
		try {
			SerialPort a= SerialPortTools.openComPort("COM3", 38400, 8, 1, 0);
			while (true) {
				Thread.sleep(500);
				byte x[] = new byte [] {0x02,0x45,0x31,0x30,0x30,0x41,0x30,0x30,0x30,0x32,0x30,0x30,0x30,0x30,0x03,0x39,0x43};
				SerialPortTools.sendDataToComPort(a,x);
			}
		} catch (NoSuchPortException e) {
			logger.error(e.getMessage(),e);
		} catch (PortInUseException e) {
			logger.error(e.getMessage(),e);
		} catch (UnsupportedCommOperationException e) {
			logger.error(e.getMessage(),e);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(),e);
		}
	}
}
