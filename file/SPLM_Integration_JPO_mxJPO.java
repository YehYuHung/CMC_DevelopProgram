import static org.junit.Assert.assertNotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import com.google.gson.Gson;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.Context;
import matrix.util.StringList;

public class SPLM_Integration_JPO_mxJPO {
	public String searchGroupCode(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		busSelects.add("attribute[SPLM_Name_TC]");
		busSelects.add("attribute[SPLM_Name_EN]");

		MapList resultList = DomainObject.findObjects(context, "SPLM_GroupCode", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, Short.parseShort("0"));

		ArrayList<GroupCode> groupCodeArray = new ArrayList<GroupCode>();
		for (Object object : resultList) {
			Map map = (Map) object;
			GroupCode groupCode = new GroupCode();
			groupCode.GROUP_NAME_TC = (String) map.get("attribute[SPLM_Name_TC]");
			groupCode.GROUP_NAME_EN = (String) map.get("attribute[SPLM_Name_EN]");
			groupCode.GROUP_CODE 	= (String) map.get(DomainConstants.SELECT_NAME);
			groupCode.CHANGE_DATE 	= sdf.format(new Date((String) map.get(DomainConstants.SELECT_MODIFIED)));

			groupCodeArray.add(groupCode);
		}
		return gson.toJson(groupCodeArray);
	}

	public class GroupCode {
		public String GROUP_CODE;
		public String GROUP_NAME_TC;
		public String GROUP_NAME_EN;
		public String CHANGE_DATE;
	}

	public String searchPnc(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		busSelects.add("attribute[SPLM_Name_TC]");
		busSelects.add("attribute[SPLM_Name_EN]");
		busSelects.add("attribute[SPLM_PNC_Type]");
//		busSelects.add("attribute[SPLM_PNC_From]");
//		busSelects.add("attribute[SPLM_PNC_CodeType]");

		MapList resultList = DomainObject.findObjects(context, "SPLM_PNC", // type
				"*", // name
				"-", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, Short.parseShort("0"));

		ArrayList<PNC> pncArray = new ArrayList<PNC>();
		for (Object object : resultList) {
			Map map = (Map) object;
			PNC pnc = new PNC();
			pnc.PNC_NAME_TC 	= (String) map.get("attribute[SPLM_Name_TC]");
			pnc.PNC_NAME_EN 	= (String) map.get("attribute[SPLM_Name_EN]");
			pnc.PNC_From 		= (String) map.get("attribute[SPLM_PNC_From]");
//			part.PNC_TYPE  		= (String)map.get("attribute[SPLM_PNC_Type]");
//			part.PNC_CodeType 	= (String)map.get("attribute[SPLM_PNC_CodeType]");
			pnc.PNC_NO 			= (String) map.get(DomainConstants.SELECT_NAME);
			pnc.CHANGE_DATE 	= sdf.format(new Date((String) map.get(DomainConstants.SELECT_MODIFIED)));

			pncArray.add(pnc);
		}
		return gson.toJson(pncArray);
	}

	public class PNC {
		public String PNC_NO;
		public String PNC_NAME_EN;
		public String PNC_NAME_TC;
		public String PNC_From;
		public String PNC_TYPE;
		public String PNC_CodeType;
		public String CHANGE_DATE;
	}

	public String searchVenderAlter(Context context, String[] args) throws Exception {
		Gson gson = new Gson();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		busSelects.add("attribute[SPLM_Vendor]");
		busSelects.add("attribute[SPLM_SO_Note]");

		MapList resultList = DomainObject.findObjects(context, "SPLM_Part", // type
				"*", // name
				"-", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, Short.parseShort("0"));

		ArrayList<VenderAlter> venderAlterArray = new ArrayList<VenderAlter>();
		DomainObject domObj = new DomainObject();
		for (Object object : resultList) {
			Map map = (Map) object;
			VenderAlter venderAlter = new VenderAlter();

			domObj.setId((String) map.get(DomainConstants.SELECT_ID));
			venderAlter.VENDOR_NO 	= domObj.getInfoList(context, "attribute[SPLM_Vendor]");
			venderAlter.PART_NO 	= (String) map.get(DomainConstants.SELECT_NAME);
			venderAlter.SO_NO 		= (String) map.get("attribute[SPLM_SO_Note]");
			venderAlter.CHANGE_DATE = sdf.format(new Date((String) map.get(DomainConstants.SELECT_MODIFIED)));

			venderAlterArray.add(venderAlter);
		}
		return gson.toJson(venderAlterArray);
	}

	public class VenderAlter {
		public String PART_NO;
		public StringList VENDOR_NO;
		public String SO_NO;
		public String CHANGE_DATE;
	}

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
				"-", // revision
				"*", // owner
				"eService Production", // vault
				"", // where
				null, false, busSelects, Short.parseShort("0"));

		ArrayList<PartModel> partModelArray = new ArrayList<PartModel>();
		DomainObject domObj = new DomainObject();
		for (Object object : resultList) {
			Map map = (Map) object;
			PartModel partModel = new PartModel();

			domObj.setId((String) map.get(DomainConstants.SELECT_ID));
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

	public class Part {
		public String PART_NO;
		public String PART_SH_NO;
	}

}
