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

	private final String DEFAULT_ROOT_DIRECTORY = "C:\\temp\\";

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
		try {
			if (dataFile.exists() != true) {
				System.out.println("We had to make " + fileName + " file.");
				dataFile.createNewFile();
			}
			PrintWriter output = new PrintWriter(new FileWriter(dataFile, true));
			output.append(dataStr + "\n\n");
			output.append("******* " + convertNowDateFormat("yyyy.MM.dd HH:mm:ss") + "******* " + "\n\n");
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("COULD NOT WRITE!!");
		}
	}

	private <E> String translateJsonIntoString(ArrayList<E> arrayList) {
		return new Gson().toJson(arrayList);
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
		searchPartAll(context, args);
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

	/* Start Line : API */
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

		MapList pncMapList = DomainObject.findObjects(context, "SPLM_PNC", // type
				"*", // name
				"-", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, (short) 10);

		ArrayList<PNCForBoth> pncForBothArray = new ArrayList<PNCForBoth>();

		// json for DMS/ERP system
		for (Object obj : pncMapList) {
			Map pncMap = (Map) obj;
			PNCForBoth pncForBothObj = new PNCForBoth();
			pncForBothObj.PNC_NO = (String) pncMap.get(DomainConstants.SELECT_NAME);
			pncForBothObj.PNC_NAME_C = (String) pncMap.get("attribute[SPLM_Name_TC]");
			pncForBothObj.PNC_NAME_E = (String) pncMap.get("attribute[SPLM_Name_EN]");
			pncForBothObj.PNC_TYPE = (String) pncMap.get("attribute[SPLM_PNC_Type]");
			pncForBothObj.CHANGE_DATE = convertDateFormat((String) pncMap.get(DomainConstants.SELECT_MODIFIED),
					"yyyyMMdd");
			pncForBothObj.GUID = (String) pncMap.get(DomainConstants.SELECT_ID);
			pncForBothArray.add(pncForBothObj);
		}

		outputDataFile("16_DMS_PNC.txt", translateJsonIntoString(pncForBothArray));
		outputDataFile("16_ERP_PNC.txt", translateJsonIntoString(pncForBothArray));
	}

	public class PNCForBoth extends CMC_ObjBaseAttribute {
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
		busSelects.add("attribute[SPLM_Name_TC]");
		busSelects.add("attribute[SPLM_Name_EN]");

		MapList groupCodeMapList = DomainObject.findObjects(context, "SPLM_GroupCode", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, Short.parseShort("0"));

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
		outputDataFile("17_DMS_Group.txt", translateJsonIntoString(groupCodeForBothArray));
		outputDataFile("17_ERP_Group.txt", translateJsonIntoString(groupCodeForBothArray));
	}

	public class GroupCodeForBoth extends CMC_ObjBaseAttribute {
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
	public void searchVenderAlter(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		busSelects.add("attribute[SPLM_SO_Note]");

		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, Short.parseShort("0"));

		DomainObject domObj = new DomainObject();
		ArrayList<VenderAlterToDMS> venderAlterToDMSArray = new ArrayList<VenderAlterToDMS>();
		ArrayList<VenderAlterToERP> venderAlterToERPArray = new ArrayList<VenderAlterToERP>();

		for (Object obj : partMapList) {
			Map partMap = (Map) obj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partModifiedDate = (String) partMap.get(DomainConstants.SELECT_MODIFIED);
			String partAttSoNo = (String) partMap.get("attribute[SPLM_SO_Note]");
			domObj.setId(partId);
			StringList partAttVendorList = domObj.getInfoList(context, "attribute[SPLM_Vendor]");

			// json for DMS system
			VenderAlterToDMS venderAlterToDMSObj = new VenderAlterToDMS();
			venderAlterToDMSObj.PART_NO = partName;
			venderAlterToDMSObj.VENDOR_NO = partAttVendorList;
			venderAlterToDMSObj.SO_NO = partAttSoNo;
			venderAlterToDMSObj.CHANGE_DATE = convertDateFormat(partModifiedDate, "yyyyMMdd");
			venderAlterToDMSObj.GUID = partId;
			venderAlterToDMSArray.add(venderAlterToDMSObj);

			// json for ERP system
			for (String partAttVendor : partAttVendorList) {
				VenderAlterToERP venderAlterToERPObj = new VenderAlterToERP();
				venderAlterToERPObj.PART_NO = partName;
				venderAlterToERPObj.SO_NO = partAttSoNo;
				venderAlterToERPObj.CHANGE_DATE = convertDateFormat(partModifiedDate, "yyyyMMdd");
				venderAlterToERPObj.VENDOR_NO = partAttVendor;
				venderAlterToERPObj.GUID = partId;
				venderAlterToERPArray.add(venderAlterToERPObj);
			}
		}
		outputDataFile("19_DMS_PartVendor.txt", translateJsonIntoString(venderAlterToDMSArray));
		outputDataFile("19_ERP_PartVendor.txt", translateJsonIntoString(venderAlterToERPArray));
	}

	public class VenderAlterToDMS extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public StringList VENDOR_NO;
		public String SO_NO;
	}

	public class VenderAlterToERP extends CMC_ObjBaseAttribute {
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
				null, false, busSelects, (short) 0);

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
		outputDataFile("14_DMS_Alter.txt", translateJsonIntoString(AlterPartToDMSArray));
		outputDataFile("14_ERP_Alter.txt", translateJsonIntoString(AlterPartToERPArray));
		outputDataFile("15_DMS_Optional.txt", translateJsonIntoString(OptionalPartToDMSArray));
		outputDataFile("15_ERP_Optional.txt", translateJsonIntoString(OptionalPartToERPArray));
	}

	// 14. Alternate Part
	public class AlterPartToDMS extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public ArrayList<String> NEW_PART_NO;
	}

	public class AlterPartToERP extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public String NEW_PART_NO;
	}

	// 15. Optional Part
	public class OptionalPartToDMS extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public ArrayList<String> REUSE_PART_NO;
	}

	public class OptionalPartToERP extends CMC_ObjBaseAttribute {
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
				null, false, busSelects, (short) 0);

		DomainObject domObj = new DomainObject();
		Map<String, ArrayList<String>> partPncMap = new HashMap<String, ArrayList<String>>();
		ArrayList<PartPncToDMS> partPncToDMSArray = new ArrayList<PartPncToDMS>();
		ArrayList<PartPncToERP> partPncToERPArray = new ArrayList<PartPncToERP>();

		for (Object obj : partMapList) {
			Map partMap = (Map) obj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);

			domObj.setId(partId);
			StringList pncNameList = domObj.getInfoList(context, "to[SPLM_RelatedPart].from[SPLM_PNC].name");

			for (String pncName : pncNameList) {
				filter(partPncMap, partName, pncName);
			}

			ArrayList<String> pncArrayList = partPncMap.get(partName);

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
				partPncToERPObj.CHANGE_DATE = convertNowDateFormat("yyyyMMdd");
				partPncToERPObj.GUID = partId;
				partPncToERPObj.PNC_NO = pncName;
				partPncToERPArray.add(partPncToERPObj);
			}
		}

		outputDataFile("11_DMS_Part-PNC.txt", translateJsonIntoString(partPncToDMSArray));
		outputDataFile("11_ERP_Part-PNC.txt", translateJsonIntoString(partPncToERPArray));
	}

	public class PartPncToDMS extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public ArrayList<String> PNC_NO;
	}

	public class PartPncToERP extends CMC_ObjBaseAttribute {
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
				null, false, busSelects, (short) 0);

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
		outputDataFile("12_DMS_Part-Group.txt", translateJsonIntoString(partGroupCodeToDMSArray));
	}

	public class PartGroupCodeToDMS extends CMC_ObjBaseAttribute {
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
				null, false, busSelects, (short) 0);

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
					null, false, busSelects, (short) 0);

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
		outputDataFile("18_ERP_Group-PNC.txt", translateJsonIntoString(groupCodePncToERPArray));
	}

	public class GroupCodePncToERP extends CMC_ObjBaseAttribute {
		public String GROUP_CODE;
		public String PNC_NO;
	}

	/**
	 * 8. ServicePart
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void searchPartAll(Context context, String[] args) throws Exception {
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
//		busSelect.add("attribute[SPLM_SO_IssueDate]"); // SO_RELEASE_DATE
//		busSelect.add("attribute[SPLM_SO_Note]"); // SO_NO
		busSelect.add("attribute[SPLM_ModelCode]"); // MODEL_CODE
		busSelect.add("attribute[SPLM_ModelSeries]"); // MODEL_SERIES

		StringList busSelectPNC = new StringList();
		busSelectPNC.add(DomainConstants.SELECT_ID);
		busSelectPNC.add("attriubte[SPLM_PNC_Type]");

		MapList partList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelect, (short) 3);

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
			String partAttMaterialGroup = (String) partMap.get("attribute[SPLM_MaterialGroup]");
			String partAttLocation = (String) partMap.get("attribute[SPLM_Location]");
//			String attSOIssueDate = (String) partMap.get("attribute[SPLM_SO_IssueDate]");
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
						null, false, busSelectPNC, (short) 0);
				for (Object pncObj : pncList) {
					attPNCType = (String) ((Map) pncObj).get("attriubte[SPLM_PNC_Type]");
				}
			}

			if (partAttLocation == "1300") {
				if (attPNCType == "Z1" || attPNCType == "Z3" || attPNCType == "Z4" || attPNCType == "Z5") {
					attTransGroup = "DUMY";
				}
			} else if (partAttLocation == "9001" || partAttLocation == "9000") {
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
				attOptionalType = attOptionalType.equalsIgnoreCase("Alternate Part") ? attOptionalType
						: "Optional Part";
				mapping.put(attOptionalType, optPartName);
			}

			part.PART_NO = (String) partMap.get(DomainConstants.SELECT_NAME);
			part.PART_SH_NO = (String) partMap.get("attribute[SPLM_OrderType]");
			part.K_D = (partAttMaterialGroup == "KD" || partAttMaterialGroup == "KDY") ? "K" : "D";
			part.GROUP_CODE = groupCodeList.isEmpty() ? "" : groupCodeList.get(0);
			part.ALT_PART_NO = mapping.containsKey("Alternate Part") ? mapping.get("Alternate Part") : "";
			part.REUSE_PART_NO = mapping.containsKey("Optional Part") ? mapping.get("Optional Part") : "";
			part.PLANT = partAttLocation;
			part.MATERIAL_TYPE = (String) partMap.get("attribute[SPLM_MaterialType]");
			part.MATERIAL_GROUP = (partAttMaterialGroup == "KD" || partAttMaterialGroup == "KDY") ? "K" : "D";
			part.UNIT = (String) partMap.get("attribute[SPLM_Unit]");
			part.PURCHASING_DEPARTMENT_NO = (String) partMap.get("attribute[SPLM_PurchasingGroup]");
			part.MANUFACTURER_CODE = ""; // Uncheck, got logic
			part.VENDOR_NO = (String) partMap.get("attribute[SPLM_Vendor]");
			part.PART_NAME_C = (String) partMap.get("attribute[SPLM_Name_TC]");
			part.PART_NAME_E = (String) partMap.get("attribute[SPLM_Name_EN]");
			part.SALES_ORG = partAttLocation == "1300" ? "3000"
					: partAttLocation == "9001" ? "9000"
							: partAttLocation == "9000" ? "SDM" : partAttLocation == "1300,9000" ? "SDM" : "";
			part.PNC_NO = attpncName;
			part.ITEM_CATEGORY_GROUP = ""; // Uncheck
			part.MODEL_SERIES = (String) partMap.get("attribute[SPLM_ModelSeries]"); // Uncheck
			part.MODEL_CODE = (String) partMap.get("attribute[SPLM_ModelCode]"); // Uncheck
			part.CAR_CAEGORY = ""; // Uncheck
			part.PART_TYPE = (String) partMap.get("attribute[SPLM_ItemSubType]");
			part.COMMISSION_GROUP = attPNCType;
			part.TRANS_GROUP = attTransGroup;
			part.BIG_CAR_TYPE = (String) partMap.get("attribute[SPLM_DTAT_Only]");
//			part.SO_NO = (String) partMap.get("attribute[SPLM_SO_Note]");
//			part.SO_RELEASE_DATE = attSOIssueDate.isEmpty() ? "" : formatDateTime(attSOIssueDate, "yyMMdd");
			part.HAS_SUBASSEMBLY = ((String) partMap.get("attribute[SPLM_HaveSubPart]")).equalsIgnoreCase("TRUE") ? "Y"
					: "N";
			part.PART_OWNER = (String) partMap.get(DomainConstants.SELECT_OWNER);
			part.IS_SERVICE_PART = ((String) partMap.get("attribute[SPLM_IsServicePart]")).equalsIgnoreCase("TRUE")
					? "Y"
					: "N";
			part.CREATE_DATE = convertDateFormat((String) partMap.get(DomainConstants.SELECT_ORIGINATED), "yyyyMMdd");
			part.CHANGE_DATE = convertDateFormat((String) partMap.get(DomainConstants.SELECT_MODIFIED), "yyyyMMdd");
			part.GUID = partId;

			partArray.add(part);
		}
		outputDataFile("8_DMS_ServicePart.txt", translateJsonIntoString(partArray));
	}

	public class Part extends CMC_ObjBaseAttribute {
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
				null, false, busSelects, (short) 0);

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
				McGroupCodeToERP mcGroupCodeToERPObj = new McGroupCodeToERP();
				mcGroupCodeToERPObj.MODEL_CODE = mcName;
				mcGroupCodeToERPObj.GROUP_NO = groupDrawingAttGroupCode;
				mcGroupCodeToERPObj.CHANGE_DATE = convertDateFormat(mcModifiedDate, "yyyyMMdd");
				mcGroupCodeToERPObj.GUID = mcId;
				mcGroupCodeToERPArray.add(mcGroupCodeToERPObj);
			}
		}
		outputDataFile("22_ERP_ModeCode-GroupCode.txt", translateJsonIntoString(mcGroupCodeToERPArray));
	}

	public class McGroupCodeToERP extends CMC_ObjBaseAttribute {
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

		MapList msMapList = DomainObject.findObjects(context, "SPLM_ModelSeries", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"from[SPLM_SBOM]==TRUE", // where
				null, false, busSelects, (short) 0);

		DomainObject domObj = new DomainObject();
		ArrayList<MsPartCategoryToERP> msPartCategoryToERPArray = new ArrayList<MsPartCategoryToERP>();

		for (Object obj : msMapList) {
			Map msMap = (Map) obj;
			String msId = (String) msMap.get(DomainConstants.SELECT_ID);
			String msName = (String) msMap.get(DomainConstants.SELECT_NAME);
			String msModifiedDate = (String) msMap.get(DomainConstants.SELECT_MODIFIED);
			domObj.setId(msId);

			StringList partsCatalogueNameList = domObj.getInfoList(context,
					"from[SPLM_SBOM].to[SPLM_GLNO].from[SPLM_RelatedPartsCatalogue].to[SPLM_PartsCatalogue].name");

			// json for ERP system
			for (String partsCatalogueName : partsCatalogueNameList) {
				MsPartCategoryToERP msPartCategoryToERPObj = new MsPartCategoryToERP();
				msPartCategoryToERPObj.MODEL_SERIES = msName;
				msPartCategoryToERPObj.PART_CATEGORY_NO = partsCatalogueName;
				msPartCategoryToERPObj.CHANGE_DATE = convertDateFormat(msModifiedDate, "yyyyMMdd");
				msPartCategoryToERPObj.GUID = msId;
				msPartCategoryToERPArray.add(msPartCategoryToERPObj);
			}
		}
		outputDataFile("20_ERP_ModelSeries-PartCategory.txt", translateJsonIntoString(msPartCategoryToERPArray));
	}

	public class MsPartCategoryToERP extends CMC_ObjBaseAttribute {
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
				null, false, busSelects, (short) 0);

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
		outputDataFile("21_ERP_PartCategory-ModelCode.txt", translateJsonIntoString(partsCatelogueMcToERPArray));
	}

	public class PartsCatelogueMcToERP extends CMC_ObjBaseAttribute {
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
		busSelects.add("attribute[SPLM_ModelCode]");
		busSelects.add("attribute[SPLM_ModelSeries]");

		MapList partMapList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"attribute[SPLM_ModelCode]!='' || attribute[SPLM_ModelSeries]!=''", // where
				null, false, busSelects, (short) 0);

		ArrayList<PartModelToDMS> partModelToDMSArray = new ArrayList<PartModelToDMS>();
		ArrayList<PartModelToERP> partModelToERPArray = new ArrayList<PartModelToERP>();

		for (Object obj : partMapList) {
			Map partMap = (Map) obj;
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String modifiedDate = (String) partMap.get(DomainConstants.SELECT_MODIFIED);
			String partAttMc = (String) partMap.get("attribute[SPLM_ModelCode]");
			String partAttMs = (String) partMap.get("attribute[SPLM_ModelSeries]");

			ArrayList<String> partAttMcArray = new ArrayList<String>();
			ArrayList<String> partAttMsArray = new ArrayList<String>();
			for (String str : partAttMc.split(splitStr)) {
				if (str.isEmpty()) {
					continue;
				}
				partAttMcArray.add(str);
			}
			for (String str : partAttMs.split(splitStr)) {
				if (str.isEmpty()) {
					continue;
				}
				partAttMsArray.add(str);
			}

			// json for DMS system
			PartModelToDMS partModelToDMSObj = new PartModelToDMS();
			partModelToDMSObj.PART_NO = partName;
			partModelToDMSObj.MODEL_CODE = (ArrayList<String>) partAttMcArray.clone();
			partModelToDMSObj.CHANGE_DATE = convertDateFormat(modifiedDate, "yyyyMMdd");
			partModelToDMSObj.GUID = partId;
			partModelToDMSArray.add(partModelToDMSObj);

			// json for ERP system
			if (partAttMcArray.isEmpty()) {
				partAttMcArray.add("");
			}
			if (partAttMsArray.isEmpty()) {
				partAttMcArray.add("");
			}

			for (String attMc : partAttMcArray) {
				for (String attMs : partAttMsArray) {
					PartModelToERP partModelToERPObj = new PartModelToERP();
					partModelToERPObj.PART_NO = partName;
					partModelToERPObj.MODEL_SERISE = attMs;
					partModelToERPObj.MODEL_CODE = attMc;
					partModelToERPObj.CHANGE_DATE = convertDateFormat(modifiedDate, "yyyyMMdd");
					partModelToERPObj.GUID = partId;
					partModelToERPArray.add(partModelToERPObj);
				}
			}
		}
		outputDataFile("13_DMS_PartModel.txt", translateJsonIntoString(partModelToDMSArray));
		outputDataFile("13_ERP_PartModel.txt", translateJsonIntoString(partModelToERPArray));
	}

	public class PartModelToDMS extends CMC_ObjBaseAttribute {
		public String PART_NO;
		public ArrayList<String> MODEL_CODE;
	}

	public class PartModelToERP extends CMC_ObjBaseAttribute {
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

		String dealerStr = null;
		SPlmWSLocator wsdl = new SPlmWSLocator();
		try {
			dealerStr = wsdl.getSPlmWSSoap().dealer(null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		dealerStr = dealerStr.replace("{}", "\"\"");
		String dealerType = "SPLM_Dealer";
		String dealerVault = "eService Production";
		String dealerPolicy = "SPLM_Dealer";
		String dealerName = "";

		ArrayList<Dealer> dealerArray = gson.fromJson(dealerStr, new TypeToken<ArrayList<Dealer>>() {
		}.getType());

		Map<String, Map<String, String>> dealerMap = new HashMap<String, Map<String, String>>();
		DomainObject createDealerDomObj = new DomainObject();

		for (Dealer dealerObj : dealerArray) {
			dealerName = dealerObj.COMPANY_ID;

			if (dealerMap.keySet().contains(dealerName)) {
				continue;
			}

			Map<String, String> dealerAttMap = new HashMap<String, String>();
			dealerAttMap.put(DomainConstants.SELECT_DESCRIPTION, dealerObj.COMPANY_DESC);
			dealerAttMap.put("SPLM_ContactNumber", dealerObj.TEL_NO);
			dealerAttMap.put("SPLM_Address", dealerObj.ADDRESS);
			dealerMap.put(dealerName, dealerAttMap);

			createDealerDomObj.createObject(context, dealerType, dealerName, null, dealerPolicy, dealerVault);
		}

		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		DomainObject modDomObj = new DomainObject();

		for (String dealerEachName : dealerMap.keySet()) {
			MapList dealerMapList = DomainObject.findObjects(context, dealerType, // type
					dealerEachName, // name
					"-", // revision
					"*", // owner
					dealerVault, // vault
					null, // where
					null, false, busSelects, (short) 0);

			String dealerId = (String) ((Map) dealerMapList.get(0)).get(DomainConstants.SELECT_ID);

			if (!dealerId.isEmpty()) {
				modDomObj.setId(dealerId);
				Map<String, String> dealerAttMap = dealerMap.get(dealerEachName);
				String dealerDescription = dealerAttMap.get(DomainConstants.SELECT_DESCRIPTION);
				modDomObj.setDescription(context, dealerDescription);
				dealerAttMap.remove(DomainConstants.SELECT_DESCRIPTION);
				modDomObj.setAttributeValues(context, dealerAttMap);
			}
		}
	}

	public class Dealer extends CMC_ObjBaseAttribute {
		public String COMPANY_ID; //
		public String COMPANY_DESC;
		public String ADDRESS;
		public String TEL_NO;
	}
}