import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.google.gson.Gson;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;

public class SPLM_SPLMWebAPI_JPO_mxJPO {

	public String test(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		Map<String, String> paramsMap = JPO.unpackArgs(args);
		Test test = gson.fromJson(paramsMap.get("json"), Test.class); // parse Json obj to class

		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID); // "id"
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_OWNER);
		busSelects.add("attribute[SPLM_Name_TC]");

		MapList personList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"owner=='" + test.owner + "'", // where
				null, true, busSelects, (short) 0);

		ArrayList<TestPart> ObjArray = new ArrayList<TestPart>();
		for (Object object : personList) {
			Map map = (Map) object;
			TestPart part = new TestPart();
			part.tcName = (String) map.get("attribute[SPLM_Name_TC]");
			part.identity = (String) map.get(DomainConstants.SELECT_ID);
			part.name = (String) map.get(DomainConstants.SELECT_NAME);
			part.owner = (String) map.get(DomainConstants.SELECT_OWNER);
			part.typeName = (String) map.get(DomainConstants.SELECT_TYPE);
			ObjArray.add(part);
		}
		return gson.toJson(ObjArray);
	}

	public class TestPart {
		public String name;
		public String owner;
		public String identity;
		public String tcName;
		public String typeName;
	}

	public class Test {
		public String owner;
	}

	/**
	 * 17. GROUP
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchGroupCode(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

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

		ArrayList<GroupCode> groupCodeArray = new ArrayList<GroupCode>();
		for (Object object : groupCodeList) {
			Map map = (Map) object;
			GroupCode groupCode = new GroupCode();
			groupCode.GROUP_CODE = (String) map.get(DomainConstants.SELECT_NAME);
			groupCode.GROUP_NAME_TC = (String) map.get("attribute[SPLM_Name_TC]");
			groupCode.GROUP_NAME_EN = (String) map.get("attribute[SPLM_Name_EN]");
			groupCode.CHANGE_DATE = sdf.format(new Date((String) map.get(DomainConstants.SELECT_MODIFIED)));
			groupCode.GUID = (String) map.get(DomainConstants.SELECT_ID);

			groupCodeArray.add(groupCode);
		}
		return gson.toJson(groupCodeArray);
	}

	public class GroupCode {
		public String GROUP_CODE;
		public String GROUP_NAME_TC;
		public String GROUP_NAME_EN;
		public String CHANGE_DATE;
		public String GUID;
	}

	/**
	 * 16.PNC
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchPnc(Context context, String[] args) throws Exception {
		String returnVal = JPO.invoke(context, "SPLM_Integration_JPO", null, "searchPnc", args, String.class);
		return returnVal;
	}

	/**
	 * 19.PartVendor
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchVenderAlter(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_MODIFIED);
//		busSelects.add("attribute[SPLM_SO_Note]");

		MapList resultList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, Short.parseShort("0"));

		ArrayList<VenderAlter> venderAlterArray = new ArrayList<VenderAlter>();
		DomainObject domObj = new DomainObject();
		for (Object object : resultList) {
			Map map = (Map) object;
			VenderAlter venderAlter = new VenderAlter();
			String partId = (String) map.get(DomainConstants.SELECT_ID);
			domObj.setId(partId);
			venderAlter.PART_NO = (String) map.get(DomainConstants.SELECT_NAME);
			venderAlter.VENDOR_NO = domObj.getInfoList(context, "attribute[SPLM_Vendor]");
//			venderAlter.SO_NO  		= (String)map.get("attribute[SPLM_SO_Note]");
			venderAlter.CHANGE_DATE = sdf.format(new Date((String) map.get(DomainConstants.SELECT_MODIFIED)));
			venderAlter.GUID = (String) map.get(DomainConstants.SELECT_ID);

			venderAlterArray.add(venderAlter);
		}
		return gson.toJson(venderAlterArray);
	}

	public class VenderAlter {
		public String PART_NO;
		public StringList VENDOR_NO;
//		public String SO_NO;
		public String CHANGE_DATE;
		public String GUID;
	}

	/**
	 * 14/15 Opt/Alt
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchOptPart(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Map<String, String> paramsMap = JPO.unpackArgs(args);
		Opt inputOptPart = gson.fromJson(paramsMap.get("json"), Opt.class); // parse Json obj to class

		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);

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
		Map<String, ArrayList<String>> mapping = new HashMap<String, ArrayList<String>>();
		ArrayList<String> relArray = new ArrayList<String>();

		for (Object partObj : partList) {
			Map partMap = (Map) partObj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);

			domObj.setId(partId);
			MapList partOptList = domObj.getRelatedObjects(context, "SPLM_RelatedOptionalPart", // relationshipPattern,
					"SPLM_Part", // typePattern,
					busSelect, // StringList objectSelects,
					relSelect, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_OptionalType]==" + inputOptPart.optionalPart, // String relationshipWhere
					0); // int limit)

			for (Object partOpt : partOptList) {
				String optPartName = (String) ((Map) partOpt).get(DomainConstants.SELECT_NAME);

				if (!mapping.containsKey(partName)) {
					ArrayList<String> alterPartArray = new ArrayList<String>();
					mapping.put(partName, alterPartArray);
					alterPartArray.add(optPartName);
					continue;
				}
				mapping.get(partName).add(optPartName);
			}
		}

		ArrayList<Opt> partOptArray = new ArrayList<Opt>();
		for (Object splmPartList : partList) {
			Map partMap = (Map) splmPartList;
			String part = (String) partMap.get(DomainConstants.SELECT_NAME);

			if (mapping.containsKey(part)) {
				Opt partOpt = new Opt();
				partOpt.PART_NO = part;
				partOpt.CHANGE_DATE = sdf.format(new Date());
				partOpt.GUID = (String) partMap.get(DomainConstants.SELECT_ID);

				if (inputOptPart.optionalPart.equalsIgnoreCase("\u9078\u7528\u4ef6")) {
					partOpt.REUSE_PART_NO = mapping.get(part);
				} else {
					partOpt.NEW_PART_NO = mapping.get(part);
				}
				partOptArray.add(partOpt);
			}
		}
		return gson.toJson(partOptArray);
	}

	public class Opt {
		public String PART_NO;
		public String optionalPart;
		public ArrayList<String> NEW_PART_NO;
		public ArrayList<String> REUSE_PART_NO;
		public String CHANGE_DATE;
		public String GUID;
	}

	/**
	 * 11 Part-PNC
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchPartPnc(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

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
		Map<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();

		for (Object part : partList) {
			Map partMap = (Map) part;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);

			domObj.setId(partId);
			StringList pncNameList = domObj.getInfoList(context, "to[SPLM_RelatedPart].from[SPLM_PNC].name");

			for (String pncName : pncNameList) {
				if (!map.containsKey(partName)) {
					ArrayList<String> pncArrayList = new ArrayList<String>();
					map.put(partName, pncArrayList);
					pncArrayList.add(pncName);
				} else if (!map.get(partName).contains(pncName)) {
					map.get(partName).add(pncName);
				}
			}
		}

		ArrayList<partPnc> partPncArray = new ArrayList<partPnc>();
		for (Object part : partList) {
			Map partMap = (Map) part;
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);

			if (map.containsKey(partName)) {
				partPnc partOpt = new partPnc();
				partOpt.PART_NO = partName;
				partOpt.PNC_NO = map.get(partName);
				partOpt.CHANGE_DATE = sdf.format(new Date());
				partOpt.GUID = (String) partMap.get(DomainConstants.SELECT_ID);

				partPncArray.add(partOpt);
			}
		}
		return gson.toJson(partPncArray);
	}

	public class partPnc {
		public String PART_NO;
		public ArrayList<String> PNC_NO;
		public String CHANGE_DATE;
		public String GUID;
	}

	/**
	 * 12 Part-Group
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchPartGroup(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

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
		}

		ArrayList<PartGroupCode> partGroupCodeArray = new ArrayList<PartGroupCode>();
		for (Object part : partList) {
			Map partMap = (Map) part;
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);

			if (mapping.containsKey(partName)) {
				PartGroupCode partGroupCodeObj = new PartGroupCode();
				partGroupCodeObj.PART_NO = partName;
				partGroupCodeObj.GROUP_CODE = mapping.get(partName);
				partGroupCodeObj.CHANGE_DATE = sdf.format(new Date());
				partGroupCodeObj.GUID = (String) partMap.get(DomainConstants.SELECT_ID);

				partGroupCodeArray.add(partGroupCodeObj);
			}
		}
		return gson.toJson(partGroupCodeArray);
	}

	public class PartGroupCode {
		public String PART_NO;
		public ArrayList<String> GROUP_CODE;
		public String CHANGE_DATE;
		public String GUID;
	}

	/**
	 * 18 Group-PNC
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchGroupPnc(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

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

		Map<String, ArrayList<String>> mapping = new HashMap<String, ArrayList<String>>();

		for (Object groupDrawingObj : groupDrawingList) {
			Map groupDrawingMap = (Map) groupDrawingObj;
			String groupDrawingId = (String) groupDrawingMap.get(DomainConstants.SELECT_ID);
			String attGroupCode = (String) groupDrawingMap.get("attribute[SPLM_GroupCode]");

			domObj.setId(groupDrawingId);
			StringList pncNameList = domObj.getInfoList(context, "from[SPLM_Related_PNC].to[SPLM_PNC].name");

			for (String pncName : pncNameList) {
				if (!mapping.containsKey(attGroupCode)) {
					ArrayList<String> groupCodeArray = new ArrayList<String>();
					mapping.put(attGroupCode, groupCodeArray);
					groupCodeArray.add(pncName);
				} else if (!mapping.get(attGroupCode).contains(pncName)) {
					mapping.get(attGroupCode).add(pncName);
				}
			}
		}

		ArrayList<String> attGroupCodeArray = (ArrayList<String>) mapping.keySet();
		ArrayList<GroupCodePnc> groupCodePncArray = new ArrayList<GroupCodePnc>();
		for (String attGroupCode : attGroupCodeArray) {

			MapList groupCodeList = DomainObject.findObjects(context, "SPLM_GroupCode", // type
					attGroupCode, // name
					"*", // revision
					"*", // owner
					"eService Production", // vault
					"", // where
					null, false, busSelect, (short) 0);

			for (Object groupCodeObj : groupCodeList) {
				GroupCodePnc groupCodePncObj = new GroupCodePnc();

				groupCodePncObj.GROUP_CODE = attGroupCode;
				groupCodePncObj.PNC_NO = mapping.get(attGroupCode);
				groupCodePncObj.CHANGE_DATE = sdf.format(new Date());
				groupCodePncObj.GUID = (String) ((Map) groupCodeObj).get(DomainConstants.SELECT_ID);

				groupCodePncArray.add(groupCodePncObj);
			}
		}
		return gson.toJson(groupCodePncArray);
	}

	public class GroupCodePnc {
		public String GROUP_CODE;
		public ArrayList<String> PNC_NO;
		public String CHANGE_DATE;
		public String GUID;
	}

	public String searchPartAll(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Map<String, String> paramsMap = JPO.unpackArgs(args);
		Part test = gson.fromJson(paramsMap.get("json"), Part.class);

		StringList relSelect = new StringList();
		relSelect.add("attribute[SPLM_OptionalType]");
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID); // GUID
		busSelect.add(DomainConstants.SELECT_NAME); // PART_NO
		busSelect.add(DomainConstants.SELECT_ORIGINATED); // CREATE_DATE
		busSelect.add(DomainConstants.SELECT_MODIFIED); // CHANGE_DATE
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

		MapList partList = DomainObject.findObjects(context, "SPLM_Part", // type
				test.PART_NO.isEmpty() ? "*" : test.PART_NO, // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelect, (short) 0);

		DomainObject domObj = new DomainObject();
		ArrayList<Part> partArray = new ArrayList<Part>();
		for (Object partObj : partList) {
			Map partMap = (Map) partObj;
			Part part = new Part();
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);

			domObj.setId(partId);
			StringList pncNameList = domObj.getInfoList(context, "to[SPLM_RelatedPart].from[SPLM_PNC].name");
			StringList groupCodeList = domObj.getInfoList(context,
					"to[SPLM_RelatedPart].from[SPLM_GroupDrawing].attribute[SPLM_GroupCode]");

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

			Map<String, String> mapping = new HashMap<String, String>();
			for (Object partOpt : partOptList) {
				String attOptionalType = (String) ((Map) partOpt).get("attribute[SPLM_OptionalType]");
				String optPartName = (String) ((Map) partOpt).get(DomainConstants.SELECT_NAME);
				mapping.put(attOptionalType, optPartName);
			}

			part.PART_NO = (String) partMap.get(DomainConstants.SELECT_NAME);
			part.PART_SH_NO = (String) partMap.get("attribute[SPLM_OrderType]");
			part.GROUP_CODE = groupCodeList.isEmpty() ? "" : groupCodeList.get(0);
			part.ALT_PART_NO = mapping.containsKey("\u66ff\u4ee3") ? mapping.get("\u66ff\u4ee3") : "";
			part.REUSE_PART_NO = mapping.containsKey("\u9078\u7528\u4ef6") ? mapping.get("\u9078\u7528\u4ef6") : "";
			part.MATERIAL_TYPE = (String) partMap.get("attribute[SPLM_MaterialType]");
			part.MATERIAL_GROUP = (String) partMap.get("attribute[SPLM_MaterialGroup]");
			part.UNIT = (String) partMap.get("attribute[SPLM_Unit]");
			part.PURCHASING_DEPARTMENT_NO = (String) partMap.get("attribute[SPLM_PurchasingGroup]");
			part.VENDOR_NO = (String) partMap.get("attribute[SPLM_Vendor]");
			part.PART_NAME_C = (String) partMap.get("attribute[SPLM_Name_TC]");
			part.PART_NAME_E = (String) partMap.get("attribute[SPLM_Name_EN]");
			part.PNC_NO = pncNameList.isEmpty() ? "" : pncNameList.get(0);
			part.PART_TYPE = (String) partMap.get("attribute[SPLM_ItemSubType]");
			part.BIG_CAR_TYPE = (String) partMap.get("attribute[SPLM_DTAT_Only]");
			part.HAS_SUBASSEMBLY = ((String) partMap.get("attribute[SPLM_HaveSubPart]")).equalsIgnoreCase("TRUE") ? "Y"
					: "N";
			part.IS_SERVICE_PART = ((String) partMap.get("attribute[SPLM_IsServicePart]")).equalsIgnoreCase("TRUE")
					? "Y"
					: "N";
			part.CREATE_DATE = sdf.format(new Date((String) partMap.get(DomainConstants.SELECT_ORIGINATED)));
			part.CHANGE_DATE = sdf.format(new Date((String) partMap.get(DomainConstants.SELECT_MODIFIED)));
			part.GUID = partId;

			partArray.add(part);
		}
		return gson.toJson(partArray);
	}

	public class Part {
		public String PART_NO;
		public String PART_SH_NO;
		public String GROUP_CODE;
		public String ALT_PART_NO;
		public String REUSE_PART_NO;
		public String PLANT;
		public String MATERIAL_TYPE;
		public String MATERIAL_GROUP;
		public String UNIT;
		public String PURCHASING_DEPARTMENT_NO;
		public String VENDOR_NO;
		public String PART_NAME_C;
		public String PART_NAME_E;
		public String PNC_NO;
		public String PART_TYPE;
		public String BIG_CAR_TYPE;
		public String HAS_SUBASSEMBLY;
		public String IS_SERVICE_PART;
		public String CREATE_DATE;
		public String CHANGE_DATE;
		public String GUID;
	}

	/**
	 * 22. ModeCode_GroupCode
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchMcGroupCode(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);
		busSelect.add(DomainConstants.SELECT_MODIFIED);

		MapList modelCodeList = DomainObject.findObjects(context, "SPLM_ModelCode", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"from[SPLM_SBOM]==TRUE", // where
				null, false, busSelect, (short) 0);

		DomainObject domObj = new DomainObject();
		Map<String, ArrayList<String>> mapping = new HashMap<String, ArrayList<String>>();

		for (Object modelCodeObj : modelCodeList) {
			Map modelCodeMap = (Map) modelCodeObj;
			String modelCodeId = (String) modelCodeMap.get(DomainConstants.SELECT_ID);
			String modelCodeName = (String) modelCodeMap.get(DomainConstants.SELECT_NAME);

			domObj.setId(modelCodeId);
			StringList attGroupCodeList = domObj.getInfoList(context,
					"to[SPLM_SBOM].from[SPLM_GLNO].from[SPLM_RelatedPartsCatalogue].to[SPLM_PartsCatalogue].from[SPLM_RelatedGroupDrawing].to[SPLM_GroupDrawing].attribute[SPLM_GroupCode]");

			for (String attGroupCode : attGroupCodeList) {
				if (attGroupCode.isEmpty()) {
					continue;
				}
				if (!mapping.containsKey(modelCodeName)) {
					ArrayList<String> mcArray = new ArrayList<String>();
					mapping.put(modelCodeName, mcArray);
					mcArray.add(attGroupCode);
					continue;
				}
				mapping.get(modelCodeName).add(attGroupCode);
			}
		}

		ArrayList<McGroupCode> mcGroupCodeArray = new ArrayList<McGroupCode>();
		for (Object modelCodeObj : modelCodeList) {
			Map modelCodeMap = (Map) modelCodeObj;
			String modelCodeName = (String) modelCodeMap.get(DomainConstants.SELECT_NAME);
			if (mapping.containsKey(modelCodeName)) {
				McGroupCode mcGroupCodeObj = new McGroupCode();
				mcGroupCodeObj.MODEL_CODE = modelCodeName;
				mcGroupCodeObj.GROUP_NO = mapping.get(modelCodeName);
				mcGroupCodeObj.CHANGE_DATE = sdf
						.format(new Date((String) modelCodeMap.get(DomainConstants.SELECT_MODIFIED)));
				mcGroupCodeObj.GUID = (String) modelCodeMap.get(DomainConstants.SELECT_ID);

				mcGroupCodeArray.add(mcGroupCodeObj);
			}
		}
		return gson.toJson(mcGroupCodeArray);
	}

	public class McGroupCode {
		public String MODEL_CODE;
		public ArrayList<String> GROUP_NO;
		public String CHANGE_DATE;
		public String GUID;
	}

	/**
	 * 20. ModelSeries_PartCategory
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchMsPartCategory(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

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
		Map<String, ArrayList<String>> mapping = new HashMap<String, ArrayList<String>>();

		for (Object modelSeriesObj : modelSeriesList) {
			Map modelSeriesMap = (Map) modelSeriesObj;
			String modelSeriesId = (String) modelSeriesMap.get(DomainConstants.SELECT_ID);
			String modelSeriesName = (String) modelSeriesMap.get(DomainConstants.SELECT_NAME);

			domObj.setId(modelSeriesId);
			StringList partsCatalogueNameList = domObj.getInfoList(context,
					"from[SPLM_SBOM].to[SPLM_GLNO].from[SPLM_RelatedPartsCatalogue].to[SPLM_PartsCatalogue].name");

			for (String partsCatalogueName : partsCatalogueNameList) {
				if (partsCatalogueName.isEmpty()) {
					continue;
				}
				if (!mapping.containsKey(modelSeriesName)) {
					ArrayList<String> partCategoryArray = new ArrayList<String>();
					mapping.put(modelSeriesName, partCategoryArray);
					partCategoryArray.add(partsCatalogueName);
					continue;
				}
				mapping.get(modelSeriesName).add(partsCatalogueName);
			}
		}

		ArrayList<MsPartCategory> mcGroupCodeArray = new ArrayList<MsPartCategory>();
		for (Object modelSeriesObj : modelSeriesList) {
			Map modelSeriesMap = (Map) modelSeriesObj;
			String modelCodeName = (String) modelSeriesMap.get(DomainConstants.SELECT_NAME);

			if (mapping.containsKey(modelCodeName)) {
				MsPartCategory msPartCategoryObj = new MsPartCategory();
				msPartCategoryObj.MODEL_SERIES = modelCodeName;
				msPartCategoryObj.PART_CATEGORY_NO = mapping.get(modelCodeName);
				msPartCategoryObj.CHANGE_DATE = sdf
						.format(new Date((String) modelSeriesMap.get(DomainConstants.SELECT_MODIFIED)));
				msPartCategoryObj.GUID = (String) modelSeriesMap.get(DomainConstants.SELECT_ID);

				mcGroupCodeArray.add(msPartCategoryObj);
			}
		}
		return gson.toJson(mcGroupCodeArray);
	}

	public class MsPartCategory {
		public String MODEL_SERIES;
		public ArrayList<String> PART_CATEGORY_NO;
		public String CHANGE_DATE;
		public String GUID;
	}

	/**
	 * 21. PartCategory_ModelCode
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchPartsCatelogueMc(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);
		busSelect.add(DomainConstants.SELECT_MODIFIED);

		MapList partsCatelogueList = DomainObject.findObjects(context, "SPLM_PartsCatalogue", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"relationship[SPLM_RelatedPartsCatalogue]==TRUE", // where
				null, false, busSelect, (short) 0);
		System.out.println(partsCatelogueList);

		Map<String, ArrayList<ModelCodeCTG>> mapping = new HashMap<String, ArrayList<ModelCodeCTG>>();
		for (Object partsCatelogueObj : partsCatelogueList) {
			DomainObject domObj = new DomainObject();
			Map partsCatelogueMap = (Map) partsCatelogueObj;
			String partsCatelogueId = (String) partsCatelogueMap.get(DomainConstants.SELECT_ID);
			String partsCatelogueName = (String) partsCatelogueMap.get(DomainConstants.SELECT_NAME);
			domObj.setId(partsCatelogueId);

			StringList modelCodeList = domObj.getInfoList(context,
					"to[SPLM_RelatedPartsCatalogue].from[SPLM_GLNO].from[SPLM_SBOM].to[SPLM_ModelCode].name");

			for (String modelCode : modelCodeList) {
				if (modelCode.isEmpty()) {
					continue;
				}

				ModelCodeCTG modelCodeCTGObj = new ModelCodeCTG();
				ArrayList<ModelCodeCTG> modelCodeCTGArray = new ArrayList<ModelCodeCTG>();
				modelCodeCTGObj.MODEL_CODE = modelCode;
				modelCodeCTGObj.CTG_MODEL = "";
				modelCodeCTGArray.add(modelCodeCTGObj);
				if (mapping.containsKey(partsCatelogueName)) {
					mapping.get(partsCatelogueName).add(modelCodeCTGObj);
					continue;
				}
				mapping.put(partsCatelogueName, modelCodeCTGArray);
			}
		}

		ArrayList<PartsCatelogueMc> partsCatelogueArray = new ArrayList<PartsCatelogueMc>();
		for (Object partsCatelogueObj : partsCatelogueList) {
			Map partsCatelogueMap = (Map) partsCatelogueObj;
			String partsCatelogueName = (String) partsCatelogueMap.get(DomainConstants.SELECT_NAME);

			if (mapping.containsKey(partsCatelogueName)) {
				PartsCatelogueMc partsCatelogueMcObj = new PartsCatelogueMc();
				partsCatelogueMcObj.PART_CATEGORY_NO = partsCatelogueName;
				partsCatelogueMcObj.MODEL_CODE_LIST = mapping.get(partsCatelogueName);
				partsCatelogueMcObj.CHANGE_DATE = sdf
						.format(new Date((String) partsCatelogueMap.get(DomainConstants.SELECT_MODIFIED)));
				partsCatelogueMcObj.GUID = (String) partsCatelogueMap.get(DomainConstants.SELECT_ID);

				partsCatelogueArray.add(partsCatelogueMcObj);
			}
		}
		return gson.toJson(partsCatelogueArray);
	}

	public class PartsCatelogueMc {
		public String PART_CATEGORY_NO;
		public ArrayList<ModelCodeCTG> MODEL_CODE_LIST;
		public String CHANGE_DATE;
		public String GUID;
	}

	public class ModelCodeCTG {
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
	public String searchPartModel(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
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

		ArrayList<PartModel> partModelArray = new ArrayList<PartModel>();
		for (Object partObj : partList) {
			Map partMap = (Map) partObj;
			ArrayList<String> attMcArray = new ArrayList<String>();
			ArrayList<String> attMsArray = new ArrayList<String>();
			String modelSeriesId = (String) partMap.get(DomainConstants.SELECT_ID);
			String attMc = (String) partMap.get("attribute[SPLM_ModelCode]");
			String attMs = (String) partMap.get("attribute[SPLM_ModelSeries]");

			for (String str : attMc.split(splitStr)) {
				if(str.isEmpty())
					continue;
				attMcArray.add(str);
			}
			for (String str : attMs.split(splitStr)) {
				if(str.isEmpty())
					continue;
				attMsArray.add(str);
			}
			
			PartModel partModelObj = new PartModel();

			partModelObj.PART_NO = (String) partMap.get(DomainConstants.SELECT_NAME);
			partModelObj.MODEL_CODE = attMcArray;
			partModelObj.MODEL_SERISE = attMsArray;
			partModelObj.CHANGE_DATE = sdf.format(new Date((String) partMap.get(DomainConstants.SELECT_MODIFIED)));
			partModelObj.GUID = modelSeriesId;

			partModelArray.add(partModelObj);
		}
		return gson.toJson(partModelArray);
	}

	public class PartModel {
		public String PART_NO;
		public ArrayList<String> MODEL_CODE;
		public ArrayList<String> MODEL_SERISE;
		public String CHANGE_DATE;
		public String GUID;
	}
}