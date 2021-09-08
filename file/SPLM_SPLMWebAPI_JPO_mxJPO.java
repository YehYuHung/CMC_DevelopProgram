import java.util.ArrayList;
import java.util.Map;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.google.gson.Gson;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

public class SPLM_SPLMWebAPI_JPO_mxJPO  {
	
	public String test(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		Map<String, String> paramsMap = JPO.unpackArgs(args);
		Test test = gson.fromJson(paramsMap.get("json"), Test.class);  // parse Json obj to class
		
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
				"owner == ' "+ test.owner+" ' ", // where
				null, true, busSelects, (short) 0);	

//		for (Object object : personList) {
//			Map map = (Map) object;
//			String tc = (String)map.get("attribute[SPLM_Name_TC]");
//			map.remove("attribute[SPLM_Name_TC]");
//			map.put("TC", tc);
//		}
		
//		String sObjName = (String) map.get(DomainConstants.SELECT_NAME);
//		System.out.println("sObjName:" + sObjName + " sObjID:" + sObjID);

		return gson.toJson(personList);
	}	
		
	public class Test {
		public String name;
		public String owner;
		public ArrayList<String> abc;
		public String attributeString;
	}
	
	public class Part{
		public String name;
		public String owner;
		public String identity;
		public String nameTC;
	}
}