import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.tempuri.SPlmWSLocator;
import org.tempuri.SPlmWSSoap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.Context;
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

	private final String TEST_FILE_PATH = "C:\\temp\\Morga\\json.txt";
	private final String DEFAULT_ROOT_DIRECTORY = "C:\\temp\\Morga\\";
	private final String CALL_WSDL = "DMS";
	private final String PLM_ENV = System.getenv("PLM_ENV");
	private final short SEARCH_AMOUT = 5;

	private String getPropertyURL(String keyStr) {
		return null;
	}

	/* ---CMC wsdl connection--- */
	private SPlmWSSoap getSPlmWSSoapDMS() throws Exception {
		String url = getPropertyURL("DMS");
		SPlmWSLocator locator = new SPlmWSLocator();
		if (StringUtils.isEmpty(url)) {
			return locator.getSPlmWSSoap();
		} else {
			return locator.getSPlmWSSoap(new URL(url));
		}
	}

	private YMM20_PortType getYMM20ERP() throws Exception {
		String url = getPropertyURL("ERP");
		YMM20_ServiceLocator locator = new YMM20_ServiceLocator();
		if (StringUtils.isEmpty(url)) {
			return locator.getYMM20();
		} else {
			return locator.getYMM20(new URL(url));
		}
	}

	private YPP16_PortType getYPP16ERP() throws Exception {
		String url = getPropertyURL("ERP");
		YPP16_ServiceLocator locator = new YPP16_ServiceLocator();
		if (StringUtils.isEmpty(url)) {
			return locator.getYPP16();
		} else {
			return locator.getYPP16(new URL(url));
		}
	}

	private YSD041_NEW_PortType getYSD041ERP() throws Exception {
		String url = getPropertyURL("ERP");
		YSD041_NEW_ServiceLocator locator = new YSD041_NEW_ServiceLocator();
		if (StringUtils.isEmpty(url)) {
			return locator.getYSD041_NEW();
		} else {
			return locator.getYSD041_NEW(new URL(url));
		}
	}

	/* ---myMethod using--- */
	private String convertNowDateFormat(String formatType) {
		return convertDateFormat(new Date().toString(), formatType);
	}

	private String convertDateFormat(String targetDate, String dateFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		Date d = new Date(targetDate);
		String modifiedDate = sdf.format(d);
		return modifiedDate;
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
			java.io.BufferedWriter bw = new java.io.BufferedWriter(
					new java.io.OutputStreamWriter(new java.io.FileOutputStream(dataFile, true), "utf-8"));
			bw.write(dataStr + "");
			bw.newLine();
			bw.newLine();
			bw.write("******* " + convertNowDateFormat("yyyy.MM.dd HH:mm:ss") + "******* " + "\n\n");
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

	abstract class CMC_ObjBaseAttribute implements Cloneable {
		protected String CHANGE_DATE;
		protected String CREATE_DATE;
		protected String GUID;

		protected Object clone() throws CloneNotSupportedException {
			Object cloneObj = super.clone();
			return cloneObj;
		}
	}

	public void processAllMethod(Context context, String[] args) throws Exception {
		searchServicePart(context, args);
		searchGroupCode(context, args);
		searchGroupPnc(context, args);
		searchMcGroupCode(context, args);
		searchMsPartCategory(context, args);
		searchOptAltPart(context, args);
		searchPartGroup(context, args);
		searchPartModel(context, args);
		searchPartPnc(context, args);
		searchPartsCatelogueMc(context, args);
		searchPnc(context, args);
		searchPartVendor(context, args);
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
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		busSelects.add("attribute[SPLM_Name_TC]");
		busSelects.add("attribute[SPLM_Name_EN]");
		busSelects.add("attribute[SPLM_CommissionGroup]");

		MapList pncMapList = DomainObject.findObjects(context, "SPLM_PNC", // type
				"*", // name
				"-", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
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
			pncForBothObj.CHANGE_DATE = convertDateFormat((String) pncMap.get(DomainConstants.SELECT_MODIFIED),
					"yyyyMMdd");
			pncForBothObj.GUID = (String) pncMap.get(DomainConstants.SELECT_ID);
			pncForBothArray.add(pncForBothObj);
		}
		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				outputDataFile("16_DMS_PNC.txt", translateJsonIntoString(pncForBothArray));
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.PNC(translateJsonIntoString(pncForBothArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				outputDataFile("16_ERP_PNC.txt", translateJsonIntoString(pncForBothArray));
				YSD041_NEW_PortType erp = getYSD041ERP();
				String result = erp.YPutSplmP01(translateJsonIntoString(pncForBothArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			outputDataFile("16_ERP_PNC.txt", translateJsonIntoString(pncForBothArray));
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	private class PNCForBoth extends CMC_ObjBaseAttribute {
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
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add("attribute[SPLM_Name_TC]");
		busSelects.add("attribute[SPLM_Name_EN]");

		MapList groupCodeMapList = DomainObject.findObjects(context, "SPLM_GroupCode", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		ArrayList<GroupCodeForBoth> groupCodeForBothArray = new ArrayList<GroupCodeForBoth>();

		// json for DMS/ERP system
		for (Object obj : groupCodeMapList) {
			Map groupCodeMap = (Map) obj;
			GroupCodeForBoth groupCodeForBothObj = new GroupCodeForBoth();
			groupCodeForBothObj.GROUP_CODE = (String) groupCodeMap.get(DomainConstants.SELECT_NAME);
			groupCodeForBothObj.GROUP_NAME_C = (String) groupCodeMap.get("attribute[SPLM_Name_TC]");
			groupCodeForBothObj.GROUP_NAME_E = (String) groupCodeMap.get("attribute[SPLM_Name_EN]");
			groupCodeForBothObj.CHANGE_DATE = convertDateFormat(
					(String) groupCodeMap.get(DomainConstants.SELECT_MODIFIED), "yyyyMMdd");
			groupCodeForBothObj.GUID = (String) groupCodeMap.get(DomainConstants.SELECT_ID);
			groupCodeForBothArray.add(groupCodeForBothObj);
		}
		// connect CMC for test
		if (CALL_WSDL.equalsIgnoreCase("DMS")) {
			outputDataFile("17_DMS_Group.txt", translateJsonIntoString(groupCodeForBothArray));
			SPlmWSSoap dms = getSPlmWSSoapDMS();
			String result = dms.group(translateJsonIntoString(groupCodeForBothArray));
			System.out.println('\n' + result + '\n');
		}

		if (CALL_WSDL.equalsIgnoreCase("ERP")) {
			outputDataFile("17_ERP_Group.txt", translateJsonIntoString(groupCodeForBothArray));
			YSD041_NEW_PortType erp = getYSD041ERP();
			String result = erp.YPutSplmG01(translateJsonIntoString(groupCodeForBothArray));
			System.out.println('\n' + result + '\n');
		}

		if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			outputDataFile("16_ERP_PNC.txt", translateJsonIntoString(pncForBothArray));
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
		}
	}

	private class GroupCodeForBoth extends CMC_ObjBaseAttribute {
		public String GROUP_CODE;
		public String GROUP_NAME_C;
		public String GROUP_NAME_E;
	}

	/**
	 * 19.PartVendor
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchPartVendor(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_MODIFIED);

		StringList relatedBusSelects = new StringList();
		relatedBusSelects.add("current.actual");
		relatedBusSelects.add(DomainConstants.SELECT_NAME);
		StringList relatedRelSelects = new StringList();

		StringList relatedBusSelectsVendor = new StringList();
		relatedBusSelectsVendor.add(DomainConstants.SELECT_NAME);
		StringList relatedRelSelectsVendor = new StringList();

		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part", // type
				"CW745737", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"from[SPLM_RelatedVendor].attribute[SPLM_Valid]==TRUE", // where
//				"", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		ArrayList<VenderAlterToDMS> venderAlterToDMSArray = new ArrayList<VenderAlterToDMS>();
		ArrayList<VenderAlterToERP> venderAlterToERPArray = new ArrayList<VenderAlterToERP>();

		for (Object obj : partMapList) {
			Map partMap = (Map) obj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partModifiedDate = (String) partMap.get(DomainConstants.SELECT_MODIFIED);
			domObj.setId(partId);

			MapList vendoreMapList = domObj.getRelatedObjects(context, "SPLM_RelatedVendor", // relationshipPattern,
					"SPLM_Vendor", // typePattern,
					relatedBusSelectsVendor, // StringList objectSelects,
					relatedRelSelectsVendor, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_Valid]==TRUE", // String relationshipWhere
					0); // int limit)

			StringList partAttVendorList = new StringList();
			for (Object vendorObj : vendoreMapList) {
				Map vendorMap = (Map) vendorObj;
				String vendorName = (String) vendorMap.get(DomainConstants.SELECT_NAME);
				partAttVendorList.add(vendorName);
			}

			// SPLM_SO data processing
			MapList soMapList = domObj.getRelatedObjects(context, "SPLM_AffectedItem", // relationshipPattern,
					"SPLM_SO", // typePattern,
					relatedBusSelects, // StringList objectSelects,
					relatedRelSelects, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			String soLatestName = "";
			if (!soMapList.isEmpty()) {
				soMapList.addSortKey("current.actual", "descending", "date");
				soMapList.sort();
				soLatestName = (String) ((Map) soMapList.get(0)).get(DomainConstants.SELECT_NAME);

				// json for DMS system
				VenderAlterToDMS venderAlterToDMSObj = new VenderAlterToDMS();
				venderAlterToDMSObj.PART_NO = partName;
				venderAlterToDMSObj.VENDOR_NO = partAttVendorList;
				venderAlterToDMSObj.SO_NO = soLatestName;
				venderAlterToDMSObj.CHANGE_DATE = convertDateFormat(partModifiedDate, "yyyyMMdd");
				venderAlterToDMSObj.GUID = partId;
				venderAlterToDMSArray.add(venderAlterToDMSObj);

				// json for ERP system
				for (String partAttVendor : partAttVendorList) {
					VenderAlterToERP venderAlterToERPObj = new VenderAlterToERP();
					venderAlterToERPObj.PART_NO = partName;
					venderAlterToERPObj.SO_NO = soLatestName;
					venderAlterToERPObj.CHANGE_DATE = convertDateFormat(partModifiedDate, "yyyyMMdd");
					venderAlterToERPObj.VENDOR_NO = partAttVendor;
					venderAlterToERPObj.GUID = partId;
					venderAlterToERPArray.add(venderAlterToERPObj);
				}
			}
		}
		// connect CMC for test
		if (CALL_WSDL.equalsIgnoreCase("DMS")) {
			outputDataFile("19_DMS_PartVendor.txt", translateJsonIntoString(venderAlterToDMSArray));
			SPlmWSSoap dms = getSPlmWSSoapDMS();
			String result = dms.partVendor(translateJsonIntoString(venderAlterToDMSArray));
			System.out.println('\n' + result + '\n');
		}

		if (CALL_WSDL.equalsIgnoreCase("ERP")) {
			outputDataFile("19_ERP_PartVendor.txt", translateJsonIntoString(venderAlterToERPArray));
			YSD041_NEW_PortType erp = getYSD041ERP();
			String result = erp.YPutSplmM02(translateJsonIntoString(venderAlterToDMSArray));
			System.out.println('\n' + result + '\n');
		}

		if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			outputDataFile("16_ERP_PNC.txt", translateJsonIntoString(pncForBothArray));
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
		}
	}

	private class VenderAlterToDMS extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public StringList VENDOR_NO;
		public String SO_NO;
	}

	private class VenderAlterToERP extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public String VENDOR_NO;
		public String SO_NO;
	}

	/**
	 * 14/15 Opt/Alt
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchOptAltPart(Context context, String[] args) throws Exception {
		String strAltOpt = args[0];
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		StringList relSelects = new StringList();
		relSelects.add("attribute[SPLM_OptionalType]");

		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"from[SPLM_RelatedOptionalPart]==TRUE", // where
				null, false, busSelects, (short) (SEARCH_AMOUT * 2));

		DomainObject domObj = new DomainObject();
		ArrayList<OptionalPartToERP> OptionalPartToERPArray = new ArrayList<OptionalPartToERP>();
		ArrayList<OptionalPartToDMS> OptionalPartToDMSArray = new ArrayList<OptionalPartToDMS>();
		ArrayList<AlterPartToERP> AlterPartToERPArray = new ArrayList<AlterPartToERP>();
		ArrayList<AlterPartToDMS> AlterPartToDMSArray = new ArrayList<AlterPartToDMS>();
		String altStr = "Alternate Part";
		String optStr = "Optional Part";

		for (Object obj : partMapList) {
			Map partMap = (Map) obj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partModifiedDate = (String) partMap.get(DomainConstants.SELECT_MODIFIED);

			domObj.setId(partId);
			MapList partOptMapList = domObj.getRelatedObjects(context, "SPLM_RelatedOptionalPart", // relationshipPattern,
					"SPLM_Part", // typePattern,
					busSelects, // StringList objectSelects,
					relSelects, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			Map<String, ArrayList<String>> partOptAttOptionalTypeMap = new HashMap<String, ArrayList<String>>();
			for (Object partOptObj : partOptMapList) {
				Map partOptMap = (Map) partOptObj;
				String partOptName = (String) partOptMap.get(DomainConstants.SELECT_NAME);
				String partOptAttOptionalType = (String) partOptMap.get("attribute[SPLM_OptionalType]");
				String OptionalType = partOptAttOptionalType.equalsIgnoreCase(altStr) ? altStr : optStr;

				filter(partOptAttOptionalTypeMap, OptionalType, partOptName);
			}

			// json for Optional_type
			if (partOptAttOptionalTypeMap.containsKey(optStr)) {
				ArrayList<String> partOptOptionalPartArray = partOptAttOptionalTypeMap.get(optStr);

				// DMS system
				OptionalPartToDMS OptionalPartToDMSObj = new OptionalPartToDMS();
				OptionalPartToDMSObj.PART_NO = partName;
				OptionalPartToDMSObj.REUSE_PART_NO = partOptOptionalPartArray;
				OptionalPartToDMSObj.CHANGE_DATE = convertDateFormat(partModifiedDate, "yyyyMMdd");
				OptionalPartToDMSObj.GUID = partId;
				OptionalPartToDMSArray.add(OptionalPartToDMSObj);

				// ERP system
				for (String partOptOptionalPartName : partOptOptionalPartArray) {
					OptionalPartToERP OptionalPartToERPObj = new OptionalPartToERP();
					OptionalPartToERPObj.PART_NO = partName;
					OptionalPartToERPObj.REUSE_PART_NO = partOptOptionalPartName;
					OptionalPartToERPObj.CHANGE_DATE = convertDateFormat(partModifiedDate, "yyyyMMdd");
					OptionalPartToERPObj.GUID = partId;
					OptionalPartToERPArray.add(OptionalPartToERPObj);
				}
			}

			// json for Alternate_type
			if (partOptAttOptionalTypeMap.containsKey(altStr)) {
				ArrayList<String> partOptAlterPartArray = partOptAttOptionalTypeMap.get(altStr);

				// DMS system
				AlterPartToDMS AlterPartToDMSObj = new AlterPartToDMS();
				AlterPartToDMSObj.PART_NO = partName;
				AlterPartToDMSObj.NEW_PART_NO = partOptAlterPartArray;
				AlterPartToDMSObj.CHANGE_DATE = convertDateFormat(partModifiedDate, "yyyyMMdd");
				AlterPartToDMSObj.GUID = partId;
				AlterPartToDMSArray.add(AlterPartToDMSObj);

				// ERP system
				for (String partOptAlterPartName : partOptAlterPartArray) {
					AlterPartToERP AlterPartToERPObj = new AlterPartToERP();
					AlterPartToERPObj.PART_NO = partName;
					AlterPartToERPObj.NEW_PART_NO = partOptAlterPartName;
					AlterPartToERPObj.CHANGE_DATE = convertDateFormat(partModifiedDate, "yyyyMMdd");
					AlterPartToERPObj.GUID = partId;
					AlterPartToERPArray.add(AlterPartToERPObj);
				}
			}
		}

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				if (strAltOpt.equalsIgnoreCase("ALT")) {
					outputDataFile("14_DMS_Alter.txt", translateJsonIntoString(AlterPartToDMSArray));
					SPlmWSSoap dms = getSPlmWSSoapDMS();
					String result = dms.partAlt(translateJsonIntoString(AlterPartToDMSArray));
					System.out.println('\n' + result + '\n');
				} else if (strAltOpt.equalsIgnoreCase("OPT")) {
					outputDataFile("15_DMS_Optional.txt", translateJsonIntoString(OptionalPartToDMSArray));
					SPlmWSSoap dms = getSPlmWSSoapDMS();
					String result = dms.partOpt(translateJsonIntoString(OptionalPartToDMSArray));
					System.out.println('\n' + result + '\n');
				}
			}

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
//			if (strAltOpt.equalsIgnoreCase("ALT")) {
				outputDataFile("14_ERP_Alter.txt", translateJsonIntoString(AlterPartToERPArray));
				YMM20_PortType erp = getYMM20ERP();
				String result = erp.ymmReplaceMatToSap(translateJsonIntoString(AlterPartToERPArray));
				System.out.println('\n' + result + '\n');
//			} else if (ALT_OPT.equalsIgnoreCase("OPT")){
//				outputDataFile("15_ERP_Optional.txt", translateJsonIntoString(OptionalPartToERPArray));
//			}
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	// 14. Alternate Part
	private class AlterPartToDMS extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public ArrayList<String> NEW_PART_NO;
	}

	private class AlterPartToERP extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public String NEW_PART_NO;
	}

	// 15. Optional Part
	private class OptionalPartToDMS extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public ArrayList<String> REUSE_PART_NO;
	}

	private class OptionalPartToERP extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public String REUSE_PART_NO;
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

		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"to[SPLM_RelatedPart]==TRUE", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		Map<String, ArrayList<String>> partPncMap = new HashMap<String, ArrayList<String>>();
		ArrayList<PartPncToDMS> partPncToDMSArray = new ArrayList<PartPncToDMS>();
		ArrayList<PartPncToERP> partPncToERPArray = new ArrayList<PartPncToERP>();
		ArrayList<String> pncArrayList = new ArrayList<String>();

		for (Object obj : partMapList) {
			Map partMap = (Map) obj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);

			domObj.setId(partId);
			StringList pncNameList = domObj.getInfoList(context, "to[SPLM_RelatedPart].from[SPLM_PNC].name");

			if (pncNameList.isEmpty()) {
				pncNameList.add("");
			}

			for (String pncName : pncNameList) {
				filter(partPncMap, partName, pncName);
			}
			pncArrayList = partPncMap.get(partName);

			// json for DMS system
			PartPncToDMS partPncToDMSObj = new PartPncToDMS();
			partPncToDMSObj.PART_NO = partName;
			partPncToDMSObj.PNC_NO = pncArrayList;
			partPncToDMSObj.CHANGE_DATE = convertNowDateFormat("yyyyMMdd");
			partPncToDMSObj.GUID = partId;
			partPncToDMSArray.add(partPncToDMSObj);

			// json for ERP system
			for (String pncName : pncArrayList) {
				PartPncToERP partPncToERPObj = new PartPncToERP();
				partPncToERPObj.PART_NO = partName;
				partPncToERPObj.PNC_NO = pncName;
				partPncToERPObj.CHANGE_DATE = convertNowDateFormat("yyyyMMdd");
				partPncToERPObj.GUID = partId;
				partPncToERPArray.add(partPncToERPObj);
			}
		}

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				outputDataFile("11_DMS_Part-PNC.txt", translateJsonIntoString(partPncToDMSArray));
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partPnc(translateJsonIntoString(partPncToDMSArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				outputDataFile("11_ERP_Part-PNC.txt", translateJsonIntoString(partPncToERPArray));
				YSD041_NEW_PortType erp = getYSD041ERP();
				String result = erp.YPutSplmM03(translateJsonIntoString(partPncToERPArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}
	}

	private class PartPncToDMS extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public ArrayList<String> PNC_NO;
	}

	private class PartPncToERP extends CMC_ObjBaseAttribute {
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
		busSelects.add("attribute[SPLM_GroupCode]");
		StringList relSelect = new StringList();

		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"to[SPLM_RelatedPart]==TRUE", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		Map<String, ArrayList<String>> partGroupCodeMap = new HashMap<String, ArrayList<String>>();
		ArrayList<PartGroupCodeToDMS> partGroupCodeToDMSArray = new ArrayList<PartGroupCodeToDMS>();

		for (Object obj : partMapList) {
			Map partMap = (Map) obj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			domObj.setId(partId);

			MapList groupDrawingMapList = domObj.getRelatedObjects(context, "SPLM_RelatedPart", // relationshipPattern,
					"SPLM_GroupDrawing", // typePattern,
					busSelects, // StringList objectSelects,
					relSelect, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			for (Object groupDrawingObj : groupDrawingMapList) {
				String groupDrawingAttGroupCode = (String) ((Map) groupDrawingObj).get("attribute[SPLM_GroupCode]");
				filter(partGroupCodeMap, partName, groupDrawingAttGroupCode);
			}

			// json for DMS System
			PartGroupCodeToDMS partGroupCodeToDMSObj = new PartGroupCodeToDMS();
			partGroupCodeToDMSObj.PART_NO = partName;
			partGroupCodeToDMSObj.GROUP_CODE = partGroupCodeMap.get(partName);
			partGroupCodeToDMSObj.CHANGE_DATE = convertNowDateFormat("yyyyMMdd");
			partGroupCodeToDMSObj.GUID = partId;
			partGroupCodeToDMSArray.add(partGroupCodeToDMSObj);
		}

		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				outputDataFile("12_DMS_Part-Group.txt", translateJsonIntoString(partGroupCodeToDMSArray));
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partGroup(translateJsonIntoString(partGroupCodeToDMSArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	private class PartGroupCodeToDMS extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public ArrayList<String> GROUP_CODE;
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
				"eService Production", // vault
				"from[SPLM_Related_PNC]==TRUE", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		ArrayList<GroupCodePncToERP> groupCodePncToERPArray = new ArrayList<GroupCodePncToERP>();

		for (Object obj : groupDrawingMapList) {
			Map groupDrawingMap = (Map) obj;
			String groupDrawingId = (String) groupDrawingMap.get(DomainConstants.SELECT_ID);
			String groupDrawingAttGroupCode = (String) groupDrawingMap.get("attribute[SPLM_GroupCode]");
			domObj.setId(groupDrawingId);
			StringList pncNameList = domObj.getInfoList(context, "from[SPLM_Related_PNC].to[SPLM_PNC].name");

			MapList groupCodeMapList = DomainObject.findObjects(context, "SPLM_GroupCode", // type
					groupDrawingAttGroupCode, // name
					"*", // revision
					"*", // owner
					"eService Production", // vault
					"", // where
					null, false, busSelects, (short) 1);

			// json for ERP system
			for (Object groupCodeObj : groupCodeMapList) {
				for (String pncName : pncNameList) {
					GroupCodePncToERP groupCodePncToERPObj = new GroupCodePncToERP();
					groupCodePncToERPObj.PNC_NO = pncName;
					groupCodePncToERPObj.GROUP_CODE = groupDrawingAttGroupCode;
					groupCodePncToERPObj.CHANGE_DATE = convertNowDateFormat("yyyyMMdd");
					groupCodePncToERPObj.GUID = (String) ((Map) groupCodeObj).get(DomainConstants.SELECT_ID);
					groupCodePncToERPArray.add(groupCodePncToERPObj);
				}
			}
		}
		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				outputDataFile("18_ERP_Group-PNC.txt", translateJsonIntoString(groupCodePncToERPArray));
				YSD041_NEW_PortType erp = getYSD041ERP();
				String result = erp.YPutSplmP03(translateJsonIntoString(groupCodePncToERPArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	private class GroupCodePncToERP extends CMC_ObjBaseAttribute {
		public String GROUP_CODE;
		public String PNC_NO;
	}

	// doing
	/**
	 * 8. ServicePart
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchServicePart(Context context, String[] args) throws Exception {

		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID); // GUID
		busSelect.add(DomainConstants.SELECT_NAME); // PART_NO
		busSelect.add(DomainConstants.SELECT_ORIGINATED); // CREATE_DATE
		busSelect.add(DomainConstants.SELECT_MODIFIED); // CHANGE_DATE
		busSelect.add(DomainConstants.SELECT_OWNER); // CHANGE_DATE
		busSelect.add("attribute[SPLM_OrderType]"); // PART_SH_NO
		busSelect.add("attribute[SPLM_Location]"); // PLANT
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

		StringList busSelectPartOpt = new StringList();
		busSelectPartOpt.add(DomainConstants.SELECT_NAME); // PART_NO
		StringList relSelectPartOpt = new StringList();
		relSelectPartOpt.add("attribute[SPLM_OptionalType]");

		StringList busSelectSO = new StringList();
		busSelectSO.add(DomainConstants.SELECT_NAME);
		busSelectSO.add("current.actual");
		busSelectSO.add("attribute[SPLM_SO_Location]");
		StringList relSelectSO = new StringList();

		String splitStr = ",";
		String altStr = "Alternate Part";
		String optStr = "Optional Part";
		String domesticSoLocation = "Domestic Item";
		String exportSoLocation = "Export Item";

		MapList partList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelect, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		ArrayList<Part> partArray = new ArrayList<Part>();

		for (Object partObj : partList) {
			MapList partOptList = new MapList();
			Map<String, String> mapping = new HashMap<String, String>();

			Map partMap = (Map) partObj;
			Part part = new Part();
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);

			domObj.setId(partId);
			StringList pncList = domObj.getInfoList(context, "to[SPLM_RelatedPart].from[SPLM_PNC].name");
			StringList groupCodeList = domObj.getInfoList(context,
					"to[SPLM_RelatedPart].from[SPLM_GroupDrawing].attribute[SPLM_GroupCode]");
			StringList vendorList = domObj.getInfoList(context, "from[SPLM_RelatedVendor].to[SPLM_Vendor].name");
			String partAttMaterialGroup = (String) partMap.get("attribute[SPLM_MaterialGroup]");
			String partAttLocation = (String) partMap.get("attribute[SPLM_Location]");
			String attBigCarType = (String) partMap.get("attribute[SPLM_DTAT_Only]");
			String vendorName = vendorList.isEmpty() ? "" : vendorList.get(0);
			String partAttMs = (String) partMap.get("attribute[SPLM_ModelSeries]");
			String partAttMc = (String) partMap.get("attribute[SPLM_CMC_Code]");
			String partAttPNCType = (String) partMap.get("attribute[SPLM_CommissionGroup]");
			String attTransGroup = "";
			String attSoLatestName = "";
			String attSoLatestDate = "";
			String attSoAttLocation = "";
			String attCarCategory = "";
			String attManufactureCode = "";
			String attItemCategoryGroup = "";
			String attSalesOrg = "";

			// SALE_ORG data processing
			for (String partLocation : partAttLocation.split(splitStr)) {
				switch (partLocation) {
				case "1300":
					attSalesOrg = "3000";
					break;
				case "9001":
					attSalesOrg = "9000";
					break;
				case "9000":
					attSalesOrg = "SDM";
				}
			}

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

			for (String mcStr : partAttMc.split(splitStr)) {
				if (mcStr.isEmpty() || !partAttMcArray.isEmpty()) {
					continue;
				}
				MapList mcMapList = DomainObject.findObjects(context, "SPLM_ModelCode", // type
						mcStr, // name
						"*", // revision
						"*", // owner
						"eService Production", // vault
						"attribute[SPLM_CMC_Code]!=''", // where
						null, false, new StringList("attribute[SPLM_CMC_Code]"), (short) 0);
				for (Object mcObj : mcMapList) {
					partAttMcArray.add((String) ((Map) mcObj).get("attribute[SPLM_CMC_Code]"));
				}
			}

			for (String str : partAttMs.split(splitStr)) {
				if (str.isEmpty()) {
					continue;
				}
				partAttMsArray.add(str);
			}

			// TRANS_GROUP data processing
			if (partAttLocation.equals("1300")) {
				if (partAttPNCType.equals("Z1") || partAttPNCType.equals("Z3") || partAttPNCType.equals("Z4")
						|| partAttPNCType.equals("Z5")) {
					attTransGroup = "DUMY";
				}
			} else if (partAttLocation.equals("9001") || partAttLocation.equals("9000")) {
				attTransGroup = "ZS01";
			}

			// ALT_PART_NO / REUSE_PART_NO data processing
			partOptList = domObj.getRelatedObjects(context, "SPLM_RelatedOptionalPart", // relationshipPattern,
					"SPLM_Part", // typePattern,
					busSelectPartOpt, // StringList objectSelects,
					relSelectPartOpt, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			mapping = new HashMap<String, String>();
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
					relSelectSO, // StringList relationshipSelects,
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
						"eService Production", // vault
						"", // where
						null, false, new StringList("attribute[SPLM_Vehicles]"), (short) 0);
				for (Object msObj : msList) {
					attCarCategory = (String) ((Map) msObj).get("attribute[SPLM_Vehicles]");
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
			part.PART_NO = (String) partMap.get(DomainConstants.SELECT_NAME);
			part.PART_SH_NO = (String) partMap.get("attribute[SPLM_OrderType]");
			part.K_D = (partAttMaterialGroup.equalsIgnoreCase("KD") || partAttMaterialGroup.equalsIgnoreCase("KDY"))
					? "K"
					: "D";
			part.GROUP_CODE = groupCodeList.isEmpty() ? "" : groupCodeList.get(0);
			part.ALT_PART_NO = mapping.containsKey(altStr) ? mapping.get(altStr) : "";
			part.REUSE_PART_NO = mapping.containsKey(optStr) ? mapping.get(optStr) : "";
			part.MATERIAL_TYPE = (String) partMap.get("attribute[SPLM_MaterialType]");

			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				part.MATERIAL_GROUP = (partAttMaterialGroup.equalsIgnoreCase("KD")
						|| partAttMaterialGroup.equalsIgnoreCase("KDY")) ? "K" : "D";
				part.PLANT = partAttLocation.equalsIgnoreCase("1300,9000") ? "SDM" : partAttLocation;
			}
			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				part.MATERIAL_GROUP = partAttMaterialGroup;
				part.PLANT = partAttLocation;
			}

			part.UNIT = (String) partMap.get("attribute[SPLM_Unit]");
			part.PURCHASING_DEPARTMENT_NO = (String) partMap.get("attribute[SPLM_PurchasingGroup]");
			part.MANUFACTURER_CODE = attManufactureCode;
			part.VENDOR_NO = vendorName;
			part.PART_NAME_C = (String) partMap.get("attribute[SPLM_Name_TC]");
			part.PART_NAME_E = (String) partMap.get("attribute[SPLM_Name_EN]");
			part.SALES_ORG = attSalesOrg;
			part.PNC_NO = pncList.isEmpty() ? "" : pncList.get(0);
			part.ITEM_CATEGORY_GROUP = attItemCategoryGroup;
			part.MODEL_SERIES = partAttMsArray.isEmpty() ? "" : partAttMsArray.get(0);
			part.MODEL_CODE = partAttMcArray.isEmpty() ? "" : partAttMcArray.get(0);
			part.CAR_CAEGORY = attCarCategory;
			part.PART_TYPE = partAttItemSubType;
			part.COMMISSION_GROUP = partAttPNCType;
			part.TRANS_GROUP = attTransGroup;
			part.BIG_CAR_TYPE = attBigCarType;
			part.SO_NO = attSoLatestName;
			part.SO_RELEASE_DATE = attSoLatestDate.isEmpty() ? "" : convertDateFormat(attSoLatestDate, "yyMMdd");
			part.HAS_SUBASSEMBLY = ((String) partMap.get("attribute[SPLM_HaveSubPart]")).equalsIgnoreCase("TRUE") ? "Y"
					: "N";
			part.PART_OWNER = (String) partMap.get(DomainConstants.SELECT_OWNER);
			part.IS_SERVICE_PART = ((String) partMap.get("attribute[SPLM_IsServicePart]")).equalsIgnoreCase("TRUE")
					? "Y"
					: "N";
			part.EXPORT_PART = ((String) partMap.get("attribute[SPLM_OverseaSalesOnly]")).equalsIgnoreCase("TRUE") ? "Y"
					: "N";
			part.CREATE_DATE = convertDateFormat((String) partMap.get(DomainConstants.SELECT_ORIGINATED), "yyyyMMdd");
			part.CHANGE_DATE = convertDateFormat((String) partMap.get(DomainConstants.SELECT_MODIFIED), "yyyyMMdd");
			part.GUID = partId;

			partArray.add(part);
		}
		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				outputDataFile("8_DMS_ServicePart.txt", translateJsonIntoString(partArray));
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.servicesPart(translateJsonIntoString(partArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				outputDataFile("8_ERP_ServicePart.txt", translateJsonIntoString(partArray));
				YMM20_PortType erp = getYMM20ERP();
				String result = erp.ymmSplmMatToSap(translateJsonIntoString(partArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	private class Part extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public String PART_SH_NO;
		public String K_D;
		public String GROUP_CODE;
		public String ALT_PART_NO;
		public String REUSE_PART_NO;
		public String PLANT;
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
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_MODIFIED);

		MapList mcMapList = DomainObject.findObjects(context, "SPLM_ModelCode", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"to[SPLM_SBOM]==TRUE", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		ArrayList<McGroupCodeToERP> mcGroupCodeToERPArray = new ArrayList<McGroupCodeToERP>();

		for (Object obj : mcMapList) {
			Map mcMap = (Map) obj;
			String mcId = (String) mcMap.get(DomainConstants.SELECT_ID);
			String mcName = (String) mcMap.get(DomainConstants.SELECT_NAME);
			String mcModifiedDate = (String) mcMap.get(DomainConstants.SELECT_MODIFIED);
			domObj.setId(mcId);
			StringList groupDrawingAttGroupCodeList = domObj.getInfoList(context,
					"to[SPLM_SBOM].from[SPLM_GLNO].from[SPLM_RelatedPartsCatalogue].to[SPLM_PartsCatalogue].from[SPLM_RelatedGroupDrawing].to[SPLM_GroupDrawing].attribute[SPLM_GroupCode]");

			// json for ERP system
			for (String groupDrawingAttGroupCode : groupDrawingAttGroupCodeList) {
				if (!groupDrawingAttGroupCode.isEmpty()) {
					McGroupCodeToERP mcGroupCodeToERPObj = new McGroupCodeToERP();
					mcGroupCodeToERPObj.MODEL_CODE = mcName;
					mcGroupCodeToERPObj.GROUP_NO = groupDrawingAttGroupCode;
					mcGroupCodeToERPObj.CHANGE_DATE = convertDateFormat(mcModifiedDate, "yyyyMMdd");
					mcGroupCodeToERPObj.GUID = mcId;
					mcGroupCodeToERPArray.add(mcGroupCodeToERPObj);
				}
			}
		}
		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				outputDataFile("22_ERP_ModeCode-GroupCode.txt", translateJsonIntoString(mcGroupCodeToERPArray));
				YSD041_NEW_PortType erp = getYSD041ERP();
				String result = erp.YPutSplmMcg(translateJsonIntoString(mcGroupCodeToERPArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	private class McGroupCodeToERP extends CMC_ObjBaseAttribute {
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
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		busSelects.add("attribute[SPLM_Brand]");

		MapList msMapList = DomainObject.findObjects(context, "SPLM_ModelSeries", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"from[SPLM_SBOM]==TRUE", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		ArrayList<MsPartCategoryToERP> msPartCategoryToERPArray = new ArrayList<MsPartCategoryToERP>();

		for (Object obj : msMapList) {
			Map msMap = (Map) obj;
			String msId = (String) msMap.get(DomainConstants.SELECT_ID);
			String msName = (String) msMap.get(DomainConstants.SELECT_NAME);
			String msModifiedDate = (String) msMap.get(DomainConstants.SELECT_MODIFIED);
			String msBrand = (String) msMap.get("attribute[SPLM_Brand]");
			domObj.setId(msId);

			StringList partsCatalogueNameList = domObj.getInfoList(context,
					"from[SPLM_SBOM].to[SPLM_GLNO].from[SPLM_RelatedPartsCatalogue].to[SPLM_PartsCatalogue].name");

			// json for ERP system
			for (String partsCatalogueName : partsCatalogueNameList) {
				MsPartCategoryToERP msPartCategoryToERPObj = new MsPartCategoryToERP();
				msPartCategoryToERPObj.MODEL_SERIES = msName;
				msPartCategoryToERPObj.PART_CATEGORY_NO = partsCatalogueName;
				msPartCategoryToERPObj.MITSUBISHI_FLAG = msBrand.equalsIgnoreCase("mmc") ? "Y" : "N";
				msPartCategoryToERPObj.CHANGE_DATE = convertDateFormat(msModifiedDate, "yyyyMMdd");
				msPartCategoryToERPObj.GUID = msId;
				msPartCategoryToERPArray.add(msPartCategoryToERPObj);
			}
		}
		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				outputDataFile("20_ERP_ModelSeries-PartCategory.txt",
						translateJsonIntoString(msPartCategoryToERPArray));
				YSD041_NEW_PortType erp = getYSD041ERP();
				String result = erp.YPutSplmMpc(translateJsonIntoString(msPartCategoryToERPArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	private class MsPartCategoryToERP extends CMC_ObjBaseAttribute {
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
		busSelects.add(DomainConstants.SELECT_MODIFIED);

		MapList partsCatelogueMapList = DomainObject.findObjects(context, "SPLM_PartsCatalogue", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"to[SPLM_RelatedPartsCatalogue]==TRUE", // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		DomainObject domObj = new DomainObject();
		ArrayList<PartsCatelogueMcToERP> partsCatelogueMcToERPArray = new ArrayList<PartsCatelogueMcToERP>();

		for (Object obj : partsCatelogueMapList) {
			Map partsCatelogueMap = (Map) obj;
			String partsCatelogueId = (String) partsCatelogueMap.get(DomainConstants.SELECT_ID);
			String partsCatelogueName = (String) partsCatelogueMap.get(DomainConstants.SELECT_NAME);
			String partsCatelogueModifiedDate = (String) partsCatelogueMap.get(DomainConstants.SELECT_MODIFIED);
			domObj.setId(partsCatelogueId);

			StringList mcNameList = domObj.getInfoList(context,
					"to[SPLM_RelatedPartsCatalogue].from[SPLM_GLNO].from[SPLM_SBOM].to[SPLM_ModelCode].name");

			for (String mcName : mcNameList) {
				PartsCatelogueMcToERP partsCatelogueMcToERPObj = new PartsCatelogueMcToERP();
				partsCatelogueMcToERPObj.PART_CATEGORY_NO = partsCatelogueName;
				partsCatelogueMcToERPObj.MODEL_CODE = mcName;
				partsCatelogueMcToERPObj.CTG_MODEL = "";
				partsCatelogueMcToERPObj.CHANGE_DATE = convertDateFormat(partsCatelogueModifiedDate, "yyyyMMdd");
				partsCatelogueMcToERPObj.GUID = partsCatelogueId;
				partsCatelogueMcToERPArray.add(partsCatelogueMcToERPObj);
			}
		}
		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				outputDataFile("21_ERP_PartCategory-ModelCode.txt",
						translateJsonIntoString(partsCatelogueMcToERPArray));
				YSD041_NEW_PortType erp = getYSD041ERP();
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
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	private class PartsCatelogueMcToERP extends CMC_ObjBaseAttribute {
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
		String splitStr = ",";
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		busSelects.add("attribute[SPLM_CMC_Code]");
		busSelects.add("attribute[SPLM_ModelSeries]");
		String where = "";
		if (CALL_WSDL.equalsIgnoreCase("DMS")) {
			where = "attribute[SPLM_CMC_Code]!=''";
		}
		if (CALL_WSDL.equalsIgnoreCase("ERP")) {
			where = "attribute[SPLM_ModelSeries]!=''";
		}

		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				where, // where
				null, false, busSelects, (short) SEARCH_AMOUT);

		ArrayList<PartModelToDMS> partModelToDMSArray = new ArrayList<PartModelToDMS>();
		ArrayList<PartModelToERP> partModelToERPArray = new ArrayList<PartModelToERP>();

		for (Object obj : partMapList) {
			Map partMap = (Map) obj;
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String modifiedDate = (String) partMap.get(DomainConstants.SELECT_MODIFIED);
			String partAttMc = (String) partMap.get("attribute[SPLM_CMC_Code]");
			String partAttMs = (String) partMap.get("attribute[SPLM_ModelSeries]");

			ArrayList<String> partAttMcArray = new ArrayList<String>();
			ArrayList<String> partAttMsArray = new ArrayList<String>();

			// SPLM_CMC_Code data processing
			for (String mcStr : partAttMc.split(splitStr)) {
				if (!mcStr.isEmpty()) {
					continue;
				}
//				MapList mcMapList = DomainObject.findObjects(context, "SPLM_ModelCode", // type
//						mcStr, // name
//						"*", // revision
//						"*", // owner
//						"eService Production", // vault
//						where, // where
//						null, false, new StringList("attribute[SPLM_CMC_Code]"), (short) 0);
//				for (Object mcObj : mcMapList) {
//					partAttMcArray.add((String) ((Map) mcObj).get("attribute[SPLM_CMC_Code]"));
//				}
				partAttMsArray.add(mcStr);
			}
			for (String msStr : partAttMs.split(splitStr)) {
				if (!msStr.isEmpty()) {
					continue;
				}
				partAttMsArray.add(msStr);
			}
			// json for DMS system
			PartModelToDMS partModelToDMSObj = new PartModelToDMS();
			partModelToDMSObj.PART_NO = partName;
			partModelToDMSObj.MODEL_CODE = (ArrayList<String>) partAttMcArray.clone();
			partModelToDMSObj.CHANGE_DATE = convertDateFormat(modifiedDate, "yyyyMMdd");
			partModelToDMSObj.GUID = partId;
			partModelToDMSArray.add(partModelToDMSObj);

			// json for ERP system
			for (String attMs : partAttMsArray) {
				PartModelToERP partModelToERPObj = new PartModelToERP();
				partModelToERPObj.PART_NO = partName;
				partModelToERPObj.MODEL_SERISE = attMs;
				partModelToERPObj.CHANGE_DATE = convertDateFormat(modifiedDate, "yyyyMMdd");
				partModelToERPObj.GUID = partId;
				partModelToERPArray.add(partModelToERPObj);
			}
		}
		try {
			if (CALL_WSDL.equalsIgnoreCase("DMS")) {
				outputDataFile("13_DMS_PartModel.txt", translateJsonIntoString(partModelToDMSArray));
				SPlmWSSoap dms = getSPlmWSSoapDMS();
				String result = dms.partModel(translateJsonIntoString(partModelToDMSArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				outputDataFile("13_ERP_PartModel.txt", translateJsonIntoString(partModelToERPArray));
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(partModelToERPArray));
//			System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			outputDataFile("16_ERP_PNC.txt", translateJsonIntoString(pncForBothArray));
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	private class PartModelToDMS extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public ArrayList<String> MODEL_CODE;
	}

	private class PartModelToERP extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public String MODEL_CODE;
		public String MODEL_SERISE;
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
			SPlmWSSoap locator = new SPlmWSLocator().getSPlmWSSoap();
			dealerStr = locator.dealer();
			String dealerType = "SPLM_Dealer";
			String dealerVault = "eService Production";
			String dealerPolicy = "SPLM_Dealer";
			ArrayList<Dealer> dealerArray = gson.fromJson(dealerStr, new TypeToken<ArrayList<Dealer>>() {
			}.getType());

			DomainObject createDealerDomObj = new DomainObject();
			DomainObject modDomObj = new DomainObject();
			Map<String, ArrayList<String>> masterDealerMap = new HashMap<String, ArrayList<String>>();
			Map<String, String> masterIdDealerMap = new HashMap<String, String>();

			for (Dealer dealerObj : dealerArray) {
				String dealerName = dealerObj.COMPANY_ID;
				String dealerMasterName = dealerObj.MASTER_DEALER.isEmpty() ? dealerName + "Id"
						: dealerObj.MASTER_DEALER;

				MapList dealerMapList = DomainObject.findObjects(context, dealerType, // type
						dealerName, // name
						"-", // revision
						"*", // owner
						dealerVault, // vault
						null, // where
						null, false, new StringList(DomainConstants.SELECT_ID), (short) 0);

				if (dealerMapList.isEmpty()) {
					createDealerDomObj.createObject(context, dealerType, dealerName, "-", dealerPolicy, dealerVault);
					System.out.println("Create DealerName : " + dealerName);
				} else {
					String dealerId = (String) ((Map) (dealerMapList.get(0))).get(DomainConstants.SELECT_ID);
					createDealerDomObj.setId(dealerId);
					System.out.println("Exist  DealerName : " + dealerName);
				}
				createDealerDomObj.setDescription(context, dealerObj.COMPANY_DESC);
				createDealerDomObj.setAttributeValue(context, "SPLM_Address", dealerObj.ADDRESS);
				createDealerDomObj.setAttributeValue(context, "SPLM_ContactNumber", dealerObj.TEL_NO);
				filter(masterDealerMap, dealerMasterName, createDealerDomObj.getId(context));
			}

			for (String masterDealerName : masterDealerMap.keySet()) {
				if (masterDealerName.endsWith("Id")) {
					continue;
				}
				String masterDealerId = masterDealerMap.get(masterDealerName + "Id").get(0);
				DomainObject masterDealerDomObj = new DomainObject(masterDealerId);
				for (String subDealerId : masterDealerMap.get(masterDealerName)) {
					DomainRelationship.connect(context, masterDealerDomObj, "SPLM_SubDealer",
							new DomainObject(subDealerId));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("DMS SPlmWSLocator can't log! check your VPN is correct or not.");
		}
	}

	private class Dealer extends CMC_ObjBaseAttribute {
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
	 * 9. MRP BOM
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public void searchMRP_BOM(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_NAME);

		StringList busSelectsSoPart = new StringList();
		busSelectsSoPart.add(DomainConstants.SELECT_ID);
		busSelectsSoPart.add(DomainConstants.SELECT_NAME);
		busSelectsSoPart.add("attribute[SPLM_Location]");
		StringList relSelectsSoPart = new StringList();

		StringList busSelectsSubPart = new StringList();
		busSelectsSubPart.add(DomainConstants.SELECT_NAME);
		StringList relSelectsSubPart = new StringList();
		relSelectsSubPart.add("attribute[SPLM_EnableDate]"); // Start Date
		relSelectsSubPart.add("attribute[SPLM_DisableDate]"); // End Date
		relSelectsSubPart.add("attribute[SPLM_Materiel_KD_Type]");
		relSelectsSubPart.add("attribute[Quantity]");

		MapList soMapList = DomainObject.findObjects(context, "SPLM_SO", // type
				"*", // name
				"-", // revision
				"*", // owner
				"eService Production", // vault
				"from[SPLM_AffectedItem].to[SPLM_Part].attribute[SPLM_HaveSubPart]==TRUE", // where
				null, false, busSelects, (short) 0);

		DomainObject domObj = new DomainObject();
		DomainObject parentPartDomObj = new DomainObject();
		ArrayList<MRP_BOMToERP> mrpBOMToERPArray = new ArrayList<MRP_BOMToERP>();

		// check SPLM_SO->SPLM_Part rel[SPLM_AffectedItem].att[SPLM_HaveSubPart]==TRUE
		for (Object obj : soMapList) {
			Map soMap = (Map) obj;
			String soName = (String) soMap.get(DomainConstants.SELECT_NAME);
			String soId = (String) soMap.get(DomainConstants.SELECT_ID);
			domObj.setId(soId);

			MapList soPartMapList = domObj.getRelatedObjects(context, "SPLM_AffectedItem", // relationshipPattern,
					"SPLM_Part", // typePattern,
					busSelectsSoPart, // StringList objectSelects,
					relSelectsSoPart, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"attribute[SPLM_HaveSubPart]==TRUE", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			// parent Part
			for (Object soPartObj : soPartMapList) {
				Map soPartMap = (Map) soPartObj;
				String parentPartName = (String) soPartMap.get(DomainConstants.SELECT_NAME);
				String parentPartId = (String) soPartMap.get(DomainConstants.SELECT_ID);
				String parentPartAttLocation = (String) soPartMap.get("attribute[SPLM_Location]");
				parentPartDomObj.setId(parentPartId);
				StringList parentPartVendorList = parentPartDomObj.getInfoList(context,
						"from[SPLM_RelatedVendor].to[SPLM_Vendor].name");

				// sub Part
				MapList subPartMapList = parentPartDomObj.getRelatedObjects(context, "SPLM_SBOM", // relationshipPattern,
						"SPLM_Part", // typePattern,
						busSelectsSubPart, // StringList objectSelects,
						relSelectsSubPart, // StringList relationshipSelects,
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

						MRP_BOMToERP mrpBOMToERP = new MRP_BOMToERP();
						mrpBOMToERP.SO_NO = soName;
						mrpBOMToERP.VENDOR_NO = parentPartVendorList.isEmpty() ? "" : parentPartVendorList.get(0);
						mrpBOMToERP.PARENT_PART_NO = parentPartName;
						mrpBOMToERP.PARENT_PART_PLANT = parentPartAttLocation;
						mrpBOMToERP.CHILD_PART_NO = (String) subPartMap.get(DomainObject.SELECT_NAME);
						mrpBOMToERP.QUANTITY = (String) subPartMap.get("attribute[Quantity]");
						mrpBOMToERP.CHILD_PART_START_DATE = subPartAttStartDate.isEmpty() ? ""
								: convertDateFormat(subPartAttStartDate, "yyyyMMdd");
						mrpBOMToERP.CHILD_PART_END_DATE = subPartAttEndDate.isEmpty() ? ""
								: convertDateFormat(subPartAttEndDate, "yyyyMMdd");

						mrpBOMToERPArray.add(mrpBOMToERP);
					} catch (Exception e) {
						outputDataFile("Error.txt", e.getMessage() + "\nParent Part : " + parentPartName
								+ "\nChild Part : " + (String) subPartMap.get(DomainObject.SELECT_NAME));
					}
				}
			}
		}
		// connect CMC for test
		try {
			if (CALL_WSDL.equalsIgnoreCase("ERP")) {
				outputDataFile("9.ERP_MRPBOM.txt", translateJsonIntoString(mrpBOMToERPArray));
				YPP16_PortType erp = getYPP16ERP();
				String result = erp.yppSplmBomToSap(translateJsonIntoString(mrpBOMToERPArray));
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(translateJsonIntoString(pncForBothArray));
//			System.out.println(result);
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}

	private class MRP_BOMToERP extends CMC_ObjBaseAttribute {
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
				YSD041_NEW_PortType erp = getYSD041ERP();
				String result = erp.YPutSplmP01(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println('\n' + result + '\n');
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
				YSD041_NEW_PortType erp = getYSD041ERP();
				String result = erp.YPutSplmG01(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println(result);
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
				YSD041_NEW_PortType erp = getYSD041ERP();
				String result = erp.YPutSplmM02(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println(result);
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
				YMM20_PortType erp = getYMM20ERP();
				String result = erp.ymmReplaceMatToSap(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println(result);
			}
		} catch (Exception e) {
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

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println(result);
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
				YSD041_NEW_PortType erp = getYSD041ERP();
				String result = erp.YPutSplmM03(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println(result);
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

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println(result);
			}
		} catch (Exception e) {
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
				YMM20_PortType erp = getYMM20ERP();
				String result = erp.ymmSplmMatToSap(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println(result);
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
				YSD041_NEW_PortType erp = getYSD041ERP();
				String result = erp.YPutSplmMcg(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println(result);
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
				YSD041_NEW_PortType erp = getYSD041ERP();
				String result = erp.YPutSplmMpc(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println(result);
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
				YSD041_NEW_PortType erp = getYSD041ERP();
				String result = erp.YPutSplmMpc(inputJson);
				System.out.println('\n' + result + '\n');
			}

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println(result);
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

			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println(result);
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
				YPP16_PortType erp = getYPP16ERP();
				String result = erp.yppSplmBomToSap(inputJson);
				System.out.println('\n' + result + '\n');
			}
			if (CALL_WSDL.equalsIgnoreCase("GLM")) {
//			SPlmWSSoap dms = getSPlmWSSoapDMS();
//			String result = dms.PNC(inputJson);
//			System.out.println(result);
			}
		} catch (Exception e) {
			outputDataFile("Error.txt", e.getMessage());
		}

	}
}