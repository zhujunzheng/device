package rfid;

import java.time.format.DateTimeFormatter;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mot.rfid.api3.*;

import rtx.RdfSendRunnable;
import utils.SendUtil;


/**
 * RFID基本操作类  包括RFID设备的连接，登录，注销，开始读、停止读、写入、事件处理等
 * @author rzdata
 * @version 1.0
 */
public class RFIDBase {
	private final static Logger logger = LoggerFactory.getLogger(RFIDBase.class);
	RFIDReader myReader = null;
	public String hostName = null;
	public int port = 5084;
	public boolean isConnected;

	ReaderManagement rm = null;
	public boolean isRmConnected = false;
	public LoginInfo loginInfo;
	public boolean isAccessSequenceRunning = false;
	public AntennaInfo antennaInfo = null;
	public TriggerInfo triggerInfo = null;
	public AccessFilter accessFilter = null;
	public boolean isAccessFilterSet = false;

	public Hashtable<String,Object> tagStore = null;

	String[] memoryBank = new String[] { "Reserved", "EPC", "TID", "USER" };
	String[] tagState = new String[] { "New", "Gone", "Back", "Moving", "Stationary", "None" };

	public int rowId = 0;
	public long uniqueTags = 0;
	public long totalTags = 0;

	public static final String API_SUCCESS = "Function Succeeded";
	public static final String PARAM_ERROR = "Parameter Error";

	public int numOfgpis=0;
	public int numOfgpos=0;


	DateTimeFormatter  dfms = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");


	/**
	 * 串口心跳发送线程
	 */
	public RdfSendRunnable rdfSendRunnable;

	public RdfSendRunnable getRdfSendRunnable() {
		return rdfSendRunnable;
	}
	public void setRdfSendRunnable(RdfSendRunnable rdfSendRunnable) {
		this.rdfSendRunnable = rdfSendRunnable;
	}

	public RFIDBase(String strUserName) {
		myReader = new RFIDReader();
		tagStore = new Hashtable<String,Object>();

		rm = new ReaderManagement();
		loginInfo = new LoginInfo();
		loginInfo.setUserName(strUserName);

		isAccessSequenceRunning = false;

		antennaInfo = new AntennaInfo();
		triggerInfo = new TriggerInfo();

		triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
		triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);

		triggerInfo.TagEventReportInfo.setReportNewTagEvent(TAG_EVENT_REPORT_TRIGGER.MODERATED);
		triggerInfo.TagEventReportInfo.setNewTagEventModeratedTimeoutMilliseconds((short) 500);

		triggerInfo.TagEventReportInfo.setReportTagInvisibleEvent(TAG_EVENT_REPORT_TRIGGER.MODERATED);
		triggerInfo.TagEventReportInfo.setTagInvisibleEventModeratedTimeoutMilliseconds((short) 500);

		triggerInfo.TagEventReportInfo.setReportTagBackToVisibilityEvent(TAG_EVENT_REPORT_TRIGGER.MODERATED);
		triggerInfo.TagEventReportInfo.setTagBackToVisibilityModeratedTimeoutMilliseconds((short) 500);

		triggerInfo.setEnableTagEventReport(false);

		triggerInfo.setTagReportTrigger(1);

		accessFilter = new AccessFilter();

	}

	/**
	 * 连接RFID读写设备
	 *
	 * @param
	 * @exception InvalidUsageException OperationFailureException.
	 * @return 连接成功返回TRUE,连接失败返回FLASE
	 */
	public boolean connectToReader(String strIp, int intPort) {
		boolean retVal = false;
		myReader.setHostName(strIp);
		myReader.setPort(intPort);
		loginInfo.setHostName(strIp);
		try {
			myReader.connect();
			myReader.Events.setInventoryStartEvent(true);
			myReader.Events.setInventoryStopEvent(true);
			myReader.Events.setAccessStartEvent(true);
			myReader.Events.setAccessStopEvent(true);
			myReader.Events.setAntennaEvent(true);
			myReader.Events.setGPIEvent(true);
			myReader.Events.setBufferFullEvent(true);
			myReader.Events.setBufferFullWarningEvent(true);
			myReader.Events.setReaderDisconnectEvent(true);
			myReader.Events.setReaderExceptionEvent(true);
			myReader.Events.setTagReadEvent(true);
			myReader.Events.setAttachTagDataWithReadEvent(false);
			myReader.Events.setTemperatureAlarmEvent(true);
			myReader.Events.addEventsListener(new EventsHandler());
			retVal = true;
			isConnected = true;

		} catch (InvalidUsageException ex) {
			logger.error("RFIDBase连接失败:"+PARAM_ERROR+ex.getVendorMessage(), ex);
		} catch (OperationFailureException ex) {
			SendUtil.sendErr(ConfigMsg.DEFAULT_ERR_RFID_MSG);
			logger.error("RFIDBase连接失败:"+ex.getStatusDescription()+ex.getVendorMessage(), ex);
		}
		return retVal;
	}

	/**
	 * 断开与RFID读写设备连接
	 *
	 * @param
	 * @exception InvalidUsageException OperationFailureException.
	 * @return 断开成功返回TRUE,断开失败返回FLASE
	 */
	public boolean disconnectReader() {
		try {
			myReader.disconnect();
			isConnected = false;
			return true;

		} catch (InvalidUsageException ex) {
			logger.error(PARAM_ERROR, ex.getVendorMessage());
			return false;

		} catch (OperationFailureException ex) {
			logger.error(ex.getStatusDescription(), ex.getVendorMessage());
//			throw ex1;
			return false;
		}
	}

	/**
	 * 登录RFID读写设备，在操作数据写入到标签之前必须先登录，建议后台服务在开启后，自动登录设备，以确保能完成后续所有写入操作
	 *
	 * @param READER_TYPE readerType
	 *                    RFID设备类型：READER_TYPE.XR---早期固定式的设备、READER_TYPE.MC---手持设备、READER_TYPE.FX---诺唯赞采购的设备
	 * @exception InvalidUsageException OperationFailureException.
	 * @return 登录成功返回TRUE,登录失败返回FLASE
	 */
	public boolean loginIn(READER_TYPE readerType) {
		boolean retVal = false;

		try {
			rm.login(loginInfo, readerType);
			retVal = true;
			isRmConnected = true;
			//设置只开启天线
			short a[]={4};
			antennaInfo.setAntennaID(a);
		} catch (InvalidUsageException ex) {
			logger.error("RFIDBase登录失败:"+PARAM_ERROR+ex.getVendorMessage(), ex);
		} catch (OperationFailureException ex) {
			logger.error("RFIDBase登录失败:"+ex.getStatusDescription()+ex.getVendorMessage(), ex);
		}

		return retVal;

	}

	/**
	 * 针对RFID读写设备的登录注销 ，如果直接断开与RFID设置的连接，则后台直接注销，无需显性的调用该方法
	 *
	 * @param
	 * @exception InvalidUsageException OperationFailureException.
	 * @return 注销成功返回TRUE,注销失败返回FLASE
	 */
	public boolean loginOut() {
		try {
			rm.logout();

		} catch (InvalidUsageException ex) {

			return false;
		} catch (OperationFailureException ex) {
			return false;
		}
		isRmConnected = false;
		return true;
	}

	/**
	 * 开始从RFID设备读取标签数据
	 *
	 * @param intMemoryBank 存储区域标志（0 MEMORY_BANK_RESERVED,1 MEMORY_BANK_EPC,2
	 *                      MEMORY_BANK_TID,3 MEMORY_BANK_USER）.
	 * @exception InvalidUsageException OperationFailureException.
	 *
	 */
	public void startRead(int intMemoryBank) {
		PostFilter myPostFilter = null;
		AntennaInfo myAntennInfo = null;
		AccessFilter myAccessFilter = null;

		if (antennaInfo.getAntennaID() != null) {
			myAntennInfo = antennaInfo;
		}
		if (isAccessFilterSet) {
			myAccessFilter = accessFilter;
		}
		try {
			if (intMemoryBank > 0) {
				TagAccess tagaccess = new TagAccess();
				MEMORY_BANK memoryBank = MEMORY_BANK.MEMORY_BANK_EPC;
				TagAccess.Sequence opSequence = tagaccess.new Sequence(tagaccess);
				TagAccess.Sequence.Operation op1 = opSequence.new Operation();
				op1.setAccessOperationCode(ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ);

				switch (intMemoryBank) {
					case 0:
						memoryBank = MEMORY_BANK.MEMORY_BANK_RESERVED;
						break;
					case 1:
						memoryBank = MEMORY_BANK.MEMORY_BANK_EPC;
						break;
					case 2:
						memoryBank = MEMORY_BANK.MEMORY_BANK_TID;
						break;
					case 3:
						memoryBank = MEMORY_BANK.MEMORY_BANK_USER;
						break;
					default:
						break;
				}
				op1.ReadAccessParams.setMemoryBank(memoryBank);
				op1.ReadAccessParams.setByteCount(0);
				op1.ReadAccessParams.setByteOffset(4);
				op1.ReadAccessParams.setAccessPassword(0);
				myReader.Actions.TagAccess.OperationSequence.deleteAll();
				myReader.Actions.TagAccess.OperationSequence.add(op1);
				myReader.Actions.purgeTags();
				myReader.Actions.TagAccess.OperationSequence.performSequence(myAccessFilter, triggerInfo, myAntennInfo);

				isAccessSequenceRunning = true;

			} else
			{
				myReader.Actions.purgeTags();
				myReader.Actions.Inventory.perform(myPostFilter, triggerInfo, myAntennInfo);

			}
		} catch (InvalidUsageException ex) {
			logger.error(ex.getVendorMessage(),ex);
		} catch (OperationFailureException ex) {
			logger.error(ex.getMessage(),ex);
		} catch(Exception ex) {
			logger.error(ex.getMessage(),ex);
		}

	}

	/**
	 * 停止从RFID设备读取标签数据
	 *
	 * @param
	 * @exception InvalidUsageException OperationFailureException.
	 *
	 */
	public void stopRead() {

		try {
			if (isAccessSequenceRunning) {
				myReader.Actions.TagAccess.OperationSequence.stopSequence();
				myReader.Actions.TagAccess.OperationSequence.deleteAll();
				isAccessSequenceRunning = false;
			} else {
				myReader.Actions.Inventory.stop();
			}
		} catch (InvalidUsageException ex) {
			logger.error(PARAM_ERROR+ex.getVendorMessage(),ex );
		} catch (OperationFailureException ex) {
			logger.error(ex.getStatusDescription()+ ex.getVendorMessage(),ex);
		}
	}


	/**
	 * 开灯关灯
	 *
	 * @param
	 * openLight=TRUE 则亮灯
	 * openLight=FALSE 则灭灯
	 * portNum  GPIO的端口编号
	 * @exception InvalidUsageException OperationFailureException.
	 *
	 */
	public void setGpo(boolean openLight,int portNum) {
		try {
			if (openLight) {
				myReader.Config.GPO.setPortState(portNum, GPO_PORT_STATE.TRUE);
				myReader.Config.GPI.getPortState(1);
			} else {
				myReader.Config.GPO.setPortState(portNum, GPO_PORT_STATE.FALSE);
			}

		} catch (InvalidUsageException ex) {
			logger.error(ex.getMessage(),ex);
		} catch (OperationFailureException ex) {
			logger.error(ex.getMessage(),ex);
		}
	}

	public String getGpioState() {
		StringBuilder rst=new StringBuilder();
		//得到本设备有多少个GPI口
		numOfgpis=myReader.ReaderCapabilities.getNumGPIPorts();
		//得到本设备有多少个GPO口
		numOfgpos=myReader.ReaderCapabilities.getNumGPOPorts();
		//逐一判断端口是否被激活
		rst.append("GPI 有");
		try {
			for (int i=1;i<numOfgpis;i++) {
				if (myReader.Config.GPI.isPortEnabled(i)){
					rst.append(i+",");
				}
			}
			rst.append("号端口被激活！GPO 有");
			for (int i=1;i<numOfgpos;i++) {
				if (myReader.Config.GPI.isPortEnabled(i)){
					rst.append(i+",");
				}
			}
			rst.append("号端口被激活！");
		}catch (InvalidUsageException ex) {
			logger.error(ex.getMessage(),ex);
		} catch (OperationFailureException ex) {
			logger.error(ex.getMessage(),ex);
		}
		return rst.toString();
	}

	/**
	 * RFID设备写标签数据操作，固定写入EPC区域
	 *
	 * @param String strWriteData 写入到标签中的数据（24位长度的字符串） int intWriteDataLength
	 *               写入到标签中的数据的长度（长度为12 配置） int intOffSet 标签中数据的偏移量（固定是4，配置
	 *               用于保留校验位，控制位） String strPwd 写入标签时的密码，现在固定字符串"0" String strTagId
	 *               要写入数据的标签ID
	 * @throws OperationFailureException
	 * @exception InvalidUsageException OperationFailureException.
	 *
	 */
	public void writeActionPerformed(String strWriteData, int intWriteDataLength, int intOffSet, String strPwd,
									 String strTagId) throws InvalidUsageException, OperationFailureException {
		TagAccess tagAccess = new TagAccess();
		TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();
		// 固定写入EPC区域
		MEMORY_BANK memBank = MEMORY_BANK.MEMORY_BANK_EPC;

		writeAccessParams.setMemoryBank(memBank);
		byte[] writeData = StringUtils.hexStringToByteArray(strWriteData);
		writeAccessParams.setWriteData(writeData);
		writeAccessParams.setWriteDataLength(intWriteDataLength);
		writeAccessParams.setByteOffset(intOffSet);

		writeAccessParams.setAccessPassword(Long.parseLong(strPwd, 16));
		if (strTagId.length() > 0) {

			myReader.Actions.TagAccess.writeWait(strTagId, writeAccessParams,
					antennaInfo.getAntennaID() != null ? antennaInfo : null);
		} else {
			myReader.Actions.TagAccess.writeEvent(writeAccessParams, isAccessFilterSet ? accessFilter : null,
					antennaInfo.getAntennaID() != null ? antennaInfo : null);

		}
	}


	public class EventsHandler implements RfidEventsListener {
		@Override
		public void eventReadNotify(RfidReadEvents rre) {
			TagData[] myTags = myReader.Actions.getReadTags(100);
			if (myTags != null) {
				for (int index = 0; index < myTags.length; index++) {
					TagData tag = myTags[index];
					String data=tag.getMemoryBankData();//真值
					logger.debug(data+"获取到标签时二维码:"+ConfigMsg.CODE);
					if(data!=null && data.length()>0 && ConfigMsg.RFID==null) {
//						ConfigMsg.RFID.append(data);
						ConfigMsg.RFID=data;
						//读到RFID 关闭
						stopRead();
						tagStore.clear();
						//二维码不为null是发送
//						if(ConfigMsg.CODE.length()>0) {
//		                    logger.debug("获取的标签发送");
//							SendUtil.secunityCodeMsg(ConfigMsg.CODE.toString(),data);
//						}
					}
				}
			}
		}
		@Override
		public void eventStatusNotify(RfidStatusEvents rse) {
			STATUS_EVENT_TYPE statusType = rse.StatusEventData.getStatusEventType();
			if (statusType == STATUS_EVENT_TYPE.DISCONNECTION_EVENT) {//如果USB端口
				//停止心跳
				if(rdfSendRunnable!=null) {
					rdfSendRunnable.stop();
					isConnected=false;
				}
				SendUtil.sendErr(ConfigMsg.DEFAULT_ERR_RFID_MSG);
			} else if (statusType == STATUS_EVENT_TYPE.GPI_EVENT) {
				//如果为二维码加标签模式
				if(ConfigMsg.MODE_1.equals(ConfigMsg.MODE_ID)) {
					//触发器1 给的GPI事件
					boolean is=rse.StatusEventData.GPIEventData.getGPIPort()==ConfigMsg.VALUE_1 || rse.StatusEventData.GPIEventData.getGPIPort()==ConfigMsg.VALUE_2;
					if(is) {
						boolean isOpen=rse.StatusEventData.GPIEventData.getGPIEventState();
						if(isOpen) {
							logger.info("GPI关");
							stopRead();
							tagStore.clear();
							String code=ConfigMsg.CODE==null?ConfigMsg.NO_READ:ConfigMsg.CODE;
							String rfid=ConfigMsg.RFID==null?ConfigMsg.NO_RFID_READ:ConfigMsg.RFID;
							logger.info("发送");

							SendUtil.secunityCodeMsg(code,rfid);
						}else {
							logger.info("GPI开");
							ConfigMsg.RFID=null;
							ConfigMsg.CODE=null;
							startRead(1);
						}
					}
				}else {
					boolean is=rse.StatusEventData.GPIEventData.getGPIPort()==ConfigMsg.VALUE_1 || rse.StatusEventData.GPIEventData.getGPIPort()==ConfigMsg.VALUE_2;
					if(is) {
						boolean isOpen=rse.StatusEventData.GPIEventData.getGPIEventState();
						if(isOpen) {
							logger.debug("GPI关");
							String code=ConfigMsg.CODE.length()==0?ConfigMsg.NO_READ:ConfigMsg.CODE.toString();
							SendUtil.secunityCodeMsg(code,ConfigMsg.NO_RFID_READ);
						}else {
							logger.debug("GPI开");
							ConfigMsg.CODE=null;
						}
					}
				}
			}
		}
	}
}
