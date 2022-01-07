import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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

	private final static String TEST_FILE_PATH = "C:\\temp\\Morga\\json.txt";
	private final static String DEFAULT_ROOT_DIRECTORY = "C:\\temp\\Morga\\";
	private final static String CALL_WSDL = "ERP";
	private final static String PLM_ENV = System.getenv("PLM_ENV");
	private final static String VAULT = "eService Production";
	private final static short SEARCH_AMOUT = 0;
	private final static int LIMIT_NUMS_DMS = 30;
	private final static String ERP_YSD041_300 = "ERP.YSD041.300";
	private final static String ERP_YSD041_310 = "ERP.YSD041.310";
	private final static String ERP_YMM20_300 = "ERP.YMM20.300";
	private final static String ERP_YMM20_310 = "ERP.YMM20.310";
	private final static String ERP_YPP16_300 = "ERP.YPP16.300";
	private final static String ERP_YPP16_310 = "ERP.YPP16.310";
	private final static String DMS = "DMS";
	private static final String SPLIT_STR = ",";

	/* Get URL web */
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

	/* ---create/modify Part Send SO To ERP/DMS--- */
	public void ReleasePart(Context context, String[] args) throws Exception {
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add("current.actual");

		MapList soMapList = DomainObject.findObjects(context, "SPLM_SO", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"current=='Complete' && attribute[SPLM_SendToERP_DMS]=='Processing'", // where
				null, false, new StringList(DomainConstants.SELECT_ID), (short) SEARCH_AMOUT);

		DomainObject soObj = new DomainObject();
		for (Object so : soMapList) {
			String soId = (String) ((Map) so).get(DomainConstants.SELECT_ID);
			soObj.setId(soId);

			this._searchServicePart(context, this.getSOIsServicePartAI(context, soObj, getServicePartSelects()), "so");
			this._searchOptAltPart(context, this.getSOIsServicePartAI(context, soObj, getAltOptPartSelects()), "alt");
			this._searchOptAltPart(context, this.getSOIsServicePartAI(context, soObj, getAltOptPartSelects()), "opt");
			this._searchPartVendor(context, this.getSOIsServicePartAI(context, soObj, getVendorSelects()));
			this._searchMRP_BOM(context, soObj);

			soObj.setAttributeValue(context, "SPLM_SendToERP_DMS", "Complete");
		}
		System.out.println("*********************SO Sending Done.**************************");
	}

	/* ---only search Part then Call itSelf method for DMS/ERP--- */
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

	/* ---Dassault API package simple using--- */

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

	/* ---Business Select Management--- */
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
		return busSelect;
	}

	private StringList getAltOptPartSelects() {
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID); // GUID
		busSelect.add(DomainConstants.SELECT_NAME); // PART_NO
		busSelect.add(DomainConstants.SELECT_MODIFIED); // CHANGE_DATE
		busSelect.add("attribute[SPLM_Location]");
		return busSelect;
	}

	private StringList getVendorSelects() {
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID); // GUID
		busSelect.add(DomainConstants.SELECT_NAME); // PART_NO
		busSelect.add(DomainConstants.SELECT_MODIFIED); // CHANGE_DATE
		busSelect.add("attribute[SPLM_Location]");
		return busSelect;
	}

	/* ---CMC wsdl connection--- */
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

	private GetGLMCustSoap getGLMCust() throws Exception {
		String url = getPropertyURL("GLM");
		GetGLMCustLocator locator = new GetGLMCustLocator();
		if (StringUtils.isEmpty(url)) {
			return locator.getGetGLMCustSoap();
		} else {
			return locator.getGetGLMCustSoap(new URL(url));
		}
	}

	/* ---Our Method using--- */
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

	abstract class Abstract_CMC_ObjBaseAttribute extends Object implements Cloneable {
		protected String CHANGE_DATE = convertNowDateFormat("yyyyMMdd");
		protected String CREATE_DATE;
		protected String GUID;
		protected String PLANT;

		protected void setPLANT(String plant) {
			this.PLANT = plant;
		}

		@Override
		protected Object clone() throws CloneNotSupportedException {
			Object cloneObj = super.clone();
			return cloneObj;
		}
	}

	/* ---Test Part--- */
	public void test(Context context, String[] args) throws Exception {
		Pattern pattern = new Pattern("");
		pattern.addPattern(CALL_WSDL);
		pattern.addPattern("asdf");
	}

	/* ---Start Line : API--- */
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
			pncForBothObj.PNC_NO = (String) pncMap.get(DomainConstants.SELECT_NAME);
			pncForBothObj.PNC_NAME_C = (String) pncMap.get("attribute[SPLM_Name_TC]");
			pncForBothObj.PNC_NAME_E = (String) pncMap.get("attribute[SPLM_Name_EN]");
			pncForBothObj.PNC_TYPE = (String) pncMap.get("attribute[SPLM_CommissionGroup]");
			pncForBothObj.GUID = (String) pncMap.get(DomainConstants.SELECT_ID);
			pncForBothArray.add(pncForBothObj);
		}

		DomainObject pncDomObj = new DomainObject();
		String methodName = new Object(){}.getClass().getEnclosingMethod().getName();

		// connect CMC for test
		try {
			if (pncForBothArray.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.PNC(translateJsonIntoString(pncForBothArray));
				outputDataFile("16_DMS_PNC.txt", translateJsonIntoString(pncForBothArray));
				System.out.println('\n' + DMS + " " + methodName + " Result :" + result + '\n');

				MqlUtil.mqlCommand(context, "trigger off");
				for (PNCForBoth pncObj : pncForBothArray) {
					pncDomObj.setId(pncObj.GUID);
					pncDomObj.setAttributeValue(context, "SPLM_PNC_DMS", "Complete");
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + DMS + " " + methodName + " Data : " + pncForBothArray + " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = DMS + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}

		try {
			if (pncForBothArray.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmP01(translateJsonIntoString(pncForBothArray));
				outputDataFile("16_ERP_PNC.txt", translateJsonIntoString(pncForBothArray));
				System.out.println('\n' + ERP_YSD041_300 + " " + methodName + " Result :" + result + '\n');
				
				MqlUtil.mqlCommand(context, "trigger off");
				for (PNCForBoth pncObj : pncForBothArray) {
					pncDomObj.setId(pncObj.GUID);
					pncDomObj.setAttributeValue(context, "SPLM_PNC_ERP", "Complete");
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + ERP_YSD041_300 + " " + methodName + " Data : " + pncForBothArray + " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = ERP_YSD041_300 + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}
	}

	private class PNCForBoth extends Abstract_CMC_ObjBaseAttribute {
		public String PNC_NO;
		public String PNC_NAME_E;
		public String PNC_NAME_C;
		public String PNC_TYPE;
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
			groupCodeForBothObj.GROUP_CODE = (String) groupCodeMap.get(DomainConstants.SELECT_NAME);
			groupCodeForBothObj.GROUP_NAME_C = (String) groupCodeMap.get("attribute[SPLM_Name_TC]");
			groupCodeForBothObj.GROUP_NAME_E = (String) groupCodeMap.get("attribute[SPLM_Name_EN]");
			groupCodeForBothObj.GUID = (String) groupCodeMap.get(DomainConstants.SELECT_ID);
			groupCodeForBothArray.add(groupCodeForBothObj);
		}

		DomainObject groupDomObj = new DomainObject();
		String methodName = new Object(){}.getClass().getEnclosingMethod().getName();

		// connect CMC for test
		try {
			if (groupCodeForBothArray.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.group(translateJsonIntoString(groupCodeForBothArray));
				outputDataFile("17_DMS_Group.txt", translateJsonIntoString(groupCodeForBothArray));
				System.out.println('\n' + DMS + " " + methodName + " Result :" + result + '\n');

				MqlUtil.mqlCommand(context, "trigger off");
				for (GroupCodeForBoth groupObj : groupCodeForBothArray) {
					groupDomObj.setId(groupObj.GUID);
					groupDomObj.setAttributeValue(context, "SPLM_GroupCode_DMS", "Complete");
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + DMS + " " + methodName + " Data : " + groupCodeForBothArray + " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = DMS + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}

		try {
			if (groupCodeForBothArray.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmG01(translateJsonIntoString(groupCodeForBothArray));
				outputDataFile("17_ERP_Group.txt", translateJsonIntoString(groupCodeForBothArray));
				System.out.println('\n' + ERP_YSD041_300 + " " + methodName + " Result :" + result + '\n');

				MqlUtil.mqlCommand(context, "trigger off");
				for (GroupCodeForBoth groupObj : groupCodeForBothArray) {
					groupDomObj.setId(groupObj.GUID);
					groupDomObj.setAttributeValue(context, "SPLM_GroupCode_ERP", "Complete");
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + ERP_YSD041_300 + " " + methodName + " Data : " + groupCodeForBothArray + " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = ERP_YSD041_300 + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}
	}

	private class GroupCodeForBoth extends Abstract_CMC_ObjBaseAttribute {
		public String GROUP_CODE;
		public String GROUP_NAME_C;
		public String GROUP_NAME_E;
	}

	/**
	 * 19.PartVendor
	 * 
	 * @param context
	 * @param partMapList
	 * @throws Exception
	 */
	private void _searchPartVendor(Context context, MapList partMapList) throws Exception {
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
			String partLocation = (String) partMap.get("attribute[SPLM_Location]");
			domObj.setId(partId);

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
				VenderAlterToDMS venderAlterToDMSObj = new VenderAlterToDMS();
				venderAlterToDMSObj.PART_NO = partName;
				venderAlterToDMSObj.VENDOR_NO = partAttVendorList;
				venderAlterToDMSObj.SO_NO = soLatestName;
				venderAlterToDMSObj.GUID = partId;
				venderAlterToDMSArray.add(venderAlterToDMSObj);

				// json for ERP system
				for (String partAttVendor : partAttVendorList) {
					for (String loaciotn : partLocation.split(SPLIT_STR)) {
						VenderAlterToERP venderAlterToERPObj = new VenderAlterToERP();
						venderAlterToERPObj.PART_NO = partName;
						venderAlterToERPObj.VENDOR_NO = partAttVendor;
						venderAlterToERPObj.SO_NO = soLatestName;
						venderAlterToERPObj.LOCATION = loaciotn;
						venderAlterToERPObj.GUID = partId;
						venderAlterToERPArray.add(venderAlterToERPObj);
					}
				}
			}
		}

		List<VenderAlterToERP> partVendorListToERPForCMC = venderAlterToERPArray.stream()
				.filter(obj -> obj.LOCATION.equals("1300")).collect(Collectors.toList());
		List<VenderAlterToERP> partVendorListToERPForSDM = venderAlterToERPArray.stream()
				.filter(obj -> obj.LOCATION.equals("9000") || obj.LOCATION.equals("9001")).collect(Collectors.toList());
		venderAlterToERPArray.stream().forEach(obj -> obj.LOCATION = null);

		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();
		DomainObject pncDomObj = new DomainObject();
		try {
			if (venderAlterToDMSArray.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partVendor(translateJsonIntoString(venderAlterToDMSArray));
				outputDataFile("19_DMS_PartVendor.txt", translateJsonIntoString(venderAlterToDMSArray));
				System.out.println('\n' + DMS + " " + methodName + " Result :" + result + '\n');
				MqlUtil.mqlCommand(context, "trigger off");
				for (VenderAlterToDMS pncObj : venderAlterToDMSArray) {
					pncDomObj.setId(pncObj.GUID);
					pncDomObj.setAttributeValue(context, "SPLM_PNC_DMS", "Complete");
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + DMS + " " + methodName + " Data : " + venderAlterToDMSArray
						+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = DMS + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}

		try {
			if (partVendorListToERPForCMC.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmM02(translateJsonIntoString(partVendorListToERPForCMC));
				outputDataFile("19_ERP_PartVendor_CMC.txt", translateJsonIntoString(partVendorListToERPForCMC));
				System.out.println('\n' + ERP_YSD041_300 + " " + methodName + " Result :" + result + '\n');
			} else {
				System.out.println('\n' + ERP_YSD041_300 + " " + methodName + " Data : " + partVendorListToERPForCMC
						+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = ERP_YSD041_300 + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}

		try {
			if (partVendorListToERPForSDM.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_310ERP();
				String result = erp.YPutSplmM02(translateJsonIntoString(partVendorListToERPForSDM));
				outputDataFile("19_ERP_PartVendor_SDM.txt", translateJsonIntoString(partVendorListToERPForSDM));
				System.out.println('\n' + ERP_YSD041_310 + " " + methodName + " Result :" + result + '\n');
			} else {
				System.out.println('\n' + ERP_YSD041_310 + " " + methodName + " Data : " + partVendorListToERPForSDM
						+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = ERP_YSD041_310 + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}
	}

	private class VenderAlterToDMS extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public List<String> VENDOR_NO;
		public String SO_NO;
	}

	private class VenderAlterToERP extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public String VENDOR_NO;
		public String SO_NO;
		public String LOCATION;
	}

	/**
	 * 14/15 Opt/Alt
	 * 
	 * @param context
	 * @param partMapList
	 * @param strAltOpt
	 * @throws Exception
	 */
	private void _searchOptAltPart(Context context, MapList partMapList, String strAltOpt) throws Exception {
		String relWhere = null;
		if (strAltOpt.equalsIgnoreCase("alt")) {
			relWhere = "attribute[SPLM_OptionalType]=='Alternate Part'";
		} else if (strAltOpt.equalsIgnoreCase("opt")) {
			relWhere = "attribute[SPLM_OptionalType]!='Alternate Part' && attribute[SPLM_OptionalType]!=''";
		} else {
			System.out.println("args ONLY get 'alt' or 'opt'.");
			return;
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
			String partLocation = (String) partMap.get("attribute[SPLM_Location]");
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
				for (String location : partLocation.split(SPLIT_STR)) {
					OptAltToERP OptAltToERPObj = new OptAltToERP(strAltOpt);
					OptAltToERPObj.setPART_NO(partName);
					OptAltToERPObj.setALT_OPT_PART(altOptName);
					OptAltToERPObj.setLOCATION(location);
					OptAltToERPObj.setEXCHANGEABLE(altOptExchange);
					OptAltToERPObj.GUID = partId;
					OptAltToERPArray.add(OptAltToERPObj);
				}
			}

			// DMS system
			if (!altOptPart.isEmpty()) {
				OptAltToDMS OptAltToDMSObj = new OptAltToDMS(strAltOpt);
				OptAltToDMSObj.setPART_NO(partName);
				OptAltToDMSObj.setALT_OPT_PART(altOptPart);
				OptAltToDMSObj.GUID = partId;
				OptAltToDMSArray.add(OptAltToDMSObj);
			}
		}

		List<OptAltToERP> AlterPartToERPArrayForCMC = OptAltToERPArray.stream()
				.filter(alt -> alt.getLOCATION().equals("1300")).collect(Collectors.toList());

		List<OptAltToERP> AlterPartToERPArrayForSDM = OptAltToERPArray.stream()
				.filter(alt -> alt.getLOCATION().equals("9000") || alt.getLOCATION().equals("9001"))
				.collect(Collectors.toList());

		OptAltToERPArray.stream().forEach(obj -> obj.setLOCATION(null));

		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName() + "_" + strAltOpt;
		DomainObject partDomObj = new DomainObject();
		// connect CMC for test
		if (strAltOpt.equalsIgnoreCase("ALT")) {
			try {
				if (OptAltToDMSArray.size() > 0) {
					SPlmWSSoap dms = getSPlmWSSoapDMS();
					String result = dms.partAlt(translateJsonIntoString(OptAltToDMSArray));
					outputDataFile("14_DMS_Alter.txt", translateJsonIntoString(OptAltToDMSArray));
					System.out.println('\n' + DMS + " " + methodName + " Result :" + result + '\n');
				} else {
					System.out.println('\n' + DMS + " " + methodName + " Data : " + OptAltToDMSArray
							+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
				}
			} catch (Exception e) {
				String error = DMS + " " + methodName + " error : " + e.getMessage();
				System.err.println(error);
				outputDataFile("Error.txt", error);
			}
			try {
				if (AlterPartToERPArrayForCMC.size() > 0) {
					YMM20_PortType erpForCMC = getYMM20_300ERP();
					String result = erpForCMC.ymmReplaceMatToSap(translateJsonIntoString(AlterPartToERPArrayForCMC));
					outputDataFile("14_ERP_Alter_ToCMC.txt", translateJsonIntoString(AlterPartToERPArrayForCMC));
					System.out.println('\n' + ERP_YMM20_300 + " " + methodName + " Result :" + result + '\n');
				} else {
					System.out.println('\n' + ERP_YMM20_300 + " " + methodName + " Data : " + AlterPartToERPArrayForCMC
							+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
				}
			} catch (Exception e) {
				String error = ERP_YMM20_300 + " " + methodName + " error : " + e.getMessage();
				System.err.println(error);
				outputDataFile("Error.txt", error);
			}
			try {
				if (AlterPartToERPArrayForSDM.size() > 0) {
					YMM20_PortType erpForSDM = getYMM20_310ERP();
					String result = erpForSDM.ymmReplaceMatToSap(translateJsonIntoString(AlterPartToERPArrayForSDM));
					outputDataFile("14_ERP_Alter_ToSDM.txt", translateJsonIntoString(AlterPartToERPArrayForSDM));
					System.out.println('\n' + ERP_YMM20_310 + " " + methodName + " Result :" + result + '\n');
				} else {
					System.out.println('\n' + ERP_YMM20_310 + " " + methodName + " Data : " + AlterPartToERPArrayForSDM
							+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
				}
			} catch (Exception e) {
				String error = ERP_YMM20_310 + " " + methodName + " error : " + e.getMessage();
				System.err.println(error);
				outputDataFile("Error.txt", error);
			}
		}

		if (strAltOpt.equalsIgnoreCase("OPT")) {
			try {
				if (OptAltToDMSArray.size() > 0) {
					SPlmWSSoap dms = getSPlmWSSoapDMS();
					String result = dms.partOpt(translateJsonIntoString(OptAltToDMSArray));
					outputDataFile("15_DMS_Optional.txt", translateJsonIntoString(OptAltToDMSArray));
					System.out.println('\n' + DMS + " " + methodName + " Result :" + result + '\n');
				} else {
					System.out.println('\n' + DMS + " " + methodName + " Data : " + OptAltToDMSArray
							+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
				}
			} catch (Exception e) {
				String error = DMS + " " + methodName + " error : " + e.getMessage();
				System.err.println(error);
				outputDataFile("Error.txt", error);
			}
		}

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
				partPncToDMSObj.PART_NO = partName;
				partPncToDMSObj.PNC_NO = pncNameLists;
				partPncToDMSObj.GUID = partId;
				partPncToDMSArray.add(partPncToDMSObj);

				// json for ERP system
				for (String pncName : pncNameLists) {
					for (String partLocation : partAttLocationForERP.split(SPLIT_STR)) {
						PartPncToERP partPncToERPObj = new PartPncToERP();
						partPncToERPObj.PART_NO = partName;
						partPncToERPObj.PNC_NO = pncName;
						partPncToERPObj.PLANT = partLocation;
						partPncToERPObj.GUID = partId;
						partPncToERPArray.add(partPncToERPObj);
					}
				}
			}
		}

		List<PartPncToERP> partPncToERPForCMCArray = partPncToERPArray.stream().filter(obj -> obj.PLANT.equals("1300"))
				.collect(Collectors.toList());
		List<PartPncToERP> partPncToERPForSDMArray = partPncToERPArray.stream()
				.filter(obj -> obj.PLANT.equals("9000") || obj.PLANT.equals("9001")).collect(Collectors.toList());

		partPncToERPArray.stream().forEach(obj -> obj.setPLANT(null));

		DomainObject partPncDomObj = new DomainObject();
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();

		// connect CMC for test
		try {
			if (partPncToDMSArray.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partPnc(translateJsonIntoString(partPncToDMSArray));
				outputDataFile("11_DMS_Part-PNC.txt", translateJsonIntoString(partPncToDMSArray));
				System.out.println('\n' + DMS + " " + methodName + " Result :" + result + '\n');

				MqlUtil.mqlCommand(context, "trigger off");
				for (PartPncToDMS partPncObj : partPncToDMSArray) {
					
					partPncDomObj.setId(partPncObj.GUID);
					partPncDomObj.setAttributeValue(context, "SPLM_PartPNC_DMS", "Complete");
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + DMS + " " + methodName + " Data : " + partPncToDMSArray
						+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = DMS + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}

		try {
			if (partPncToERPForCMCArray.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmM03(translateJsonIntoString(partPncToERPForCMCArray));
				outputDataFile("11_ERP_Part-PNC_ToCMC.txt", translateJsonIntoString(partPncToERPForCMCArray));
				System.out.println('\n' + ERP_YSD041_300 + " " + methodName + " Result :" + result + '\n');

				MqlUtil.mqlCommand(context, "trigger off");
				for (PartPncToERP partPncObj : partPncToERPForCMCArray) {
					partPncDomObj.setId(partPncObj.GUID);
					partPncDomObj.setAttributeValue(context, "SPLM_PartPNC_ERP300", "Complete");
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + ERP_YSD041_300 + " " + methodName + " Data : " + partPncToERPForCMCArray
						+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = ERP_YSD041_300 + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}
		try {
			if (partPncToERPForSDMArray.size() > 0) {
				YSD041_NEW_PortType erp = getYSD041_310ERP();
				String result = erp.YPutSplmM03(translateJsonIntoString(partPncToERPForSDMArray));
				outputDataFile("11_ERP_Part-PNC_ToSDM.txt", translateJsonIntoString(partPncToERPForSDMArray));
				System.out.println('\n' + ERP_YSD041_310 + " " + methodName + " Result :" + result + '\n');

				MqlUtil.mqlCommand(context, "trigger off");
				for (PartPncToERP partPncObj : partPncToERPForSDMArray) {
					partPncDomObj.setId(partPncObj.GUID);
					partPncDomObj.setAttributeValue(context, "SPLM_PartPNC_ERP310", "Complete");
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + ERP_YSD041_310 + " " + methodName + " Data : " + partPncToERPForSDMArray
						+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = ERP_YSD041_310 + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}
	}

	private class PartPncToDMS extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public List<String> PNC_NO;
	}

	private class PartPncToERP extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public String PNC_NO;
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
				partGroupCodeToDMSObj.PART_NO = partName;
				partGroupCodeToDMSObj.GROUP_CODE = partGroupCodeList;
				partGroupCodeToDMSObj.GUID = partId;
				partGroupCodeToDMSArray.add(partGroupCodeToDMSObj);
			}
		}

//		List<PartGroupCodeToDMS> partGroupCodeForDMSLimitArray = partGroupCodeToDMSArray.stream()
//				.sorted(Comparator.comparing(PartGroupCodeToDMS::getGUID)).limit(LIMIT_NUMS_DMS)
//				.collect(Collectors.toList());

		DomainObject partGroupCodeDomObj = new DomainObject();
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();

		// connect CMC for test
		try {
			if (partGroupCodeToDMSArray.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partGroup(translateJsonIntoString(partGroupCodeToDMSArray));
				outputDataFile("12_DMS_Part-Group.txt", translateJsonIntoString(partGroupCodeToDMSArray));
				System.out.println('\n' + DMS + " " + methodName + " Result :" + result + '\n');

				MqlUtil.mqlCommand(context, "trigger off");
				for (PartGroupCodeToDMS partGroupCodeObj : partGroupCodeToDMSArray) {
					partGroupCodeDomObj.setId(partGroupCodeObj.GUID);
					partGroupCodeDomObj.setAttributeValue(context, "SPLM_PartGroupCode_DMS", "Complete");
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + DMS + " " + methodName + " Data : " + partGroupCodeToDMSArray
						+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = DMS + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}
	}

	private class PartGroupCodeToDMS extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public List<String> GROUP_CODE;
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
					groupCodePncToERPObj.GROUP_CODE = groupDrawingAttGroupCode;
					groupCodePncToERPObj.PNC_NO = pncName;
					groupCodePncToERPObj.GUID = (String) ((Map) groupCodeObj).get(DomainConstants.SELECT_ID);
					groupCodePncToERPArray.add(groupCodePncToERPObj);
				}
			}
		}
		// connect CMC for test
		try {
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
			outputDataFile("18_ERP_Group-PNC.txt", translateJsonIntoString(groupCodePncToERPArray));
			YSD041_NEW_PortType erp = getYSD041_300ERP();
			String result = erp.YPutSplmP03(translateJsonIntoString(groupCodePncToERPArray));
			System.out.println('\n' + result + '\n');
//			}

		} catch (Exception e) {
			System.err.println("Error detail in Error.txt.");
			String error = "searchGroupPnc error : " + e.getMessage();
			outputDataFile("Error.txt", error);
		}

	}

	private class GroupCodePncToERP extends Abstract_CMC_ObjBaseAttribute {
		public String GROUP_CODE;
		public String PNC_NO;
	}

	/**
	 * 8. ServicePart
	 * 
	 * @param context
	 * @param partList
	 * @param triggerFrom
	 * @throws Exception
	 */
	private void _searchServicePart(Context context, MapList partList, String triggerFrom) throws Exception {
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
			Map partMap = (Map) partObj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);

			domObj.setId(partId);
			StringList groupCodeList = domObj.getInfoList(context,
					"to[SPLM_RelatedPart].from[SPLM_GroupDrawing].attribute[SPLM_GroupCode]");
			String partAttMaterialGroup = (String) partMap.get("attribute[SPLM_MaterialGroup]");
			String partAttLocationForERP = (String) partMap.get("attribute[SPLM_Location]");
			String partAttLocationForDMS = (String) partMap.get("attribute[SPLM_Location_DMS]");
			String attBigCarType = (String) partMap.get("attribute[SPLM_DTAT_Only]");
			String partAttMs = (String) partMap.get("attribute[SPLM_ModelSeries]");
			String partAttMc = (String) partMap.get("attribute[SPLM_CMC_Code]");
			String partAttPNCType = (String) partMap.get("attribute[SPLM_CommissionGroup]");
			String attTransGroup = "";
			String attSoLatestName = "";
			String attSoLatestDate = "";
			String attSoAttLocation = "";
			String attCarCaegory = "";
			String attManufactureCode = "";
			String attItemCategoryGroup = "";
			String attSalesOrg = "";

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

			for (String msStr : partAttMs.split(SPLIT_STR)) {
				if (!msStr.isEmpty()) {
					partAttMsArray.add(msStr);
				}
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

			for (String partLocation : partAttLocationForERP.split(SPLIT_STR)) {
				Part part = new Part();
				part.PART_NO = (String) partMap.get(DomainConstants.SELECT_NAME);
				part.PART_SH_NO = (String) partMap.get("attribute[SPLM_OrderType]");
				part.K_D = (partAttMaterialGroup.equalsIgnoreCase("KD") || partAttMaterialGroup.equalsIgnoreCase("KDY"))
						? "K"
						: "D";
				part.GROUP_CODE = groupCodeList.isEmpty() ? "" : groupCodeList.get(0);
				part.ALT_PART_NO = mapping.containsKey(altStr) ? mapping.get(altStr) : "";
				part.REUSE_PART_NO = mapping.containsKey(optStr) ? mapping.get(optStr) : "";
				part.MATERIAL_TYPE = (String) partMap.get("attribute[SPLM_MaterialType]");
				part.UNIT = (String) partMap.get("attribute[SPLM_Unit]");
				part.PURCHASING_DEPARTMENT_NO = (String) partMap.get("attribute[SPLM_PurchasingGroup]");
				part.MANUFACTURER_CODE = attManufactureCode;
				part.VENDOR_NO = vendorName;
				part.PART_NAME_C = (String) partMap.get("attribute[SPLM_Name_TC]");
				part.PART_NAME_E = (String) partMap.get("attribute[SPLM_Name_EN]");
				part.SALES_ORG = partLocation.equals("1300") ? "3000"
						: partLocation.equals("9001") ? "9000" : partLocation.equals("9000") ? "SDM" : "";
				part.PNC_NO = (String) partMap.get("attribute[SPLM_PNC]");
				part.ITEM_CATEGORY_GROUP = attItemCategoryGroup;
				part.MODEL_SERIES = partAttMsArray.isEmpty() ? "" : partAttMsArray.get(0);
				part.MODEL_CODE = partAttMcArray.isEmpty() ? "" : partAttMcArray.get(0);
				part.CAR_CAEGORY = attCarCaegory;
				part.PART_TYPE = partAttItemSubType;
				part.BIG_CAR_TYPE = attBigCarType;
				part.COMMISSION_GROUP = partAttPNCType;
				part.TRANS_GROUP = attTransGroup;
				part.SO_NO = attSoLatestName;
				part.SO_RELEASE_DATE = attSoLatestDate.isEmpty() ? "" : convertDateFormat(attSoLatestDate, "yyMMdd");
				part.HAS_SUBASSEMBLY = ((String) partMap.get("attribute[SPLM_HaveSubPart]")).equalsIgnoreCase("TRUE")
						? "Y"
						: "N";
				part.PART_OWNER = (String) partMap.get(DomainConstants.SELECT_OWNER);
				part.IS_SERVICE_PART = ((String) partMap.get("attribute[SPLM_IsServicePart]")).equalsIgnoreCase("TRUE")
						? "Y"
						: "N";
				part.EXPORT_PART = ((String) partMap.get("attribute[SPLM_OverseaSalesOnly]")).equalsIgnoreCase("TRUE")
						? "Y"
						: "N";
				part.CREATE_DATE = convertDateFormat((String) partMap.get(DomainConstants.SELECT_ORIGINATED),
						"yyyyMMdd");
				part.GUID = partId;

				part.MATERIAL_GROUP = partAttMaterialGroup;
				part.PLANT = partLocation;
				partArrayToERP.add(part);
//				if (part.EXPORT_PART.equalsIgnoreCase("N")) {
				if ( !partAttLocationForDMS.isEmpty() ) {
					Part partDMS = (Part) part.clone();
					partDMS.MATERIAL_GROUP = (partAttMaterialGroup.equalsIgnoreCase("KD")
							|| partAttMaterialGroup.equalsIgnoreCase("KDY")) ? "K" : "D";
					partDMS.PLANT = partAttLocationForDMS;
					partListToDMS.add(partDMS);
				}
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
		String methodName = new Object() {}.getClass().getEnclosingMethod().getName();

//		// If PLANT == "" then set Flag Complete
//		MqlUtil.mqlCommand(context, "trigger off");
//		for (Part partObj : partArrayToERP) {
//			partDomObj.setId(partObj.GUID);
//			if (triggerFrom.equalsIgnoreCase("so")) {
//				partDomObj.setAttributeValue(context, "SPLM_DMS_Sync", "True");
//				partDomObj.setAttributeValue(context, "SPLM_ERP1300_Sync", "True");
//				partDomObj.setAttributeValue(context, "SPLM_ERP9000_Sync", "True");
//				partDomObj.setAttributeValue(context, "SPLM_ERP9001_Sync", "True");
//			} else {
//				partDomObj.setAttributeValue(context, "SPLM_ServicePart_DMS", "Complete");
//				partDomObj.setAttributeValue(context, "SPLM_ServicePart_ERP300", "Complete");
//				partDomObj.setAttributeValue(context, "SPLM_ServicePart_ERP310", "Complete");
//			}
//		}
//		MqlUtil.mqlCommand(context, "trigger on");

		
		// connect CMC for test
		try {
			if (partListToDMS.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.servicesPart(translateJsonIntoString(partListToDMS));
				outputDataFile(dmsFileName, translateJsonIntoString(partListToDMS));
				System.out.println('\n' + DMS + " " + methodName + "_" + triggerFrom + " Result :" + result + '\n');

				MqlUtil.mqlCommand(context, "trigger off");
				for (Part partObj : partListToDMS) {
					partDomObj.setId(partObj.GUID);
					if (triggerFrom.equalsIgnoreCase("so")) {
						partDomObj.setAttributeValue(context, "SPLM_DMS_Sync", "True");
					} else {
						partDomObj.setAttributeValue(context, "SPLM_ServicePart_DMS", "Complete");
					}
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + DMS + " " + methodName + "_" + triggerFrom + " Data : " + partListToDMS
						+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = DMS + " " + methodName + "_" + triggerFrom + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error" + dmsFileName, error);
		}

		try {
			if (partListToERPForCMC.size() > 0) {
				YMM20_PortType erpForCMC = getYMM20_300ERP();
				String result = erpForCMC.ymmSplmMatToSap(translateJsonIntoString(partListToERPForCMC));
				outputDataFile(erp300FileName, translateJsonIntoString(partListToERPForCMC));
				System.out.println(
						'\n' + ERP_YMM20_300 + " " + methodName + "_" + triggerFrom + " Result :" + result + '\n');

				MqlUtil.mqlCommand(context, "trigger off");
				for (Part partObj : partListToERPForCMC) {
					partDomObj.setId(partObj.GUID);
					if (triggerFrom.equalsIgnoreCase("so")) {
						partDomObj.setAttributeValue(context, "SPLM_ERP1300_Sync", "True");
					} else {
						partDomObj.setAttributeValue(context, "SPLM_ServicePart_ERP300", "Complete");
					}
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + ERP_YMM20_300 + " " + methodName + "_" + triggerFrom + " Data : "
						+ partListToERPForCMC + " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = ERP_YMM20_300 + " " + methodName + "_" + triggerFrom + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error" + erp300FileName, error);
		}

		try {
			if (partListToERPForSDM.size() > 0) {
				YMM20_PortType erpForSDM = getYMM20_310ERP();
				String result = erpForSDM.ymmSplmMatToSap(translateJsonIntoString(partListToERPForSDM));
				outputDataFile(erp310FileName, translateJsonIntoString(partListToERPForSDM));
				System.out.println(
						'\n' + ERP_YMM20_310 + " " + methodName + "_" + triggerFrom + " Result :" + result + '\n');

				MqlUtil.mqlCommand(context, "trigger off");
				for (Part partObj : partListToERPForSDM) {
					partDomObj.setId(partObj.GUID);
					String erpLocation = partObj.PLANT;
					if (triggerFrom.equalsIgnoreCase("so")) {
						if(partObj.PLANT.equals(9000)) {							
							partDomObj.setAttributeValue(context, "SPLM_ERP9000_Sync", "True");
						} else {							
							partDomObj.setAttributeValue(context, "SPLM_ERP9001_Sync", "True");
						}
					} else {
						partDomObj.setAttributeValue(context, "SPLM_ServicePart_ERP310", "Complete");
					}
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + ERP_YMM20_310 + " " + methodName + "_" + triggerFrom + " Data : "
						+ partListToERPForSDM + " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = ERP_YMM20_310 + " " + methodName + "_" + triggerFrom + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error" + erp310FileName, error);
		}
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
					mcGroupCodeToERPObj.MODEL_CODE = mcAttCMCCode;
					mcGroupCodeToERPObj.GROUP_NO = groupDrawingAttGroupCode;
					mcGroupCodeToERPObj.GUID = mcId;
					mcGroupCodeToERPArray.add(mcGroupCodeToERPObj);
				}
			}
		}
		// connect CMC for test
		try {
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
			outputDataFile("22_ERP_ModeCode-GroupCode.txt", translateJsonIntoString(mcGroupCodeToERPArray));
			YSD041_NEW_PortType erp = getYSD041_300ERP();
			String result = erp.YPutSplmMcg(translateJsonIntoString(mcGroupCodeToERPArray));
			System.out.println('\n' + result + '\n');
//			}
		} catch (Exception e) {
			System.err.println("Error detail in Error.txt.");
			String error = "searchMcGroupCode error : " + e.getMessage();
			outputDataFile("Error.txt", error);
		}
	}

	private class McGroupCodeToERP extends Abstract_CMC_ObjBaseAttribute {
		public String MODEL_CODE;
		public String GROUP_NO;
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
					msPartCategoryToERPObj.MODEL_SERIES = msName;
					msPartCategoryToERPObj.PART_CATEGORY_NO = partsCatalogueName;
					msPartCategoryToERPObj.MITSUBISHI_FLAG = msBrand.equalsIgnoreCase("mmc") ? "Y" : "N";
					msPartCategoryToERPObj.GUID = msId;
					msPartCategoryToERPArray.add(msPartCategoryToERPObj);
				}
			}
		}
		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				outputDataFile("20_ERP_ModelSeries-PartCategory.txt",
						translateJsonIntoString(msPartCategoryToERPArray));
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmMsp(translateJsonIntoString(msPartCategoryToERPArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
			}
		} catch (Exception e) {
			System.err.println("Error detail in Error.txt.");
			String error = "searchMsPartCategory error : " + e.getMessage();
			outputDataFile("Error.txt", error);
		}

	}

	private class MsPartCategoryToERP extends Abstract_CMC_ObjBaseAttribute {
		public String MODEL_SERIES;
		public String PART_CATEGORY_NO;
		public String MITSUBISHI_FLAG;
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
					partsCatelogueMcToERPObj.PART_CATEGORY_NO = partsCatelogueName;
					partsCatelogueMcToERPObj.MODEL_CODE = mcName;
					partsCatelogueMcToERPObj.CTG_MODEL = "";
					partsCatelogueMcToERPObj.GUID = partsCatelogueId;
					partsCatelogueMcToERPArray.add(partsCatelogueMcToERPObj);
				}
			}
		}
		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				outputDataFile("21_ERP_PartCategory-ModelCode.txt",
						translateJsonIntoString(partsCatelogueMcToERPArray));
				YSD041_NEW_PortType erp = getYSD041_300ERP();
				String result = erp.YPutSplmMpc(translateJsonIntoString(partsCatelogueMcToERPArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			outputDataFile("16_ERP_PNC.txt", translateJsonIntoString(pncForBothArray));
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
			}
		} catch (Exception e) {
			System.err.println("Error detail in Error.txt.");
			String error = "searchPartsCatelogueMc error : " + e.getMessage();
			outputDataFile("Error.txt", error);
		}
	}

	private class PartsCatelogueMcToERP extends Abstract_CMC_ObjBaseAttribute {
		public String PART_CATEGORY_NO;
		public String MODEL_CODE;
		public String CTG_MODEL;
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
		busSelects.add("attribute[SPLM_ModelSeries]");

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
			String partAttMs = (String) partMap.get("attribute[SPLM_ModelSeries]");

			ArrayList<String> partAttMcArray = new ArrayList<String>();
			ArrayList<String> partAttMsArray = new ArrayList<String>();

			// SPLM_CMC_Code data processing
			for (String mcStr : partAttMc.split(SPLIT_STR)) {
				partAttMcArray.add(mcStr);
			}
			
			// SPLM_ModelSeries data processing
			for (String msStr : partAttMs.split(SPLIT_STR)) {
				partAttMsArray.add(msStr);
			}

			// json for DMS system
			PartModelToDMS partModelToDMSObj = new PartModelToDMS();
			partModelToDMSObj.PART_NO = partName;
			partModelToDMSObj.MODEL_CODE = partAttMcArray;
			partModelToDMSObj.MODEL_SERISE = partAttMsArray;
			partModelToDMSObj.GUID = partId;
			partModelToDMSArray.add(partModelToDMSObj);
		}

//		List<PartModelToDMS> partModelForDMS = partModelToDMSArray.stream().limit(LIMIT_NUMS_DMS)
//				.collect(Collectors.toList());

		DomainObject partModelDomObj = new DomainObject();
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();

		try {
			if (partModelToDMSArray.size() > 0) {
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partModel(translateJsonIntoString(partModelToDMSArray));
				outputDataFile("13_DMS_PartModel.txt", translateJsonIntoString(partModelToDMSArray));
				System.out.println('\n' + DMS + " " + methodName + " Result :" + result + '\n');

				MqlUtil.mqlCommand(context, "trigger off");
				for (PartModelToDMS partModelObj : partModelToDMSArray) {
					partModelDomObj.setId(partModelObj.GUID);
					partModelDomObj.setAttributeValue(context, "SPLM_PartModel_DMS", "Complete");
				}
				MqlUtil.mqlCommand(context, "trigger on");
			} else {
				System.out.println('\n' + DMS + " " + methodName + " Data : " + partModelToDMSArray
						+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = DMS + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}

	}

	private class PartModelToDMS extends Abstract_CMC_ObjBaseAttribute {
		public String PART_NO;
		public ArrayList<String> MODEL_CODE;
		public ArrayList<String> MODEL_SERISE;
	}

	/**
	 * 29. Dealer
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public void createDealer(Context context, String[] args) throws Exception {
		Gson gson = new Gson();

		String dealerStr = "";
		try {
			SPlmWSSoap locator = getSPlmWSSoapDMS();
			dealerStr = locator.dealer();
			String dealerType = "SPLM_Dealer";
			String dealerVault = VAULT;
			String dealerPolicy = "SPLM_Dealer";
			ArrayList<Dealer> dealerArray = gson.fromJson(dealerStr, new TypeToken<ArrayList<Dealer>>() {
			}.getType());

			DomainObject createDealerDomObj = new DomainObject();
			DomainObject modDomObj = new DomainObject();
			Map<String, ArrayList<String>> dealerDetailMap = new HashMap<String, ArrayList<String>>();
			Map<String, String> masterIdDealerMap = new HashMap<String, String>();

			for (Dealer dealerObj : dealerArray) {
				String dealerName = dealerObj.COMPANY_ID;

				MapList dealerMapList = DomainObject.findObjects(context, dealerType, // type
						dealerName, // name
						"-", // revision
						"*", // owner
						dealerVault, // vault
						null, // where
						null, false, new StringList(DomainConstants.SELECT_ID), (short) 0);

				// collect new Dealer
				if (dealerMapList.isEmpty()) {
					createDealerDomObj.createObject(context, dealerType, dealerName, "-", dealerPolicy, dealerVault);
					System.out.println("Create DealerName : " + dealerName);
				} else {
					String dealerId = (String) ((Map) (dealerMapList.get(0))).get(DomainConstants.SELECT_ID);
					createDealerDomObj.setId(dealerId);
					System.out.println("Exist  DealerName : " + dealerName);
				}

				// override attribute
				createDealerDomObj.setDescription(context, dealerObj.COMPANY_DESC);
				createDealerDomObj.setAttributeValue(context, "SPLM_Address", dealerObj.ADDRESS);
				createDealerDomObj.setAttributeValue(context, "SPLM_ContactNumber", dealerObj.TEL_NO);
				createDealerDomObj.setAttributeValue(context, "SPLM_Wheel", "Four Wheel");

				// collect Master Dealer
				filter(dealerDetailMap, dealerName, createDealerDomObj.getId(context));
				filter(dealerDetailMap, dealerName, dealerObj.MASTER_DEALER);

				// collect Master Dealer already connection name AND override
				// attribute[SPLM_DealerType]
				if (!StringUtils.isNotBlank(dealerObj.MASTER_DEALER)) {
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
			System.err.println("Error detail in Error.txt.");
			outputDataFile("Error.txt", e.getMessage());
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

		String dealerStr = "";
		try {
			GetGLMCustSoap locator = getGLMCust();
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
				String dealerName = dealerObj.DEALER_ID;

				MapList dealerMapList = DomainObject.findObjects(context, dealerType, // type
						dealerName, // name
						"-", // revision
						"*", // owner
						dealerVault, // vault
						null, // where
						null, false, new StringList(DomainConstants.SELECT_ID), (short) 0);

				// collect new Dealer
				if (dealerMapList.isEmpty()) {
					createDealerDomObj.createObject(context, dealerType, dealerName, "-", dealerPolicy, dealerVault);
					System.out.println("Create DealerName : " + dealerName);
				} else {
					String dealerId = (String) ((Map) (dealerMapList.get(0))).get(DomainConstants.SELECT_ID);
					createDealerDomObj.setId(dealerId);
					System.out.println("Exist  DealerName : " + dealerName);
				}

				switch (dealerObj.DEALER_TYPE) {
				case "1":
				case "2":
					dealerObj.DEALER_TYPE = "Two Wheel";
					break;
				case "7":
					dealerObj.DEALER_TYPE = "Four Wheel";
				}

				// override attribute
				createDealerDomObj.setDescription(context, dealerObj.DEALER_NAME_C);
				createDealerDomObj.setAttributeValue(context, "SPLM_EmailAddress", dealerObj.DEALER_MAIL);
				createDealerDomObj.setAttributeValue(context, "SPLM_ContactNumber", dealerObj.TEL_NO);
				createDealerDomObj.setAttributeValue(context, "SPLM_ContactPerson", dealerObj.CONTACT_PERSON);
				createDealerDomObj.setAttributeValue(context, "SPLM_Address", dealerObj.ADDRESS);
				createDealerDomObj.setAttributeValue(context, "SPLM_Wheel", dealerObj.DEALER_TYPE);

				// collect Master Dealer
				filter(dealerDetailMap, dealerName, createDealerDomObj.getId(context));
				filter(dealerDetailMap, dealerName, dealerObj.MASTER_DEALER);

				// collect Master Dealer already connection name
				if (!StringUtils.isNotBlank(dealerObj.MASTER_DEALER)) {
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
			System.err.println("Error detail in Error.txt.");
			outputDataFile("Error.txt", e.getMessage());
		}
	}

	private class GLMDealer extends Abstract_CMC_ObjBaseAttribute {
		public String DEALER_ID; //
		public String DEALER_NAME_C;
		public String ADDRESS;
		public String TEL_NO;
		public String MASTER_DEALER;
		public String DEALER_TYPE;
		public String DEALER_MAIL;
		public String CONTACT_PERSON;
	}

	/**
	 * 9. MRP BOM
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	private void _searchMRP_BOM(Context context, DomainObject soObj) throws Exception {
		String affectedItemPartType = "SPLM_Part,SPLM_ColorPart";

		StringList affectedItemBusSelects = new StringList();
		affectedItemBusSelects.add(DomainConstants.SELECT_ID);
		affectedItemBusSelects.add(DomainConstants.SELECT_NAME);
		affectedItemBusSelects.add("attribute[SPLM_Location]");
		StringList affectedItemRelSelects = new StringList();

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
		String soId = soObj.getInfo(context, DomainConstants.SELECT_ID);

		MapList soAffectedItemList = soObj.getRelatedObjects(context, "SPLM_AffectedItem", // relationshipPattern,
				affectedItemPartType, // typePattern,
				affectedItemBusSelects, // StringList objectSelects,
				affectedItemRelSelects, // StringList relationshipSelects,
				false, // boolean getTo,
				true, // boolean getFrom,
				(short) 1, // short recurseToLevel,
				"attribute[SPLM_HaveSubPart]=='True'", // String objectWhere,
				"", // String relationshipWhere
				0); // int limit)

		// parent Part
		for (Object affectedItem : soAffectedItemList) {
			Map affectedItemInfo = (Map) affectedItem;
			String affectedItemName = (String) affectedItemInfo.get(DomainConstants.SELECT_NAME);
			String affectedItemId = (String) affectedItemInfo.get(DomainConstants.SELECT_ID);
			String affectedItemLocation = (String) affectedItemInfo.get("attribute[SPLM_Location]");
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

			Optional affectedItemVendorOptional = affectedItemVendorMapList.stream()
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME)).findFirst();
			String affectedItemVendor = (String) affectedItemVendorOptional.orElse("");

			// sub Part
			MapList subPartMapList = affectedItemObj.getRelatedObjects(context, "SPLM_SBOM", // relationshipPattern,
					affectedItemPartType, // typePattern,
					subPartBusSelects, // StringList objectSelects,
					subPartRelSelects, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_Materiel_KD_Type]=='D-'||attribute[SPLM_Materiel_KD_Type]=='K-'", // String
																										// relationshipWhere
					0); // int limit)

			for (Object subPartObj : subPartMapList) {
				Map subPartMap = (Map) subPartObj;
				String subPartAttStartDate = (String) subPartMap.get("attribute[SPLM_EnableDate]");
				String subPartAttEndDate = (String) subPartMap.get("attribute[SPLM_DisableDate]");

				// json for ERP system
				try {
					for (String location : affectedItemLocation.split(SPLIT_STR)) {

						MRP_BOMToERP mrpBOMToERP = new MRP_BOMToERP();
						mrpBOMToERP.SO_NO = soName;
						mrpBOMToERP.VENDOR_NO = affectedItemVendor;
						mrpBOMToERP.PARENT_PART_NO = affectedItemName;
						mrpBOMToERP.PARENT_PART_PLANT = location;
						mrpBOMToERP.CHILD_PART_NO = (String) subPartMap.get(DomainObject.SELECT_NAME);
						mrpBOMToERP.QUANTITY = (String) subPartMap.get("attribute[Quantity]");
						mrpBOMToERP.CHILD_PART_START_DATE = subPartAttStartDate.isEmpty() ? ""
								: convertDateFormat(subPartAttStartDate, "yyyyMMdd");
						mrpBOMToERP.CHILD_PART_END_DATE = subPartAttEndDate.isEmpty() ? ""
								: convertDateFormat(subPartAttEndDate, "yyyyMMdd");
						mrpBOMToERPArray.add(mrpBOMToERP);
					}
				} catch (Exception e) {
					outputDataFile("Error.txt", e.getMessage() + "\nParent Part : " + affectedItemName
							+ "\nChild Part : " + (String) subPartMap.get(DomainObject.SELECT_NAME));
				}
			}
		}

		DomainObject partDomObj = new DomainObject();
		List<MRP_BOMToERP> mrpBOMToERPArrayForCMC = mrpBOMToERPArray.stream()
				.filter(mrp -> mrp.PARENT_PART_PLANT.equals("1300")).collect(Collectors.toList());
		List<MRP_BOMToERP> mrpBOMToERPArrayForSDM = mrpBOMToERPArray.stream()
				.filter(mrp -> mrp.PARENT_PART_PLANT.equals("9000") || mrp.PARENT_PART_PLANT.equals("9001"))
				.collect(Collectors.toList());
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();

		// connect CMC for test
		try {
			if (mrpBOMToERPArrayForCMC.size() > 0) {
				YPP16_PortType erpForCMC = getYPP16_300ERP();
				String result = erpForCMC.yppSplmBomToSap(translateJsonIntoString(mrpBOMToERPArrayForCMC));
				outputDataFile("9.ERP_MRPBOM_ForCMC.txt", translateJsonIntoString(mrpBOMToERPArrayForCMC));
				System.out.println('\n' + ERP_YPP16_300 + " " + methodName + " Result :" + result + '\n');
			} else {
				System.out.println('\n' + ERP_YPP16_300 + " " + methodName + " Data : " + mrpBOMToERPArrayForCMC
						+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}

		} catch (Exception e) {
			String error = ERP_YPP16_300 + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}

		try {
			if (mrpBOMToERPArrayForSDM.size() > 0) {
				YPP16_PortType erpForSDM = getYPP16_310ERP();
				String result = erpForSDM.yppSplmBomToSap(translateJsonIntoString(mrpBOMToERPArrayForSDM));
				outputDataFile("9.ERP_MRPBOM_ForSDM.txt", translateJsonIntoString(mrpBOMToERPArrayForSDM));
				System.out.println('\n' + ERP_YPP16_310 + " " + methodName + " Result :" + result + '\n');
			} else {
				System.out.println('\n' + ERP_YPP16_310 + " " + methodName + " Data : " + mrpBOMToERPArrayForSDM
						+ " (\u4e0d\u62cb\u8f49\u8cc7\u6599)");
			}
		} catch (Exception e) {
			String error = ERP_YPP16_310 + " " + methodName + " error : " + e.getMessage();
			System.err.println(error);
			outputDataFile("Error.txt", error);
		}

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
	}

	/* ---TEST Part--- */
//	public void searchPncTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
//				SPlmWSSoap dms = getSPlmWSSoapDMS();
//				String result = dms.PNC(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
//				YSD041_NEW_PortType erp = getYSD041ERP();
//				String result = erp.YPutSplmP01(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println('\n' + result + '\n');
//			}
//		} catch (Exception e) {
//			outputDataFile("Error.txt", e.getMessage());
//		}
//	}
//
//	public void searchGroupCodeTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
//				SPlmWSSoap dms = getSPlmWSSoapDMS();
//				String result = dms.group(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
//				YSD041_NEW_PortType erp = getYSD041ERP();
//				String result = erp.YPutSplmG01(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			outputDataFile("Error.txt", e.getMessage());
//		}
//
//	}
//
//	public void searchPartVendorTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
//				SPlmWSSoap dms = getSPlmWSSoapDMS();
//				String result = dms.partVendor(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
//				YSD041_NEW_PortType erp = getYSD041ERP();
//				String result = erp.YPutSplmM02(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			outputDataFile("Error.txt", e.getMessage());
//		}
//
//	}
//
//	public void searchAltPartTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
//				SPlmWSSoap dms = getSPlmWSSoapDMS();
//				String result = dms.partAlt(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
//				YMM20_PortType erp = getYMM20ERP();
//				String result = erp.ymmReplaceMatToSap(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			outputDataFile("Error.txt", e.getMessage());
//		}
//	}
//
//	public void searchPartPncTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
//				SPlmWSSoap dms = getSPlmWSSoapDMS();
//				String result = dms.partPnc(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
//				YSD041_NEW_PortType erp = getYSD041ERP();
//				String result = erp.YPutSplmM03(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			System.err.println("Error detail in Error.txt.");
//			outputDataFile("Error.txt", e.getMessage());
//		}
//	}
//
//	public void searchOptPartTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
//				SPlmWSSoap dms = getSPlmWSSoapDMS();
//				String result = dms.partOpt(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			outputDataFile("Error.txt", e.getMessage());
//		}
//
//	}
//
//	public void searchPncPartTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
//				SPlmWSSoap dms = getSPlmWSSoapDMS();
//				String result = dms.partPnc(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
//				YSD041_NEW_PortType erp = getYSD041ERP();
//				String result = erp.YPutSplmM03(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			outputDataFile("Error.txt", e.getMessage());
//		}
//
//	}
//
//	public void searchPartGroupTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
//				SPlmWSSoap dms = getSPlmWSSoapDMS();
//				String result = dms.partGroup(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			System.err.println("Error detail in Error.txt.");
//			outputDataFile("Error.txt", e.getMessage());
//		}
//
//	}
//
//	public void searchServicePartTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
//				SPlmWSSoap dms = getSPlmWSSoapDMS();
//				String result = dms.servicesPart(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
//				YMM20_PortType erp = getYMM20_310ERP();
//				String result = erp.ymmSplmMatToSap(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			outputDataFile("Error.txt", e.getMessage());
//		}
//
//	}
//
//	public void searchMcGroupCodeTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
//				YSD041_NEW_PortType erp = getYSD041ERP();
//				String result = erp.YPutSplmMcg(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			outputDataFile("Error.txt", e.getMessage());
//		}
//
//	}
//
//	public void searchMsPartCategoryTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
//				YSD041_NEW_PortType erp = getYSD041ERP();
//				String result = erp.YPutSplmMsp(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			outputDataFile("Error.txt", e.getMessage());
//		}
//
//	}
//
//	public void searchPartsCatelogueMcTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
//				YSD041_NEW_PortType erp = getYSD041ERP();
//				String result = erp.YPutSplmMpc(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			outputDataFile("Error.txt", e.getMessage());
//		}
//	}
//
//	public void searchPartModelTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
//				SPlmWSSoap dms = getSPlmWSSoapDMS();
//				String result = dms.partModel(inputJson);
//				System.out.println(result);
//			}
//
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			outputDataFile("Error.txt", e.getMessage());
//		}
//	}
//
//	public void searchMRP_BOMTest(Context context, String[] args) throws Exception {
//		String inputJson = String.join("", Files.readAllLines(Paths.get(TEST_FILE_PATH)));
//
//		// connect CMC for test
//		try {
//			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
//				YPP16_PortType erp = getYPP16ERP();
//				String result = erp.yppSplmBomToSap(inputJson);
//				System.out.println('\n' + result + '\n');
//			}
//			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
////			SPlmWSSoap dms = getSPlmWSSoapDMS();
////			String result = dms.PNC(inputJson);
////			System.out.println(result);
//			}
//		} catch (Exception e) {
//			outputDataFile("Error.txt", e.getMessage());
//		}
//	}
}