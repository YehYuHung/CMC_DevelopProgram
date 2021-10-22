import java.util.ArrayList;
import java.util.Map;

import com.google.gson.Gson;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

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
		String returnVal = JPO.invoke(context, "SPLM_Integration_JPO", null, "searchGroupCode", args, String.class);
		return returnVal;
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
		String returnVal = JPO.invoke(context, "SPLM_Integration_JPO", null, "searchVenderAlter", args, String.class);
		return returnVal;
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
		String returnVal = JPO.invoke(context, "SPLM_Integration_JPO", null, "searchOptPart", args, String.class);
		return returnVal;
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
		String returnVal = JPO.invoke(context, "SPLM_Integration_JPO", null, "searchPartPnc", args, String.class);
		return returnVal;
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
		String returnVal = JPO.invoke(context, "SPLM_Integration_JPO", null, "searchPartGroup", args, String.class);
		return returnVal;
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
		String returnVal = JPO.invoke(context, "SPLM_Integration_JPO", null, "searchGroupPnc", args, String.class);
		return returnVal;
	}
	
	/**
	 * Part All
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchPartAll(Context context, String[] args) throws Exception {
		String returnVal = JPO.invoke(context, "SPLM_Integration_JPO", null, "searchPartAll", args, String.class);
		return returnVal;
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
		String returnVal = JPO.invoke(context, "SPLM_Integration_JPO", null, "searchMcGroupCode", args, String.class);
		return returnVal;
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
		String returnVal = JPO.invoke(context, "SPLM_Integration_JPO", null, "searchMsPartCategory", args, String.class);
		return returnVal;
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
		String returnVal = JPO.invoke(context, "SPLM_Integration_JPO", null, "searchPartsCatelogueMc", args, String.class);
		return returnVal;
	}

	/**
	 * 13. PartModel
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String searchPartModel(Context context, String[] args) throws Exception {
		String returnVal = JPO.invoke(context, "SPLM_Integration_JPO", null, "searchPartModel", args, String.class);
		return returnVal;
	}
}