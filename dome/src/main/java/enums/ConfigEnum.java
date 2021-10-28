package enums;

import lombok.Getter;

/**
 * 连接 枚举
 * @author zjz
 * @time 2021年9月14日 下午5:14:23
 * @version 1.0.0 
 * @description
 */

@Getter
public enum ConfigEnum {

	SOCKET_IP("service.socket.ip", "socket服务端地址"),
	SOCLET_PORT("service.socket.port", "socket服务端端口"),
	RECONNECT_TIME("my.reconnect.time", "重连时间"),
	DELIMITER("$#", "终止符号");
    private String code;
    private String msg;
    ConfigEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
