package rfid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
/**
 * util
 * @author 52456
 *
 */
public class Utils {
	private final static Logger logger = LoggerFactory.getLogger(Utils.class);

	/**
	 * 16进制字符串转十进制字节数组
	 * 这是常用的方法，如某些硬件的通信指令就是提供的16进制字符串，发送时需要转为字节数组再进行发送
	 *
	 * @param strSource 16进制字符串，如 "455A432F5600"，每两位对应字节数组中的一个10进制元素
	 *                  默认会去除参数字符串中的空格，所以参数 "45 5A 43 2F 56 00" 也是可以的
	 * @return 十进制字节数组, 如 [69, 90, 67, 47, 86, 0]
	 */
	public static byte[] hexString2Bytes(String strSource) {
		if (strSource == null || "".equals(strSource.trim())) {
			if(logger.isDebugEnabled()) {
	        	logger.debug("hexString2Bytes 参数为空，放弃转换.");
	        }
			return null;
		}
		strSource = strSource.replace(" ", "");
		int l = strSource.length() / 2;
		byte[] ret = new byte[l];
		for (int i = 0; i < l; i++) {
			ret[i] = Integer.valueOf(strSource.substring(i * 2, i * 2 + 2), 16).byteValue();
		}
		return ret;
	}

//    private byte[] HexStringToByteArray(String s)
//    {
//        s = s.replace(" ", "");
//        byte[] buffer = new byte[s.length() / 2];
//        for (int i = 0; i < s.length(); i += 2)
//            buffer[i / 2] = (byte)Convert.ToByte(s.substring(i, 2), 16);
//        Character.
//        return buffer;
//    }

	/**
	 * Convert hex string to byte[]
	 * 
	 * @param hexString the hex string
	 * @return byte[]
	 */
	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] buffer = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			buffer[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return buffer;
	}

	/**
	 * Convert char to byte
	 * 
	 * @param c char
	 * @return byte
	 */
	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

	public static String byteArrayToHexString(byte[] data) {
		StringBuilder sb = new StringBuilder(data.length * 3);
		String strHex = "";
		for (byte b : data) {
			strHex = Integer.toHexString(b & 0xFF);
		}
        // 每个字节由两个字符表示，位数不够，高位补0
		sb.append((strHex.length() == 1) ? "0" + strHex : strHex);

		return sb.toString().toUpperCase();
	}

	public static String byteArrayToHexString(byte[] data, int leng) {
		StringBuilder sb = new StringBuilder(data.length * 3);
		String strHex = "";
		for (int i = 0; i < leng; i++) {
			byte b = data[i];
			strHex = Integer.toHexString(b & 0xFF);
//            sb.Append(Convert.ToString(b, 16).PadLeft(2, '0'));
			sb.append((strHex.length() == 1) ? "0" + strHex : strHex);
		}
		return sb.toString().toUpperCase();
	}

	public static String byteArrayToHexString(byte[] data, int offset, int leng) {
		StringBuilder sb = new StringBuilder(data.length * 3);
		String strHex = "";
		for (int i = offset; i < offset + leng; i++) {
			byte b = data[i];
			strHex = Integer.toHexString(b & 0xFF);
//          sb.Append(Convert.ToString(b, 16).PadLeft(2, '0'));
			sb.append((strHex.length() == 1) ? "0" + strHex : strHex);
		}
		return sb.toString().toUpperCase();
	}

	public static int byteStringArrayToInt16(byte[] data, int offset) {
		int b1, b2, b3, b4; 
        byte d = (byte)0x39; 
		if (data[offset] <= d) { 
			b2 = data[offset] - 0x30;
		}else {
			b2 = data[offset] - 0x41 + 0x0A;
		}

		if (data[offset + 1] <= d) {
			b1 = data[offset + 1] - 0x30;
		}else {
			b1 = data[offset + 1] - 0x41 + 0x0A;
		}
		if (data[offset + ConfigMsg.VALUE_2] <= d) {
			b4 = data[offset + 2] - 0x30;
		}else {
			b4 = data[offset + 2] - 0x41 + 0x0A;
		}
		if (data[offset + ConfigMsg.VALUE_3] <= d) {
			b3 = data[offset + 3] - 0x30;
		}else {
			b3 = data[offset + 3] - 0x41 + 0x0A;
		}
//        return (UInt16)(b1+ b2*0x10+b3*0x100+b4*0x1000);
		int rst = b1 + b2 * 0x10 + b3 * 0x100 + b4 * 0x1000;
		return rst & 0xFFFF;
	}

}
