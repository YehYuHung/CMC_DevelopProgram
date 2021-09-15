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

		ArrayList<Part> ObjArray = new ArrayList<Part>();
		for (Object object : personList) {
			Map map = (Map) object;
			Part part = new Part();
			part.tcName 	= (String) map.get("attribute[SPLM_Name_TC]");
			part.identity 	= (String) map.get(DomainConstants.SELECT_ID);
			part.name 		= (String) map.get(DomainConstants.SELECT_NAME);
			part.owner 		= (String) map.get(DomainConstants.SELECT_OWNER);
			part.typeName 	= (String) map.get(DomainConstants.SELECT_TYPE);
			ObjArray.add(part);
		}
		return gson.toJson(ObjArray);
	}

	public class Part {
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
			groupCode.GROUP_CODE 	= (String) map.get(DomainConstants.SELECT_NAME);
			groupCode.GROUP_NAME_TC = (String) map.get("attribute[SPLM_Name_TC]");
			groupCode.GROUP_NAME_EN = (String) map.get("attribute[SPLM_Name_EN]");
			groupCode.CHANGE_DATE 	= sdf.format(new Date((String) map.get(DomainConstants.SELECT_MODIFIED)));
			groupCode.GUID 			= (String) map.get(DomainConstants.SELECT_ID);

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
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
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
				null, false, busSelects, Short.parseShort("0"));

		ArrayList<PNC> pncArray = new ArrayList<PNC>();
		for (Object object : pncList) {
			Map map = (Map) object;
			PNC pnc = new PNC();
			pnc.PNC_NO 		= (String) map.get(DomainConstants.SELECT_NAME);
			pnc.PNC_NAME_TC = (String) map.get("attribute[SPLM_Name_TC]");
			pnc.PNC_NAME_EN = (String) map.get("attribute[SPLM_Name_EN]");
			pnc.PNC_TYPE 	= (String) map.get("attribute[SPLM_PNC_Type]");
			pnc.CHANGE_DATE = sdf.format(new Date((String) map.get(DomainConstants.SELECT_MODIFIED)));
			pnc.GUID 		= (String) map.get(DomainConstants.SELECT_ID);

			pncArray.add(pnc);
		}
		return gson.toJson(pncArray);
	}

	public class PNC {
		public String PNC_NO;
		public String PNC_NAME_EN;
		public String PNC_NAME_TC;
		public String PNC_TYPE;
		public String CHANGE_DATE;
		public String GUID;
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
			venderAlter.PART_NO 	= (String) map.get(DomainConstants.SELECT_NAME);
			venderAlter.VENDOR_NO 	= domObj.getInfoList(context, "attribute[SPLM_Vendor]");
//			venderAlter.SO_NO  		= (String)map.get("attribute[SPLM_SO_Note]");
			venderAlter.CHANGE_DATE = sdf.format(new Date((String) map.get(DomainConstants.SELECT_MODIFIED)));
			venderAlter.GUID 		= (String) map.get(DomainConstants.SELECT_ID);

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
	 * 13. Part-Model
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchPartModel(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		busSelects.add("attribute[SPLM_ModelCode]");
		busSelects.add("attribute[SPLM_ModelSeries]");

		MapList resultList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, Short.parseShort("0"));

		ArrayList<PartModel> partModelArray = new ArrayList<PartModel>();
		DomainObject domObj = new DomainObject();
		for (Object object : resultList) {
			Map map = (Map) object;
			PartModel partModel = new PartModel();

			String partId = (String) map.get(DomainConstants.SELECT_ID);
			domObj.setId(partId);
			partModel.PART_NO 		= (String) map.get(DomainConstants.SELECT_NAME);
			partModel.MODEL_CODE 	= domObj.getInfoList(context, "attribute[SPLM_ModelCode]");
			partModel.CHANGE_DATE 	= sdf.format(new Date((String) map.get(DomainConstants.SELECT_MODIFIED)));

			partModelArray.add(partModel);
		}
		return gson.toJson(partModelArray);
	}

	public class PartModel {
		public String PART_NO;
		public StringList MODEL_CODE;
		public StringList MODEL_SERISE;
		public String CHANGE_DATE;
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

		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);

		MapList partList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"relationship[SPLM_RelatedOptionalPart]", // where
				null, false, busSelect, (short) 0);

		DomainObject domObj = new DomainObject();
		Map<String, ArrayList<String>> mapping = new HashMap<String, ArrayList<String>>();
		ArrayList<String> relArray = new ArrayList<String>();

		for (Object partObj : partList) {
			Map partMap = (Map) partObj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);

			domObj.setId(partId);
			StringList relIds = domObj.getInfoList(context, "from[SPLM_RelatedOptionalPart].id");
			for (String relId : relIds) {
				if (relArray.contains(relId))
					continue;
				relArray.add(relId);

				DomainRelationship relObj = new DomainRelationship(relId);
				relObj.open(context);
				String part = relObj.getFrom().getName(); // from.part be replace
				String newPart = relObj.getTo().getName(); // to.part prepare to replace/alter
				relObj.close(context);

//				if(optType.equals("選用件")) {
//					System.out.println("optType : " + optType);					
//				}

				if (!mapping.containsKey(part)) {
					ArrayList<String> alterPartArray = new ArrayList<String>();
					mapping.put(part, alterPartArray);
					alterPartArray.add(newPart);
					continue;
				}
				mapping.get(part).add(newPart);
			}
		}

		ArrayList<Opt> partOptArray = new ArrayList<Opt>();
		for (Object splmPartList : partList) {
			Map partMap = (Map) splmPartList;
			String part = (String) partMap.get(DomainConstants.SELECT_NAME);

			if (mapping.containsKey(part)) {
				Opt partOpt = new Opt();
				partOpt.PART_NO 	= part;
				partOpt.NEW_PART_NO = mapping.get(part);
				partOpt.CHANGE_DATE = sdf.format(new Date());
				partOpt.GUID 		= (String) partMap.get(DomainConstants.SELECT_ID);

				partOptArray.add(partOpt);
			}
		}
		return gson.toJson(partOptArray);
	}

	public class Opt {
		public String PART_NO;
		public ArrayList<String> NEW_PART_NO;
		public String CHANGE_DATE;
		public String GUID;
	}

	/**
	 * 11 Part-Pnc
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
				"relationship[SPLM_RelatedPart]", // where
				null, false, busSelect, (short) 0);

		DomainObject domObj = new DomainObject();
		Map<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();

		for (Object part : partList) {
			Map partMap = (Map) part;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);

			domObj.setId(partId);
			StringList relIds = domObj.getInfoList(context, "to[SPLM_RelatedPart].id");
			for (String relId : relIds) {

				DomainRelationship relObj = new DomainRelationship(relId);
				relObj.open(context);
				String pncName = relObj.getFrom().getName(); // from.part be replace
				String fromType = relObj.getFrom().getTypeName(); // from.type for check
				relObj.close(context);

				if (!fromType.equalsIgnoreCase("SPLM_PNC")) {
					continue;
				}

				if (!map.containsKey(partName)) {
					ArrayList<String> pncArrayList = new ArrayList<String>();
					map.put(partName, pncArrayList);
					pncArrayList.add(pncName);
					continue;
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
				partOpt.PART_NO 	= partName;
				partOpt.PNC_NO 		= map.get(partName);
				partOpt.CHANGE_DATE = sdf.format(new Date());
				partOpt.GUID 		= (String) partMap.get(DomainConstants.SELECT_ID);

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
		StringList relSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);
		busSelect.add("attribute[SPLM_GroupCode]");

		MapList partList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"relationship[SPLM_RelatedPart]", // where
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
				Map groupDrawingMap = (Map) groupDrawingObj;
				String groupDrawingName = (String) groupDrawingMap.get(DomainConstants.SELECT_NAME);
				String groupCode = (String) groupDrawingMap.get("attribute[SPLM_GroupCode]");

				if (!mapping.containsKey(partName)) {
					ArrayList<String> groupCodeArrayList = new ArrayList<String>();
					mapping.put(partName, groupCodeArrayList);
					groupCodeArrayList.add(groupCode);
					continue;
				} else if (!mapping.get(partName).contains(groupCode)) {
					mapping.get(partName).add(groupCode);
				}
			}
		}

		ArrayList<PartGroupCode> partGroupCodeArray = new ArrayList<PartGroupCode>();
		for (Object part : partList) {
			Map partMap = (Map) part;
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);

			if (mapping.containsKey(partName)) {
				PartGroupCode partGroupCodeObj = new PartGroupCode();
				partGroupCodeObj.PART_NO 		= partName;
				partGroupCodeObj.GROUP_CODE 	= mapping.get(partName);
				partGroupCodeObj.CHANGE_DATE 	= sdf.format(new Date());
				partGroupCodeObj.GUID 			= (String) partMap.get(DomainConstants.SELECT_ID);

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

	// 18 Part-Group
	public String searchGroupPnc(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add(DomainConstants.SELECT_NAME);
		busSelect.add("attribute[SPLM_GroupCode]");

		MapList groupDrawingList = DomainObject.findObjects(context, "SPLM_GroupDrawing", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"relationship[SPLM_RelatedPart]", // where
				null, false, busSelect, (short) 0);

		DomainObject domObj = new DomainObject();
		Map<String, ArrayList<String>> mapping = new HashMap<String, ArrayList<String>>();
		ArrayList<String> attGroupCodeArray = new ArrayList<String>();

		for (Object groupDrawingObj : groupDrawingList) {
			Map groupDrawingMap 	= (Map) groupDrawingObj;
			String groupDrawingId 	= (String) groupDrawingMap.get(DomainConstants.SELECT_ID);
			String attGroupCode 	= (String) groupDrawingMap.get("attribute[SPLM_GroupCode]");

			domObj.setId(groupDrawingId);
			StringList relIds = domObj.getInfoList(context, "from[SPLM_RelatedPart].to.to[SPLM_RelatedPart].id");

			for (String relId : relIds) {

				DomainRelationship relObj = new DomainRelationship(relId);
				relObj.open(context);
				String pnc = relObj.getFrom().getName();
				String fromPncType = relObj.getFrom().getTypeName();
				relObj.close(context);

				if (!fromPncType.equalsIgnoreCase("SPLM_PNC")) {
					continue;
				}

				if (!mapping.containsKey(attGroupCode)) {
					ArrayList<String> groupCodeArray = new ArrayList<String>();
					mapping.put(attGroupCode, groupCodeArray);
					groupCodeArray.add(pnc);
					attGroupCodeArray.add(attGroupCode);
					continue;
				} else if (!mapping.get(attGroupCode).contains(pnc)) {
					mapping.get(attGroupCode).add(pnc);
				}
			}
		}

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
				GroupCodePnc partGroupCodeObj = new GroupCodePnc();

				partGroupCodeObj.GROUP_CODE 	= attGroupCode;
				partGroupCodeObj.PNC_NO 		= mapping.get(attGroupCode);
				partGroupCodeObj.CHANGE_DATE 	= sdf.format(new Date());
				partGroupCodeObj.GUID 			= (String) ((Map) groupCodeObj).get(DomainConstants.SELECT_ID);

				groupCodePncArray.add(partGroupCodeObj);
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
}