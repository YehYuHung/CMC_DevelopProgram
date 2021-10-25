import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.tempuri.SPlmWSLocator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

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

	private String formatNowDateTime(String formatType) {
		return formatDateTime(new Date().toString(), formatType);
	}

	private String formatDateTime(String goalDate, String formatType) {
		SimpleDateFormat sdf = new SimpleDateFormat(formatType);
		Date date = new Date(goalDate);
		String alterDate = sdf.format(date);
		return alterDate;
	}

	private void outputDataFile(String fileName, String Data) throws IOException {
		File log = new File("C:\\temp\\" + fileName);
		try {
			if (log.exists() != true) {
				System.out.println("We had to make " + fileName + " file.");
				log.createNewFile();
			}
			PrintWriter out = new PrintWriter(new FileWriter(log, true));
			out.append(Data + "\n\n");
			out.append("******* " + formatNowDateTime("yyyy.MM.dd HH:mm:ss") + "******* " + "\n\n");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("COULD NOT LOG!!");
		}
	}

	private <E> String jsonToString(ArrayList<E> arrayList) {
		return new Gson().toJson(arrayList);
	}

	abstract class CMC_ObjectBase implements Cloneable {
		protected String CHANGE_DATE;
		protected String CREATE_DATE;
		protected String GUID;

		protected Object clone() throws CloneNotSupportedException {
			Object cloneObj = super.clone();
			return cloneObj;
		}
	}

	public void tryAll(Context context, String[] args) throws Exception {
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
		searchVenderAlter(context, args);
	}

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
		busSelects.add("attribute[SPLM_PNC_Type]");

		MapList pncList = DomainObject.findObjects(context, "SPLM_PNC", // type
				"*", // name
				"-", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, (short) 6);

		ArrayList<PNCForBoth> pncArray = new ArrayList<PNCForBoth>();

		// json for DMS/ERP system
		for (Object object : pncList) {
			Map map = (Map) object;
			PNCForBoth pncForBothObj = new PNCForBoth();
			pncForBothObj.PNC_NO = (String) map.get(DomainConstants.SELECT_NAME);
			pncForBothObj.PNC_NAME_TC = (String) map.get("attribute[SPLM_Name_TC]");
			pncForBothObj.PNC_NAME_EN = (String) map.get("attribute[SPLM_Name_EN]");
			pncForBothObj.PNC_TYPE = (String) map.get("attribute[SPLM_PNC_Type]");
			pncForBothObj.CHANGE_DATE = formatDateTime((String) map.get(DomainConstants.SELECT_MODIFIED), "yyyyMMdd");
			pncForBothObj.GUID = (String) map.get(DomainConstants.SELECT_ID);
			pncArray.add(pncForBothObj);
		}
		outputDataFile("16_DMS.txt", jsonToString(pncArray));
		outputDataFile("16_ERP.txt", jsonToString(pncArray));
	}

	public class PNCForBoth extends CMC_ObjectBase {
		public String PNC_NO;
		public String PNC_NAME_EN;
		public String PNC_NAME_TC;
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
		busSelects.add("attribute[SPLM_Name_TC]");
		busSelects.add("attribute[SPLM_Name_EN]");

		MapList groupCodeList = DomainObject.findObjects(context, "SPLM_GroupCode", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, Short.parseShort("0"));

		ArrayList<GroupCodeForBoth> groupCodeArray = new ArrayList<GroupCodeForBoth>();

		// json for DMS/ERP system
		for (Object object : groupCodeList) {
			Map map = (Map) object;
			GroupCodeForBoth groupCodeForBothObj = new GroupCodeForBoth();
			groupCodeForBothObj.GROUP_CODE = (String) map.get(DomainConstants.SELECT_NAME);
			groupCodeForBothObj.GROUP_NAME_TC = (String) map.get("attribute[SPLM_Name_TC]");
			groupCodeForBothObj.GROUP_NAME_EN = (String) map.get("attribute[SPLM_Name_EN]");
			groupCodeForBothObj.CHANGE_DATE = formatDateTime((String) map.get(DomainConstants.SELECT_MODIFIED),
					"yyyyMMdd");
			groupCodeForBothObj.GUID = (String) map.get(DomainConstants.SELECT_ID);
			groupCodeArray.add(groupCodeForBothObj);
		}
		outputDataFile("17_DMS.txt", jsonToString(groupCodeArray));
		outputDataFile("17_ERP.txt", jsonToString(groupCodeArray));
	}

	public class GroupCodeForBoth extends CMC_ObjectBase {
		public String GROUP_CODE;
		public String GROUP_NAME_TC;
		public String GROUP_NAME_EN;
	}

	/**
	 * 19.PartVendor
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchVenderAlter(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		busSelects.add("attribute[SPLM_SO_Note]");

		MapList resultList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, Short.parseShort("0"));

		DomainObject domObj = new DomainObject();
		ArrayList<VenderAlterToDMS> venderAlterToDMSArray = new ArrayList<VenderAlterToDMS>();
		ArrayList<VenderAlterToERP> venderAlterToERPArray = new ArrayList<VenderAlterToERP>();

		for (Object object : resultList) {
			Map map = (Map) object;
			String partId = (String) map.get(DomainConstants.SELECT_ID);
			String partName = (String) map.get(DomainConstants.SELECT_NAME);
			String partModifiedDate = (String) map.get(DomainConstants.SELECT_MODIFIED);
			String partSoNo = (String) map.get("attribute[SPLM_SO_Note]");
			domObj.setId(partId);
			StringList partVendor = domObj.getInfoList(context, "attribute[SPLM_Vendor]");

			// json for DMS system
			VenderAlterToDMS venderAlterToDMSObj = new VenderAlterToDMS();
			venderAlterToDMSObj.PART_NO = partName;
			venderAlterToDMSObj.VENDOR_NO = partVendor;
			venderAlterToDMSObj.SO_NO = partSoNo;
			venderAlterToDMSObj.CHANGE_DATE = formatDateTime(partModifiedDate, "yyyyMMdd");
			venderAlterToDMSObj.GUID = partId;
			venderAlterToDMSArray.add(venderAlterToDMSObj);

			// json for ERP system
			for (String vendor : partVendor) {
				VenderAlterToERP venderAlterToERPObj = new VenderAlterToERP();
				venderAlterToERPObj.PART_NO = partName;
				venderAlterToERPObj.SO_NO = partSoNo;
				venderAlterToERPObj.CHANGE_DATE = formatDateTime(partModifiedDate, "yyyyMMdd");
				venderAlterToERPObj.VENDOR_NO = vendor;
				venderAlterToERPObj.GUID = partId;
				venderAlterToERPArray.add(venderAlterToERPObj);
			}
		}
		outputDataFile("19_DMS.txt", jsonToString(venderAlterToDMSArray));
		outputDataFile("19_ERP.txt", jsonToString(venderAlterToERPArray));
	}

	public class VenderAlterToDMS extends CMC_ObjectBase {
		public String PART_NO;
		public StringList VENDOR_NO;
		public String SO_NO;
	}

	public class VenderAlterToERP extends CMC_ObjectBase {
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

		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);
		busSelect.add(DomainConstants.SELECT_MODIFIED);
		StringList relSelect = new StringList();
		relSelect.add("attribute[SPLM_OptionalType]");

		MapList partList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"from[SPLM_RelatedOptionalPart]==TRUE", // where
				null, false, busSelect, (short) 0);

		DomainObject domObj = new DomainObject();
		ArrayList<OptionPartToERP> OptionPartToERPArray = new ArrayList<OptionPartToERP>();
		ArrayList<AlterPartToERP> AlterPartToERPArray = new ArrayList<AlterPartToERP>();
		ArrayList<OptionPartToDMS> OptionPartToDMSArray = new ArrayList<OptionPartToDMS>();
		ArrayList<AlterPartToDMS> AlterPartToDMSArray = new ArrayList<AlterPartToDMS>();

		for (Object partObj : partList) {
			Map partMap = (Map) partObj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partModifiedDate = (String) partMap.get(DomainConstants.SELECT_MODIFIED);

			domObj.setId(partId);
			MapList partOptList = domObj.getRelatedObjects(context, "SPLM_RelatedOptionalPart", // relationshipPattern,
					"SPLM_Part", // typePattern,
					busSelect, // StringList objectSelects,
					relSelect, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			Map<String, ArrayList<String>> partOptAttTypeMap = new HashMap<String, ArrayList<String>>();
			for (Object partOptObj : partOptList) {
				Map partOptMap = (Map) partOptObj;
				String partOptName = (String) partOptMap.get(DomainConstants.SELECT_NAME);
				String partOptAttType = (String) partOptMap.get("attribute[SPLM_OptionalType]");
				partOptAttType = partOptAttType.equalsIgnoreCase("Alternate Part") ? partOptAttType : "Optional Part";

				if (!partOptAttTypeMap.containsKey(partOptAttType)) {
					ArrayList<String> partOptAttTypeArray = new ArrayList<String>();
					partOptAttTypeMap.put(partOptAttType, partOptAttTypeArray);
					partOptAttTypeArray.add(partOptName);
					continue;
				}
				partOptAttTypeMap.get(partOptAttType).add(partOptName);
			}

			// json for Optional_type
			if (partOptAttTypeMap.containsKey("Optional Part")) {
				ArrayList<String> partOptOptionPartArray = partOptAttTypeMap.get("Optional Part");

				// DMS system
				OptionPartToDMS OptionPartToDMSObj = new OptionPartToDMS();
				OptionPartToDMSObj.PART_NO = partName;
				OptionPartToDMSObj.REUSE_PART_NO = partOptAttTypeMap.get("Optional Part");
				OptionPartToDMSObj.CHANGE_DATE = formatDateTime(partModifiedDate, "yyyyMMdd");
				OptionPartToDMSObj.GUID = partId;
				OptionPartToDMSArray.add(OptionPartToDMSObj);

				// ERP system
				for (String partOptOptionalPartName : partOptOptionPartArray) {
					OptionPartToERP OptionPartToERPObj = new OptionPartToERP();
					OptionPartToERPObj.PART_NO = partName;
					OptionPartToERPObj.REUSE_PART_NO = partOptOptionalPartName;
					OptionPartToERPObj.CHANGE_DATE = formatDateTime(partModifiedDate, "yyyyMMdd");
					OptionPartToERPObj.GUID = partId;
					OptionPartToERPArray.add(OptionPartToERPObj);
				}
			}

			// json for Alternate_type
			if (partOptAttTypeMap.containsKey("Alternate Part")) {
				ArrayList<String> partOptAlterPartArray = partOptAttTypeMap.get("Alternate Part");

				// DMS system
				AlterPartToDMS AlterPartToDMSObj = new AlterPartToDMS();
				AlterPartToDMSObj.PART_NO = partName;
				AlterPartToDMSObj.NEW_PART_NO = partOptAlterPartArray;
				AlterPartToDMSObj.CHANGE_DATE = formatDateTime(partModifiedDate, "yyyyMMdd");
				AlterPartToDMSObj.GUID = partId;
				AlterPartToDMSArray.add(AlterPartToDMSObj);

				// ERP system
				for (String partOptAlterPartName : partOptAlterPartArray) {
					AlterPartToERP AlterPartToERPObj = new AlterPartToERP();
					AlterPartToERPObj.PART_NO = partName;
					AlterPartToERPObj.NEW_PART_NO = partOptAlterPartName;
					AlterPartToERPObj.CHANGE_DATE = formatDateTime(partModifiedDate, "yyyyMMdd");
					AlterPartToERPObj.GUID = partId;
					AlterPartToERPArray.add(AlterPartToERPObj);
				}
			}
		}
		outputDataFile("14_Alter_DMS.txt", jsonToString(AlterPartToDMSArray));
		outputDataFile("14_Optional_DMS.txt", jsonToString(OptionPartToDMSArray));
		outputDataFile("15_Alter_ERP.txt", jsonToString(AlterPartToERPArray));
		outputDataFile("15_Optional_ERP.txt", jsonToString(OptionPartToERPArray));
	}

	public class OptionPartToERP extends CMC_ObjectBase {
		public String PART_NO;
		public String REUSE_PART_NO;
	}

	public class AlterPartToERP extends CMC_ObjectBase {
		public String PART_NO;
		public String NEW_PART_NO;
	}

	public class OptionPartToDMS extends CMC_ObjectBase {
		public String PART_NO;
		public ArrayList<String> REUSE_PART_NO;
	}

	public class AlterPartToDMS extends CMC_ObjectBase {
		public String PART_NO;
		public ArrayList<String> NEW_PART_NO;
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
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);

		MapList partList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"to[SPLM_RelatedPart]==TRUE", // where
				null, false, busSelect, (short) 0);

		DomainObject domObj = new DomainObject();
		Map<String, ArrayList<String>> mapping = new HashMap<String, ArrayList<String>>();
		ArrayList<partPncToDMS> partPncToDMSArray = new ArrayList<partPncToDMS>();
		ArrayList<partPncToERP> partPncToERPArray = new ArrayList<partPncToERP>();

		for (Object part : partList) {
			Map partMap = (Map) part;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);

			domObj.setId(partId);
			StringList pncNameList = domObj.getInfoList(context, "to[SPLM_RelatedPart].from[SPLM_PNC].name");

			for (String pncName : pncNameList) {
				if (!mapping.containsKey(partName)) {
					ArrayList<String> pncArrayList = new ArrayList<String>();
					mapping.put(partName, pncArrayList);
					pncArrayList.add(pncName);
				} else if (!mapping.get(partName).contains(pncName)) {
					mapping.get(partName).add(pncName);
				}
			}

			ArrayList<String> pncArrayList = mapping.get(partName);

			// json for DMS system
			partPncToDMS partPncToDMSObj = new partPncToDMS();
			partPncToDMSObj.PART_NO = partName;
			partPncToDMSObj.PNC_NO = pncArrayList;
			partPncToDMSObj.CHANGE_DATE = formatDateTime(null, "yyyyMMdd");
			partPncToDMSObj.GUID = partId;
			partPncToDMSArray.add(partPncToDMSObj);

			// json for ERP system
			for (String pncName : pncArrayList) {
				partPncToERP partPncToERPObj = new partPncToERP();
				partPncToERPObj.PART_NO = partName;
				partPncToERPObj.CHANGE_DATE = formatDateTime(null, "yyyyMMdd");
				partPncToERPObj.GUID = partId;
				partPncToERPObj.PNC_NO = pncName;
				partPncToERPArray.add(partPncToERPObj);
			}
		}

		outputDataFile("11_DMS.txt", jsonToString(partPncToDMSArray));
		outputDataFile("11_ERP.txt", jsonToString(partPncToERPArray));
	}

	public class partPncToDMS extends CMC_ObjectBase {
		public String PART_NO;
		public ArrayList<String> PNC_NO;
	}

	public class partPncToERP extends CMC_ObjectBase {
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
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);
		busSelect.add("attribute[SPLM_GroupCode]");
		StringList relSelect = new StringList();

		MapList partList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"to[SPLM_RelatedPart]==TRUE", // where
				null, false, busSelect, (short) 0);

		DomainObject domObj = new DomainObject();
		Map<String, ArrayList<String>> mapping = new HashMap<String, ArrayList<String>>();
		ArrayList<PartGroupCodeToDMS> partGroupCodeArray = new ArrayList<PartGroupCodeToDMS>();

		for (Object part : partList) {
			Map partMap = (Map) part;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			domObj.setId(partId);

			MapList groupDrawingList = domObj.getRelatedObjects(context, "SPLM_RelatedPart", // relationshipPattern,
					"SPLM_GroupDrawing", // typePattern,
					busSelect, // StringList objectSelects,
					relSelect, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			for (Object groupDrawingObj : groupDrawingList) {
				String attGroupCode = (String) ((Map) groupDrawingObj).get("attribute[SPLM_GroupCode]");

				if (!mapping.containsKey(partName)) {
					ArrayList<String> groupCodeArrayList = new ArrayList<String>();
					mapping.put(partName, groupCodeArrayList);
					groupCodeArrayList.add(attGroupCode);
				} else if (!mapping.get(partName).contains(attGroupCode)) {
					mapping.get(partName).add(attGroupCode);
				}
			}

			// json for DMS System
			PartGroupCodeToDMS partGroupCodeToDMSObj = new PartGroupCodeToDMS();
			partGroupCodeToDMSObj.PART_NO = partName;
			partGroupCodeToDMSObj.GROUP_CODE = mapping.get(partName);
			partGroupCodeToDMSObj.CHANGE_DATE = formatNowDateTime("yyyyMMdd");
			partGroupCodeToDMSObj.GUID = partId;
			partGroupCodeArray.add(partGroupCodeToDMSObj);
		}
		outputDataFile("12_DMS.txt", jsonToString(partGroupCodeArray));
	}

	public class PartGroupCodeToDMS extends CMC_ObjectBase {
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
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add("attribute[SPLM_GroupCode]");

		MapList groupDrawingList = DomainObject.findObjects(context, "SPLM_GroupDrawing", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"from[SPLM_Related_PNC]==TRUE", // where
				null, false, busSelect, (short) 0);

		DomainObject domObj = new DomainObject();
		ArrayList<GroupCodePncToERP> groupCodePncArray = new ArrayList<GroupCodePncToERP>();

		for (Object groupDrawingObj : groupDrawingList) {
			Map groupDrawingMap = (Map) groupDrawingObj;
			String groupDrawingId = (String) groupDrawingMap.get(DomainConstants.SELECT_ID);
			String groupDrawingGroupCode = (String) groupDrawingMap.get("attribute[SPLM_GroupCode]");
			domObj.setId(groupDrawingId);
			StringList pncNameList = domObj.getInfoList(context, "from[SPLM_Related_PNC].to[SPLM_PNC].name");

			MapList groupCodeList = DomainObject.findObjects(context, "SPLM_GroupCode", // type
					groupDrawingGroupCode, // name
					"*", // revision
					"*", // owner
					"eService Production", // vault
					"", // where
					null, false, busSelect, (short) 0);

			// json for ERP system
			for (Object groupCodeObj : groupCodeList) {
				for (String pncName : pncNameList) {
					GroupCodePncToERP groupCodePncObj = new GroupCodePncToERP();
					groupCodePncObj.PNC_NO = pncName;
					groupCodePncObj.GROUP_CODE = groupDrawingGroupCode;
					groupCodePncObj.CHANGE_DATE = formatNowDateTime("yyyyMMdd");
					groupCodePncObj.GUID = (String) ((Map) groupCodeObj).get(DomainConstants.SELECT_ID);
					groupCodePncArray.add(groupCodePncObj);
				}
			}
		}
		outputDataFile("18_ERP.txt", jsonToString(groupCodePncArray));
	}

	public class GroupCodePncToERP extends CMC_ObjectBase {
		public String GROUP_CODE;
		public String PNC_NO;
	}

	/**
	 * PartAll
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchPartAll(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		Map<String, String> paramsMap = JPO.unpackArgs(args);
		Part test = gson.fromJson(paramsMap.get("json"), Part.class);

		StringList relSelectPartOpt = new StringList();
		relSelectPartOpt.add("attribute[SPLM_OptionalType]");
		StringList busSelectPartOpt = new StringList();
		busSelectPartOpt.add(DomainConstants.SELECT_NAME); // PART_NO
		busSelectPartOpt.add(DomainConstants.SELECT_ID); // GUID

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
		busSelect.add("attribute[SPLM_Vendor]"); // VENDOR_NO
		busSelect.add("attribute[SPLM_Name_TC]"); // PART_NAME_C
		busSelect.add("attribute[SPLM_Name_EN]"); // PART_NAME_E
		busSelect.add("attribute[SPLM_HaveSubPart]"); // HAS_SUBASSEMBLY
		busSelect.add("attribute[SPLM_IsServicePart]"); // IS_SERVICE_PART
		busSelect.add("attribute[SPLM_DTAT_Only]"); // BIG_CAR_TYPE
		busSelect.add("attribute[SPLM_ItemSubType]"); // PART_TYPE
		busSelect.add("attribute[SPLM_SO_IssueDate]"); // SO_RELEASE_DATE
		busSelect.add("attribute[SPLM_SO_Note]"); // SO_NO
		busSelect.add("attribute[SPLM_ModelCode]"); // MODEL_CODE
		busSelect.add("attribute[SPLM_ModelSeries]"); // MODEL_SERIES

		StringList busSelectPNC = new StringList();
		busSelectPNC.add(DomainConstants.SELECT_ID);
		busSelectPNC.add("attriubte[SPLM_PNC_Type]");

		MapList partList = DomainObject.findObjects(context, "SPLM_Part", // type
				test.PART_NO.isEmpty() ? "*" : test.PART_NO, // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelect, (short) 0);

		DomainObject domObj = new DomainObject();
		ArrayList<Part> partArray = new ArrayList<Part>();
		MapList partOptList = null;
		MapList pncList = null;
		Map<String, String> mapping = null;

		for (Object partObj : partList) {
			Map partMap = (Map) partObj;
			Part part = new Part();
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);

			domObj.setId(partId);
			StringList pncNameList = domObj.getInfoList(context, "to[SPLM_RelatedPart].from[SPLM_PNC].name");
			StringList groupCodeList = domObj.getInfoList(context,
					"to[SPLM_RelatedPart].from[SPLM_GroupDrawing].attribute[SPLM_GroupCode]");
			String attMaterialGroup = (String) partMap.get("attribute[SPLM_MaterialGroup]");
			String attLocation = (String) partMap.get("attribute[SPLM_Location]");
			String attSOIssueDate = (String) partMap.get("attribute[SPLM_SO_IssueDate]");
			String attpncName = pncNameList.isEmpty() ? "" : pncNameList.get(0);
			String attPNCType = "";
			String attTransGroup = "";

			if (!attpncName.isEmpty()) {
				pncList = DomainObject.findObjects(context, "SPLM_PNC", // type
						pncNameList.get(0), // name
						"*", // revision
						"*", // owner
						"eService Production", // vault
						"", // where
						null, false, busSelectPNC, (short) 5);
				for (Object pncObj : pncList) {
					attPNCType = (String) ((Map) pncObj).get("attriubte[SPLM_PNC_Type]");
				}
			}

			if (attLocation == "1300") {
				if (attPNCType == "Z1" || attPNCType == "Z3" || attPNCType == "Z4" || attPNCType == "Z5") {
					attTransGroup = "DUMY";
				}
			} else if (attLocation == "9001" || attLocation == "9000") {
				attTransGroup = "ZS01";
			}

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
				String attOptionalType = (String) ((Map) partOpt).get("attribute[SPLM_OptionalType]");
				String optPartName = (String) ((Map) partOpt).get(DomainConstants.SELECT_NAME);
				mapping.put(attOptionalType, optPartName);
			}

			part.PART_NO = (String) partMap.get(DomainConstants.SELECT_NAME);
			part.PART_SH_NO = (String) partMap.get("attribute[SPLM_OrderType]");
			part.K_D = (attMaterialGroup == "KD" || attMaterialGroup == "KDY") ? "K" : "D";
			part.GROUP_CODE = groupCodeList.isEmpty() ? "" : groupCodeList.get(0);
			part.ALT_PART_NO = mapping.containsKey("\u66ff\u4ee3") ? mapping.get("\u66ff\u4ee3") : "";
			part.REUSE_PART_NO = mapping.containsKey("\u9078\u7528\u4ef6") ? mapping.get("\u9078\u7528\u4ef6") : "";
			part.PLANT = attLocation;
			part.MATERIAL_TYPE = (String) partMap.get("attribute[SPLM_MaterialType]");
			part.MATERIAL_GROUP = (attMaterialGroup == "KD" || attMaterialGroup == "KDY") ? "K" : "D";
			part.UNIT = (String) partMap.get("attribute[SPLM_Unit]");
			part.PURCHASING_DEPARTMENT_NO = (String) partMap.get("attribute[SPLM_PurchasingGroup]");
			part.MANUFACTURER_CODE = ""; // Uncheck, got logic
			part.VENDOR_NO = (String) partMap.get("attribute[SPLM_Vendor]");
			part.PART_NAME_C = (String) partMap.get("attribute[SPLM_Name_TC]");
			part.PART_NAME_E = (String) partMap.get("attribute[SPLM_Name_EN]");
			part.SALES_ORG = attLocation == "1300" ? "3000"
					: attLocation == "9001" ? "9000" : attLocation == "9000" ? "SDM" : "";
			part.PNC_NO = attpncName;
			part.ITEM_CATEGORY_GROUP = ""; // Uncheck
			part.MODEL_SERIES = (String) partMap.get("attribute[SPLM_ModelSeries]"); // Uncheck
			part.MODEL_CODE = (String) partMap.get("attribute[SPLM_ModelCode]"); // Uncheck
			part.CAR_CAEGORY = ""; // Uncheck
			part.PART_TYPE = (String) partMap.get("attribute[SPLM_ItemSubType]");
			part.COMMISSION_GROUP = attPNCType;
			part.TRANS_GROUP = attTransGroup;
			part.BIG_CAR_TYPE = (String) partMap.get("attribute[SPLM_DTAT_Only]");
			part.SO_NO = (String) partMap.get("attribute[SPLM_SO_Note]");
			part.SO_RELEASE_DATE = attSOIssueDate.isEmpty() ? "" : formatDateTime(attSOIssueDate, "yyMMdd");
			part.HAS_SUBASSEMBLY = ((String) partMap.get("attribute[SPLM_HaveSubPart]")).equalsIgnoreCase("TRUE") ? "Y"
					: "N";
			part.PART_OWNER = (String) partMap.get(DomainConstants.SELECT_OWNER);
			part.IS_SERVICE_PART = ((String) partMap.get("attribute[SPLM_IsServicePart]")).equalsIgnoreCase("TRUE")
					? "Y"
					: "N";
			part.CREATE_DATE = formatDateTime((String) partMap.get(DomainConstants.SELECT_ORIGINATED), "yyyyMMdd");
			part.CHANGE_DATE = formatDateTime((String) partMap.get(DomainConstants.SELECT_MODIFIED), "yyyyMMdd");
			part.GUID = partId;

			partArray.add(part);
		}
		return gson.toJson(partArray);
	}

	public class Part extends CMC_ObjectBase {
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
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);
		busSelect.add(DomainConstants.SELECT_MODIFIED);

		MapList modelCodeList = DomainObject.findObjects(context, "SPLM_ModelCode", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"to[SPLM_SBOM]==TRUE", // where
				null, false, busSelect, (short) 0);

		DomainObject domObj = new DomainObject();
		ArrayList<McGroupCodeToERP> mcGroupCodeArray = new ArrayList<McGroupCodeToERP>();

		for (Object modelCodeObj : modelCodeList) {
			Map modelCodeMap = (Map) modelCodeObj;
			String modelCodeId = (String) modelCodeMap.get(DomainConstants.SELECT_ID);
			String modelCodeName = (String) modelCodeMap.get(DomainConstants.SELECT_NAME);
			String modelCodeModifiedDate = (String) modelCodeMap.get(DomainConstants.SELECT_MODIFIED);
			domObj.setId(modelCodeId);
			StringList mcGroupCodeList = domObj.getInfoList(context,
					"to[SPLM_SBOM].from[SPLM_GLNO].from[SPLM_RelatedPartsCatalogue].to[SPLM_PartsCatalogue].from[SPLM_RelatedGroupDrawing].to[SPLM_GroupDrawing].attribute[SPLM_GroupCode]");

			// json for ERP system
			for (String mcGroupCode : mcGroupCodeList) {
				McGroupCodeToERP mcGroupCodeObj = new McGroupCodeToERP();
				mcGroupCodeObj.MODEL_CODE = modelCodeName;
				mcGroupCodeObj.GROUP_NO = mcGroupCode;
				mcGroupCodeObj.CHANGE_DATE = formatDateTime(modelCodeModifiedDate, "yyyyMMdd");
				mcGroupCodeObj.GUID = modelCodeId;
				mcGroupCodeArray.add(mcGroupCodeObj);
			}
		}
		outputDataFile("22_ERP.txt", jsonToString(mcGroupCodeArray));
	}

	public class McGroupCodeToERP extends CMC_ObjectBase {
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
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);
		busSelect.add(DomainConstants.SELECT_MODIFIED);

		MapList modelSeriesList = DomainObject.findObjects(context, "SPLM_ModelSeries", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"from[SPLM_SBOM]==TRUE", // where
				null, false, busSelect, (short) 0);

		DomainObject domObj = new DomainObject();
		ArrayList<MsPartCategoryToERP> mcGroupCodeArray = new ArrayList<MsPartCategoryToERP>();
		for (Object modelSeriesObj : modelSeriesList) {
			Map modelSeriesMap = (Map) modelSeriesObj;
			String modelSeriesId = (String) modelSeriesMap.get(DomainConstants.SELECT_ID);
			String modelSeriesName = (String) modelSeriesMap.get(DomainConstants.SELECT_NAME);
			String modelSeriesModifiedDate = (String) modelSeriesMap.get(DomainConstants.SELECT_MODIFIED);
			domObj.setId(modelSeriesId);
			StringList partsCatalogueNameList = domObj.getInfoList(context,
					"from[SPLM_SBOM].to[SPLM_GLNO].from[SPLM_RelatedPartsCatalogue].to[SPLM_PartsCatalogue].name");

			// json for ERP system
			for (String partsCatalogueName : partsCatalogueNameList) {
				MsPartCategoryToERP msPartCategoryToERPObj = new MsPartCategoryToERP();
				msPartCategoryToERPObj.MODEL_SERIES = modelSeriesName;
				msPartCategoryToERPObj.PART_CATEGORY_NO = partsCatalogueName;
				msPartCategoryToERPObj.CHANGE_DATE = formatDateTime(modelSeriesModifiedDate, "yyyyMMdd");
				msPartCategoryToERPObj.GUID = modelSeriesId;
				mcGroupCodeArray.add(msPartCategoryToERPObj);
			}
		}
		outputDataFile("20_ERP.txt", jsonToString(mcGroupCodeArray));
	}

	public class MsPartCategoryToERP extends CMC_ObjectBase {
		public String MODEL_SERIES;
		public String PART_CATEGORY_NO;
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
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);
		busSelect.add(DomainConstants.SELECT_MODIFIED);

		MapList partsCatelogueList = DomainObject.findObjects(context, "SPLM_PartsCatalogue", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"to[SPLM_RelatedPartsCatalogue]==TRUE", // where
				null, false, busSelect, (short) 0);

		ArrayList<PartsCatelogueMcToERP> partsCatelogueMcArray = new ArrayList<PartsCatelogueMcToERP>();
		DomainObject domObj = new DomainObject();
		for (Object partsCatelogueObj : partsCatelogueList) {
			Map partsCatelogueMap = (Map) partsCatelogueObj;
			String partsCatelogueId = (String) partsCatelogueMap.get(DomainConstants.SELECT_ID);
			String partsCatelogueName = (String) partsCatelogueMap.get(DomainConstants.SELECT_NAME);
			String partsCatelogueModifiedDate = (String) partsCatelogueMap.get(DomainConstants.SELECT_MODIFIED);
			domObj.setId(partsCatelogueId);

			StringList modelCodeNameList = domObj.getInfoList(context,
					"to[SPLM_RelatedPartsCatalogue].from[SPLM_GLNO].from[SPLM_SBOM].to[SPLM_ModelCode].name");

			for (String modelCodeName : modelCodeNameList) {
				PartsCatelogueMcToERP partsCatelogueMcToERPObj = new PartsCatelogueMcToERP();
				partsCatelogueMcToERPObj.PART_CATEGORY_NO = partsCatelogueName;
				partsCatelogueMcToERPObj.MODEL_CODE = modelCodeName;
				partsCatelogueMcToERPObj.CTG_MODEL = "";
				partsCatelogueMcToERPObj.CHANGE_DATE = formatDateTime(partsCatelogueModifiedDate, "yyyyMMdd");
				partsCatelogueMcToERPObj.GUID = partsCatelogueId;
				partsCatelogueMcArray.add(partsCatelogueMcToERPObj);
			}
		}
		outputDataFile("21_ERP.txt", jsonToString(partsCatelogueMcArray));
	}

	public class PartsCatelogueMcToERP extends CMC_ObjectBase {
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
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);
		busSelect.add(DomainConstants.SELECT_MODIFIED);
		busSelect.add("attribute[SPLM_ModelCode]");
		busSelect.add("attribute[SPLM_ModelSeries]");

		MapList partList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"attribute[SPLM_ModelCode]!='' || attribute[SPLM_ModelSeries]!=''", // where
				null, false, busSelect, (short) 0);

		ArrayList<PartModelToDMS> partModelToDMSArray = new ArrayList<PartModelToDMS>();
		ArrayList<PartModelToERP> partModelToERPArray = new ArrayList<PartModelToERP>();

		for (Object partObj : partList) {
			Map partMap = (Map) partObj;
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String modelSeriesId = (String) partMap.get(DomainConstants.SELECT_ID);
			String modifiedDate = (String) partMap.get(DomainConstants.SELECT_MODIFIED);
			ArrayList<String> attMcArray = new ArrayList<String>();
			ArrayList<String> attMsArray = new ArrayList<String>();
			String attMcAll = (String) partMap.get("attribute[SPLM_ModelCode]");
			String attMsAll = (String) partMap.get("attribute[SPLM_ModelSeries]");

			for (String str : attMcAll.split(splitStr)) {
				if (str.isEmpty()) {
					continue;
				}
				attMcArray.add(str);
			}
			for (String str : attMsAll.split(splitStr)) {
				if (str.isEmpty()) {
					continue;
				}
				attMsArray.add(str);
			}

			// json for DMS system
			PartModelToDMS partModelToDMSObj = new PartModelToDMS();
			partModelToDMSObj.PART_NO = partName;
			partModelToDMSObj.MODEL_CODE = attMcArray;
			partModelToDMSObj.MODEL_SERISE = attMsArray;
			partModelToDMSObj.CHANGE_DATE = formatDateTime(modifiedDate, "yyyyMMdd");
			partModelToDMSObj.GUID = modelSeriesId;
			partModelToDMSArray.add(partModelToDMSObj);

			// json for ERP system
			PartModelToERP partModelToERPObj = new PartModelToERP();
			partModelToERPObj.PART_NO = partName;
			partModelToERPObj.MODEL_SERISE = "";
			partModelToERPObj.MODEL_CODE = "";
			partModelToERPObj.CHANGE_DATE = formatDateTime(modifiedDate, "yyyyMMdd");
			partModelToERPObj.GUID = modelSeriesId;

			if (attMcArray.isEmpty()) {
				for (String attMs : attMsArray) {
					PartModelToERP partModelObjNoMc = (PartModelToERP) partModelToERPObj.clone();
					partModelObjNoMc.MODEL_SERISE = attMs;
					partModelToERPArray.add(partModelObjNoMc);
				}
				continue;
			}

			if (attMsArray.isEmpty()) {
				for (String attMc : attMcArray) {
					PartModelToERP partModelObjNoMs = (PartModelToERP) partModelToERPObj.clone();
					partModelObjNoMs.MODEL_CODE = attMc;
					partModelToERPArray.add(partModelObjNoMs);
				}
				continue;
			}

			for (String attMc : attMcArray) {
				for (String attMs : attMsArray) {
					PartModelToERP partModelObjAll = (PartModelToERP) partModelToERPObj.clone();
					partModelObjAll.MODEL_CODE = attMc;
					partModelObjAll.MODEL_SERISE = attMs;
					partModelToERPArray.add(partModelObjAll);
				}
			}
		}
		outputDataFile("13_DMS.txt", jsonToString(partModelToDMSArray));
		outputDataFile("13_ERP.txt", jsonToString(partModelToERPArray));
	}

	public class PartModelToDMS extends CMC_ObjectBase {
		public String PART_NO;
		public ArrayList<String> MODEL_CODE;
		public ArrayList<String> MODEL_SERISE;
	}

	public class PartModelToERP extends CMC_ObjectBase {
		public String PART_NO;
		public String MODEL_CODE;
		public String MODEL_SERISE;
	}

	public void createDealer(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SPlmWSLocator wsdl = new SPlmWSLocator();
		String dealerDate = null;

		try {
			dealerDate = wsdl.getSPlmWSSoap().dealer(null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String dealerType = "SPLM_Dealer";
		String dealerVault = "eService Production";
		String dealerPolicy = "SPLM_Dealer";
		String dealerName = "";

		dealerDate = dealerDate.replace("{}", "\"\"");
		ArrayList<Dealer> inputDataArray = gson.fromJson(dealerDate, new TypeToken<ArrayList<Dealer>>() {
		}.getType());

		Map<String, Map<String, String>> dealerTotalMap = new HashMap<String, Map<String, String>>();
		DomainObject createDealerObj = new DomainObject();

		for (Dealer dealerObj : inputDataArray) {
			dealerName = dealerObj.COMPANY_ID;

			if (dealerTotalMap.keySet().contains(dealerName)) {
				continue;
			}

			Map<String, String> dealerAttributeMap = new HashMap<String, String>();
			dealerAttributeMap.put(DomainConstants.SELECT_DESCRIPTION, dealerObj.COMPANY_DESC);
			dealerAttributeMap.put("SPLM_ContactNumber", dealerObj.TEL_NO);
			dealerAttributeMap.put("SPLM_Address", dealerObj.ADDRESS);
			dealerTotalMap.put(dealerName, dealerAttributeMap);

			createDealerObj.createObject(context, dealerType, dealerName, null, dealerPolicy, dealerVault);
		}

		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		DomainObject domObj = new DomainObject();

		for (String dealerEachName : dealerTotalMap.keySet()) {
			MapList dealerList = DomainObject.findObjects(context, dealerType, // type
					dealerEachName, // name
					"-", // revision
					"*", // owner
					dealerVault, // vault
					null, // where
					null, false, busSelect, (short) 0);

			String dealerId = (String) ((Map) dealerList.get(0)).get(DomainConstants.SELECT_ID);

			if (!dealerId.isEmpty()) {
				domObj.setId(dealerId);
				Map dealerAttributeMap = dealerTotalMap.get(dealerEachName);
				String description = (String) dealerAttributeMap.get(DomainConstants.SELECT_DESCRIPTION);
				domObj.setDescription(context, description);
				dealerAttributeMap.remove(DomainConstants.SELECT_DESCRIPTION);
				domObj.setAttributeValues(context, dealerAttributeMap);
			}
		}
	}

	class Dealer extends CMC_ObjectBase {
		public String COMPANY_ID;
		public String COMPANY_DESC;
		public String ADDRESS;
		public String TEL_NO;
	}
}