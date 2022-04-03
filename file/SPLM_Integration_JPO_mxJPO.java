import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrData;
import org.tempuri.GetGLMCustLocator;
import org.tempuri.GetGLMCustSoap;
import org.tempuri.SPlmWSLocator;
import org.tempuri.SPlmWSSoap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.i18nNow;

import matrix.db.Context;
import matrix.util.Pattern;
import matrix.util.StringList;
import mc_style.functions.soap.sap.document.sap_com.YMM20_PortType;
import mc_style.functions.soap.sap.document.sap_com.YMM20_ServiceLocator;
import mc_style.functions.soap.sap.document.sap_com.YPP16_PortType;
import mc_style.functions.soap.sap.document.sap_com.YPP16_ServiceLocator;
import mc_style.functions.soap.sap.document.sap_com.YSD041_NEW_PortType;
import mc_style.functions.soap.sap.document.sap_com.YSD041_NEW_ServiceLocator;

/*
 	DMS System Example :
  	{
    "GROUP_CODE": "CW123456",
    "PNC_NO": [
      "76169M"
      "89153M"
    ],
    "CHANGE_DATE": "20200116",
    "GUID": "23231231232131231231232"
	}
	
	ERP System Example :
	{
    	"GROUP_CODE": "CW123456",
    	"PNC_NO": "76169M",
    	"CHANGE_DATE": "20200116",
    	"GUID": "23231231232131231231232"
	},
	{
    	"GROUP_CODE": "CW123456",
    	"PNC_NO": "89153M",
    	"CHANGE_DATE": "20200116",
    	"GUID": "23231231232131231231232"
	}
 */

public class SPLM_Integration_JPO_mxJPO {

	private final static String TEST_FILE_PATH = "C:\\TEMP\\Morga\\json.txt";
	private final static String DEFAULT_ROOT_DIRECTORY = "C:\\TEMP\\Morga\\";
	private final static String CALL_WSDL = "DMS";
	private final static String PLM_ENV = System.getenv("PLM_ENV");
	private final static String VAULT = "eService Production";
	private final static short SEARCH_AMOUT = 0;
	private final static String ERP_YSD041_300 = "ERP.YSD041.300";
	private final static String ERP_YSD041_310 = "ERP.YSD041.310";
	private final static String ERP_YMM20_300 = "ERP.YMM20.300";
	private final static String ERP_YMM20_310 = "ERP.YMM20.310";
	private final static String ERP_YPP16_300 = "ERP.YPP16.300";
	private final static String ERP_YPP16_310 = "ERP.YPP16.310";
	private final static String GLM_DEALER = "GLM.DEALER";
	private final static String DMS = "DMS";
	private static final String SPLIT_STR = ",";

	/* 透過URL切換 UAT/PRD */
	private String getYSD041_300URL() throws Exception {
		return getPropertyURL(ERP_YSD041_300);
	}

	private String getYSD041_310URL() throws Exception {
		return getPropertyURL(ERP_YSD041_310);
	}

	private String getYMM20_300URL() throws Exception {
		return getPropertyURL(ERP_YMM20_300);
	}

	private String getYMM20_310URL() throws Exception {
		return getPropertyURL(ERP_YMM20_310);
	}

	private String getYPP16_300URL() throws Exception {
		return getPropertyURL(ERP_YPP16_300);
	}

	private String getYPP16_310URL() throws Exception {
		return getPropertyURL(ERP_YPP16_310);
	}

	private String getDMSURL() throws Exception {
		return getPropertyURL(DMS);
	}
	
	private String getGLMURL() throws Exception {
		return getPropertyURL(GLM_DEALER);
	}
	
	private String getPropertyURL(String keyStr) throws Exception {
		if (!"PRD,UAT".contains(PLM_ENV)) {
			throw new IllegalArgumentException(PLM_ENV + " is not PRD / UAT Enviroment");
		}
		String url = i18nNow.getI18nString(keyStr + "." + PLM_ENV, "emxSPLM", "");
		if (url.equals(keyStr + "." + PLM_ENV)) {
			throw new IllegalArgumentException("emxSPLM Property not found : " + keyStr + "." + PLM_ENV);
		}
		return url;
	}

	/* --- 影響件號發送SO 資訊 --- */
	public void ReleasePart(Context context, String[] args) throws Exception {
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);

		MapList soMapList = DomainObject.findObjects(context, "SPLM_SO", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"current=='Complete' && attribute[SPLM_SendToERP_DMS]=='Processing'", // where
				null, false, busSelect, (short) SEARCH_AMOUT);

		DomainObject soObj = new DomainObject();
		for (Object so : soMapList) {
			String soId = (String) ((Map) so).get(DomainConstants.SELECT_ID);
			String soName = (String) ((Map) so).get(DomainConstants.SELECT_NAME);
			soObj.setId(soId);
			
			System.out.println("\u73fe\u5728\u958b\u59cb " + soName + " \u62cb\u8f49");
			String seviceStatus = this._searchServicePart(context, this.getSOAI(context, soObj, getServicePartSelects()), "so");
			String altStatus = this._searchOptAltPart(context, this.getSOAI(context, soObj, getAltOptPartSelects()), "alt");
			String optStatus = this._searchOptAltPart(context, this.getSOAI(context, soObj, getAltOptPartSelects()), "opt");
			String partVendorStatus = this._searchPartVendor(context, this.getSOAI(context, soObj, getVendorSelects()));
			String mrpStatus = this._searchMRP_BOM(context, soObj);

			if("Error".equalsIgnoreCase(seviceStatus) || "Error".equalsIgnoreCase(altStatus) || "Error".equalsIgnoreCase(optStatus)|| "Error".equalsIgnoreCase(partVendorStatus) ||"Error".equalsIgnoreCase(mrpStatus)) {
				System.out.println("servicePart : " + seviceStatus);
				System.out.println("alterPart : " + altStatus);
				System.out.println("optionalPart : " + optStatus);
				System.out.println("partVendor : " + partVendorStatus);
				System.out.println("MRP_BOM : " + mrpStatus);
				System.out.println("\u62cb\u8f49\u5931\u6557 \u7b49\u5f85\u4e0b\u6b21\u6392\u6210\u57f7\u884c");
				soObj.setAttributeValue(context, "SPLM_SendToERP_DMS", "Error");
				continue;
			}
			soObj.setAttributeValue(context, "SPLM_SendToERP_DMS", "Complete");
		}
		System.out.println("*********************SO Sending Done.**************************");
	}

	/* --- 一般API抓取資料 發送 DMS/ERP (因為共用抓取模式) --- */
	public void searchServicePart(Context context, String[] args) throws Exception {
		String WhereSelect = "current=='Release' && attribute[SPLM_ServicePart_DMS]=='Wait' && attribute[SPLM_ServicePart_ERP300]=='Wait' && attribute[SPLM_ServicePart_ERP310]=='Wait'"; // where
		StringList busSelects = getServicePartSelects();

		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part,SPLM_ColorPart", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				WhereSelect, // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		this._searchServicePart(context, partMapList, "normal");
	}

	public void searchOptAltPart(Context context, String[] args) throws Exception {
		String strAltOpt = args[0]; // "alt", "opt"
		String WhereSelect = "from[SPLM_RelatedOptionalPart]==TRUE"; // where
		StringList busSelects = getAltOptPartSelects();

		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part,SPLM_ColorPart", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				WhereSelect, // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		this._searchOptAltPart(context, partMapList, strAltOpt);
	}

	public void searchPartVendor(Context context, String[] args) throws Exception {
		String WhereSelect = "from[SPLM_RelatedVendor].attribute[SPLM_Valid]==Y"; // where
		StringList busSelects = getVendorSelects();

		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part,SPLM_ColorPart", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				WhereSelect, // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		this._searchPartVendor(context, partMapList);
	}

	/* --- Dassault API 簡單封裝使用查詢區 --- */
	private MapList getSOAI(Context context, DomainObject soObj, StringList busSelect) throws Exception {
		return getSOAI(context, soObj, busSelect, "");
	}
	
	private MapList getSOHaveSubPartAI(Context context, DomainObject soObj, StringList busSelect) throws Exception {
		return getSOAI(context, soObj, busSelect, "attribute[SPLM_HaveSubPart]=='True'");
	}

	private MapList getSOIsServicePartAI(Context context, DomainObject soObj, StringList busSelect) throws Exception {
		return getSOAI(context, soObj, busSelect, "attribute[SPLM_IsServicePart]=='True'");
	}

	private MapList getSOAI(Context context, DomainObject soObj, StringList busSelect, String objectWhere)
			throws Exception {
		return soObj.getRelatedObjects(context, "SPLM_AffectedItem", // relationshipPattern,
				"*", // typePattern,
				busSelect, // StringList objectSelects,
				null, // StringList relationshipSelects,
				false, // boolean getTo,
				true, // boolean getFrom,
				(short) 1, // short recurseToLevel,
				objectWhere, // String objectWhere,
				"", // String relationshipWhere
				0); // int limit)
	}

	private Map<String, String> getDealerInfo(Context context) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_NAME);
		
		MapList dealerMapList= DomainObject.findObjects(context, "SPLM_Dealer",
				"*",
				"-",
				"*",
				VAULT,
				"",
				null, false, busSelects, SEARCH_AMOUT);
		
		return (Map<String, String>) dealerMapList.stream()
				.collect(Collectors.toMap(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME)
						, obj -> (String) ((Map) obj).get(DomainConstants.SELECT_ID)));
	}
	
	private Map<String, String> getCmcCodeSpecifyInfo(Context context, ArrayList<String> mcList) throws Exception {
		StringList busSelect = new StringList();
		busSelect.add("attribute[SPLM_CMC_Code]");
		busSelect.add("attribute[SPLM_EnableDate]");

		MapList mcMapList = DomainObject.findObjects(context, "SPLM_ModelCode", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"attribute[SPLM_CMC_Code] smatchlist '" + String.join(",", mcList) + "' ',' ", // where
				null, false, busSelect, (short) 0);

		return (Map<String, String>) mcMapList.stream()
				.filter(obj -> !((String) ((Map) obj).get("attribute[SPLM_EnableDate]")).isEmpty())
				.collect(Collectors.toMap(obj -> ((Map) obj).get("attribute[SPLM_CMC_Code]"),
						obj -> ((String) ((Map) obj).get("attribute[SPLM_EnableDate]")).isEmpty() ? "1900/01/01"
								: exchangeDate((String) ((Map) obj).get("attribute[SPLM_EnableDate]")),
						(o, n) -> n = o));
	}
	
	private Map<String, String> getMsSpecifyInfo(Context context, ArrayList<String> msList) throws Exception {
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_NAME);
		busSelect.add(DomainConstants.SELECT_MODIFIED);

		MapList mcMapList = DomainObject.findObjects(context, "SPLM_ModelSeries", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				DomainConstants.SELECT_NAME + " smatchlist '" + String.join(",", msList) + "' ',' ", // where
				null, false, busSelect, (short) 0);

		return (Map<String, String>) mcMapList.stream()
				.filter(obj -> !((String) ((Map) obj).get(DomainConstants.SELECT_NAME)).isEmpty())
				.collect(Collectors.toMap(obj -> ((Map) obj).get(DomainConstants.SELECT_NAME),
						obj -> ((String) ((Map) obj).get(DomainConstants.SELECT_MODIFIED)).isEmpty() ? "1900/01/01"
								: exchangeDate((String) ((Map) obj).get(DomainConstants.SELECT_MODIFIED)),
						(o, n) -> n = o));
	}
	
	private String exchangeDate(String date) {
		String[] dates = date.substring(0, date.indexOf(" ")).split("/");
		return dates[2] + "/" + this.fillPrefixZero(dates[0], 2) + "/" + this.fillPrefixZero(dates[1], 2);
	}
	
	private String fillPrefixZero(String value, int fillLength) {
		int valueLength = value.length();
		if (valueLength < fillLength) {
			int left = fillLength - valueLength;
			value = String.format("%0" + left + "d%s", 0, value);
		}
		return value;
	}

	/* --- BO物件 busSelect欄位資料 --- */
	private StringList getServicePartSelects() {
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID); // GUID
		busSelect.add(DomainConstants.SELECT_NAME); // PART_NO
		busSelect.add(DomainConstants.SELECT_ORIGINATED); // CREATE_DATE
		busSelect.add(DomainConstants.SELECT_MODIFIED); // CHANGE_DATE
		busSelect.add(DomainConstants.SELECT_OWNER); // CHANGE_DATE
		busSelect.add("attribute[SPLM_OrderType]"); // PART_SH_NO
		busSelect.add("attribute[SPLM_Location]"); // PLANT
		busSelect.add("attribute[SPLM_Location_DMS]"); // PLANT
		busSelect.add("attribute[SPLM_MaterialType]"); // MATERIAL_TYPE
		busSelect.add("attribute[SPLM_MaterialGroup]"); // MATERIAL_GROUP
		busSelect.add("attribute[SPLM_Unit]"); // UNIT
		busSelect.add("attribute[SPLM_PurchasingGroup]"); // PURCHASING_DEPARTMENT_NO
		busSelect.add("attribute[SPLM_Name_TC]"); // PART_NAME_C
		busSelect.add("attribute[SPLM_Name_EN]"); // PART_NAME_E
		busSelect.add("attribute[SPLM_HaveSubPart]"); // HAS_SUBASSEMBLY
		busSelect.add("attribute[SPLM_IsServicePart]"); // IS_SERVICE_PART
		busSelect.add("attribute[SPLM_DTAT_Only]"); // BIG_CAR_TYPE
		busSelect.add("attribute[SPLM_ItemSubType]"); // PART_TYPE
		busSelect.add("attribute[SPLM_CMC_Code]"); // MODEL_CODE
		busSelect.add("attribute[SPLM_ModelSeries]"); // MODEL_SERIES
		busSelect.add("attribute[SPLM_OverseaSalesOnly]"); // MODEL_SERIES
		busSelect.add("attribute[SPLM_CommissionGroup]"); // PNC_TYPE
		busSelect.add("attribute[SPLM_ServicePart_DMS]"); // For DMS Status
		busSelect.add("attribute[SPLM_ServicePart_ERP300]"); // For ERP Status
		busSelect.add("attribute[SPLM_PNC]"); // For PNC
		busSelect.add("attribute[SPLM_GroupCode]"); // For GroupCode 為了建檔使用
		return busSelect;
	}

	private StringList getAltOptPartSelects() {
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID); // GUID
		busSelect.add(DomainConstants.SELECT_NAME); // PART_NO
		busSelect.add(DomainConstants.SELECT_MODIFIED); // CHANGE_DATE
		busSelect.add("attribute[SPLM_Location]");
		busSelect.add("attribute[SPLM_Location_DMS]");
		return busSelect;
	}

	private StringList getVendorSelects() {
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID); // GUID
		busSelect.add(DomainConstants.SELECT_NAME); // PART_NO
		busSelect.add(DomainConstants.SELECT_MODIFIED); // CHANGE_DATE
		busSelect.add("attribute[SPLM_Location]");
		busSelect.add("attribute[SPLM_Location_DMS]");
		return busSelect;
	}

	/* --- 中華CMC wsdl connection設定 --- */
	private SPlmWSSoap getSPlmWSSoapDMS() throws Exception {
		String url = getDMSURL();
		SPlmWSLocator locator = new SPlmWSLocator();
		if (StringUtils.isBlank(url)) {
			return locator.getSPlmWSSoap();
		} else {
			return locator.getSPlmWSSoap(new URL(url));
		}
	}

	private YMM20_PortType _getYMM20ERP(String url) throws Exception {
		YMM20_ServiceLocator locator = new YMM20_ServiceLocator();
		if (StringUtils.isEmpty(url)) {
			return locator.getYMM20();
		} else {
			return locator.getYMM20(new URL(url));
		}
	}

	private YMM20_PortType getYMM20_300ERP() throws Exception {
		return _getYMM20ERP(getYMM20_300URL());
	}

	private YMM20_PortType getYMM20_310ERP() throws Exception {
		return _getYMM20ERP(getYMM20_310URL());
	}

	private YPP16_PortType _getYPP16ERP(String url) throws Exception {
		YPP16_ServiceLocator locator = new YPP16_ServiceLocator();
		if (StringUtils.isEmpty(url)) {
			return locator.getYPP16();
		} else {
			return locator.getYPP16(new URL(url));
		}
	}

	private YPP16_PortType getYPP16_300ERP() throws Exception {
		return _getYPP16ERP(getYPP16_300URL());
	}

	private YPP16_PortType getYPP16_310ERP() throws Exception {
		return _getYPP16ERP(getYPP16_310URL());
	}

	private YSD041_NEW_PortType _getYSD041ERP(String url) throws Exception {
		YSD041_NEW_ServiceLocator locator = new YSD041_NEW_ServiceLocator();
		if (StringUtils.isEmpty(url)) {
			return locator.getYSD041_NEW();
		} else {
			return locator.getYSD041_NEW(new URL(url));
		}
	}

	private YSD041_NEW_PortType getYSD041_300ERP() throws Exception {
		return _getYSD041ERP(getYSD041_300URL());
	}

	private YSD041_NEW_PortType getYSD041_310ERP() throws Exception {
		return _getYSD041ERP(getYSD041_310URL());
	}

	private GetGLMCustSoap getCustGLM() throws Exception {
		String url = getGLMURL();
		GetGLMCustLocator locator = new GetGLMCustLocator();
		if (StringUtils.isEmpty(url)) {
			return locator.getGetGLMCustSoap();
		} else {
			return locator.getGetGLMCustSoap(new URL(url));
		}
	}

	/* --- 一般function 使用區 --- */
	private String convertNowDateFormat(String formatType) {
		return convertDateFormat(new Date().toGMTString(), formatType);
	}

	private String convertDateFormat(String targetDate, String dateFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		Date d = new Date(targetDate);
		return sdf.format(d);
	}

	private void outputDataFile(String fileName, String dataStr) throws IOException {
		File dataFile = new File(DEFAULT_ROOT_DIRECTORY + fileName);
		File dataRootFile = new File(DEFAULT_ROOT_DIRECTORY);
		File dataTestFile = new File(TEST_FILE_PATH);
		try {
			if (dataRootFile.exists() != true) {
				dataRootFile.mkdirs();
			}
			if (dataTestFile.exists() != true) {
				dataTestFile.createNewFile();
			}
			if (dataFile.exists() != true) {
				System.out.println("We had to make " + fileName + " file.");
				dataFile.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(dataFile, true), StandardCharsets.UTF_8));
			bw.write(dataStr + "");
			bw.newLine();
			bw.newLine();
			bw.write("******* " + convertNowDateFormat("yyyy:MM:dd HH:mm:ss") + " ******* " + "\n\n");
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("COULD NOT WRITE!!");
		}
	}

	private String translateJsonIntoString(Object obj) {
		return new GsonBuilder().disableHtmlEscaping().create().toJson(obj);
	}

	private <E> void filter(Map<String, ArrayList<E>> map, String filterStr, E input) {
		if (!map.containsKey(filterStr)) {
			ArrayList<E> array = new ArrayList<E>();
			map.put(filterStr, array);
		}
		map.get(filterStr).add(input);
	}

	/**
	 * 測試用 compareDate理解
	 */
	public void compareDateOnlyYMDTesting() {
		System.out.println(this.compareDateOnlyYMD("2022/01/05", "9999/12/31"));
		System.out.println(this.compareDateOnlyYMD("2022/01/08", "2022/01/09"));
		System.out.println(this.compareDateOnlyYMD("2022/01/11", "2022/01/09"));
		System.out.println(this.compareDateOnlyYMD("2022/01/09", "2022/01/09"));
		System.out.println(this.compareDateOnlyYMD("2023/01/09", "2022/01/09"));
		System.out.println(this.compareDateOnlyYMD("2022/11/09", "2022/01/09"));
		System.out.println(this.compareDateOnlyYMD("2022/11/09", "2022/12/09"));
	}

	// 開始結束的日期格式需要相同 -> startDate >= endDate (設計概念)
	private boolean compareDateOnlyYMD(String startDate, String endDate) {
		if(startDate.isEmpty()) {
			return true;
		} else if (endDate.isEmpty()) {
			return false;
		}
		
		char[] startDateChars = startDate.toCharArray();
		char[] endDateChars = endDate.toCharArray();
		for (int i = 0; i < startDateChars.length; ++i) {
			int temp = endDateChars[i] - startDateChars[i];
			if (temp == 0) {
				continue;
			} else if (temp < 0) {
				return false;
			} else if (temp > 0) {
				return true;
			}
		}
		return true; // 當結束日和開始日期相同
	}
	
	abstract class Abstract_CMC_ObjBaseAttribute extends Object implements Cloneable {
		protected String CHANGE_DATE = convertNowDateFormat("yyyyMMdd");
		protected String CREATE_DATE;
		protected String GUID;
		protected String PLANT;

		protected void setPLANT(String plant) {
			this.PLANT = plant;
		}

		protected String getPLANT() {
			return PLANT;
		}

		protected void setCHANGE_DATE(String cHANGE_DATE) {
			CHANGE_DATE = cHANGE_DATE;
		}

		protected void setCREATE_DATE(String cREATE_DATE) {
			CREATE_DATE = cREATE_DATE;
		}

		protected void setGUID(String gUID) {
			GUID = gUID;
		}

		protected String getGUID() {
			return GUID;
		}

		@Override
		protected Object clone() throws CloneNotSupportedException {
			Object cloneObj = super.clone();
			return cloneObj;
		}
	}

	/* --- API 抓取資料 --- */
	/**
	 * 16.PNC
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchPnc(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add("attribute[SPLM_Name_TC]");
		busSelects.add("attribute[SPLM_Name_EN]");
		busSelects.add("attribute[SPLM_CommissionGroup]");

		MapList pncMapList = DomainObject.findObjects(context, "SPLM_PNC", // type
				"*", // name
				"-", // revision
				"*", // owner
				VAULT, // vault
				"attribute[SPLM_PNC_DMS]=='Wait' && attribute[SPLM_PNC_ERP]=='Wait'", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		ArrayList<PNCForBoth> pncForBothArray = new ArrayList<PNCForBoth>();

		// json for DMS/ERP system
		for (Object obj : pncMapList) {
			Map pncMap = (Map) obj;
			PNCForBoth pncForBothObj = new PNCForBoth();
			pncForBothObj.setPNC_NO((String) pncMap.get(DomainConstants.SELECT_NAME));
			pncForBothObj.setPNC_NAME_C((String) pncMap.get("attribute[SPLM_Name_TC]"));
			pncForBothObj.setPNC_NAME_E((String) pncMap.get("attribute[SPLM_Name_EN]"));
			pncForBothObj.setPNC_TYPE((String) pncMap.get("attribute[SPLM_CommissionGroup]"));
			pncForBothObj.setGUID((String) pncMap.get(DomainConstants.SELECT_ID));
			pncForBothArray.add(pncForBothObj);
		}

		DomainObject pncDomObj = new DomainObject();
		String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		String result = "";

		// connect CMC for test
		try {
			MqlUtil.mqlCommand(context, "trigger off");
			if (pncForBothArray.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				result = dms.PNC(translateJsonIntoString(pncForBothArray));
				for (PNCForBoth pncObj : pncForBothArray) {
					pncDomObj.setId(pncObj.GUID);
					pncDomObj.setAttributeValue(context, "SPLM_PNC_DMS", "Complete");
				}
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			for (PNCForBoth pncObj : pncForBothArray) {
				pncDomObj.setId(pncObj.GUID);
				pncDomObj.setAttributeValue(context, "SPLM_PNC_DMS", "Error");
			}
			result = "(Error) " + e.getMessage();
		} finally {
			System.out.println(DMS + " " + methodName + " Result :" + result);
			System.out.println(DMS + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(pncForBothArray));
			System.out.println();
			MqlUtil.mqlCommand(context, "trigger on");			
		}

		try {
			MqlUtil.mqlCommand(context, "trigger off");
			if (pncForBothArray.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				result = erp.YPutSplmP01(translateJsonIntoString(pncForBothArray));
				for (PNCForBoth pncObj : pncForBothArray) {
					pncDomObj.setId(pncObj.GUID);
					pncDomObj.setAttributeValue(context, "SPLM_PNC_ERP", "Complete");
				}
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			for (PNCForBoth pncObj : pncForBothArray) {
				pncDomObj.setId(pncObj.GUID);
				pncDomObj.setAttributeValue(context, "SPLM_PNC_ERP", "Error");
			}
			result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
		} finally {
			System.out.println(ERP_YSD041_300 + " " + methodName + " Result :" + result);
			System.out.println(ERP_YSD041_300 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(pncForBothArray));
			System.out.println();
			MqlUtil.mqlCommand(context, "trigger on");			
		}
	}

	private class PNCForBoth extends Abstract_CMC_ObjBaseAttribute {
		public String PNC_NO;
		public String PNC_NAME_E;
		public String PNC_NAME_C;
		public String PNC_TYPE;
		public void setPNC_NO(String pNC_NO) {
			PNC_NO = pNC_NO;
		}
		public void setPNC_NAME_E(String pNC_NAME_E) {
			PNC_NAME_E = pNC_NAME_E;
		}
		public void setPNC_NAME_C(String pNC_NAME_C) {
			PNC_NAME_C = pNC_NAME_C;
		}
		public void setPNC_TYPE(String pNC_TYPE) {
			PNC_TYPE = pNC_TYPE;
		}
	}

	/**
	 * 17. GROUP
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchGroupCode(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add("attribute[SPLM_Name_TC]");
		busSelects.add("attribute[SPLM_Name_EN]");

		MapList groupCodeMapList = DomainObject.findObjects(context, "SPLM_GroupCode", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"attribute[SPLM_GroupCode_DMS]=='Wait' && attribute[SPLM_GroupCode_ERP]=='Wait'", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		ArrayList<GroupCodeForBoth> groupCodeForBothArray = new ArrayList<GroupCodeForBoth>();

		// json for DMS/ERP system
		for (Object obj : groupCodeMapList) {
			Map groupCodeMap = (Map) obj;
			GroupCodeForBoth groupCodeForBothObj = new GroupCodeForBoth();
			groupCodeForBothObj.setGROUP_CODE((String) groupCodeMap.get(DomainConstants.SELECT_NAME));
			groupCodeForBothObj.setGROUP_NAME_C((String) groupCodeMap.get("attribute[SPLM_Name_TC]"));
			groupCodeForBothObj.setGROUP_NAME_E((String) groupCodeMap.get("attribute[SPLM_Name_EN]"));
			groupCodeForBothObj.setGUID((String) groupCodeMap.get(DomainConstants.SELECT_ID));
			groupCodeForBothArray.add(groupCodeForBothObj);
		}

		DomainObject groupDomObj = new DomainObject();
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();
		String result = "";
		
		// connect CMC for test
		try {
			MqlUtil.mqlCommand(context, "trigger off");
			if (groupCodeForBothArray.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				result = dms.group(translateJsonIntoString(groupCodeForBothArray));
				for (GroupCodeForBoth groupObj : groupCodeForBothArray) {
					groupDomObj.setId(groupObj.GUID);
					groupDomObj.setAttributeValue(context, "SPLM_GroupCode_DMS", "Complete");
				}
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			for (GroupCodeForBoth groupObj : groupCodeForBothArray) {
				groupDomObj.setId(groupObj.GUID);
				groupDomObj.setAttributeValue(context, "SPLM_GroupCode_DMS", "Error");
			}
			result = "(Error) " + e.getMessage();
		} finally {
			System.out.println(DMS + " " + methodName + " Result :" + result);
			System.out.println(DMS + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(groupCodeForBothArray));
			System.out.println();
			MqlUtil.mqlCommand(context, "trigger on");			
		}

		try {
			MqlUtil.mqlCommand(context, "trigger off");
			if (groupCodeForBothArray.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				result = erp.YPutSplmG01(translateJsonIntoString(groupCodeForBothArray));
				for (GroupCodeForBoth groupObj : groupCodeForBothArray) {
					groupDomObj.setId(groupObj.GUID);
					groupDomObj.setAttributeValue(context, "SPLM_GroupCode_ERP", "Complete");
				}
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			for (GroupCodeForBoth groupObj : groupCodeForBothArray) {
				groupDomObj.setId(groupObj.GUID);
				groupDomObj.setAttributeValue(context, "SPLM_GroupCode_ERP", "Error");
			}
			result = "(Error) " + e.getMessage();
		} finally {
			System.out.println(ERP_YSD041_300 + " " + methodName + " Result :" + result);
			System.out.println(ERP_YSD041_300 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(groupCodeForBothArray));
			System.out.println();
			MqlUtil.mqlCommand(context, "trigger on");			
		}
		
	}

	private class GroupCodeForBoth extends Abstract_CMC_ObjBaseAttribute {
		public String GROUP_CODE;
		public String GROUP_NAME_C;
		public String GROUP_NAME_E;
		public void setGROUP_CODE(String gROUP_CODE) {
			GROUP_CODE = gROUP_CODE;
		}
		public void setGROUP_NAME_C(String gROUP_NAME_C) {
			GROUP_NAME_C = gROUP_NAME_C;
		}
		public void setGROUP_NAME_E(String gROUP_NAME_E) {
			GROUP_NAME_E = gROUP_NAME_E;
		}
	}

	/**
	 * 19.PartVendor
	 * 
	 * @param context
	 * @param partMapList
	 * @return 
	 * @throws Exception
	 */
	private String _searchPartVendor(Context context, MapList partMapList) throws Exception {
		String sendStatus = "Complete";
		StringList relatedBusSelects = new StringList();
		relatedBusSelects.add("current.actual");
		relatedBusSelects.add(DomainConstants.SELECT_NAME);

		DomainObject domObj = new DomainObject();
		ArrayList<VenderAlterToDMS> venderAlterToDMSArray = new ArrayList<VenderAlterToDMS>();
		ArrayList<VenderAlterToERP> venderAlterToERPArray = new ArrayList<VenderAlterToERP>();

		for (Object obj : partMapList) {
			Map partMap = (Map) obj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partModifiedDate = (String) partMap.get(DomainConstants.SELECT_MODIFIED);
			String partLocationForERP = (String) partMap.get("attribute[SPLM_Location]");
			String partLocationForDMS = (String) partMap.get("attribute[SPLM_Location_DMS]");
			domObj.setId(partId);

			MapList vendorMapList = domObj.getRelatedObjects(context, "SPLM_RelatedVendor", // relationshipPattern,
					"SPLM_Vendor", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_Valid]=='Y'", // String relationshipWhere
					0); // int limit)

			List<String> partAttVendorList = (List<String>) vendorMapList.stream()
					.map(vendor -> (String) ((Map) vendor).get(DomainConstants.SELECT_NAME))
					.collect(Collectors.toList());

			// SPLM_SO data processing
			MapList soMapList = domObj.getRelatedObjects(context, "SPLM_AffectedItem", // relationshipPattern,
					"SPLM_SO", // typePattern,
					relatedBusSelects, // StringList objectSelects,
					null, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			String soLatestName = "";
			if (!soMapList.isEmpty() && !partAttVendorList.isEmpty()) {
				soMapList.addSortKey("current.actual", "descending", "date");
				soMapList.sort();
				soLatestName = (String) ((Map) soMapList.get(0)).get(DomainConstants.SELECT_NAME);

				// json for DMS system
				if( !partLocationForDMS.isEmpty() ) {					
					VenderAlterToDMS venderAlterToDMSObj = new VenderAlterToDMS();
					venderAlterToDMSObj.setPART_NO(partName);
					venderAlterToDMSObj.setVENDOR_NO(partAttVendorList);
					venderAlterToDMSObj.setSO_NO(soLatestName);
					venderAlterToDMSObj.setGUID(partId);
					venderAlterToDMSArray.add(venderAlterToDMSObj);
				}

				// json for ERP system
				for (String partAttVendor : partAttVendorList) {
					for (String loaciotn : partLocationForERP.split(SPLIT_STR)) {
						VenderAlterToERP venderAlterToERPObj = new VenderAlterToERP();
						venderAlterToERPObj.setPART_NO(partName);
						venderAlterToERPObj.setVENDOR_NO(partAttVendor);
						venderAlterToERPObj.setSO_NO(soLatestName);
						venderAlterToERPObj.setLOCATION(loaciotn);
						venderAlterToERPObj.setGUID(partId);
						venderAlterToERPArray.add(venderAlterToERPObj);
					}
				}
			}
		}

		List<VenderAlterToERP> partVendorListToERPForCMC = venderAlterToERPArray.stream()
				.filter(obj -> obj.getLOCATION().equals("1300")).collect(Collectors.toList());
		List<VenderAlterToERP> partVendorListToERPForSDM = venderAlterToERPArray.stream()
				.filter(obj -> obj.getLOCATION().equals("9000") || obj.getLOCATION().equals("9001")).collect(Collectors.toList());
		venderAlterToERPArray.stream().forEach(obj -> obj.LOCATION = null);

		DomainObject pncDomObj = new DomainObject();
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();
		String result = "";
		
		try {
			if (venderAlterToDMSArray.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				result = dms.partVendor(translateJsonIntoString(venderAlterToDMSArray));
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			result = "(Error) " + e.getMessage();
			sendStatus = "Error";
		}finally {
			System.out.println(DMS + " " + methodName + " Result :" + result);
			System.out.println(DMS + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(venderAlterToDMSArray));
			System.out.println();
		}

		try {
			if (partVendorListToERPForCMC.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				result = erp.YPutSplmM02(translateJsonIntoString(partVendorListToERPForCMC));
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			result = "(Error) " + e.getMessage();
			sendStatus = "Error";
		} finally {
			System.out.println(ERP_YSD041_300 + " " + methodName + " Result :" + result);
			System.out.println(ERP_YSD041_300 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(partVendorListToERPForCMC));
			System.out.println();
		}

		try {
			if (partVendorListToERPForSDM.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_310ERP();
				result = erp.YPutSplmM02(translateJsonIntoString(partVendorListToERPForSDM));
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			result = "(Error) " + e.getMessage();
			sendStatus = "Error";
		} finally {
			System.out.println(ERP_YSD041_310 + " " + methodName + " Result :" + result);
			System.out.println(ERP_YSD041_310 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(partVendorListToERPForSDM));
			System.out.println();
		}
		
		return sendStatus;
	}

	private class VenderAlterToDMS extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public List<String> VENDOR_NO;
		public String SO_NO;
		public void setPART_NO(String pART_NO) {
			PART_NO = pART_NO;
		}
		public void setVENDOR_NO(List<String> vENDOR_NO) {
			VENDOR_NO = vENDOR_NO;
		}
		public void setSO_NO(String sO_NO) {
			SO_NO = sO_NO;
		}
	}

	private class VenderAlterToERP extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public String VENDOR_NO;
		public String SO_NO;
		public String LOCATION;
		public void setPART_NO(String pART_NO) {
			PART_NO = pART_NO;
		}
		public void setVENDOR_NO(String vENDOR_NO) {
			VENDOR_NO = vENDOR_NO;
		}
		public void setSO_NO(String sO_NO) {
			SO_NO = sO_NO;
		}
		public void setLOCATION(String lOCATION) {
			LOCATION = lOCATION;
		}
		public String getLOCATION() {
			return LOCATION;
		}
	}

	/**
	 * 14/15 Opt/Alt
	 * 
	 * @param context
	 * @param partMapList
	 * @param strAltOpt
	 * @return 
	 * @throws Exception
	 */
	private String _searchOptAltPart(Context context, MapList partMapList, String strAltOpt) throws Exception {
		String sendStatus = "Complete";

		String relWhere = null;
		if (strAltOpt.equalsIgnoreCase("alt")) {
			relWhere = "attribute[SPLM_OptionalType]=='Alternate Part'";
		} else if (strAltOpt.equalsIgnoreCase("opt")) {
			relWhere = "attribute[SPLM_OptionalType]!='Alternate Part' && attribute[SPLM_OptionalType]!=''";
		} else {
			System.out.println("args ONLY get 'alt' or 'opt'.");
			return "Error";
		}

		DomainObject domObj = new DomainObject();
		ArrayList<OptAltToDMS> OptAltToDMSArray = new ArrayList<OptAltToDMS>();
		ArrayList<OptAltToERP> OptAltToERPArray = new ArrayList<OptAltToERP>(); // SPLM_Part.att[Location] // =
																				// 9000,9001

		StringList reloptAltSelects = new StringList();
		reloptAltSelects.add("attribute[SPLM_OptionalType]");
		reloptAltSelects.add("attribute[SPLM_OptionalExchangeable]");

		for (Object partObj : partMapList) {
			Map partMap = (Map) partObj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partModifiedDate = (String) partMap.get(DomainConstants.SELECT_MODIFIED);
			String partLocationForERP = (String) partMap.get("attribute[SPLM_Location]");
			String partLocationForDMS = (String) partMap.get("attribute[SPLM_Location_DMS]");
			domObj.setId(partId);

			MapList optAltMapList = domObj.getRelatedObjects(context, "SPLM_RelatedOptionalPart", // relationshipPattern,
					"SPLM_Part", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					reloptAltSelects, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					relWhere, // String relationshipWhere
					0); // int limit)

			ArrayList<String> altOptPart = new ArrayList<String>();
			for (Object altOptObj : optAltMapList) {
				Map altOptMap = (Map) altOptObj;
				String altOptName = (String) altOptMap.get(DomainConstants.SELECT_NAME);
				String altOptExchange = (String) altOptMap.get("attribute[SPLM_OptionalExchangeable]");
				altOptPart.add(altOptName);
				// ERP system
				for (String location : partLocationForERP.split(SPLIT_STR)) {
					OptAltToERP OptAltToERPObj = new OptAltToERP(strAltOpt);
					OptAltToERPObj.setPART_NO(partName);
					OptAltToERPObj.setALT_OPT_PART(altOptName);
					OptAltToERPObj.setLOCATION(location);
					OptAltToERPObj.setEXCHANGEABLE(altOptExchange);
					OptAltToERPObj.setGUID(partId);
					OptAltToERPArray.add(OptAltToERPObj);
				}
			}

			// DMS system
			if (!altOptPart.isEmpty() && !partLocationForDMS.isEmpty()) {
				OptAltToDMS OptAltToDMSObj = new OptAltToDMS(strAltOpt);
				OptAltToDMSObj.setPART_NO(partName);
				OptAltToDMSObj.setALT_OPT_PART(altOptPart);
				OptAltToDMSObj.setGUID(partId);
				OptAltToDMSArray.add(OptAltToDMSObj);
			}
		}

		List<OptAltToERP> AlterPartToERPArrayForCMC = OptAltToERPArray.stream()
				.filter(alt -> alt.getLOCATION().equals("1300")).collect(Collectors.toList());

		List<OptAltToERP> AlterPartToERPArrayForSDM = OptAltToERPArray.stream()
				.filter(alt -> alt.getLOCATION().equals("9000") || alt.getLOCATION().equals("9001"))
				.collect(Collectors.toList());

		OptAltToERPArray.stream().forEach(obj -> obj.setLOCATION(null));

		DomainObject partDomObj = new DomainObject();
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName() + "_" + strAltOpt;
		String result = "";
		
		// connect CMC for test
		if (strAltOpt.equalsIgnoreCase("ALT")) {
			try {
				if (OptAltToDMSArray.size() > 0) {
					SPlmWSSoap dms = getSPlmWSSoapDMS();
					result = dms.partAlt(translateJsonIntoString(OptAltToDMSArray));
				} else {
					result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
				}
			} catch (Exception e) {
				result = "(Error) " + e.getMessage();
				sendStatus = "Error";
			} finally {
				System.out.println(DMS + " " + methodName + " Result :" + result);
				System.out.println(DMS + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(OptAltToDMSArray));
				System.out.println();
			}
			
			try {
				if (AlterPartToERPArrayForCMC.size() > 0) {
					YMM20_PortType erpForCMC = getYMM20_300ERP();
					result = erpForCMC.ymmReplaceMatToSap(translateJsonIntoString(AlterPartToERPArrayForCMC));
				} else {
					result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
				}
			} catch (Exception e) {
				result = "(Error) " + e.getMessage();
				sendStatus = "Error";
			} finally {
				System.out.println(ERP_YMM20_300 + " " + methodName + " Result :" + result);
				System.out.println(ERP_YMM20_300 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(AlterPartToERPArrayForCMC));
				System.out.println();
			}
			
			try {
				if (AlterPartToERPArrayForSDM.size() > 0) {
					YMM20_PortType erpForSDM = getYMM20_310ERP();
					result = erpForSDM.ymmReplaceMatToSap(translateJsonIntoString(AlterPartToERPArrayForSDM));
				} else {
					result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
				}
			} catch (Exception e) {
				result = "(Error) " + e.getMessage();
				sendStatus = "Error";
			} finally {
				System.out.println(ERP_YMM20_310 + " " + methodName + " Result :" + result);
				System.out.println(ERP_YMM20_310 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(AlterPartToERPArrayForSDM));
				System.out.println();
			}
		}

		if (strAltOpt.equalsIgnoreCase("OPT")) {
			try {
				if (OptAltToDMSArray.size() > 0) {
					SPlmWSSoap dms = getSPlmWSSoapDMS();
					result = dms.partOpt(translateJsonIntoString(OptAltToDMSArray));
				} else {
					result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
				}
			} catch (Exception e) {
				result = "(Error) " + e.getMessage();
				sendStatus = "Error";
			} finally {
				System.out.println(DMS + " " + methodName + " Result :" + result);
				System.out.println(DMS + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(OptAltToDMSArray));
				System.out.println();
			}
		}
		return sendStatus;
	}

	// 14. Alternate Part /15. Optional Part
	private class OptAltToDMS extends Abstract_CMC_ObjBaseAttribute {
		private String optType;

		public OptAltToDMS(String optType) {
			this.optType = optType;
		}

		private String PART_NO;
		private ArrayList<String> NEW_PART_NO;
		private ArrayList<String> REUSE_PART_NO;

		public String getPART_NO() {
			return PART_NO;
		}

		public void setPART_NO(String part_No) {
			this.PART_NO = part_No;
		}

		public void setALT_OPT_PART(ArrayList<String> SUB_PART_NO) {
			if (optType.equalsIgnoreCase("alt")) {
				NEW_PART_NO = SUB_PART_NO;
			} else if (optType.equalsIgnoreCase("opt")) {
				REUSE_PART_NO = SUB_PART_NO;
			}
			optType = null;
		}
	}

	private class OptAltToERP extends Abstract_CMC_ObjBaseAttribute {
		private String optType;

		public OptAltToERP(String optType) {
			this.optType = optType;
		}

		private String PART_NO;
		private String NEW_PART_NO;
		private String REUSE_PART_NO;
		private String LOCATION;
		private String EXCHANGEABLE;

		public String getPART_NO() {
			return PART_NO;
		}

		public void setPART_NO(String part_No) {
			this.PART_NO = part_No;
		}

		public String getALT_OPT_PART() {
			if (this.optType.equalsIgnoreCase("alt")) {
				return this.NEW_PART_NO;
			} else if (this.optType.equalsIgnoreCase("opt")) {
				return this.REUSE_PART_NO;
			} else {
				return "";
			}
		}

		public void setALT_OPT_PART(String subPart_No) {
			if (this.optType.equalsIgnoreCase("alt")) {
				this.NEW_PART_NO = subPart_No;
			} else if (this.optType.equalsIgnoreCase("opt")) {
				this.REUSE_PART_NO = subPart_No;
			}
			this.optType = null;
		}

		public String getLOCATION() {
			return this.LOCATION;
		}

		public void setLOCATION(String LOCATION) {
			this.LOCATION = LOCATION;
		}

		public void setEXCHANGEABLE(String exchangeable) {
			this.EXCHANGEABLE = exchangeable;
		}
	}

	/**
	 * 11 Part-PNC
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchPartPnc(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add("attribute[SPLM_Location]");

		String WhereSelect = "current=='Release' && to[SPLM_RelatedPart].from[SPLM_PNC]!='' && attribute[SPLM_PartPNC_DMS]=='Wait' && attribute[SPLM_PartPNC_ERP310]=='Wait' && attribute[SPLM_PartPNC_ERP300]=='Wait'"; // where

		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part,SPLM_ColorPart", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				WhereSelect, // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		ArrayList<PartPncToDMS> partPncToDMSArray = new ArrayList<PartPncToDMS>();
		ArrayList<PartPncToERP> partPncToERPArray = new ArrayList<PartPncToERP>();

		for (Object partObj : partMapList) {
			Map partMap = (Map) partObj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partAttLocationForERP = (String) partMap.get("attribute[SPLM_Location]");

			domObj.setId(partId);
			StringList pncNameList = domObj.getInfoList(context, "to[SPLM_RelatedPart].from[SPLM_PNC].name");
			List<String> pncNameLists = pncNameList.stream().distinct().collect(Collectors.toList());

			if (!pncNameLists.isEmpty()) {
				// json for DMS system
				PartPncToDMS partPncToDMSObj = new PartPncToDMS();
				partPncToDMSObj.setPART_NO(partName);
				partPncToDMSObj.setPNC_NO(pncNameLists);
				partPncToDMSObj.setGUID(partId);
				partPncToDMSArray.add(partPncToDMSObj);

				// json for ERP system
				for (String pncName : pncNameLists) {
					for (String partLocation : partAttLocationForERP.split(SPLIT_STR)) {
						PartPncToERP partPncToERPObj = new PartPncToERP();
						partPncToERPObj.setPART_NO(partName);
						partPncToERPObj.setPNC_NO(pncName);
						partPncToERPObj.setPLANT(partLocation);
						partPncToERPObj.setGUID(partId);
						partPncToERPArray.add(partPncToERPObj);
					}
				}
			}
		}

		List<PartPncToERP> partPncToERPForCMCArray = partPncToERPArray.stream().filter(obj -> obj.getPLANT().equals("1300"))
				.collect(Collectors.toList());
		List<PartPncToERP> partPncToERPForSDMArray = partPncToERPArray.stream()
				.filter(obj -> obj.getPLANT().equals("9000") || obj.getPLANT().equals("9001")).collect(Collectors.toList());

		partPncToERPArray.stream().forEach(obj -> obj.setPLANT(null));

		DomainObject partPncDomObj = new DomainObject();
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();
		String result = "";
		
		// connect CMC for test
		try {
			MqlUtil.mqlCommand(context, "trigger off");
			if (partPncToDMSArray.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				result = dms.partPnc(translateJsonIntoString(partPncToDMSArray));
				for (PartPncToDMS partPncObj : partPncToDMSArray) {
					partPncDomObj.setId(partPncObj.getGUID());
					partPncDomObj.setAttributeValue(context, "SPLM_PartPNC_DMS", "Complete");
				}
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			for (PartPncToDMS partPncObj : partPncToDMSArray) {
				partPncDomObj.setId(partPncObj.getGUID());
				partPncDomObj.setAttributeValue(context, "SPLM_PartPNC_DMS", "Error");
			}
			result = "(Error) " + e.getMessage();
		} finally {
			System.out.println(DMS + " " + methodName + " Result :" + result);
			System.out.println(DMS + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(partPncToDMSArray));
			System.out.println();
			MqlUtil.mqlCommand(context, "trigger on");
		}
		
		try {
			MqlUtil.mqlCommand(context, "trigger off");
			if (partPncToERPForCMCArray.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				result = erp.YPutSplmM03(translateJsonIntoString(partPncToERPForCMCArray));
				for (PartPncToERP partPncObj : partPncToERPForCMCArray) {
					partPncDomObj.setId(partPncObj.getGUID());
					partPncDomObj.setAttributeValue(context, "SPLM_PartPNC_ERP300", "Complete");
				}
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			for (PartPncToERP partPncObj : partPncToERPForCMCArray) {
				partPncDomObj.setId(partPncObj.getGUID());
				partPncDomObj.setAttributeValue(context, "SPLM_PartPNC_ERP300", "Error");
			}
			result = "(Error) " + e.getMessage();
		} finally {
			System.out.println(ERP_YSD041_300 + " " + methodName + " Result :" + result);
			System.out.println(ERP_YSD041_300 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(partPncToERPForCMCArray));
			System.out.println();
			MqlUtil.mqlCommand(context, "trigger on");
		}
		
		try {
			MqlUtil.mqlCommand(context, "trigger off");
			if (partPncToERPForSDMArray.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_310ERP();
				result = erp.YPutSplmM03(translateJsonIntoString(partPncToERPForSDMArray));
				for (PartPncToERP partPncObj : partPncToERPForSDMArray) {
					partPncDomObj.setId(partPncObj.getGUID());
					partPncDomObj.setAttributeValue(context, "SPLM_PartPNC_ERP310", "Complete");
				}
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			for (PartPncToERP partPncObj : partPncToERPForSDMArray) {
				partPncDomObj.setId(partPncObj.getGUID());
				partPncDomObj.setAttributeValue(context, "SPLM_PartPNC_ERP310", "Error");
			}
			result = "(Error) " + e.getMessage();
		} finally {
			System.out.println(ERP_YSD041_310 + " " + methodName + " Result :" + result);
			System.out.println(ERP_YSD041_310 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(partPncToERPForSDMArray));
			System.out.println();
			MqlUtil.mqlCommand(context, "trigger on");
		}
	}

	private class PartPncToDMS extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public List<String> PNC_NO;
		public void setPART_NO(String pART_NO) {
			PART_NO = pART_NO;
		}
		public void setPNC_NO(List<String> pNC_NO) {
			PNC_NO = pNC_NO;
		}
	}

	private class PartPncToERP extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public String PNC_NO;
		public void setPART_NO(String pART_NO) {
			PART_NO = pART_NO;
		}
		public void setPNC_NO(String pNC_NO) {
			PNC_NO = pNC_NO;
		}
	}

	/**
	 * 12 Part-Group
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchPartGroup(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_NAME);

		// find Object
		String WhereSelect = "current=='Release' && to[SPLM_RelatedPart].from[SPLM_GroupDrawing]!='' && attribute[SPLM_PartGroupCode_DMS]=='Wait'"; // where

		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part,SPLM_ColorPart", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				WhereSelect, // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		ArrayList<PartGroupCodeToDMS> partGroupCodeToDMSArray = new ArrayList<PartGroupCodeToDMS>();

		for (Object partObj : partMapList) {
			Map partMap = (Map) partObj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			domObj.setId(partId);

			StringList aa = domObj.getInfoList(context,
					"to[SPLM_RelatedPart].from[SPLM_GroupDrawing].attribute[SPLM_GroupCode]");
			List<String> partGroupCodeList = aa.stream().distinct().collect(Collectors.toList());

			// json for DMS System
			if (!partGroupCodeList.isEmpty()) {
				PartGroupCodeToDMS partGroupCodeToDMSObj = new PartGroupCodeToDMS();
				partGroupCodeToDMSObj.setPART_NO(partName);
				partGroupCodeToDMSObj.setGROUP_CODE(partGroupCodeList);
				partGroupCodeToDMSObj.setGUID(partId);
				partGroupCodeToDMSArray.add(partGroupCodeToDMSObj);
			}
		}

		DomainObject partGroupCodeDomObj = new DomainObject();
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();
		String result = "";
		
		// connect CMC for test
		try {
			MqlUtil.mqlCommand(context, "trigger off");
			if (partGroupCodeToDMSArray.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				result = dms.partGroup(translateJsonIntoString(partGroupCodeToDMSArray));
				for (PartGroupCodeToDMS partGroupCodeObj : partGroupCodeToDMSArray) {
					partGroupCodeDomObj.setId(partGroupCodeObj.getGUID());
					partGroupCodeDomObj.setAttributeValue(context, "SPLM_PartGroupCode_DMS", "Complete");
				}
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			for (PartGroupCodeToDMS partGroupCodeObj : partGroupCodeToDMSArray) {
				partGroupCodeDomObj.setId(partGroupCodeObj.getGUID());
				partGroupCodeDomObj.setAttributeValue(context, "SPLM_PartGroupCode_DMS", "Error");
			}
			result = "(Error) " + e.getMessage();
		} finally {
			System.out.println(DMS + " " + methodName + " Result :" + result);
			System.out.println(DMS + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(partGroupCodeToDMSArray));
			System.out.println();
			MqlUtil.mqlCommand(context, "trigger on");			
		}
	}

	private class PartGroupCodeToDMS extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public List<String> GROUP_CODE;
		public void setPART_NO(String pART_NO) {
			PART_NO = pART_NO;
		}
		public void setGROUP_CODE(List<String> gROUP_CODE) {
			GROUP_CODE = gROUP_CODE;
		}
	}

	/**
	 * 18 Group-PNC
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchGroupPnc(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add("attribute[SPLM_GroupCode]");

		MapList groupDrawingMapList = DomainObject.findObjects(context, "SPLM_GroupDrawing", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"from[SPLM_Related_PNC]==TRUE", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		ArrayList<GroupCodePncToERP> groupCodePncToERPArray = new ArrayList<GroupCodePncToERP>();

		for (Object groupDrawingObj : groupDrawingMapList) {
			Map groupDrawingMap = (Map) groupDrawingObj;
			String groupDrawingId = (String) groupDrawingMap.get(DomainConstants.SELECT_ID);
			String groupDrawingAttGroupCode = (String) groupDrawingMap.get("attribute[SPLM_GroupCode]");
			domObj.setId(groupDrawingId);
			StringList pncNameList = domObj.getInfoList(context, "from[SPLM_Related_PNC].to[SPLM_PNC].name");

			MapList groupCodeMapList = DomainObject.findObjects(context, "SPLM_GroupCode", // type
					groupDrawingAttGroupCode, // name
					"*", // revision
					"*", // owner
					VAULT, // vault
					null, // where
					null, false, new StringList(DomainConstants.SELECT_ID), (short) 1);

			// json for ERP system
			for (Object groupCodeObj : groupCodeMapList) {
				for (String pncName : pncNameList) {
					GroupCodePncToERP groupCodePncToERPObj = new GroupCodePncToERP();
					groupCodePncToERPObj.setGROUP_CODE(groupDrawingAttGroupCode);
					groupCodePncToERPObj.setPNC_NO(pncName);
					groupCodePncToERPObj.setGUID((String) ((Map) groupCodeObj).get(DomainConstants.SELECT_ID));
					groupCodePncToERPArray.add(groupCodePncToERPObj);
				}
			}
		}
		String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		String result = "";

		try {
			if (groupCodePncToERPArray.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				result = erp.YPutSplmP03(translateJsonIntoString(groupCodePncToERPArray));
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			result = "(Error) " + e.getMessage();
		} finally {
			System.out.println(ERP_YSD041_300 + " " + methodName + " Result :" + result);
			System.out.println(ERP_YSD041_300 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(groupCodePncToERPArray));
			System.out.println();
		}
	}

	private class GroupCodePncToERP extends Abstract_CMC_ObjBaseAttribute {
		public String GROUP_CODE;
		public String PNC_NO;
		public void setGROUP_CODE(String gROUP_CODE) {
			GROUP_CODE = gROUP_CODE;
		}
		public void setPNC_NO(String pNC_NO) {
			PNC_NO = pNC_NO;
		}
	}

	/**
	 * 8. ServicePart
	 * 
	 * @param context
	 * @param partList
	 * @param triggerFrom
	 * @throws Exception
	 */
	private String _searchServicePart(Context context, MapList partList, String triggerFrom) throws Exception {
		String sendStatus = "Complete";
		
		StringList busSelectSO = new StringList();
		busSelectSO.add(DomainConstants.SELECT_NAME);
		busSelectSO.add("current.actual");
		busSelectSO.add("attribute[SPLM_SO_Location]");

		String altStr = "Alternate Part";
		String optStr = "Optional Part";
		String domesticSoLocation = "Domestic Item";
		String exportSoLocation = "Export Item";

		DomainObject domObj = new DomainObject();
		List<Part> partListToDMS = new ArrayList<Part>();
		List<Part> partArrayToERP = new ArrayList<Part>();

		for (Object partObj : partList) {
			Map<String, String> partMap = (Map) partObj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);

			domObj.setId(partId);
//			StringList groupCodeList = domObj.getInfoList(context,
//					"to[SPLM_RelatedPart].from[SPLM_GroupDrawing].attribute[SPLM_GroupCode]");
			String groupCode = partMap.get("attribute[SPLM_GroupCode]");
			String partAttMaterialGroup = partMap.get("attribute[SPLM_MaterialGroup]");
			String partAttLocationForERP = partMap.get("attribute[SPLM_Location]");
			String partAttLocationForDMS = partMap.get("attribute[SPLM_Location_DMS]");
			String attBigCarType = partMap.get("attribute[SPLM_DTAT_Only]");
			String partAttMs = partMap.get("attribute[SPLM_ModelSeries]");
			String partAttMc = partMap.get("attribute[SPLM_CMC_Code]");
			String partAttPNCType = partMap.get("attribute[SPLM_CommissionGroup]");
			String attTransGroup = "";
			String attSoLatestName = "";
			String attSoLatestDate = "";
			String attSoAttLocation = "";
			String attCarCaegory = "";
			String attManufactureCode = "";
			String attItemCategoryGroup = "";
			String attSalesOrg = "";
			String partLatestMs = "";
			String partLatestMc = "";

			// VENDOR data processing
			MapList vendorMapList = domObj.getRelatedObjects(context, "SPLM_RelatedVendor", // relationshipPattern,
					"SPLM_Vendor", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_Valid]==Y", // String relationshipWhere
					0); // int limit)

			List<String> partAttVendorList = (List<String>) vendorMapList.stream()
					.map(vendor -> (String) ((Map) vendor).get(DomainConstants.SELECT_NAME))
					.collect(Collectors.toList());

			String vendorName = partAttVendorList.isEmpty() ? "" : partAttVendorList.get(0);

			// ITEM_CATEGORY_GROUP data processing
			switch (partAttPNCType) {
			case "Z1":
			case "Z4":
			case "Z5":
				attItemCategoryGroup = "BANS";
				break;
			case "Z3":
				attItemCategoryGroup = "BANC";
				break;
			default:
				attItemCategoryGroup = "NORM";
			}

			// MS / MC data processing
			ArrayList<String> partAttMcArray = new ArrayList<String>();
			ArrayList<String> partAttMsArray = new ArrayList<String>();

			for (String mcStr : partAttMc.split(SPLIT_STR)) {
				if (!mcStr.isEmpty()) {
					partAttMcArray.add(mcStr);
				}
			}
			
			Map<String, String> mcMap = getCmcCodeSpecifyInfo(context, partAttMcArray);
			// 找最新的(EnableDate)Mc 丟完主檔後後續對方不再更新 或者是 有修改持續更新之類的方式
			if(mcMap.size() == 1) {
				partLatestMc = mcMap.keySet().stream().findFirst().get();					
			} else if(mcMap.size() > 1) {
				String partLatestMcDate = "";
				for (String mcStr : mcMap.keySet()) {
					if(compareDateOnlyYMD(partLatestMcDate, mcMap.get(mcStr))) {
						partLatestMcDate = mcMap.get(mcStr);
						partLatestMc = mcStr;
					}
				}
			} else if ( !partAttMcArray.isEmpty() ) {
				partLatestMc = partAttMcArray.get(partAttMcArray.size() -1);
			}

			for (String msStr : partAttMs.split(SPLIT_STR)) {
				if (!msStr.isEmpty()) {
					partAttMsArray.add(msStr);
				}
			}
			
			// 找最新的(Originated)Ms 丟完主檔後後續對方不再更新 或者是 有修改持續更新之類的方式
			Map<String, String> msMap = getMsSpecifyInfo(context, partAttMsArray);
			if(msMap.size() == 1) {
				partLatestMs = msMap.keySet().stream().findFirst().get();					
			} else if(msMap.size() > 1) {
				String partLatestMsDate = "";
				for (String msStr : msMap.keySet()) {
					if(compareDateOnlyYMD(partLatestMsDate, msMap.get(msStr))) {
						partLatestMsDate = msMap.get(msStr);
						partLatestMs = msStr;
					}
				}
			} else if ( !partAttMsArray.isEmpty() ) {
				partLatestMs = partAttMsArray.get(partAttMsArray.size() -1);
			}

			// TRANS_GROUP data processing
			if (partAttLocationForERP.equals("1300")) {
				if (partAttPNCType.equals("Z1") || partAttPNCType.equals("Z3") || partAttPNCType.equals("Z4")
						|| partAttPNCType.equals("Z5")) {
					attTransGroup = "DUMY";
				}
			} else if (partAttLocationForERP.equals("9001") || partAttLocationForERP.equals("9000")) {
				attTransGroup = "ZS01";
			}

			// ALT_PART_NO / REUSE_PART_NO data processing
			MapList partOptList = domObj.getRelatedObjects(context, "SPLM_RelatedOptionalPart", // relationshipPattern,
					"SPLM_Part", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					new StringList("attribute[SPLM_OptionalType]"), // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			Map<String, String> mapping = new HashMap<String, String>();
			for (Object partOpt : partOptList) {
				String optPartName = (String) ((Map) partOpt).get(DomainConstants.SELECT_NAME);
				String attOptionalType = (String) ((Map) partOpt).get("attribute[SPLM_OptionalType]");
				attOptionalType = attOptionalType.equalsIgnoreCase(altStr) ? altStr : optStr;
				mapping.put(attOptionalType, optPartName);
			}

			// SPLM_SO data processing
			MapList soMapList = domObj.getRelatedObjects(context, "SPLM_AffectedItem", // relationshipPattern,
					"SPLM_SO", // typePattern,
					busSelectSO, // StringList objectSelects,
					null, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			if (!soMapList.isEmpty()) {
				soMapList.addSortKey("current.actual", "descending", "date");
				soMapList.sort();
				attSoLatestName = (String) ((Map) soMapList.get(0)).get(DomainConstants.SELECT_NAME);
				attSoLatestDate = (String) ((Map) soMapList.get(0)).get("current.actual");
				attSoAttLocation = (String) ((Map) soMapList.get(0)).get("attribute[SPLM_SO_Location]");
			}

			// CAR_CAEGORY data processing
			if (!partAttMsArray.isEmpty()) {
				String msName = partAttMsArray.get(0);
				MapList msList = DomainObject.findObjects(context, "SPLM_ModelSeries", // type
						msName, // name
						"*", // revision
						"*", // owner
						VAULT, // vault
						"", // where
						null, false, new StringList("attribute[SPLM_Vehicles]"), (short) 0);
				for (Object msObj : msList) {
					attCarCaegory = (String) ((Map) msObj).get("attribute[SPLM_Vehicles]");
				}
			}

			// MANUFACTURER_CODE data processing
			if (attSoAttLocation.equalsIgnoreCase(domesticSoLocation)) {
				if (vendorName.equalsIgnoreCase("MAE") || vendorName.equalsIgnoreCase("MMC")
						|| vendorName.equalsIgnoreCase("MFTBC") || vendorName.equalsIgnoreCase("MM")) {
					attManufactureCode = vendorName;
				}
			} else if (attSoAttLocation.equalsIgnoreCase(exportSoLocation)) {
				if (vendorName.equalsIgnoreCase("MMC") || vendorName.equalsIgnoreCase("MMAL")
						|| vendorName.equalsIgnoreCase("MMMA") || vendorName.equalsIgnoreCase("MMSE")) {
					attManufactureCode = vendorName;
				}
			}

			// PART_TYPE data processing
			String partAttItemSubType = ((String) partMap.get("attribute[SPLM_ItemSubType]"));
			if (!partAttItemSubType.isEmpty()) {
				partAttItemSubType = partAttItemSubType.substring(0, 1);
			}
			// json for ERP/DMS system
			Part part = new Part();
			part.setPART_NO(partMap.get(DomainConstants.SELECT_NAME));
			part.setPART_SH_NO(partMap.get("attribute[SPLM_OrderType]"));
			part.setK_D((partAttMaterialGroup.equalsIgnoreCase("KD") || partAttMaterialGroup.equalsIgnoreCase("KDY"))
					? "K"
					: "D");
//			part.GROUP_CODE = groupCodeList.isEmpty() ? "" : groupCodeList.get(0);
			part.setGROUP_CODE(groupCode); // 抓自身身上的ModelCode 單純只要初始件建立用
			part.setALT_PART_NO(mapping.containsKey(altStr) ? mapping.get(altStr) : "");
			part.setREUSE_PART_NO(mapping.containsKey(optStr) ? mapping.get(optStr) : "");
			part.setMATERIAL_TYPE(partMap.get("attribute[SPLM_MaterialType]"));
			part.setUNIT(partMap.get("attribute[SPLM_Unit]"));
			part.setPURCHASING_DEPARTMENT_NO(partMap.get("attribute[SPLM_PurchasingGroup]"));
			part.setMANUFACTURER_CODE(attManufactureCode);
			part.setVENDOR_NO(vendorName);
			part.setPART_NAME_C(partMap.get("attribute[SPLM_Name_TC]"));
			part.setPART_NAME_E(partMap.get("attribute[SPLM_Name_EN]"));
			part.setSALES_ORG(attSalesOrg);
			part.setPNC_NO(partMap.get("attribute[SPLM_PNC]"));
			part.setITEM_CATEGORY_GROUP(attItemCategoryGroup);
			part.setMODEL_SERIES(partLatestMs);
			part.setMODEL_CODE(partLatestMc);
			part.setCAR_CAEGORY(attCarCaegory);
			part.setPART_TYPE(partAttItemSubType);
			part.setBIG_CAR_TYPE(attBigCarType);
			part.setCOMMISSION_GROUP(partAttPNCType);
			part.setTRANS_GROUP(attTransGroup);
			part.setSO_NO(attSoLatestName);
			part.setSO_RELEASE_DATE(attSoLatestDate.isEmpty() ? "" : convertDateFormat(attSoLatestDate, "yyMMdd"));
			part.setHAS_SUBASSEMBLY(partMap.get("attribute[SPLM_HaveSubPart]").equalsIgnoreCase("TRUE")
					? "Y"
					: "N");
			part.setPART_OWNER(partMap.get(DomainConstants.SELECT_OWNER));
			part.setIS_SERVICE_PART(partMap.get("attribute[SPLM_IsServicePart]").equalsIgnoreCase("TRUE")
					? "Y"
					: "N");
			part.setEXPORT_PART(partMap.get("attribute[SPLM_OverseaSalesOnly]").equalsIgnoreCase("TRUE")
					? "Y"
					: "N");
			part.setCREATE_DATE(convertDateFormat(partMap.get(DomainConstants.SELECT_ORIGINATED),
					"yyyyMMdd"));
			part.setGUID(partId);

			for (String partLocation : partAttLocationForERP.split(SPLIT_STR)) {
				Part partERP = (Part) part.clone(); 
				partERP.setMATERIAL_GROUP(partAttMaterialGroup);
				partERP.setPLANT(partLocation);
				partERP.setSALES_ORG(partLocation.equals("1300") ? "3000"
						: partLocation.equals("9001") ? "9000" : partLocation.equals("9000") ? "SDM" : "");
				partArrayToERP.add(partERP);
			}
			
			if (!partAttLocationForDMS.isEmpty()) {
				Part partDMS = (Part) part.clone(); 
				partDMS.setMATERIAL_GROUP(partAttMaterialGroup.equalsIgnoreCase("KD")
						|| partAttMaterialGroup.equalsIgnoreCase("KDY") ? "K" : "D");
				partDMS.setPLANT(partAttLocationForDMS);
				partListToDMS.add(partDMS);
			}
		}

		// SO repackage
		List<Part> partListToERPForCMC = partArrayToERP.stream().filter(part -> part.PLANT.equals("1300"))
				.collect(Collectors.toList());
		List<Part> partListToERPForSDM = partArrayToERP.stream()
				.filter(part -> part.PLANT.equals("9000") || part.PLANT.equals("9001")).collect(Collectors.toList());

		DomainObject partDomObj = new DomainObject();

		String dmsFileName = "8_DMS_" + triggerFrom + "_ServicePart.txt";
		String erp300FileName = "8_ERP_" + triggerFrom + "_ServicePart_CMC.txt";
		String erp310FileName = "8_ERP_" + triggerFrom + "_ServicePart_SDM.txt";
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();
		String result = "";
		
		// connect CMC for test
		try {
			MqlUtil.mqlCommand(context, "trigger off");
			if (partListToDMS.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				result = dms.servicesPart(translateJsonIntoString(partListToDMS));
				for (Part partObj : partListToDMS) {
					partDomObj.setId(partObj.getGUID());
					if (triggerFrom.equalsIgnoreCase("so")) {
						partDomObj.setAttributeValue(context, "SPLM_DMS_Sync", "True");
					} else {
						partDomObj.setAttributeValue(context, "SPLM_ServicePart_DMS", "Complete");
					}
				}
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			if ( !triggerFrom.equalsIgnoreCase("so") ) {
				for (Part partObj : partListToDMS) {
					partDomObj.setId(partObj.getGUID());
					partDomObj.setAttributeValue(context, "SPLM_ServicePart_DMS", "Error");
				}
			}
			result = "(Error) " + e.getMessage();
			sendStatus = "Error";
		} finally {
			System.out.println(DMS + " " + methodName + "_" + triggerFrom + " Result :" + result);
			System.out.println(DMS + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(partListToDMS));
			System.out.println();
			MqlUtil.mqlCommand(context, "trigger on");			
		}

		try {
			MqlUtil.mqlCommand(context, "trigger off");
			if (partListToERPForCMC.size() > 0) {
				YMM20_PortType erpForCMC = getYMM20_300ERP();
				result = erpForCMC.ymmSplmMatToSap(translateJsonIntoString(partListToERPForCMC));
				for (Part partObj : partListToERPForCMC) {
					partDomObj.setId(partObj.getGUID());
					if (triggerFrom.equalsIgnoreCase("so")) {
						partDomObj.setAttributeValue(context, "SPLM_ERP1300_Sync", "True");
					} else {
						partDomObj.setAttributeValue(context, "SPLM_ServicePart_ERP300", "Complete");
					}
				}
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			if ( !triggerFrom.equalsIgnoreCase("so") ) {
				for (Part partObj : partListToERPForCMC) {
					partDomObj.setId(partObj.getGUID());
					partDomObj.setAttributeValue(context, "SPLM_ServicePart_ERP300", "Error");
				}
			}
			result = "(Error) " + e.getMessage();
			sendStatus = "Error";
		} finally {
			System.out.println(ERP_YMM20_300 + " " + methodName + "_" + triggerFrom + " Result :" + result);
			System.out.println(ERP_YMM20_300 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(partListToERPForCMC));
			System.out.println();
			MqlUtil.mqlCommand(context, "trigger on");			
		}

		try {
			MqlUtil.mqlCommand(context, "trigger off");
			if (partListToERPForSDM.size() > 0) {
				YMM20_PortType erpForSDM = getYMM20_310ERP();
				result = erpForSDM.ymmSplmMatToSap(translateJsonIntoString(partListToERPForSDM));
				for (Part partObj : partListToERPForSDM) {
					partDomObj.setId(partObj.getGUID());
					String erpLocation = partObj.PLANT;
					if (triggerFrom.equalsIgnoreCase("so")) {
						if (partObj.PLANT.equals("9000")) {
							partDomObj.setAttributeValue(context, "SPLM_ERP9000_Sync", "True");
						} 
						else{
							partDomObj.setAttributeValue(context, "SPLM_ERP9001_Sync", "True");
						}
					} else {
						partDomObj.setAttributeValue(context, "SPLM_ServicePart_ERP310", "Complete");
					}
				}
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			if ( !triggerFrom.equalsIgnoreCase("so") ) {
				for (Part partObj : partListToERPForSDM) {
					partDomObj.setId(partObj.getGUID());
					partDomObj.setAttributeValue(context, "SPLM_ServicePart_ERP310", "Error");
				}
			}
			result = "(Error) " + e.getMessage();
			sendStatus = "Error";
		} finally {
			System.out.println(ERP_YMM20_310 + " " + methodName + "_" + triggerFrom + " Result :" + result);
			System.out.println(ERP_YMM20_310 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(partListToERPForSDM));
			System.out.println();
			MqlUtil.mqlCommand(context, "trigger on");			
		}
		
		return sendStatus;
	}
	
	private class Part extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public String PART_SH_NO;
		public String K_D;
		public String GROUP_CODE;
		public String ALT_PART_NO;
		public String REUSE_PART_NO;
		public String MATERIAL_TYPE;
		public String MATERIAL_GROUP;
		public String UNIT;
		public String PURCHASING_DEPARTMENT_NO;
		public String MANUFACTURER_CODE;
		public String VENDOR_NO;
		public String PART_NAME_C;
		public String PART_NAME_E;
		public String SALES_ORG;
		public String PNC_NO;
		public String ITEM_CATEGORY_GROUP;
		public String MODEL_SERIES;
		public String MODEL_CODE;
		public String CAR_CAEGORY;
		public String COMMISSION_GROUP;
		public String TRANS_GROUP;
		public String BIG_CAR_TYPE;
		public String PART_TYPE;
		public String SO_NO;
		public String SO_RELEASE_DATE;
		public String HAS_SUBASSEMBLY;
		public String PART_OWNER;
		public String IS_SERVICE_PART;
		public String EXPORT_PART;
		public void setPART_NO(String pART_NO) {
			PART_NO = pART_NO;
		}
		public void setPART_SH_NO(String pART_SH_NO) {
			PART_SH_NO = pART_SH_NO;
		}
		public void setK_D(String k_D) {
			K_D = k_D;
		}
		public void setGROUP_CODE(String gROUP_CODE) {
			GROUP_CODE = gROUP_CODE;
		}
		public void setALT_PART_NO(String aLT_PART_NO) {
			ALT_PART_NO = aLT_PART_NO;
		}
		public void setREUSE_PART_NO(String rEUSE_PART_NO) {
			REUSE_PART_NO = rEUSE_PART_NO;
		}
		public void setMATERIAL_TYPE(String mATERIAL_TYPE) {
			MATERIAL_TYPE = mATERIAL_TYPE;
		}
		public void setMATERIAL_GROUP(String mATERIAL_GROUP) {
			MATERIAL_GROUP = mATERIAL_GROUP;
		}
		public void setUNIT(String uNIT) {
			UNIT = uNIT;
		}
		public void setPURCHASING_DEPARTMENT_NO(String pURCHASING_DEPARTMENT_NO) {
			PURCHASING_DEPARTMENT_NO = pURCHASING_DEPARTMENT_NO;
		}
		public void setMANUFACTURER_CODE(String mANUFACTURER_CODE) {
			MANUFACTURER_CODE = mANUFACTURER_CODE;
		}
		public void setVENDOR_NO(String vENDOR_NO) {
			VENDOR_NO = vENDOR_NO;
		}
		public void setPART_NAME_C(String pART_NAME_C) {
			PART_NAME_C = pART_NAME_C;
		}
		public void setPART_NAME_E(String pART_NAME_E) {
			PART_NAME_E = pART_NAME_E;
		}
		public void setSALES_ORG(String sALES_ORG) {
			SALES_ORG = sALES_ORG;
		}
		public void setPNC_NO(String pNC_NO) {
			PNC_NO = pNC_NO;
		}
		public void setITEM_CATEGORY_GROUP(String iTEM_CATEGORY_GROUP) {
			ITEM_CATEGORY_GROUP = iTEM_CATEGORY_GROUP;
		}
		public void setMODEL_SERIES(String mODEL_SERIES) {
			MODEL_SERIES = mODEL_SERIES;
		}
		public void setMODEL_CODE(String mODEL_CODE) {
			MODEL_CODE = mODEL_CODE;
		}
		public void setCAR_CAEGORY(String cAR_CAEGORY) {
			CAR_CAEGORY = cAR_CAEGORY;
		}
		public void setCOMMISSION_GROUP(String cOMMISSION_GROUP) {
			COMMISSION_GROUP = cOMMISSION_GROUP;
		}
		public void setTRANS_GROUP(String tRANS_GROUP) {
			TRANS_GROUP = tRANS_GROUP;
		}
		public void setBIG_CAR_TYPE(String bIG_CAR_TYPE) {
			BIG_CAR_TYPE = bIG_CAR_TYPE;
		}
		public void setPART_TYPE(String pART_TYPE) {
			PART_TYPE = pART_TYPE;
		}
		public void setSO_NO(String sO_NO) {
			SO_NO = sO_NO;
		}
		public void setSO_RELEASE_DATE(String sO_RELEASE_DATE) {
			SO_RELEASE_DATE = sO_RELEASE_DATE;
		}
		public void setHAS_SUBASSEMBLY(String hAS_SUBASSEMBLY) {
			HAS_SUBASSEMBLY = hAS_SUBASSEMBLY;
		}
		public void setPART_OWNER(String pART_OWNER) {
			PART_OWNER = pART_OWNER;
		}
		public void setIS_SERVICE_PART(String iS_SERVICE_PART) {
			IS_SERVICE_PART = iS_SERVICE_PART;
		}
		public void setEXPORT_PART(String eXPORT_PART) {
			EXPORT_PART = eXPORT_PART;
		}
	}

	/**
	 * 22. ModeCode_GroupCode
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchMcGroupCode(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add("attribute[SPLM_CMC_Code]");

		MapList mcMapList = DomainObject.findObjects(context, "SPLM_ModelCode", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"to[SPLM_SBOM]==TRUE", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		ArrayList<McGroupCodeToERP> mcGroupCodeToERPArray = new ArrayList<McGroupCodeToERP>();

		for (Object obj : mcMapList) {
			Map mcMap = (Map) obj;
			String mcAttCMCCode = (String) mcMap.get("attribute[SPLM_CMC_Code]");
			String mcId = (String) mcMap.get(DomainConstants.SELECT_ID);
			domObj.setId(mcId);
			StringList groupDrawingAttGroupCodeList = domObj.getInfoList(context,
					"to[SPLM_SBOM].from[SPLM_GLNO].from[SPLM_RelatedPartsCatalogue].to[SPLM_PartsCatalogue].from[SPLM_RelatedGroupDrawing].to[SPLM_GroupDrawing].attribute[SPLM_GroupCode]");

			// json for ERP system
			for (String groupDrawingAttGroupCode : groupDrawingAttGroupCodeList) {
				if (!groupDrawingAttGroupCode.isEmpty()) {
					McGroupCodeToERP mcGroupCodeToERPObj = new McGroupCodeToERP();
					mcGroupCodeToERPObj.setMODEL_CODE(mcAttCMCCode);
					mcGroupCodeToERPObj.setGROUP_NO(groupDrawingAttGroupCode);
					mcGroupCodeToERPObj.setGUID(mcId);
					mcGroupCodeToERPArray.add(mcGroupCodeToERPObj);
				}
			}
		}
		String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		String result = "";

		try {
			if (mcGroupCodeToERPArray.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				result = erp.YPutSplmMcg(translateJsonIntoString(mcGroupCodeToERPArray));
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			result = "(Error) " + e.getMessage();
		} finally {
			System.out.println(ERP_YSD041_300 + " " + methodName + " Result :" + result);
			System.out.println(ERP_YSD041_300 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(mcGroupCodeToERPArray));
			System.out.println();
		}
	}

	private class McGroupCodeToERP extends Abstract_CMC_ObjBaseAttribute {
		public String MODEL_CODE;
		public String GROUP_NO;
		public void setMODEL_CODE(String mODEL_CODE) {
			MODEL_CODE = mODEL_CODE;
		}
		public void setGROUP_NO(String gROUP_NO) {
			GROUP_NO = gROUP_NO;
		}
	}

	/**
	 * 20. ModelSeries_PartCategory
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchMsPartCategory(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add("attribute[SPLM_Brand]");

		MapList msMapList = DomainObject.findObjects(context, "SPLM_ModelSeries", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"from[SPLM_SBOM]==TRUE", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		ArrayList<MsPartCategoryToERP> msPartCategoryToERPArray = new ArrayList<MsPartCategoryToERP>();

		for (Object obj : msMapList) {
			Map msMap = (Map) obj;
			String msId = (String) msMap.get(DomainConstants.SELECT_ID);
			String msName = (String) msMap.get(DomainConstants.SELECT_NAME);
			String msBrand = (String) msMap.get("attribute[SPLM_Brand]");
			domObj.setId(msId);

			StringList partsCatalogueNameList = domObj.getInfoList(context,
					"from[SPLM_SBOM].to[SPLM_GLNO].from[SPLM_RelatedPartsCatalogue].to[SPLM_PartsCatalogue].name");

			// json for ERP system
			for (String partsCatalogueName : partsCatalogueNameList) {
				if (!partsCatalogueName.isEmpty()) {
					MsPartCategoryToERP msPartCategoryToERPObj = new MsPartCategoryToERP();
					msPartCategoryToERPObj.setMODEL_SERIES(msName);
					msPartCategoryToERPObj.setPART_CATEGORY_NO(partsCatalogueName);
					msPartCategoryToERPObj.setMITSUBISHI_FLAG(msBrand.equalsIgnoreCase("mmc") ? "Y" : "N");
					msPartCategoryToERPObj.setGUID(msId);
					msPartCategoryToERPArray.add(msPartCategoryToERPObj);
				}
			}
		}
		String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		String result = "";

		// connect CMC for test
		try {
			if (msPartCategoryToERPArray.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				result = erp.YPutSplmMsp(translateJsonIntoString(msPartCategoryToERPArray));
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			result = "(Error) " + e.getMessage();
		} finally {
			System.out.println(ERP_YSD041_300 + " " + methodName + " Result :" + result);
			System.out.println(ERP_YSD041_300 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(msPartCategoryToERPArray));
			System.out.println();
		}

	}

	private class MsPartCategoryToERP extends Abstract_CMC_ObjBaseAttribute {
		public String MODEL_SERIES;
		public String PART_CATEGORY_NO;
		public String MITSUBISHI_FLAG;
		public void setMODEL_SERIES(String mODEL_SERIES) {
			MODEL_SERIES = mODEL_SERIES;
		}
		public void setPART_CATEGORY_NO(String pART_CATEGORY_NO) {
			PART_CATEGORY_NO = pART_CATEGORY_NO;
		}
		public void setMITSUBISHI_FLAG(String mITSUBISHI_FLAG) {
			MITSUBISHI_FLAG = mITSUBISHI_FLAG;
		}
	}

	/**
	 * 21. PartCategory_ModelCode
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchPartsCatelogueMc(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_NAME);

		MapList partsCatelogueMapList = DomainObject.findObjects(context, "SPLM_PartsCatalogue", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"to[SPLM_RelatedPartsCatalogue]==TRUE", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		ArrayList<PartsCatelogueMcToERP> partsCatelogueMcToERPArray = new ArrayList<PartsCatelogueMcToERP>();

		for (Object obj : partsCatelogueMapList) {
			Map partsCatelogueMap = (Map) obj;
			String partsCatelogueId = (String) partsCatelogueMap.get(DomainConstants.SELECT_ID);
			String partsCatelogueName = (String) partsCatelogueMap.get(DomainConstants.SELECT_NAME);
			domObj.setId(partsCatelogueId);

			StringList mcNameList = domObj.getInfoList(context,
					"to[SPLM_RelatedPartsCatalogue].from[SPLM_GLNO].from[SPLM_SBOM].to[SPLM_ModelCode].attribute[SPLM_CMC_Code]");

			for (String mcName : mcNameList) {
				if (!mcName.isEmpty()) {
					PartsCatelogueMcToERP partsCatelogueMcToERPObj = new PartsCatelogueMcToERP();
					partsCatelogueMcToERPObj.setPART_CATEGORY_NO(partsCatelogueName);
					partsCatelogueMcToERPObj.setMODEL_CODE(mcName);
					partsCatelogueMcToERPObj.setCTG_MODEL("");
					partsCatelogueMcToERPObj.setGUID(partsCatelogueId);
					partsCatelogueMcToERPArray.add(partsCatelogueMcToERPObj);
				}
			}
		}
		String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		String result = "";

		try {
			if (partsCatelogueMcToERPArray.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				result = erp.YPutSplmMpc(translateJsonIntoString(partsCatelogueMcToERPArray));
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			result = "(Error) " + e.getMessage();
		} finally {
			System.out.println(ERP_YSD041_300 + " " + methodName + " Result :" + result);
			System.out.println(ERP_YSD041_300 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(partsCatelogueMcToERPArray));
			System.out.println();
		}
	}

	private class PartsCatelogueMcToERP extends Abstract_CMC_ObjBaseAttribute {
		public String PART_CATEGORY_NO;
		public String MODEL_CODE;
		public String CTG_MODEL;
		public void setPART_CATEGORY_NO(String pART_CATEGORY_NO) {
			PART_CATEGORY_NO = pART_CATEGORY_NO;
		}
		public void setMODEL_CODE(String mODEL_CODE) {
			MODEL_CODE = mODEL_CODE;
		}
		public void setCTG_MODEL(String cTG_MODEL) {
			CTG_MODEL = cTG_MODEL;
		}
	}

	/**
	 * 13. PartModel
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchPartModel(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add("attribute[SPLM_CMC_Code]");

		String WhereSelect = "current=='Release' && attribute[SPLM_CMC_Code]!='' && attribute[SPLM_PartModel_DMS]=='Wait'";
		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part,SPLM_ColorPart", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				WhereSelect, // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		ArrayList<PartModelToDMS> partModelToDMSArray = new ArrayList<PartModelToDMS>();

		for (Object obj : partMapList) {
			Map partMap = (Map) obj;
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partAttMc = (String) partMap.get("attribute[SPLM_CMC_Code]");

			ArrayList<String> partAttMcArray = new ArrayList<String>();
			ArrayList<String> partAttMsArray = new ArrayList<String>();

			// SPLM_CMC_Code data processing
			for (String mcStr : partAttMc.split(SPLIT_STR)) {
				partAttMcArray.add(mcStr);
			}

			// json for DMS system
			PartModelToDMS partModelToDMSObj = new PartModelToDMS();
			partModelToDMSObj.setPART_NO(partName);
			partModelToDMSObj.setMODEL_CODE(partAttMcArray);
			partModelToDMSObj.setGUID(partId);
			partModelToDMSArray.add(partModelToDMSObj);
		}

		DomainObject partModelDomObj = new DomainObject();
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();
		String result = "";
		
		try {
			MqlUtil.mqlCommand(context, "trigger off");
			if (partModelToDMSArray.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				result = dms.partModel(translateJsonIntoString(partModelToDMSArray));
				for (PartModelToDMS partModelObj : partModelToDMSArray) {
					partModelDomObj.setId(partModelObj.getGUID());
					partModelDomObj.setAttributeValue(context, "SPLM_PartModel_DMS", "Complete");
				}
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			for (PartModelToDMS partModelObj : partModelToDMSArray) {
				partModelDomObj.setId(partModelObj.getGUID());
				partModelDomObj.setAttributeValue(context, "SPLM_PartModel_DMS", "Error");
			}
			result = "(Error) " + e.getMessage();
		} finally {
			System.out.println(DMS + " " + methodName + " Result :" + result);
			System.out.println(DMS + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(partModelToDMSArray));
			System.out.println();
			MqlUtil.mqlCommand(context, "trigger on");			
		}

	}

	private class PartModelToDMS extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public ArrayList<String> MODEL_CODE;
		public void setPART_NO(String pART_NO) {
			PART_NO = pART_NO;
		}
		public void setMODEL_CODE(ArrayList<String> mODEL_CODE) {
			MODEL_CODE = mODEL_CODE;
		}
	}

	/**
	 * 29. Dealer
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public void createDMSDealer(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		
		Map<String, String> dealerMap = getDealerInfo(context);
		String dealerStr = "";
		String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		try {
			SPlmWSSoap locator = getSPlmWSSoapDMS();
			dealerStr = locator.dealer();
			String dealerType = "SPLM_Dealer";
			String dealerPolicy = "SPLM_Dealer";
			ArrayList<Dealer> dealerArray = gson.fromJson(dealerStr, new TypeToken<ArrayList<Dealer>>() {
			}.getType());

			DomainObject createDealerDomObj = new DomainObject();
			DomainObject modDomObj = new DomainObject();
			Map<String, ArrayList<String>> dealerDetailMap = new HashMap<String, ArrayList<String>>();
			Map<String, String> masterIdDealerMap = new HashMap<String, String>();

			for (Dealer dealerObj : dealerArray) {
				String dealerName = dealerObj.getCOMPANY_ID();

				// collect new Dealer
				if (!dealerMap.containsKey(dealerName)) {
					createDealerDomObj.createObject(context, dealerType, dealerName, "-", dealerPolicy, VAULT);
					dealerMap.put(dealerName, createDealerDomObj.getInfo(context, DomainConstants.SELECT_ID));
					System.out.println("Create DealerName : " + dealerName);
				} else {
					createDealerDomObj.setId(dealerMap.get(dealerName));
					System.out.println("Exist  DealerName : " + dealerName);
				}

				// override attribute
				createDealerDomObj.setDescription(context, dealerObj.getCOMPANY_DESC());
				createDealerDomObj.setAttributeValue(context, "SPLM_Address", dealerObj.getADDRESS());
				createDealerDomObj.setAttributeValue(context, "SPLM_ContactNumber", dealerObj.getTEL_NO());
				createDealerDomObj.setAttributeValue(context, "SPLM_Wheel", "Four Wheel");
				createDealerDomObj.setOwner(context, "splmuser");

				// collect Master Dealer
				filter(dealerDetailMap, dealerName, createDealerDomObj.getId(context));
				filter(dealerDetailMap, dealerName, dealerObj.getMASTER_DEALER());

				// collect Master Dealer already connection name AND override
				// attribute[SPLM_DealerType]
				if (!StringUtils.isNotBlank(dealerObj.getMASTER_DEALER())) {
					createDealerDomObj.setAttributeValue(context, "SPLM_DealerType", "HQ");
					StringList subDealerList = createDealerDomObj.getInfoList(context,
							"from[SPLM_SubDealer].to[SPLM_Dealer].name");
					dealerDetailMap.get(dealerName).addAll(subDealerList);
				} else {
					createDealerDomObj.setAttributeValue(context, "SPLM_DealerType", "Service Center");
				}
			}

			/*
			 * array structure { dealerName = [dealerId, masterDealer, ...(after here can
			 * ignore, cause all are the same thing)] }
			 */
			for (String dealerName : dealerDetailMap.keySet()) {
				String dealerId = dealerDetailMap.get(dealerName).get(0);
				String masterDealerName = dealerDetailMap.get(dealerName).get(1);
				if (!dealerDetailMap.containsKey(masterDealerName)) {
					continue;
				}

				ArrayList<String> dealerDataArray = dealerDetailMap.get(masterDealerName);
				if (!dealerDataArray.contains(dealerName)) {
					String masterDealerID = dealerDataArray.get(0);
					DomainRelationship.connect(context, new DomainObject(masterDealerID), "SPLM_SubDealer",
							new DomainObject(dealerId));
					System.out.println("Master Dealer : " + masterDealerName + " -> " + " Sub Dealer : " + dealerName
							+ " Connect!");
				}
			}
		} catch (Exception e) {
			System.out.println(DMS + " " + methodName + " Error : " + e.getMessage());
		}
	}
	
	private class Dealer extends Abstract_CMC_ObjBaseAttribute {
		public String COMPANY_ID; //
		public String COMPANY_DESC;
		public String ADDRESS;
		public String TEL_NO;
		public String MASTER_DEALER;
//		public String DEALER_TYPE;
//		public String DEALER_MAIL;
//		public String CONTACT_PERSON;
		public String getCOMPANY_ID() {
			return COMPANY_ID;
		}
		public void setCOMPANY_ID(String cOMPANY_ID) {
			COMPANY_ID = cOMPANY_ID;
		}
		public String getCOMPANY_DESC() {
			return COMPANY_DESC;
		}
		public void setCOMPANY_DESC(String cOMPANY_DESC) {
			COMPANY_DESC = cOMPANY_DESC;
		}
		public String getADDRESS() {
			return ADDRESS;
		}
		public void setADDRESS(String aDDRESS) {
			ADDRESS = aDDRESS;
		}
		public String getTEL_NO() {
			return TEL_NO;
		}
		public void setTEL_NO(String tEL_NO) {
			TEL_NO = tEL_NO;
		}
		public String getMASTER_DEALER() {
			return MASTER_DEALER;
		}
		public void setMASTER_DEALER(String mASTER_DEALER) {
			MASTER_DEALER = mASTER_DEALER;
		}
	}

	/**
	 * Create GLMDealer, Dealer Type [1, 2] = Two Wheel, Dealer Type[7] = Four Wheel
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public void createGLMDealer(Context context, String[] args) throws Exception {
		Gson gson = new Gson();

		Map<String, String> dealerMap = getDealerInfo(context);
		
		String dealerStr = "";
		String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		try {
			GetGLMCustSoap locator = getCustGLM();
			dealerStr = locator.doGetCust();

			String dealerType = "SPLM_Dealer";
			String dealerVault = VAULT;
			String dealerPolicy = "SPLM_Dealer";
			ArrayList<GLMDealer> dealerArray = gson.fromJson(dealerStr, new TypeToken<ArrayList<GLMDealer>>() {
			}.getType());

			DomainObject createDealerDomObj = new DomainObject();
			DomainObject modDomObj = new DomainObject();
			Map<String, ArrayList<String>> dealerDetailMap = new HashMap<String, ArrayList<String>>();
			Map<String, String> masterIdDealerMap = new HashMap<String, String>();

			for (GLMDealer dealerObj : dealerArray) {
				String dealerName = dealerObj.getDEALER_ID();

				// collect new Dealer
				if (!dealerMap.containsKey(dealerName)) {
					createDealerDomObj.createObject(context, dealerType, dealerName, "-", dealerPolicy, VAULT);
					dealerMap.put(dealerName, createDealerDomObj.getId(context));
					System.out.println("Create DealerName : " + dealerName);
				} else {
					System.out.println("Exist  DealerName : " + dealerName);
				}
				String dealerId = dealerMap.get(dealerName);
				createDealerDomObj.setId(dealerId);

				switch (dealerObj.DEALER_TYPE) {
				case "1":
				case "2":
					dealerObj.setDEALER_TYPE("Two Wheel");
					break;
				case "7":
					dealerObj.setDEALER_TYPE("Four Wheel");
				}

				// override attribute
				createDealerDomObj.setDescription(context, dealerObj.getDEALER_NAME_C());
				createDealerDomObj.setAttributeValue(context, "SPLM_EmailAddress", dealerObj.getDEALER_MAIL());
				createDealerDomObj.setAttributeValue(context, "SPLM_ContactNumber", dealerObj.getTEL_NO());
				createDealerDomObj.setAttributeValue(context, "SPLM_ContactPerson", dealerObj.getCONTACT_PERSON());
				createDealerDomObj.setAttributeValue(context, "SPLM_Address", dealerObj.getADDRESS());
				createDealerDomObj.setAttributeValue(context, "SPLM_Wheel", dealerObj.getDEALER_TYPE());
				createDealerDomObj.setOwner(context, "splmuser");

				// collect Master Dealer
				filter(dealerDetailMap, dealerName, dealerId);
				filter(dealerDetailMap, dealerName, dealerObj.getMASTER_DEALER());

				// collect Master Dealer already connection name
				if (!StringUtils.isNotBlank(dealerObj.getMASTER_DEALER())) {
					createDealerDomObj.setAttributeValue(context, "SPLM_DealerType", "HQ");
					StringList subDealerList = createDealerDomObj.getInfoList(context,
							"from[SPLM_SubDealer].to[SPLM_Dealer].name");
					dealerDetailMap.get(dealerName).addAll(subDealerList);
				} else {
					createDealerDomObj.setAttributeValue(context, "SPLM_DealerType", "Service Center");
				}
			}

			/*
			 * array structure { dealerName = [dealerId, masterDealer, ...(after here can
			 * ignore, cause all are the same thing)] }
			 */
			for (String dealerName : dealerDetailMap.keySet()) {
				String dealerId = dealerDetailMap.get(dealerName).get(0);
				String masterDealerName = dealerDetailMap.get(dealerName).get(1);
				if (!dealerDetailMap.containsKey(masterDealerName)) {
					continue;
				}

				ArrayList<String> dealerDataArray = dealerDetailMap.get(masterDealerName);
				if (!dealerDataArray.contains(dealerName)) {
					String masterDealerID = dealerDataArray.get(0);
					DomainRelationship.connect(context, new DomainObject(masterDealerID), "SPLM_SubDealer",
							new DomainObject(dealerId));
					System.out.println("Master Dealer : " + masterDealerName + " -> " + " Sub Dealer : " + dealerName
							+ " Connect!");
				}
			}
		} catch (Exception e) {
			System.out.println(GLM_DEALER + " " + methodName + " error : " + e.getMessage());
		}
	}

	private class GLMDealer extends Abstract_CMC_ObjBaseAttribute {
		public String DEALER_ID;
		public String DEALER_NAME_C;
		public String ADDRESS;
		public String TEL_NO;
		public String MASTER_DEALER;
		public String DEALER_TYPE;
		public String DEALER_MAIL;
		public String CONTACT_PERSON;
		public String getDEALER_ID() {
			return DEALER_ID;
		}
		public void setDEALER_ID(String dEALER_ID) {
			DEALER_ID = dEALER_ID;
		}
		public String getDEALER_NAME_C() {
			return DEALER_NAME_C;
		}
		public void setDEALER_NAME_C(String dEALER_NAME_C) {
			DEALER_NAME_C = dEALER_NAME_C;
		}
		public String getADDRESS() {
			return ADDRESS;
		}
		public void setADDRESS(String aDDRESS) {
			ADDRESS = aDDRESS;
		}
		public String getTEL_NO() {
			return TEL_NO;
		}
		public void setTEL_NO(String tEL_NO) {
			TEL_NO = tEL_NO;
		}
		public String getMASTER_DEALER() {
			return MASTER_DEALER;
		}
		public void setMASTER_DEALER(String mASTER_DEALER) {
			MASTER_DEALER = mASTER_DEALER;
		}
		public String getDEALER_TYPE() {
			return DEALER_TYPE;
		}
		public void setDEALER_TYPE(String dEALER_TYPE) {
			DEALER_TYPE = dEALER_TYPE;
		}
		public String getDEALER_MAIL() {
			return DEALER_MAIL;
		}
		public void setDEALER_MAIL(String dEALER_MAIL) {
			DEALER_MAIL = dEALER_MAIL;
		}
		public String getCONTACT_PERSON() {
			return CONTACT_PERSON;
		}
		public void setCONTACT_PERSON(String cONTACT_PERSON) {
			CONTACT_PERSON = cONTACT_PERSON;
		}
	}

	/**
	 * 9. MRP BOM
	 * 
	 * @param context
	 * @param args
	 * @return 
	 * @throws Exception
	 */
	private String _searchMRP_BOM(Context context, DomainObject soObj) throws Exception {
		String sendStatus = "Complete";
		StringList affectedItemBusSelects = new StringList();
		affectedItemBusSelects.add(DomainConstants.SELECT_ID);
		affectedItemBusSelects.add(DomainConstants.SELECT_NAME);
		affectedItemBusSelects.add("attribute[SPLM_Location]");

		StringList subPartBusSelects = new StringList();
		subPartBusSelects.add(DomainConstants.SELECT_NAME);
		StringList subPartRelSelects = new StringList();
		subPartRelSelects.add("attribute[SPLM_EnableDate]"); // Start Date
		subPartRelSelects.add("attribute[SPLM_DisableDate]"); // End Date
		subPartRelSelects.add("attribute[SPLM_Materiel_KD_Type]");
		subPartRelSelects.add("attribute[Quantity]");

		DomainObject affectedItemObj = new DomainObject();
		ArrayList<MRP_BOMToERP> mrpBOMToERPArray = new ArrayList<MRP_BOMToERP>();

		// check SPLM_SO->SPLM_Part rel[SPLM_AffectedItem].att[SPLM_HaveSubPart]==TRUE

		String soName = soObj.getInfo(context, DomainConstants.SELECT_NAME);

		MapList soAffectedItemList = soObj.getRelatedObjects(context, "SPLM_AffectedItem", // relationshipPattern,
				"*", // typePattern,
				affectedItemBusSelects, // StringList objectSelects,
				null, // StringList relationshipSelects,
				false, // boolean getTo,
				true, // boolean getFrom,
				(short) 1, // short recurseToLevel,
				"attribute[SPLM_HaveSubPart]=='True'", // String objectWhere,
				"", // String relationshipWhere
				0); // int limit)

		// parent Part
		for (Object affectedItem : soAffectedItemList) {
			Map<String, String> affectedItemInfo = (Map) affectedItem;
			String affectedItemName = affectedItemInfo.get(DomainConstants.SELECT_NAME);
			String affectedItemId = affectedItemInfo.get(DomainConstants.SELECT_ID);
			String affectedItemLocation = affectedItemInfo.get("attribute[SPLM_Location]");
			affectedItemObj.setId(affectedItemId);

			MapList affectedItemVendorMapList = affectedItemObj.getRelatedObjects(context, "SPLM_RelatedVendor", // relationshipPattern,
					"SPLM_Vendor", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_Valid]=='Y'", // String relationshipWhere
					0); // int limit)

			String affectedItemVendor = (String) affectedItemVendorMapList.stream()
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME)).findFirst().orElse("");

			// sub Part
			MapList subPartMapList = affectedItemObj.getRelatedObjects(context, "SPLM_SBOM,SPLM_MBOM", // relationshipPattern,
					"SPLM_Part,SPLM_ColorPart", // typePattern,
					subPartBusSelects, // StringList objectSelects,
					subPartRelSelects, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"(attribute[SPLM_Materiel_KD_Type]=='D-' || attribute[SPLM_Materiel_KD_Type]=='K-') && attribute[SPLM_SendSubPartToERP]!='N'", // String
																										// relationshipWhere
					0); // int limit)

			for (Object subPartObj : subPartMapList) {
				Map<String, String> subPartMap = (Map) subPartObj;
				String subPartName = subPartMap.get(DomainObject.SELECT_NAME);
				String subPartAttStartDate = subPartMap.get("attribute[SPLM_EnableDate]");
				String subPartAttEndDate = subPartMap.get("attribute[SPLM_DisableDate]");
				String subPartAttQuantity = subPartMap.get("attribute[Quantity]");
				subPartAttStartDate = subPartAttStartDate.isEmpty()?  "": convertDateFormat(subPartAttStartDate, "yyyyMMdd");
				subPartAttEndDate = subPartAttEndDate.isEmpty()?  "": convertDateFormat(subPartAttEndDate, "yyyyMMdd");

				// json for ERP system
				for (String location : affectedItemLocation.split(SPLIT_STR)) {
					MRP_BOMToERP mrpBOMToERP = new MRP_BOMToERP();
					mrpBOMToERP.setSO_NO(soName);
					mrpBOMToERP.setVENDOR_NO(affectedItemVendor);
					mrpBOMToERP.setPARENT_PART_NO(affectedItemName);
					mrpBOMToERP.setPARENT_PART_PLANT(location);
					mrpBOMToERP.setCHILD_PART_NO(subPartName);
					mrpBOMToERP.setQUANTITY(subPartAttQuantity);
					mrpBOMToERP.setCHILD_PART_START_DATE(subPartAttStartDate);
					mrpBOMToERP.setCHILD_PART_END_DATE(subPartAttEndDate);
					mrpBOMToERPArray.add(mrpBOMToERP);
				}
			}
		}

		DomainObject partDomObj = new DomainObject();
		List<MRP_BOMToERP> mrpBOMToERPArrayForCMC = mrpBOMToERPArray.stream()
				.filter(mrp -> mrp.PARENT_PART_PLANT.equals("1300")).collect(Collectors.toList());
		List<MRP_BOMToERP> mrpBOMToERPArrayForSDM = mrpBOMToERPArray.stream()
				.filter(mrp -> mrp.PARENT_PART_PLANT.equals("9000") || mrp.PARENT_PART_PLANT.equals("9001"))
				.collect(Collectors.toList());
		String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		String result = "";
		
		try {
			if (mrpBOMToERPArrayForCMC.size() > 0) {
				YPP16_PortType erpForCMC = getYPP16_300ERP();
				result = erpForCMC.yppSplmBomToSap(translateJsonIntoString(mrpBOMToERPArrayForCMC));
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			result = "(Error) " + e.getMessage();
			sendStatus = "Error";
		} finally {
			System.out.println(ERP_YPP16_300 + " " + methodName + " Result :" + result);
			System.out.println(ERP_YPP16_300 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(mrpBOMToERPArrayForCMC));
			System.out.println();
		}

		try {
			if (mrpBOMToERPArrayForSDM.size() > 0) {
				YPP16_PortType erpForSDM = getYPP16_310ERP();
				result = erpForSDM.yppSplmBomToSap(translateJsonIntoString(mrpBOMToERPArrayForSDM));
			} else {
				result = " \u8cc7\u6599\u70ba\u7a7a (\u4e0d\u62cb\u8f49\u8cc7\u6599)";
			}
		} catch (Exception e) {
			result = "(Error) " + e.getMessage();
			sendStatus = "Error";
		} finally {
			System.out.println(ERP_YPP16_310 + " " + methodName + " Result :" + result);
			System.out.println(ERP_YPP16_310 + " \u8f38\u51fa\u8cc7\u6599 : " + translateJsonIntoString(mrpBOMToERPArrayForSDM));
			System.out.println();
		}
		
		return sendStatus;
	}

	private class MRP_BOMToERP extends Abstract_CMC_ObjBaseAttribute {
		public String SO_NO;
		public String VENDOR_NO;
		public String PARENT_PART_NO;
		public String PARENT_PART_PLANT;
		public String CHILD_PART_NO;
		public String QUANTITY;
		public String CHILD_PART_START_DATE;
		public String CHILD_PART_END_DATE;
		public void setSO_NO(String sO_NO) {
			SO_NO = sO_NO;
		}
		public void setVENDOR_NO(String vENDOR_NO) {
			VENDOR_NO = vENDOR_NO;
		}
		public void setPARENT_PART_NO(String pARENT_PART_NO) {
			PARENT_PART_NO = pARENT_PART_NO;
		}
		public void setPARENT_PART_PLANT(String pARENT_PART_PLANT) {
			PARENT_PART_PLANT = pARENT_PART_PLANT;
		}
		public void setCHILD_PART_NO(String cHILD_PART_NO) {
			CHILD_PART_NO = cHILD_PART_NO;
		}
		public void setQUANTITY(String qUANTITY) {
			QUANTITY = qUANTITY;
		}
		public void setCHILD_PART_START_DATE(String cHILD_PART_START_DATE) {
			CHILD_PART_START_DATE = cHILD_PART_START_DATE;
		}
		public void setCHILD_PART_END_DATE(String cHILD_PART_END_DATE) {
			CHILD_PART_END_DATE = cHILD_PART_END_DATE;
		}
	}

	/* ---TEST Part--- */
	
	/*
	 ----------- txt資料 發送測試區 ------------
	 */
	public void searchPncTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.PNC(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmP01(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}
	}

	public void searchGroupCodeTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.group(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmG01(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	public void searchPartVendorTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partVendor(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmM02(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	public void searchAltPartTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partAlt(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				YMM20_PortType erp = getYMM20_300ERP();
				String result = erp.ymmReplaceMatToSap(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}
	}

	public void searchPartPncTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partPnc(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmM03(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			System.err.println("Error detail in Error.txt.");
			outputDataFile("Error.txt", e.getMessage());
		}
	}

	public void searchOptPartTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partOpt(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	public void searchPncPartTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partPnc(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmM03(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	public void searchPartGroupTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partGroup(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			System.err.println("Error detail in Error.txt.");
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	public void searchServicePartTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.servicesPart(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				YMM20_PortType erp = getYMM20_310ERP();
				String result = erp.ymmSplmMatToSap(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	public void searchMcGroupCodeTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmMcg(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	public void searchMsPartCategoryTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmMsp(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	public void searchPartsCatelogueMcTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmMpc(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}
	}

	public void searchPartModelTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partModel(inputJson);
				System.out.println(result);
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}
	}

	public void searchMRP_BOMTest(Context context, String[] args) throws Exception {
		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				YPP16_PortType erp = getYPP16_300ERP();
				String result = erp.yppSplmBomToSap(inputJson);
				System.out.println('\n' + result + '\n');
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}
	}

	/*
	 ----------- 抓系統資料 發送測試區 ------------
	 */
	
	/**
	 * 測試用  soId發送查驗資料正確
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public void _searchMRP_BOM_Test(Context context, String[] args) throws Exception{
		String soId = "64040.25682.40528.29250";
		this._searchMRP_BOM(context, new DomainObject(soId));
	}
	
	/**
	 * 測試用  servicePart一般發送系統測試用
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public void searchServicePartNotFromSOTest(Context context, String[] args) throws Exception {
		String namePattern = "MN203399YA";
		StringList busSelects = getServicePartSelects();
		
		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part,SPLM_ColorPart", // type
				namePattern, // name
				"-", // revision
				"*", // owner
				VAULT, // vault
				"", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		this._searchServicePart(context, partMapList, "normal");
	}
}