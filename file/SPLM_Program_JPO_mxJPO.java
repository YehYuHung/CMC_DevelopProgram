import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.xml.rpc.holders.StringHolder;

import org.apache.commons.lang3.StringUtils;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.fcs.common.ImageRequestData;
import com.matrixone.json.JSONArray;
import com.matrixone.json.JSONObject;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObjectProxy;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipQuery;
import matrix.db.RelationshipQueryIterator;
import matrix.db.RelationshipWithSelect;
import matrix.util.Pattern;
import matrix.util.StringList;
import mc_style.functions.soap.sap.document.sap_com.YPP16_ServiceLocator;
import mc_style.functions.soap.sap.document.sap_com.YQM_SPLM_GET_ISIR_DOC_ServiceLocator;


/**
 * Test Des
 * @author Administrator
 *
 */
public class SPLM_Program_JPO_mxJPO {
	private static final String PRODUCTION_VAULT = "eService Production";
	private static final String ERP_YPP16_300 = "ERP.YPP16.300";
	private static final String ERP_CAQS = "ERP.CAQS";
	private static final String SPLIT_KEY = "~QWE~";
	
	public MapList getMyCheckServicePart(Context context, String[] args) throws Exception {
		String user = context.getUser();
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_PLVC,SPLM_Part,SPLM_ColorPart", "*",
				"*", "*", PRODUCTION_VAULT, "owner=='" + user + "' && attribute[SPLM_CheckStatus]=='CHECKED'", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		
		this.buildServicePartViewTableMapList(context, objectList);
		
		return objectList;
	}
	
	public MapList getMyNotCheckServicePart(Context context, String[] args) throws Exception {
		String user = context.getUser();
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		
		MapList objectList = DomainObject.findObjects(context, "SPLM_PLVC,SPLM_Part,SPLM_ColorPart", "*",
				"*", "*", PRODUCTION_VAULT, "owner=='" + user + "' && attribute[SPLM_CheckStatus]!='CHECKED'", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		
		this.buildServicePartViewTableMapList(context, objectList);
		
		return objectList;
	}
	
	private StringList getServicePartViewBusSelect() {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_TYPE);
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_OWNER);
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		busSelects.add("attribute[SPLM_Name_TC]");
		busSelects.add("attribute[SPLM_Name_EN]");
		busSelects.add("attribute[SPLM_IsServicePart]");
		busSelects.add("attribute[SPLM_ModelSeries]");
		busSelects.add("attribute[SPLM_MaterialGroup]");
		busSelects.add("attribute[SPLM_MaterialType]");
		busSelects.add("attribute[SPLM_Plant]");
		busSelects.add("attribute[SPLM_PurchasingGroup]");
		busSelects.add("attribute[SPLM_ItemSubType]");
		busSelects.add("attribute[SPLM_SO_Note]");
		busSelects.add("attribute[SPLM_PB_Note]");
		busSelects.add("attribute[SPLM_OverseaSalesOnly]");
		busSelects.add("attribute[SPLM_DTAT_Only]");
		busSelects.add("attribute[SPLM_HaveSubPart]");
		busSelects.add("attribute[SPLM_ByGLNO]");
		busSelects.add("attribute[SPLM_CheckStatus]");
		busSelects.add("attribute[SPLM_OldPartNo]");
		busSelects.add("attribute[SPLM_InitialParentPartNo]");
		busSelects.add("attribute[SPLM_ChangeChildPartNo]");
		return busSelects;
	}
	
	private void buildServicePartViewTableMapList(Context context, MapList bomList) throws Exception {
		StringList busSelects = this.getServicePartViewBusSelect();
		Map bomMap;
		DomainObject obj = new DomainObject();
		for (Object object : bomList) {
			bomMap = (Map) object;
			obj.setId((String) bomMap.get(DomainConstants.SELECT_ID));
			bomMap.putAll(obj.getInfo(context, busSelects));
		}
	}
	
	public HashMap<String, String> setServicePartCheck(Context context, String[] args) throws Exception {
		HashMap<String, String> returnMap = new HashMap<String, String>();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String[] emxTableRowIds = (String[]) programMap.get("emxTableRowIds");
		DomainObject partObj = new DomainObject();
		String partId;
		String msg = "";
		try {
			for (String rowIds : emxTableRowIds) {
				partId = rowIds.split("\\|")[1];
				partObj.setId(partId);
				partObj.setAttributeValue(context, "SPLM_CheckStatus", "CHECKED");
			}
		} catch (Exception e) {
			msg = e.getMessage();
		}
		if (StringUtils.isNotEmpty(msg)) {
			returnMap.put("Action", "stop");
			returnMap.put("Message", msg);
		}
		return returnMap;
	}
	
	public MapList getMyActivePartsCatalogue(Context context, String[] args) throws Exception {
		return this.getMyPartsCatalogue(context, "Active");
	}
	
	public MapList getMyInActivePartsCatalogue(Context context, String[] args) throws Exception {
		return this.getMyPartsCatalogue(context, "InActive");
	}
	
	private MapList getMyPartsCatalogue(Context context, String current) throws Exception {
		String user = context.getUser();
		
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_PartsCatalogue", "*",
				"*", user, PRODUCTION_VAULT, "current=='" + current + "'", null, false, busSelects,
				Short.parseShort("0"));
		
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getMyDocument(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_DOCUMENTS,SPLM_GroupDrawing", "*",
				"*", context.getUser(), PRODUCTION_VAULT, "policy!=Version", null, true, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getShowServiceModelSeries(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_ModelSeries", "*",
				"*", "*", PRODUCTION_VAULT, "attribute[CMC_Show]==true", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getAllServiceModelSeries(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_ModelSeries", "*",
				"*", "*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getAllPNC(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_PNC", "*",
				"*", "*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getAllGroupCode(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_GroupCode", "*",
				"*", "*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getNotEditCompleteGroupDrawing(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_GroupDrawing", "*",
				"*", "*", PRODUCTION_VAULT, "attribute[SPLM_ScanStatus]!='EditCompleted'", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getEditCompleteGroupDrawing(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_GroupDrawing", "*",
				"*", "*", PRODUCTION_VAULT, "attribute[SPLM_ScanStatus]=='EditCompleted'", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getAllAbbrev(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_Abbrev", "*",
				"*", "*", PRODUCTION_VAULT, "attribute[Is Version Object]==False", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getAllEPC_PIC(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_EPC_PIC", "*",
				"*", "*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getAllMaterialGroup(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_MaterialGroup", "*",
				"*", "*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getAllMaterialType(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_MaterialType", "*",
				"*", "*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getAllVendor(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_Vendor", "*",
				"*", "*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getAllSendDept(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_SendDept", "*",
				"*", "*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getAllDealer(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_Dealer", "*",
				"*", "*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
	
	public MapList getGroupDrawingImage(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "Image",
				"SPLM_GroupDrawingImage", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedPNC(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_PNC",
				"SPLM_Related_PNC", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedGroupDrawing(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_GroupDrawing",
				"SPLM_RelatedGroupDrawing", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getApplicableModelCode(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_PartsCatalogue",
				"SPLM_RelatedGroupDrawing", null, null, "to", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedSpec(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "*",
				"SPLM_PartSpecification", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getReferenceDoc(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "*",
				"SPLM_ReferenceDoc", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getReferenceImage(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "*",
				"SPLM_ReferenceImage", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedLargeGroupCode(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_GroupCode",
				"SPLM_RelatedLargeGroupCode", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedPart(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Part",
				"SPLM_RelatedPart", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedPLVC(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_PLVC",
				"SPLM_PartSpecification", null, null, "to", (short) 1, null, null);
		
		this.buildServicePartViewTableMapList(context, objectList);
		
		return objectList;
	}
	
	public MapList getDocumentRelatedPart(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_ServicePart",
				"SPLM_ReferenceDoc,SPLM_PartSpecification", null, null, "to", (short) 1, null, null);
		
		this.buildServicePartViewTableMapList(context, objectList);
		
		return objectList;
	}
	
	public MapList getRelatedVendor(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Vendor",
				"SPLM_RelatedVendor", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedOptionalPart(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Part",
				"SPLM_RelatedOptionalPart", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	
	
	public MapList getRelatedAbbrev(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Abbrev",
				"SPLM_RelatedAbbrev", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedCatalogue(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Catalogue",
				"SPLM_RelatedCatalogue", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedPartsCataloguePDF(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_PartsCataloguePDF",
				"SPLM_RelatedPartsCataloguePDF", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	public MapList getRelatedSOPDF(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_SOPDF",
				"SPLM_RelatedSOPDF", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	public MapList getRelatedImportPart(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Part",
				"SPLM_RelatedImportPart", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedEPC_PIC(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_EPC_PIC",
				"SPLM_Related_EPC_PIC", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedDealer(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Dealer",
				"SPLM_RelatedDealer", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedPartsCatalogue(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_PartsCatalogue",
				"SPLM_RelatedPartsCatalogue", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	
	
	public MapList getEPC_PIC_Image(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "Image",
				"SPLM_EPC_PIC_Image", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	private StringList getSBOMBusSelect() {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_TYPE);
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_OWNER);
		busSelects.add(DomainConstants.SELECT_MODIFIED);
		busSelects.add(DomainConstants.SELECT_DESCRIPTION);
		busSelects.add("attribute[SPLM_Name_TC]");
		busSelects.add("attribute[SPLM_Name_EN]");
		busSelects.add("attribute[SPLM_IsServicePart]");
		busSelects.add("attribute[SPLM_ItemSubType]");
		return busSelects;
	}
	
	private StringList getSBOMRelSelect() {
		StringList relSelects = new StringList();
		relSelects.add(DomainRelationship.SELECT_LEVEL);
		relSelects.add(DomainRelationship.SELECT_ID);
		relSelects.add("attribute[Quantity]");
		relSelects.add("attribute[SPLM_ERP_Quantity]");
		relSelects.add("attribute[SPLM_SerialNumber]");
		relSelects.add("attribute[SPLM_EnableDate]");
		relSelects.add("attribute[SPLM_DisableDate]");
		relSelects.add("attribute[SPLM_GroupCode]");
		relSelects.add("attribute[SPLM_PNC]");
		relSelects.add("attribute[SPLM_UPG]");
		relSelects.add("attribute[SPLM_RelatedEO]");
		relSelects.add("attribute[SPLM_RelatedPO]");
		relSelects.add("attribute[SPLM_SortSubPart]");
		relSelects.add("attribute[SPLM_Materiel_KD_Type]");
		relSelects.add("attribute[SPLM_Materiel]");
		relSelects.add("attribute[SPLM_BOM_Note]");
		relSelects.add("attribute[SPLM_ChangeNo]");
		relSelects.add("attribute[SPLM_ChangeNoReleaseDate]");
		relSelects.add("attribute[SPLM_ChangeNoName]");
		relSelects.add("attribute[Find Number]");
		relSelects.add("attribute[SPLM_Quantity_ID]");
		return relSelects;
	}
	
	public MapList expandSBOM(Context context, String[] args)
			throws Exception {
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		String enableDateInMillis = (String) paramMap.get("SPLM_EnableDateTextBox_msvalue");
		String servicePartDest = (String) paramMap.get("SPLM_ServicePartDest");
		servicePartDest = StringUtils.isEmpty(servicePartDest) ? "-" : servicePartDest;
		
		String busWhere = "(type=='SPLM_PLVC' && revision=='" + servicePartDest + "') || (type!='SPLM_PLVC' && revision=='-')";
		String relWhere = "";
		if (StringUtils.isNotEmpty(enableDateInMillis)) {
			Long dateInMillis = Long.parseLong(enableDateInMillis);
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(dateInMillis);
			String enableDate = new SimpleDateFormat("MM/dd/yyyy").format(calendar.getTime()) + " 12:00:00 AM";
			relWhere = "attribute[SPLM_EnableDate] >= '" + enableDate + "'";
		}
		
		String objectId = (String) paramMap.get("objectId");
		String expandRel = (String) paramMap.get("relType");
		String sExpandLevels = (String) paramMap.get("emxExpandFilter");
		int expandLevel = "All".equals(sExpandLevels) ? 0 : Integer
				.parseInt(sExpandLevels);
		DomainObject partObj = new DomainObject(objectId);
		
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		
		StringList relSelects = new StringList();
		relSelects.add(DomainRelationship.SELECT_LEVEL);
		relSelects.add(DomainRelationship.SELECT_ID);
				
		MapList bomList = partObj.getRelatedObjects(context,
				expandRel, "SPLM_ServicePart", busSelects, relSelects,
				false,
				true,
				(short) expandLevel, busWhere, relWhere, 0);
		this.buildSBOMTableMapList(context, bomList);
		
		return bomList;
	}
	
	public HashMap getLevelFilterForServicePartWhereUsed(Context context, String[] args)
			throws Exception {
		String resource = "emxEngineeringCentralStringResource";
		String language = context.getSession().getLanguage();
		String[] levelOptions = {
				"emxEngineeringCentral.Part.WhereUsedLevelUpTo",
				"emxEngineeringCentral.Part.WhereUsedLevelAll",
				"emxEngineeringCentral.Part.WhereUsedLevelHighest"};

		HashMap levelMap = new HashMap(2);
		
		StringList valueList = new StringList();
		StringList displayList = new StringList();
		
		for (String levelOption : levelOptions) {
			valueList.add(EnoviaResourceBundle.getProperty(context, resource,
					new Locale("en"), levelOption));
			displayList.add(EnoviaResourceBundle.getProperty(context, resource,
					new Locale(language), levelOption));
		}
		
		levelMap.put("field_choices", valueList);
		levelMap.put("field_display_choices", displayList);

		return levelMap;
	}
	
	public MapList getServicePartWhereUsed(Context context, String[] args)
			throws Exception {
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) paramMap.get("objectId");
		String levelType = (String) paramMap.get("SPLM_ServicePartWhereUsedLevel");
		String level = (String) paramMap.get("SPLM_ServicePartWhereUsedLevelTextBox");
		
		short expandLevel;
		if ("All".equals(levelType)) {
			expandLevel = 0;
		} else if ("Highest".equals(levelType)) {
			expandLevel = -1;
		} else {
			try {
				expandLevel = Short.parseShort(level);
			} catch (NumberFormatException e) {
				expandLevel = 1;
			}
		}
		
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		
		StringList relSelects = new StringList();
		relSelects.add(DomainRelationship.SELECT_LEVEL);
		relSelects.add(DomainRelationship.SELECT_ID);
		
		DomainObject partObj = new DomainObject(objectId);
		
		MapList whereUsedList = partObj.getRelatedObjects(context,
				"SPLM_SBOM,SPLM_MBOM", "SPLM_ServicePart", busSelects, relSelects,
				true,
				false,
				(short) expandLevel, "", "", 0);
		
		this.buildServicePartWhereUsedTableMapList(context, whereUsedList);
		
		return whereUsedList;
	}
	
	private void buildServicePartWhereUsedTableMapList(Context context, MapList bomList) throws Exception {
		StringList busSelects = this.getSBOMBusSelect();
		Map<String, String> bomMap, relAttMap;
		DomainObject obj = new DomainObject();
		DomainRelationship relObj;
		String level;
		for (Object object : bomList) {
			bomMap = (Map) object;
			level = bomMap.get(DomainRelationship.SELECT_LEVEL);
			obj.setId(bomMap.get(DomainConstants.SELECT_ID));
			relObj = new DomainRelationship(bomMap.get(DomainRelationship.SELECT_ID));
			relAttMap = relObj.getAttributeMap(context);
			for (Entry<String, String> entry : relAttMap.entrySet()) {
				bomMap.put("attribute[" + entry.getKey() + "]", entry.getValue());	
			}
			bomMap.putAll(obj.getInfo(context, busSelects));
			bomMap.put("WhereUsedLevel", "-" + level);
		}
	}
	
	private void buildSBOMTableMapList(Context context, MapList bomList) throws Exception {
		StringList busSelects = this.getSBOMBusSelect();
		Map<String, String> bomMap, relAttMap;
		DomainObject obj = new DomainObject();
		DomainRelationship relObj;
		for (Object object : bomList) {
			bomMap = (Map) object;
			obj.setId(bomMap.get(DomainConstants.SELECT_ID));
			relObj = new DomainRelationship(bomMap.get(DomainRelationship.SELECT_ID));
			relAttMap = relObj.getAttributeMap(context);
			for (Entry<String, String> entry : relAttMap.entrySet()) {
				bomMap.put("attribute[" + entry.getKey() + "]", entry.getValue());	
			}
			bomMap.putAll(obj.getInfo(context, busSelects));
		}
	}
	
	
	public HashMap getMaterialTypeRange(Context context, String[] args)
			throws Exception {
		HashMap viewMap = new HashMap();
		
		StringList displayList = new StringList();
		StringList valueList = new StringList();
		
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_DESCRIPTION);
		busSelects.addElement("attribute[CMC_Sort]");
		
		MapList rangeObjList = DomainObject.findObjects(context, "SPLM_MaterialType", "*", "*",
				"*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		rangeObjList.addSortKey("attribute[CMC_Sort]", "ascending", "real");
		rangeObjList.sort();
		
		Map info;
		for (Object temp : rangeObjList) {
			info = (Map) temp;
			valueList.add((String) info.get(DomainConstants.SELECT_NAME));
			displayList.add((String) info.get(DomainConstants.SELECT_DESCRIPTION));
		}
		viewMap.put("field_choices", valueList);
		viewMap.put("field_display_choices", displayList);
		return viewMap;
	}
	
	public HashMap getMaterialGroupRange(Context context, String[] args)
			throws Exception {
		HashMap viewMap = new HashMap();
		
		StringList displayList = new StringList();
		StringList valueList = new StringList();
		
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_DESCRIPTION);
		busSelects.addElement("attribute[CMC_Sort]");
		
		MapList rangeObjList = DomainObject.findObjects(context, "SPLM_MaterialGroup", "*", "*",
				"*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		rangeObjList.addSortKey("attribute[CMC_Sort]", "ascending", "real");
		rangeObjList.sort();
		
		Map info;
		for (Object temp : rangeObjList) {
			info = (Map) temp;
			valueList.add((String) info.get(DomainConstants.SELECT_NAME));
			displayList.add((String) info.get(DomainConstants.SELECT_DESCRIPTION));
		}
		viewMap.put("field_choices", valueList);
		viewMap.put("field_display_choices", displayList);
		return viewMap;
	}
	
	public StringBuffer updateBOMInView(Context context, String [] args) throws Exception {
		Map programMap = JPO.unpackArgs(args);
		String tableMode = (String) programMap.get("TableMode");
		String relType = (String) programMap.get("RelType");
		String parentId = (String) programMap.get("ParentObjId");
		StringList childIdList = (StringList) programMap.get("ChildObjIds");
		StringBuffer sbUIChangeXMLAdd = new StringBuffer();

		sbUIChangeXMLAdd.append(" <mxRoot>");
		for (String childId : childIdList) {
			if (parentId.equals(childId)) {
				throw new Exception("\u7121\u6cd5\u5c07\u81ea\u8eab\u4f5c\u70ba\u5b50\u968e\u6dfb\u52a0");
			}
			if ("view".equalsIgnoreCase(tableMode)) {
				DomainRelationship relObj = DomainRelationship.connect(context, new DomainObject(parentId), relType, new DomainObject(childId));
				Map relInfo = relObj.getRelationshipData(context, new StringList(DomainRelationship.SELECT_ID));
				StringList relInfoList = (StringList) relInfo.get(DomainRelationship.SELECT_ID);
				String relId = relInfoList.get(0);
				
				sbUIChangeXMLAdd.append("<action>add</action>");
				sbUIChangeXMLAdd.append("<data status=\"committed\">");
				sbUIChangeXMLAdd.append("<item ");
				sbUIChangeXMLAdd.append(" oid=\"").append(childId).append("\"");
				sbUIChangeXMLAdd.append(" relType=\"").append(relType).append("\"");
				sbUIChangeXMLAdd.append(" relId=\"").append(relId).append("\"");
				sbUIChangeXMLAdd.append(" pid=\"").append(parentId).append("\"");
				sbUIChangeXMLAdd.append(" direction=\"").append("").append("\"");
				sbUIChangeXMLAdd.append(">");
				sbUIChangeXMLAdd.append("</item>");
				sbUIChangeXMLAdd.append(" </data>");
			} else {
				sbUIChangeXMLAdd.append("<action>add</action>");
				sbUIChangeXMLAdd.append("<data status=\"pending\">");
				sbUIChangeXMLAdd.append("<item ");
				sbUIChangeXMLAdd.append(" oid=\"").append(childId).append("\"");
				sbUIChangeXMLAdd.append(" relType=\"").append("relationship_" + relType).append("\"");
				sbUIChangeXMLAdd.append(" pid=\"").append(parentId).append("\"");
				sbUIChangeXMLAdd.append(">");
				sbUIChangeXMLAdd.append("<column name=\"Quantity\" edited=\"true\">1.0</column>");
				sbUIChangeXMLAdd.append("<column name=\"RelType\" edited=\"false\" a=\"" + relType + "\">" + relType.substring(5, 6) + " BOM" + "</column>");
				sbUIChangeXMLAdd.append("</item>");
				sbUIChangeXMLAdd.append(" </data>");
			}
		}
		sbUIChangeXMLAdd.append("</mxRoot>");

		return sbUIChangeXMLAdd;
	}
	
	public Vector getImageFile(Context context, String[] args) throws Exception
    {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		Map columnMap = (Map) programMap.get("columnMap");
		Map settings = (Map) columnMap.get("settings");
		String singleImageRelType = (String) settings.get("Single Image Rel Type");
		MapList mlObjectList = (MapList) programMap.get("objectList");
		Vector vectImage = new Vector(mlObjectList.size());
		ArrayList arraylist = new ArrayList();
		BusinessObjectProxy businessobjectproxy;
		StringList imageURLs = new StringList();
		StringList imageIds = new StringList();
		HashMap requestMap = (HashMap) programMap.get("paramList");
		HashMap imageData = (HashMap) requestMap.get("ImageData");
		
		String strFormat = DomainObject.FORMAT_MX_MEDIUM_IMAGE;
		for (int i = 0; i < mlObjectList.size(); i++)
		{
			String strImageId = (String)((Map) mlObjectList.get(i)).get(DomainConstants.SELECT_ID);
			String strFile = "";
			DomainObject domImage = DomainObject.newInstance(context, strImageId);
			if (StringUtils.isNotEmpty(singleImageRelType)) {
				StringList imgList = domImage.getInfoList(context, "from[" + singleImageRelType + "].to.id");
				if (!imgList.isEmpty()) {
					strImageId = imgList.get(0);
				}
			}
			domImage.setId(strImageId);
			strFile = domImage.getInfo(context, "format[" + strFormat + "].file.name");
	
			if ( strFile != null && !"".equals(strFile) && !"null".equals(strFile) )
			{
			    businessobjectproxy = new BusinessObjectProxy(strImageId, strFormat, strFile, false, false);
			    arraylist.add(businessobjectproxy);
			    imageURLs.add(strFile);
			    imageIds.add(strImageId);
			} else {
				imageURLs.add("&nbsp;");
		        imageIds.add(strImageId);
		    }
		}

		String[] strImageURLs = ImageRequestData.getImageURLS(context, arraylist, imageData);
		String imageURL;
		int count = 0;
		Iterator imageItr = imageURLs.iterator();
		int index = 0;
		while( imageItr.hasNext())
		{
		    imageURL = (String) imageItr.next();
		    if (!imageURL.equalsIgnoreCase("&nbsp;") ) {
				if( strImageURLs.length > count )
				{
				    String strFile = imageURL;
				    imageURL = strImageURLs[count];
				    count ++;
				    String url = "javascript:emxTableColumnLinkClick('../JCI/SPLM_ShowPic.jsp?objectId=" + imageIds.get(index) + "',850,650,'true','popup', '', '', '','Large')";
				    vectImage.add("<a href=\"" + url + "\"><img border='0' align='absmiddle' src='" + XSSUtil.encodeForHTMLAttribute(context,imageURL) + "' title='"+XSSUtil.encodeForHTMLAttribute(context, strFile)+"'/></a>");
				} else {
					vectImage.add("");
			    }
			} else {
			    vectImage.add("");
		    }
		    index++;
		}
		return  vectImage;
    }
	public boolean isServicePart(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String type = (String) programMap.get("type");
		if (StringUtils.isEmpty(type) || type.indexOf("_selectedType:") < 0) {
			return true;
		}
		String selectedType = type.substring(type.indexOf("_selectedType:"), type.indexOf(",")).replace("_selectedType:", "");
		return "SPLM_Part".equals(selectedType);
	}

	public boolean isCloneServicePart(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String isCloneServicePart = (String) programMap.get("isCloneServicePart");
		if (StringUtils.isEmpty(isCloneServicePart)) {
			return false;
		}
		return "true".equals(isCloneServicePart);
	}
	
	private final static String[] topRange = { "", "11", "51", "K1", "J", "4", "52", "V", "12" };

	private final static String[] bottomRange = { "", "O", "U", "P1", "P2", "P3", "P4", "P5", "W1", "W2", "W3", "R",
			"N1", "13", "14", "S1", "G1", "2", "9", "A", "C", "Q", "S2", "15", "N2", "16", "H", "17", "J" };
	private final static String[] bottomSuffixRange = { "", "-S", "-T", "-1",
			"-3", "-C1", "-C2", "-C3", "-9", "-W", "-CT", "-SR", "-Q", "Z",
			"-RC", "-PP", "-PD", "SV", "P", "ZZ", "T", "-BP", "-SM", "-DC", "",
			"-AC", "X", "Y", "" };
	private final static String[] bottomPrefixRange = { "", "", "", "", "", "",
			"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
			"", "QWSQ", "", "", "", "" };
	private final static String[] suffixCountItem = { "O", "U" };

	public HashMap getItemSubTypeRange(Context context, String[] args)
			throws Exception {
		String language = context.getSession().getLanguage();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");
		String isCloneServicePart = (String) requestMap.get("isCloneServicePart");
		
		HashMap viewMap = new HashMap();
		StringList strListStateDisplay = new StringList();
		StringList strListStateValue = new StringList();
		if (StringUtils.isEmpty(isCloneServicePart) || !"true".equals(isCloneServicePart)) {
			strListStateValue.addAll(topRange);
		} else {
			strListStateValue.addAll(bottomRange);
		}

		for (String value : strListStateValue) {
			strListStateDisplay.add(i18nNow.getRangeI18NString("SPLM_ItemSubType", value, language));
		}
		
		viewMap.put("field_choices", strListStateValue);
		viewMap.put("field_display_choices", strListStateDisplay);
		return viewMap;
	}
	
	public String getParentName(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");
		String bomObjectId = (String) requestMap.get("bomObjectId");
		String isCloneServicePart = (String) requestMap.get("isCloneServicePart");
		
		if (StringUtils.isEmpty(isCloneServicePart) || !"true".equals(isCloneServicePart)) {
			return "";
		}
		if (StringUtils.isEmpty(bomObjectId)) {
			return "";
		}
		String objName = new DomainObject(bomObjectId).getInfo(context, DomainConstants.SELECT_NAME);
		return objName;
	}
	
	public Map reloadCloneServicePartNo(Context context, String[] args)
			throws Exception {
		String name = this.getParentName(context, args);
		StringList bottomRangeList = new StringList(bottomRange);
		
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		Map fieldValues   = (Map) programMap.get("fieldValues");
		
		String itemSubType = (String) fieldValues.get("SPLM_ItemSubType");
		int index = bottomRangeList.indexOf(itemSubType);
		name = bottomPrefixRange[index] + name + bottomSuffixRange[index];
		List<String> suffixCountItemList = Arrays.asList(suffixCountItem);
		if (suffixCountItemList.contains(itemSubType)) {
			MapList objectList = DomainObject.findObjects(context, "SPLM_Part", "*",
					"*", "*", PRODUCTION_VAULT, "name ~~ '" + name + "*'", null, false, new StringList(DomainConstants.SELECT_NAME),
					Short.parseShort("0"));
			int nameCount = objectList.size() + 1;
			if (nameCount > 0) {
				name += nameCount;
			}
		}
		Map resultMap = new HashMap();
		resultMap.put("SelectedValues", name);
		resultMap.put("SelectedDisplayValues", name);
		return resultMap;
	}
	
	public HashMap<String, String> connectRelatedPart (
			Context context, String[] args) throws Exception {
		HashMap<String, String> returnMap = new HashMap<String, String>();
		
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		
		try {
			DomainObject fromPartObj = new DomainObject(objectId);
			
			String fromPartType = fromPartObj.getInfo(context, DomainConstants.SELECT_TYPE);
			if (!"SPLM_PLVC,SPLM_Part,SPLM_ColorPart".contains(fromPartType)) {
				throw new Exception("\u8acb\u5728PLVC/\u96f6\u4ef6\u756b\u9762\u4e0b\u64cd\u4f5c");
			}
			
			MapList bomList = fromPartObj.getRelatedObjects(context, "SPLM_SBOM,SPLM_MBOM",
					"*", 
					new StringList(DomainConstants.SELECT_ID), 
					new StringList("physicalid"), 
					false,
					true, (short) 1, "", "", (short) 0);
			
			if (bomList.isEmpty()) {
				throw new Exception("\u7f3a\u5c11BOM\u8cc7\u8a0a");
			}
			
			String toPartObjId, relPhysicalId;
			DomainRelationship relObj;
			StringList relSelects = new StringList();
			relSelects.add("SPLM_EnableDate");
			relSelects.add("SPLM_DisableDate");
			relSelects.add("Quantity");
			relSelects.add("SPLM_GroupDrawing");
			relSelects.add("SPLM_GroupCode");
			relSelects.add("SPLM_PNC");
			relSelects.add("SPLM_Quantity_ID");
			
			StringList relIdSelect = new StringList("physicalid");
			Map relInfo; 
			StringList relInfoList;
			AttributeList attList;
			Map<String, String> relAttMap;
			StringList alertList = new StringList();
			
			DomainObject toPartObj = new DomainObject();
			for(Object objInfo : bomList) {
				toPartObjId = (String) ((Map) objInfo).get(DomainConstants.SELECT_ID);
				toPartObj.setId(toPartObjId);	
				
				relPhysicalId = (String) ((Map) objInfo).get("physicalid");
				relObj = new DomainRelationship(relPhysicalId);
				
				attList = relObj.getAttributeValues(context, relSelects);
				relAttMap = new HashMap<String, String>();
				relAttMap.put("SPLM_CreateFromBOM", relPhysicalId);
				for (Attribute attribute : attList) {
					relAttMap.put(attribute.getName(), attribute.getValue());
				}
				
				String groupCode = relAttMap.get("SPLM_GroupCode");
				String groupDrawingNo = relAttMap.get("SPLM_GroupDrawing");
				String PNC = relAttMap.get("SPLM_PNC");
				
				if (!StringUtils.isEmpty(PNC)) {
					if ("SPLM_Part,SPLM_ColorPart".contains(toPartObj.getInfo(context, DomainConstants.SELECT_TYPE))) {
						if (StringUtils.isEmpty(toPartObj.getAttributeValue(context, "SPLM_PNC"))) {
							toPartObj.setAttributeValue(context, "SPLM_PNC", PNC);
						}
					}
				}
				
				if (!StringUtils.isEmpty(groupCode)) {
					if ("SPLM_Part,SPLM_ColorPart".contains(toPartObj.getInfo(context, DomainConstants.SELECT_TYPE))) {
						if (StringUtils.isEmpty(toPartObj.getAttributeValue(context, "SPLM_GroupCode"))) {
							toPartObj.setAttributeValue(context, "SPLM_GroupCode", groupCode);
						}
					}
				}
				
				if (StringUtils.isEmpty(groupCode) 
						|| StringUtils.isEmpty(groupDrawingNo)
						|| StringUtils.isEmpty(PNC)) {
					
					this.disconnectRelatedPart(context, toPartObjId, relPhysicalId);
					
					continue;
				}
				
				StringList plvcList = new StringList();
				
				if ("SPLM_PLVC".equals(fromPartType)) {
					plvcList.add(objectId);
				} else {
					StringList mbomPLVCList =  fromPartObj.getInfoList(context, "to[SPLM_MBOM].from[SPLM_PLVC].id");
					StringList sbomPLVCList =  fromPartObj.getInfoList(context, "to[SPLM_SBOM].from[SPLM_PLVC].id");
					StringList bomPLVCList = new StringList();
					bomPLVCList.addAll(mbomPLVCList);
					bomPLVCList.addAll(sbomPLVCList);
					
					for (String plvcId : bomPLVCList) {
						if (plvcList.contains(plvcId)) {
							continue;
						}
						plvcList.add(plvcId);
					}
				}
				if (plvcList.isEmpty()) {
					continue;
				}
				String plvcName;
				DomainObject plvcObj = new DomainObject();
				Map selectMap;
				boolean hasPNCData = false;
				StringList tempAlertList = new StringList();
				MapList groupDrawingList, PNCList;
				String groupDrawingId, groupDrawingName, PNCId;
				String groupDrawingWhere, PNCWhere;
				
				connectGroupDrawingAndPNC:
				for (String plvcId : plvcList) {
					plvcObj.setId(plvcId);
					plvcName = plvcObj.getInfo(context, DomainConstants.SELECT_NAME);
					groupDrawingWhere = "attribute[SPLM_GroupCode]=='" + groupCode + "' && name =='" + groupDrawingNo + "'";
					groupDrawingList = CMC_Method_JPO_mxJPO.expandObject(context, plvcId, "SPLM_GroupDrawing", "SPLM_PartSpecification", groupDrawingWhere, "", "from", (short) 1, null, null);
					if (groupDrawingList.isEmpty()) {
						tempAlertList.add("PLVC:" + plvcName + " \u7f3a\u5c11\u55ae\u5143\u4ee3\u78bc:" + groupCode + " \u5716\u865f:" + groupDrawingNo + " \u7684\u55ae\u5143\u5716");
						continue;
					}
					PNCWhere = "name=='" + PNC + "'";
					for (Object tempGroupDrawingData : groupDrawingList) {
						selectMap = (Map) tempGroupDrawingData;
						groupDrawingId = (String) selectMap.get(DomainConstants.SELECT_ID);
						groupDrawingName = (String) selectMap.get(DomainConstants.SELECT_NAME);
						PNCList = CMC_Method_JPO_mxJPO.expandObject(context, groupDrawingId, "SPLM_PNC", "SPLM_Related_PNC", PNCWhere, "", "from", (short) 1, null, null);
						if (PNCList.isEmpty()) {
							tempAlertList.add("\u55ae\u5143\u5716:" + groupDrawingName + " \u7f3a\u5c11PNC:" + PNC);
							continue;
						}
						PNCId = (String) ((Map) PNCList.get(0)).get(DomainConstants.SELECT_ID);
						hasPNCData = true;
						
						this.setRelatedPart(context, toPartObjId, "SPLM_GroupDrawing", groupDrawingId, relPhysicalId, relAttMap);
						
						this.setRelatedPart(context, toPartObjId, "SPLM_PNC", PNCId, relPhysicalId, relAttMap);
						
						break connectGroupDrawingAndPNC;
					}
				}
				if (hasPNCData || tempAlertList.isEmpty()) {
					continue;
				}
				for (String alert : tempAlertList) {
					if (alertList.contains(alert)) {
						continue;
					}
					alertList.add(alert);
				}
			}
			Collections.sort(alertList);
			if (!alertList.isEmpty()) {
				String msg = String.join("\n", alertList);
				throw new Exception(msg);
			} else {
				returnMap.put("Notify", "\u8655\u7406\u5b8c\u7562");
			}
		} catch (Exception e) {
			returnMap.put("Action", "stop");
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	
	private void disconnectRelatedPart(Context context, 
			String toPartObjId, 
			String relPhysicalId) throws Exception {
		
		MapList partRelatedGroupDrawingList = CMC_Method_JPO_mxJPO.expandObject(context, toPartObjId,
				"*", "SPLM_RelatedPart", "",
				"attribute[SPLM_CreateFromBOM]=='" + relPhysicalId + "'", "to", (short) 1, null, null);
	
		if (partRelatedGroupDrawingList.isEmpty()) {
			return;
		}
		
		String partRelatedId;
		for (Object object : partRelatedGroupDrawingList) {
			partRelatedId = (String) ((Map) object).get(DomainConstants.SELECT_RELATIONSHIP_ID);
			DomainRelationship.disconnect(context, partRelatedId);
		}
	}
	
	private void setRelatedPart(Context context, 
			String toPartObjId, 
			String relatedObjType, 
			String relatedObjId,
			String relPhysicalId,
			Map relAttMap) throws Exception {

		DomainObject toPartObj = new DomainObject(toPartObjId);
		DomainRelationship relatedPartObj;
		
		MapList partRelatedGroupDrawingList = CMC_Method_JPO_mxJPO.expandObject(context, toPartObjId,
				relatedObjType, "SPLM_RelatedPart", "",
				"attribute[SPLM_CreateFromBOM]=='" + relPhysicalId + "'", "to", (short) 1, null, null);
		
		if (partRelatedGroupDrawingList.isEmpty()) {
			relatedPartObj = DomainRelationship.connect(context, new DomainObject(relatedObjId), "SPLM_RelatedPart", toPartObj);
			relatedPartObj.setAttributeValues(context, relAttMap);
		} else {
			for (Object object : partRelatedGroupDrawingList) {
				String partRelatedId = (String) ((Map) object).get(DomainConstants.SELECT_RELATIONSHIP_ID);
				relatedPartObj = new DomainRelationship(partRelatedId);
				MqlUtil.mqlCommand(context, "mod connection '" + partRelatedId + "' from '" + relatedObjId + "'");
			
				relatedPartObj.setAttributeValues(context, relAttMap);
			}
		}
	}
	
	public HashMap<String, String> getInitialBOM(Context context, String[] args) throws Exception {
		String env = System.getenv("PLM_ENV");
		String errMsg = "";
		HashMap<String, String> returnMap = new HashMap<String, String>();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");

		StringList modelCodeList = new StringList();
		
		DomainObject obj = new DomainObject(objectId);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		try {
			if ("SPLM_ModelCode".equals(obj.getInfo(context, DomainConstants.SELECT_TYPE))) {
				modelCodeList.add(objectId);
			} else {
				obj.setAttributeValue(context, "SPLM_InitialBOMStatus", "Complete");
				
				modelCodeList = obj.getInfoList(context, "from[SPLM_SBOM].to[SPLM_ModelCode].id");
			}
			
			if (modelCodeList.isEmpty()) {
				throw new Exception("\u627e\u4e0d\u5230\u7d30\u8eca\u578b");
			}
			
			DomainObject modelCodeObj = new DomainObject();
			String attCMCCode, modelCodeNo;
			
			JSONArray jsonArray = new JSONArray();
			JSONObject jsonObj;
			for (String modelCodeId : modelCodeList) {
				modelCodeObj.setId(modelCodeId);
				modelCodeObj.setAttributeValue(context, "SPLM_InitialBOMStatus", "Complete");
				modelCodeObj.setAttributeValue(context, "SPLM_EnableDate", sdf.format(new Date()));
				
				modelCodeNo = modelCodeObj.getInfo(context, DomainConstants.SELECT_NAME);
				attCMCCode = modelCodeObj.getAttributeValue(context, "SPLM_CMC_Code");
				if (StringUtils.isEmpty(attCMCCode)) {
					errMsg += modelCodeNo + "\u7f3a\u5c11 CMC CODE" + "\n";
					continue;
				}
				jsonObj = new JSONObject();
				jsonObj.put("REAL_MODEL_CODE", modelCodeNo);
				jsonObj.put("MODEL_CODE", attCMCCode);
				jsonArray.put(jsonObj);
			}
			
			if (StringUtils.isNotEmpty(errMsg)) {
				throw new Exception(errMsg);
			}
			
			if ("PRD,UAT".contains(env)) {
				String url = i18nNow.getI18nString(ERP_YPP16_300 + "." + env, "emxSPLM", "");
				
				String json = jsonArray.toString();
				YPP16_ServiceLocator service = new YPP16_ServiceLocator();
				String resultValue = service.getYPP16(new URL(url)).yppSplmDmodelToSap(json);
				JSONObject result = new JSONObject(resultValue);
				String status = result.getString("STATUS");
				if ("OK".equals(status)) {
					returnMap.put("Notify", "\u5df2\u50b3\u9001\u5230ERP");
				} else {
					String message = "";
					if (result.contains("MESSAGE")) {
						message = result.getString("MESSAGE");
					}
					throw new Exception("STATUS:" + status + " MESSAGE:" + message);
				}
			} else {
				throw new Exception("\u7576\u524d\u74b0\u5883[" + env + "]\u7121\u6cd5\u6e2c\u8a66\u8a72\u529f\u80fd");
			}
		} catch (Exception e) {
			returnMap.put("Action", "stop");
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	
	public HashMap<String, String> changeBOMOwner(Context context, String[] args) throws Exception {
		HashMap<String, String> returnMap = new HashMap<String, String>();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		DomainObject msObj = new DomainObject(objectId);
		try {
			String loginUser = context.getUser();
			MapList bomList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_GLNO,SPLM_ModelCode,SPLM_PLVC", "SPLM_SBOM,SPLM_MBOM", "", "", "from", (short) 3, null, null);
			DomainObject bomObj = new DomainObject();
			String bomObjId;
			for (Object temp : bomList) {
				bomObjId = (String) ((Map) temp).get(DomainConstants.SELECT_ID);
				bomObj.setId(bomObjId);
				bomObj.setOwner(context, loginUser);
			}
			msObj.setOwner(context, loginUser);
		} catch (Exception e) {
			returnMap.put("Action", "stop");
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	
	public Vector getShowEBOMBtn(Context context, String[] args)
			throws Exception {
		Vector result = new Vector();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		if (objectList.size() <= 0) {
			return result;
		}
		
		String tableUrl = "../common/emxIndentedTable.jsp"
				+ "?showRMB=false"
				+ "&program=SPLM_Program_JPO:showEBOMData"
				+ "&table=SPLM_EBOM"
				+ "&header=emxEngineeringCentral.Common.EBOMBillOfMaterials"
				+ "&suiteKey=EngineeringCentral"
				+ "&StringResourceFileId=emxEngineeringCentralStringResource";
		
		String outStyle = "border: 1px solid #B4B6BA; padding: 5px; color: #4C5956; background-color: #F1F1F1; border-radius: 3px; text-decoration: none;";
		String overStyle = "border: 1px solid #5B5D5E; padding: 5px; color: #5B5D5E; background-color: #E6E8EA; border-radius: 3px; text-decoration: none;";
		
		String relId;
		String onClick;
		for (Object temp : objectList) {
			relId = (String) ((Map) temp).get(DomainConstants.SELECT_RELATIONSHIP_ID);
			if (StringUtils.isEmpty(relId)) {
				result.add("");
				continue;
			}
			onClick = "JavaScript:emxTableColumnLinkClick('" + XSSUtil.encodeForURL(context, tableUrl + "&relId=" + relId) + "', '700', '600', 'false', 'popup', '', '" + relId + "', 'false', '', '')";
			result.add("<p style=\"text-align:center;width:auto;\"><a onmouseover=\"this.style.cssText='" + overStyle + "'\" onmouseout=\"this.style.cssText='" + outStyle + "'\" style=\"" + outStyle + "\" href=\"" + onClick.toString() + "\">E</a></p>");
		}
		return result;
	}
	
	public MapList showEBOMData(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String relId = (String) programMap.get("relId");
		DomainRelationship relObj = new DomainRelationship(relId);
		relObj.open(context);
		String fromId = relObj.getFrom().getObjectId(context);
		String toId = relObj.getTo().getObjectId(context);
		relObj.close(context);
		
		DomainObject fromObj = new DomainObject(fromId);
		String fromType = fromObj.getInfo(context, DomainConstants.SELECT_TYPE);
		fromType = "SPLM_Part".equals(fromType) ? "Part" : fromType.replace("SPLM_", "CMC_");
		String fromName = fromObj.getInfo(context, DomainConstants.SELECT_NAME);
		
		DomainObject toObj = new DomainObject(toId);
		String toType = toObj.getInfo(context, DomainConstants.SELECT_TYPE);
		toType = "SPLM_Part".equals(toType) ? "Part" : toType.replace("SPLM_", "CMC_");
		String toName = toObj.getInfo(context, DomainConstants.SELECT_NAME);
		
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		MapList fromEBOMObj = DomainObject.findObjects(context, fromType, fromName,
				"*", "*", PRODUCTION_VAULT, "revision==last", null, false, busSelects,
				Short.parseShort("0"));
		if (fromEBOMObj.isEmpty()) {
			return new MapList();
		}
		MapList toEBOMObj = DomainObject.findObjects(context, toType, toName,
				"*", "*", PRODUCTION_VAULT, "revision==last", null, false, busSelects,
				Short.parseShort("0"));
		if (toEBOMObj.isEmpty()) {
			return new MapList();
		}
		
		String fomrEBOMLastId = (String) ((Map) fromEBOMObj.get(0)).get(DomainConstants.SELECT_ID);
		String toEBOMLastId = (String) ((Map) toEBOMObj.get(0)).get(DomainConstants.SELECT_ID);
		MapList objectList = new MapList();
		
		RelationshipQuery tempRelQuery = new RelationshipQuery();
		tempRelQuery.setRelationshipType("EBOM");
		tempRelQuery.setWhereExpression("from.id=='" + fomrEBOMLastId + "' && to.id=='" + toEBOMLastId + "'");
		RelationshipQueryIterator relList = tempRelQuery.getIterator(context, new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), (short) 0);
		String relEBOMId;
		for(RelationshipWithSelect relationshipWithSelect : relList) {
			relEBOMId = relationshipWithSelect.getSelectData(DomainConstants.SELECT_RELATIONSHIP_ID);
			Map rowMap = new HashMap();
			rowMap.put(DomainConstants.SELECT_ID, toEBOMLastId);
			rowMap.put(DomainConstants.SELECT_RELATIONSHIP_ID, relEBOMId);
			objectList.add(rowMap);
		}
		relList.close();
		tempRelQuery.close(context);
		return objectList;
	}
	
	
	public HashMap<String, String> postCreatePartsCatalogue(Context context,
			String[] args) throws Exception {
		HashMap<String, String> returnMap = new HashMap<String, String>();
		Map programMap = (Map) JPO.unpackArgs(args);
		Map requestMap = (Map) programMap.get("requestMap");
		String glnoId = (String) requestMap.get("objectId");
		
		Map paramMap = (Map) programMap.get("paramMap");
		String partCatalogueId = (String) paramMap.get("objectId");
		
		DomainObject glnoObj = new DomainObject(glnoId);
		StringList msList = glnoObj.getInfoList(context, "to[SPLM_SBOM].from[SPLM_ModelSeries].name");
		if (msList.isEmpty()) {
			String glnoName = glnoObj.getInfo(context, DomainConstants.SELECT_NAME);
			String errMsg = "GLNO: " + glnoName + " \u7f3a\u5c11 M/S";
			returnMap.put("Action", "stop");
			returnMap.put("Message", errMsg);
			return returnMap;
		}
		
		DomainObject partCatalogueObj = new DomainObject(partCatalogueId);
		partCatalogueObj.setAttributeValue(context, "SPLM_ModelSeries", String.join(" ,", msList));
		
		return returnMap;
	}
	
	
	public Map reloadParentName(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");
		String parentId = (String) requestMap.get("objectId");
		DomainObject parentObj = new DomainObject(parentId);
		String parentName = parentObj.getInfo(context, DomainConstants.SELECT_NAME);
		
		Map resultMap = new HashMap();
		resultMap.put("SelectedValues", parentName);
		resultMap.put("SelectedDisplayValues", parentName);
		return resultMap;
	}

	public void connectEPC_PIC(Context context, String[] args) throws Exception {
		Map programMap = (Map) JPO.unpackArgs(args);
		Map requestMap = (HashMap) programMap.get("reqMap");
		Map requestTableMap = (HashMap) programMap.get("reqTableMap");
		String[] objectIds = (String[]) requestMap.get( "objectId" );
        String[] emxTableRowIds = (String[]) requestMap.get( "emxTableRowId" );
        
        try {
        	String epcIc = CMC_Method_JPO_mxJPO.getTableRowObjectId(emxTableRowIds[0]);
			DomainObject partsCatalogueObj = new DomainObject(objectIds[0]);
			DomainObject EPC_PICObj = new DomainObject(epcIc);
			DomainRelationship.connect(context, partsCatalogueObj, "SPLM_Related_EPC_PIC", EPC_PICObj);
		} catch (Exception e) {
			emxContextUtil_mxJPO.mqlError(context, e.getMessage());
			throw e;
		}
	}
	
	public Vector getServicePartName(Context context, String[] args)
			throws Exception {
		Vector result = new Vector();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		if (objectList.size() <= 0) {
			return result;
		}
		
		
		String objId, objType;
		DomainObject obj = new DomainObject();
		
		StringList objSelects = new StringList();
		objSelects.add(DomainConstants.SELECT_NAME);
		objSelects.add("attribute[SPLM_CMC_Code]");
		
		Map objInfo;
		String isServicePart, createFromERP, partNo, sortKey, treeLink, newColor;
		for (Object temp : objectList) {
			objId = (String) ((Map) temp).get(DomainConstants.SELECT_ID);
			objType = (String) ((Map) temp).get(DomainConstants.SELECT_TYPE);
			
			obj.setId(objId);
			
			objInfo = obj.getInfo(context, objSelects);
			partNo = (String) objInfo.get(DomainConstants.SELECT_NAME);
			
			if ("SPLM_ModelCode".equals(objType)) {
				partNo += "(" + (String) objInfo.get("attribute[SPLM_CMC_Code]") + ")";
			}
			result.add(partNo);
		}
		return result;
	}
	
	public Vector getServicePartColor(Context context, String[] args)
			throws Exception {
		Vector result = new Vector();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		if (objectList.size() <= 0) {
			return result;
		}
		
		String objId;
		DomainObject obj = new DomainObject();
		
		StringList objSelects = new StringList();
		objSelects.add(DomainConstants.SELECT_NAME);
		objSelects.add("attribute[SPLM_IsServicePart]");
		objSelects.add("attribute[SPLM_CMC_Code]");
		objSelects.add("attribute[SPLM_CreateFromERP]");
		
		Map objInfo;
		String isServicePart, createFromERP, value, newColor;
		for (Object temp : objectList) {
			objId = (String) ((Map) temp).get(DomainConstants.SELECT_ID);
			
			obj.setId(objId);
			
			objInfo = obj.getInfo(context, objSelects);
			isServicePart = (String) objInfo.get("attribute[SPLM_IsServicePart]");
			createFromERP = (String) objInfo.get("attribute[SPLM_CreateFromERP]");
			newColor = "background-color:";
			if ("true".equalsIgnoreCase(isServicePart)) {
				newColor += "true".equalsIgnoreCase(createFromERP) ? "red" : "blue";
			} else {
				newColor += "black";
			}
			
			value = "<div style=\"" + newColor + "\">\u3000</div>";
			result.add(value);
		}
		return result;
	}
	
	private String buildEOUrl(Context context, String eoNo) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		
		StringBuffer eoURL = new StringBuffer();
		
		MapList eoMapList = DomainObject.findObjects(context, "Change Action", eoNo , "-", "*",
				PRODUCTION_VAULT, "", null, false, busSelects, Short.parseShort("0"));
		if (eoMapList.isEmpty()) {
			eoURL.append(eoNo);
		} else {
			String eoId = (String) ((Map) eoMapList.get(0)).get(DomainConstants.SELECT_ID);
			eoURL.append(CMC_Method_JPO_mxJPO.getTableTreeLink(eoId, eoNo));
		}
		
		return eoURL.toString();
	}
	
	public Vector getRelatedEOLink(Context context, String[] args)
			throws Exception {
		Vector result = new Vector();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		if (objectList.size() <= 0) {
			return result;
		}
		
		
		String relatedEO;
		String[] relatedEOs;
		Map objInfo;
		StringBuffer eoURLList;
		MapList eoMapList;
		String soId;
		for (Object temp : objectList) {
			relatedEO = (String) ((Map) temp).get("attribute[SPLM_RelatedEO]");
			
			if (StringUtils.isEmpty(relatedEO)) {
				result.add("");
				continue;
			}
			
			relatedEOs = relatedEO.split(",");
			eoURLList = new StringBuffer();
			
			for (String eo : relatedEOs) {
				eoURLList.append("<p>");
				eoURLList.append(this.buildEOUrl(context, eo.trim()));
				eoURLList.append("</p>");
			}
			
			result.add(eoURLList.toString());
		}
		return result;
	}
	
	private String buildPOUrl(String poNo) {
		StringBuffer poURL = new StringBuffer();
		
		poURL.append("<a href=\"javascript:emxTableColumnLinkClick('../JCI/SPLM_ShowPOURL.jsp?");
		poURL.append("poNo=");
		poURL.append(poNo);
		poURL.append("'");
		poURL.append(",'");
		poURL.append(800);
		poURL.append("','");
		poURL.append(600);
		poURL.append("','");
		poURL.append("false");
		poURL.append("','");
		poURL.append("popup");
		poURL.append("', '', '', 'false', '', '')\" ");
		poURL.append(" style=\"color:#368ec4;\"");
		poURL.append(">");
		poURL.append(poNo);
		poURL.append("</a>");
		
		return poURL.toString();
	}
	
	public Vector getRelatedPOLink(Context context, String[] args)
			throws Exception {
		Vector result = new Vector();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		if (objectList.size() <= 0) {
			return result;
		}
		
		
		String relatedPO;
		String[] relatedPOs;
		DomainObject obj = new DomainObject();
		
		StringList objSelects = new StringList();
		objSelects.add(DomainConstants.SELECT_NAME);
		
		Map objInfo;
		StringBuffer poURLList;
		for (Object temp : objectList) {
			relatedPO = (String) ((Map) temp).get("attribute[SPLM_RelatedPO]");
			
			if (StringUtils.isEmpty(relatedPO)) {
				result.add("");
				continue;
			}
			
			relatedPOs = relatedPO.split(",");
			poURLList = new StringBuffer();
			
			for (String po : relatedPOs) {
				poURLList.append("<p>");
				poURLList.append(this.buildPOUrl(po.trim()));
				poURLList.append("</p>");
			}
			
			result.add(poURLList.toString());
			
		}
		return result;
	}
	
	public Vector getExpandColumnValueWhenMapList(Context context, String[] args) throws Exception {
		
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		HashMap columnMap = (HashMap) programMap.get("columnMap");
		HashMap colAttrMap = (HashMap) columnMap.get("colAttrMap");
		String columnKey = (String) colAttrMap.get("expression");
				
		Vector result = new Vector();
		if (objectList.size() <= 0) {
			return result;
		}
		
		Map objInfo;
		String relId, objId;
		boolean isRoot = false;
		DomainObject rootObj = new DomainObject();
		for (Object temp : objectList) {
			objInfo = (Map) temp;
			relId = (String) ((Map) temp).get(DomainConstants.SELECT_RELATIONSHIP_ID);
			isRoot = StringUtils.isEmpty(relId);
			if (isRoot) {
				objId = (String) ((Map) temp).get(DomainConstants.SELECT_ID);
				rootObj.setId(objId);
				
				result.add(rootObj.getInfo(context, columnKey));			
			} else {
				result.add(objInfo.containsKey(columnKey) ? objInfo.get(columnKey) : "");		
			}
		}
		return result;
	}
	
	public Vector getColumnValueWhenMapList(Context context, String[] args) throws Exception {
		
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		HashMap columnMap = (HashMap) programMap.get("columnMap");
		HashMap colAttrMap = (HashMap) columnMap.get("colAttrMap");
		String columnKey = (String) colAttrMap.get("expression");
				
		Vector result = new Vector();
		if (objectList.size() <= 0) {
			return result;
		}
		
		Map objInfo;
		String relId, objId;
		for (Object temp : objectList) {
			objInfo = (Map) temp;
			result.add(objInfo.containsKey(columnKey) ? objInfo.get(columnKey) : "");
		}
		return result;
	}
	
	public HashMap<String, String> postCheckOptionalPart(Context context,
			String[] args) throws Exception {
		String msg = "";
		HashMap<String, String> returnMap = new HashMap<String, String>();
		Map programMap = (Map) JPO.unpackArgs(args);
		Map requestMap = (Map) programMap.get("requestMap");
		String objId = (String) requestMap.get("objectId");

		StringList relSelects = new StringList();
		relSelects.add("attribute[SPLM_OptionalType]");
		relSelects.add("attribute[SPLM_OptionalExchangeable]");
		MapList optionalPartList = CMC_Method_JPO_mxJPO.expandObject(context, objId, "SPLM_Part",
				"SPLM_RelatedOptionalPart", "", "", "from", (short) 0, null, relSelects);
		Map optionalPartInfo;
		String optionalPartNo, optionalType, optionalExchangeable;
		for (Object temp : optionalPartList) {
			optionalPartInfo = (Map) temp;
			optionalPartNo = (String) optionalPartInfo.get(DomainConstants.SELECT_NAME);
			optionalType = (String) optionalPartInfo.get("attribute[SPLM_OptionalType]");
			optionalExchangeable = (String) optionalPartInfo.get("attribute[SPLM_OptionalExchangeable]");
			
			if ("Alternate Part".equals(optionalType) && StringUtils.isEmpty(optionalExchangeable)) {
				msg += optionalPartNo + " \u70ba\u66ff\u4ee3\u8acb\u586b\u5beb\u65b0\u820a\u4e92\u63db " + "\n";
			} else if (!"Alternate Part".equals(optionalType) && StringUtils.isNotEmpty(optionalExchangeable)) {
				msg += optionalPartNo + " \u4e0d\u662f\u66ff\u4ee3\u8acb\u6e05\u9664\u65b0\u820a\u4e92\u63db " + "\n";
			} else {
				continue;
			}
		}
		
		if (StringUtils.isNotEmpty(msg)) {
			returnMap.put("Action", "stop");
			returnMap.put("Message", msg);
		}
		return returnMap;
	}
	
	public StringList getRelatedMBOMPart (Context context, String[] args) throws Exception {
		StringList mbomList = new StringList();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String selPartObjectId = (String) programMap.get("selPartObjectId");
		DomainObject obj = new DomainObject(selPartObjectId);
		StringList sbomParentList = obj.getInfoList(context, "to[SPLM_SBOM].from.id");
		DomainObject sbomParentObj = new DomainObject();
		for (String sbomParentId : sbomParentList) {
			sbomParentObj.setId(sbomParentId);
			StringList parentMBOMList = sbomParentObj.getInfoList(context, "from[SPLM_MBOM].to.id");
			for (String parentMBOMId : parentMBOMList) {
				if (mbomList.contains(parentMBOMId)) {
					continue;
				}
				mbomList.add(parentMBOMId);
			}
		}
		return mbomList;
	}
	
	public HashMap<String, String> createReferenceSO(Context context, String[] args) throws Exception {
		String errMsg = "";
		HashMap<String, String> returnMap = new HashMap<String, String>();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String[] emxTableRowIds = (String[]) programMap.get("emxTableRowIds");
		String referenceSOId = emxTableRowIds[0];
		DomainObject referenceSOObj = new DomainObject(referenceSOId);
		String referenceSOName = referenceSOObj.getInfo(context, DomainConstants.SELECT_NAME);
		if (referenceSOName.contains("-")) {
			referenceSOName = referenceSOName.substring(0, (referenceSOName.lastIndexOf("-") + 1));
		} else {
			referenceSOName += "-";
		}
		MapList referenceSOList = DomainObject.findObjects(context, "SPLM_SO", referenceSOName + "*", "-", "*",
				PRODUCTION_VAULT, "", null, false, new StringList(DomainConstants.SELECT_ID), Short.parseShort("0"));

		int newSOIndex = referenceSOList.size() + 1;
		int char0 = 48;
		int charPreA = 64;
		char charSuffix;
		if (newSOIndex < 10) {
			charSuffix = (char) (char0 + newSOIndex);
		} else {
			newSOIndex = newSOIndex - 9;
			charSuffix = (char) (charPreA + newSOIndex);
		}
		String newSOName = referenceSOName + (String.valueOf(charSuffix));
		
		DomainObject newSO = new DomainObject();
		newSO.createObject(context, "SPLM_SO", newSOName, "-", "SPLM_SO",
				PRODUCTION_VAULT);

		newSO.setAttributeValues(context, referenceSOObj.getAttributeMap(context));
		
		if (StringUtils.isNotEmpty(errMsg)) {
			returnMap.put("Action", "stop");
			returnMap.put("Message", errMsg);
		}
		return returnMap;
	}
	
	public HashMap<String, String> connectAffectedItem(Context context, String[] args) throws Exception {
		
		HashMap<String, String> returnMap = new HashMap<String, String>();
		Map programMap = JPO.unpackArgs(args);
		String soId = (String) programMap.get("objectId");
		String[] emxTableRowIds = (String[]) programMap.get("emxTableRowIds");
		try {
			DomainObject soObj = new DomainObject(soId);
			String soName = soObj.getInfo(context, DomainConstants.SELECT_NAME);
			StringList affectedItemIdList = soObj.getInfoList(context, "from[SPLM_AffectedItem].to.id");
			DomainObject selectedPartObj = new DomainObject();
			String selectedPartId, selectedPartNo, selectedPartType, existsSOName;
			MapList partSOList;
			for (String rowId : emxTableRowIds) {
				selectedPartId = CMC_Method_JPO_mxJPO.getTableRowObjectId(rowId);
				selectedPartObj.setId(selectedPartId);
				selectedPartType = selectedPartObj.getInfo(context, DomainConstants.SELECT_TYPE);
				selectedPartNo = selectedPartObj.getInfo(context, DomainConstants.SELECT_NAME);
				
				if (!"SPLM_ColorPart,SPLM_Part".contains(selectedPartType)) {
					throw new Exception(selectedPartNo + " \u4e0d\u662f\u96f6\u4ef6\u6216\u984f\u8272\u4ef6");
				}
				
				if (affectedItemIdList.contains(selectedPartId)) {
					throw new Exception(selectedPartNo + " \u5df2\u5b58\u5728\u6b64SO");
				}
				
				this.checkNotHaveIncompleteSO(context, selectedPartId);
				
				DomainRelationship.connect(context, soObj, "SPLM_AffectedItem", selectedPartObj);
				
				this.connectPartSOVendor(context, selectedPartId, soName);
				
				this.setSOSendDept(context, soId, selectedPartId);
			}
		} catch (Exception e) {
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	
	public HashMap<String, String> connectReferenceParentPart(Context context, String[] args) throws Exception {
		
		HashMap<String, String> returnMap = new HashMap<String, String>();
		Map programMap = JPO.unpackArgs(args);
		String soId = (String) programMap.get("objectId");
		String[] emxTableRowIds = (String[]) programMap.get("emxTableRowIds");
		try {
			DomainObject soObj = new DomainObject(soId);
			String soName = soObj.getInfo(context, DomainConstants.SELECT_NAME);
			StringList referenceParentPartIdList = soObj.getInfoList(context, "from[SPLM_ReferenceParentPart].to.id");
			DomainObject selectedPartObj = new DomainObject();
			String selectedPartId, selectedPartNo, existsSOName;
			MapList partSOList;
			for (String rowId : emxTableRowIds) {
				selectedPartId = CMC_Method_JPO_mxJPO.getTableRowObjectId(rowId);
				selectedPartObj.setId(selectedPartId);
				selectedPartNo = selectedPartObj.getInfo(context, DomainConstants.SELECT_NAME);
				if (referenceParentPartIdList.contains(selectedPartId)) {
					throw new Exception(selectedPartNo + " \u5df2\u5b58\u5728\u6b64SO");
				}
				
				DomainRelationship.connect(context, soObj, "SPLM_ReferenceParentPart", selectedPartObj);
				
			}
		} catch (Exception e) {
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	
	
	public HashMap<String, String> postConnectPartToSO(Context context, String[] args) throws Exception {
		HashMap<String, String> returnMap = new HashMap<String, String>();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");
		String partId = (String) requestMap.get("objectId");
		String emxTableRowId = (String) requestMap.get("emxTableRowIds");
		String[] emxTableRowIds = emxTableRowId.split(":");
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String soId = (String) paramMap.get("objectId");
		
		try {
			
			if (StringUtils.isEmpty(partId)) {
				throw new Exception("\u8acb\u9078\u64c7\u96f6\u4ef6");
			}
			
			StringList connectPartIdList = new StringList();
			connectPartIdList.add(partId);
			
			for (String rowId : emxTableRowIds) {
				if (StringUtils.isEmpty(rowId)) {
					continue;
				}
				String selectPartId = CMC_Method_JPO_mxJPO.getTableRowObjectId(rowId);
				if (connectPartIdList.contains(selectPartId)) {
					continue;
				}
				connectPartIdList.add(selectPartId);
			}
			DomainObject soObj = new DomainObject(soId);
			String soName = soObj.getInfo(context, DomainConstants.SELECT_NAME);
			
			DomainObject partObj = new DomainObject();
			
			for (String connectPartId : connectPartIdList) {
				partObj.setId(connectPartId);
				
				if (!"SPLM_ColorPart,SPLM_Part".contains(partObj.getInfo(context, DomainConstants.SELECT_TYPE))) {
					throw new Exception(partObj.getInfo(context, DomainConstants.SELECT_NAME)
							+ " \u4e0d\u662f\u96f6\u4ef6\u6216\u984f\u8272\u4ef6");
				}
				
				this.checkNotHaveIncompleteSO(context, connectPartId);
				
				DomainRelationship.connect(context, soObj, "SPLM_AffectedItem", partObj);
				
				this.connectPartSOVendor(context, connectPartId, soName);
				
				this.setSOSendDept(context, soId, connectPartId);
			}
		} catch (Exception e) {
			returnMap.put("Action", "stop");
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	
	private void connectPartSOVendor(Context context, String partId, String soName) throws Exception {
		DomainObject partObj = new DomainObject(partId);
		
		StringList partSOVendorList = new StringList();
		MapList partSOVendorMapList = CMC_Method_JPO_mxJPO.expandObject(context, partId, "SPLM_Vendor", "SPLM_RelatedSOReleaseItem",
				"", "attribute[SPLM_SO]=='" + soName + "' && attribute[SPLM_SOReleaseItemType]=='Vendor'", "from", (short) 0);
		String partSOVendor;
		for (Object object : partSOVendorMapList) {
			partSOVendor = (String) ((Map) object).get(DomainConstants.SELECT_NAME);
			if (partSOVendorList.contains(partSOVendor)) {
				continue;
			}
			partSOVendorList.add(partSOVendor);
		}
		
		MapList partVendorMapList = CMC_Method_JPO_mxJPO.expandObject(context, partId, "SPLM_Vendor", "SPLM_RelatedVendor",
				"", "attribute[SPLM_Valid]=='Y'", "from", (short) 0);
		Map partVendorInfo;
		String partVendor, partVendorId;
		DomainObject vendorObj = new DomainObject();
		DomainRelationship relObj;
		for (Object object : partVendorMapList) {
			partVendorInfo = (Map) object;
			partVendor = (String) partVendorInfo.get(DomainConstants.SELECT_NAME);
			
			if (partSOVendorList.contains(partVendor)) {
				continue;
			}
			
			partVendorId = (String) partVendorInfo.get(DomainConstants.SELECT_ID);
			vendorObj.setId(partVendorId);
			
			relObj = DomainRelationship.connect(context, partObj, "SPLM_RelatedSOReleaseItem", vendorObj);
			relObj.setAttributeValue(context, "SPLM_SO", soName);
			relObj.setAttributeValue(context, "SPLM_SOReleaseItemType", "Vendor");
			
			partSOVendorList.add(partVendor);
		}
	}
	
	private void setSOSendDept(Context context, String soId, String partId) throws Exception {
		DomainObject partObj = new DomainObject(partId);
		String partType = partObj.getInfo(context, DomainConstants.SELECT_TYPE);
		String purchasingGroup = partObj.getAttributeValue(context, "SPLM_PurchasingGroup");
		if (!"SPLM_Part".equals(partType) || StringUtils.isEmpty(purchasingGroup)) {
			return;
		}
		
		MapList sendDeptFindList = DomainObject.findObjects(context, "SPLM_SendDept", purchasingGroup,
				"-", "*", PRODUCTION_VAULT, "", null, false, new StringList(DomainConstants.SELECT_NAME),
				Short.parseShort("0"));
		
		if (sendDeptFindList.isEmpty()) {
			throw new Exception("\u7f3a\u5c11\u767c\u9001\u55ae\u4f4d:" + purchasingGroup);
		}
		
		DomainObject soObj = new DomainObject(soId);
		List<String> sendDeptAllList = new ArrayList<String>();
		List<String> sendDeptList = Arrays.asList(soObj.getAttributeValue(context, "SPLM_SendDept").split(SPLIT_KEY));
		
		sendDeptAllList.addAll(sendDeptList);
		
		if (sendDeptAllList.contains(purchasingGroup)) {
			return;
		}
		
		sendDeptAllList.add(purchasingGroup);
		soObj.setAttributeValue(context, "SPLM_SendDept", String.join(SPLIT_KEY, sendDeptAllList));			
	}
	
	private void checkNotHaveIncompleteSO(Context context, String partId) throws Exception {
		DomainObject partObj = new DomainObject(partId);
		String partNo = partObj.getInfo(context, DomainConstants.SELECT_NAME);
		MapList partSOList = CMC_Method_JPO_mxJPO.expandObject(context, partId, "SPLM_SO", "SPLM_AffectedItem",
				"current!='Complete'", "", "to", (short) 0);
		if (!partSOList.isEmpty()) {
			String existsSOName = (String) ((Map) partSOList.get(0)).get(DomainConstants.SELECT_NAME);
			throw new Exception(partNo + " \u5df2\u5b58\u5728 SO:" + existsSOName);
		}
	}
	
	public MapList getSO(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_SO",
				"SPLM_AffectedItem", "", null, "to", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getAffectedItem(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "*",
				"SPLM_AffectedItem", "", null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getReferenceParentPart(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "*",
				"SPLM_ReferenceParentPart", "", null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public String getLastReleaseSODate(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");
		String objectId = (String) requestMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_SO",
				"SPLM_AffectedItem", "current=='Complete'", null, "to", (short) 1,
				new StringList("current.actual"), null);
		if (objectList.isEmpty()) {
			return "";
		}
		objectList.addSortKey("current.actual", "descending", "date");
		objectList.sort();
		return (String) ((Map) objectList.get(0)).get("current.actual");
	}
	
	public int disconnectRelatedPart(Context context, String[] args)
			throws Exception {
		int flag = 0;
		String fromId = args[0];
		String toId = args[1];
		String relPhysicalId = args[2];
		
		StringList relatedPartIdList = new StringList();
		
		RelationshipQuery tempRelQuery = new RelationshipQuery();
		tempRelQuery.setRelationshipType("SPLM_RelatedPart");
		tempRelQuery.setWhereExpression("attribute[SPLM_CreateFromBOM] == '" + relPhysicalId + "' && to.id=='" + toId + "'");
		RelationshipQueryIterator relList = tempRelQuery.getIterator(context, new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), (short) 0);
		String relRelatedPartId;
		for(RelationshipWithSelect relationshipWithSelect : relList) {
			relRelatedPartId = relationshipWithSelect.getSelectData(DomainConstants.SELECT_RELATIONSHIP_ID);
			relatedPartIdList.add(relRelatedPartId);
		}
		
		relList.close();
		tempRelQuery.close(context);
		if (!relatedPartIdList.isEmpty()) {
			DomainRelationship.disconnect(context, relatedPartIdList.toArray(new String[0]));
		}
		return flag;
	}
	
	public int setBOMDefaultDate(Context context, String[] args)
			throws Exception {
		int flag = 0;
		String fromId = args[0];
		String toId = args[1];
		String relId = args[2];
		
		DomainRelationship relObj = new DomainRelationship(relId);
		relObj.setAttributeValue(context, "SPLM_EnableDate", "01/01/1900");
		relObj.setAttributeValue(context, "SPLM_DisableDate", "12/31/9999");
		
		return flag;
	}
	
	public int setSODefaultDate(Context context, String[] args)
			throws Exception {
		int flag = 0;
		String event = args[0];
		String objectId = args[1];
		
		MapList sendDeptFindList = DomainObject.findObjects(context, "SPLM_SendDept", "*",
				"-", "*", PRODUCTION_VAULT, "attribute[SPLM_IsDefaultDept]==true", null, false, new StringList(DomainConstants.SELECT_NAME),
				Short.parseShort("0"));
		
		if (sendDeptFindList.isEmpty()) {
			return flag;
		}
		
		StringList sendDeptList = new StringList();
		String sendDeptNo;
		for (Object sendDept : sendDeptFindList) {
			sendDeptNo = (String) ((Map) sendDept).get(DomainConstants.SELECT_NAME);
			if (sendDeptList.contains(sendDeptNo)) {
				continue;
			}
			sendDeptList.add(sendDeptNo);
		}
		
		DomainObject soObj = new DomainObject(objectId);
		soObj.setAttributeValue(context, "SPLM_SendDept", String.join(SPLIT_KEY, sendDeptList));
		return flag;
	}
	
	public int checkHasGroupDrawing(Context context, String[] args)
			throws Exception {
		int flag = 0;
		try {
			String objId = args[0];
			DomainObject partsCatalogueObj = new DomainObject(objId);
			String epcObjId = partsCatalogueObj.getInfo(context, "from[SPLM_Related_EPC_PIC].to[SPLM_EPC_PIC].id");
			if (StringUtils.isEmpty(epcObjId)) {
				throw new Exception("\u7f3a\u5c11\u5927\u5c0f\u55ae\u5716");
			}
			
			String groupCode;
			StringList groupCodeList = new StringList();
			DomainObject epcObj = new DomainObject(epcObjId);
			StringList relatedImageList = epcObj.getInfoList(context, "from[SPLM_EPC_PIC_Image].to[Image].name");
			
			for (String imageName : relatedImageList) {
				if (!imageName.contains("_")) {
					continue;
				}
				groupCode = imageName.split("_")[1];
				if (groupCodeList.contains(groupCode)) {
					continue;
				}
				groupCodeList.add(groupCode);
			}
			
			if (groupCodeList.isEmpty()) {
				throw new Exception("\u5927\u5c0f\u55ae\u5716\u7f3a\u5c11\u5716\u6a94");
			}
			
			StringList relatedGroupCodeList = partsCatalogueObj.getInfoList(context, "from[SPLM_RelatedGroupDrawing].to[SPLM_GroupDrawing].attribute[SPLM_GroupCode]");
			for (String relatedGroupCode : relatedGroupCodeList) {
				if (!groupCodeList.contains(relatedGroupCode)) {
					continue;
				}
				groupCodeList.remove(relatedGroupCode);
			}
			
			if (!groupCodeList.isEmpty()) {
				throw new Exception("\u7f3a\u5c11\u4ee5\u4e0b\u55ae\u5143\u4ee3\u78bc\u7684\u55ae\u5143\u5716\n" + String.join(",", groupCodeList));
			}
			
		} catch (Exception e) {
			flag = 1;
			emxContextUtil_mxJPO.mqlError(context, e.getMessage());
		}
		return flag;
	}
	
	public int demoteAffectedItemToPreliminary(Context context, String[] args)
			throws Exception {
		int flag = 0;
		String objId = args[0];
		DomainObject soObj = new DomainObject(objId);
		StringList affectedItemIdList = soObj.getInfoList(context, "from[SPLM_AffectedItem].to.id");
		DomainObject affectedItemObj = new DomainObject();
		ContextUtil.pushContext(context);
		for (String affectedItemId : affectedItemIdList) {
			affectedItemObj.setId(affectedItemId);
			if (!"Release".equals(affectedItemObj.getInfo(context, DomainConstants.SELECT_CURRENT))) {
				continue;
			}
			affectedItemObj.setState(context, "Preliminary");
		}
		ContextUtil.popContext(context);
		return flag;
	}
	
	public int hasAffectedItem(Context context, String[] args)
			throws Exception {
		int flag = 0;
		String objId = args[0];
		try {
			DomainObject soObj = new DomainObject(objId);
			StringList affectedItemIdList = soObj.getInfoList(context, "from[SPLM_AffectedItem].to.id");
			if (affectedItemIdList.isEmpty()) {
				throw new Exception("\u8acb\u6dfb\u52a0\u5f71\u97ff\u4ef6\u865f");
			}
		} catch (Exception e) {
			emxContextUtil_mxJPO.mqlError(context, e.getMessage());
			throw new FrameworkException(e.getMessage());
		}
		return flag;
	}
	
	public int checkSORequiredAttribute(Context context, String[] args)
			throws Exception {
		int flag = 0;
		String objId = args[0];
		try {
			DomainObject soObj = new DomainObject(objId);
		
		} catch (Exception e) {
			emxContextUtil_mxJPO.mqlError(context, e.getMessage());
			throw new FrameworkException(e.getMessage());
		}
		return flag;
	}
	
	public int checkAffectedItemInReview(Context context, String[] args)
			throws Exception {
		int flag = 0;
		String objId = args[0];
		try {
			this.checkAICurrent(context, objId);
			this.checkSOReleasePB(context, objId);
		} catch (Exception e) {
			emxContextUtil_mxJPO.mqlError(context, e.getMessage());
			throw new FrameworkException(e.getMessage());
		}
		return flag;
	}
	
	public int checkSOZipDownload(Context context, String[] args)
			throws Exception {
		int flag = 0;
		String objId = args[0];
		try {
			DomainObject soObj = new DomainObject(objId);
			String releaseSO = soObj.getAttributeValue(context, "SPLM_ReleaseSO");
			String releasePB = soObj.getAttributeValue(context, "SPLM_ReleasePB");
			String soExcelFlag = soObj.getAttributeValue(context, "SPLM_SOExcelFlag");
			if (("true".equalsIgnoreCase(releaseSO)
					|| "true".equalsIgnoreCase(releasePB))
					&& "Processing".equals(soExcelFlag)) {
				throw new Exception("\u8acb\u6309\u4e0b\u7522\u751fSO/PB Excel");
			}
		} catch (Exception e) {
			emxContextUtil_mxJPO.mqlError(context, e.getMessage());
			throw new FrameworkException(e.getMessage());
		}
		return flag;
	}
	
	private void checkAICurrent(Context context, String soId) throws Exception {
		DomainObject soObj = new DomainObject(soId);
		StringList affectedItemIdList = soObj.getInfoList(context, "from[SPLM_AffectedItem].to.id");
		DomainObject affectedItemObj = new DomainObject();
		String affectedItemName, affectedItemCurrent;
		String i18nReview = i18nNow.getStateI18NString("SPLM_ServicePart", "Review", context.getSession().getLanguage());
		StringBuffer errMsg = new StringBuffer();
		for (String affectedItemId : affectedItemIdList) {
			affectedItemObj.setId(affectedItemId);
			affectedItemName = affectedItemObj.getInfo(context, DomainConstants.SELECT_NAME);
			affectedItemCurrent = affectedItemObj.getInfo(context, DomainConstants.SELECT_CURRENT);
			if ("Review".equals(affectedItemCurrent)) {
				continue;
			}
			errMsg.append(affectedItemName + " \u72c0\u614b\u4e0d\u518d " + i18nReview).append("\n");
		}
		if (errMsg.length() != 0) {
			throw new Exception(errMsg.toString());
		}
	}
	
	private void checkSOReleasePB(Context context, String soId) throws Exception {
		DomainObject soObj = new DomainObject(soId);
		String releasePB = soObj.getAttributeValue(context, "SPLM_ReleasePB");
		String partsCatalogue = soObj.getAttributeValue(context, "SPLM_PartsCatalogue");
		if ("true".equalsIgnoreCase(releasePB)
				&& StringUtils.isEmpty(partsCatalogue)) {
			throw new Exception("\u8acb\u586b\u5beb\u96f6\u4ef6\u518a");
		}
	}
	
	public int releaseAffectedItem(Context context, String[] args)
			throws Exception {
		int flag = 0;
		String objId = args[0];
		DomainObject soObj = new DomainObject(objId);
		StringList affectedItemIdList = soObj.getInfoList(context, "from[SPLM_AffectedItem].to.id");
		DomainObject affectedItemObj = new DomainObject();
		String affectedItemState;
		ContextUtil.pushContext(context);
		for (String affectedItemId : affectedItemIdList) {
			affectedItemObj.setId(affectedItemId);
			
			affectedItemObj.setState(context, "Release");
			affectedItemObj.setAttributeValue(context, "SPLM_CheckStatus", "CHECKED");
			
			this.resetServicePartIntegrationFlag(context, affectedItemObj);
			this.resetSOServicePartIntegrationFlag(context, affectedItemObj);
		}
		ContextUtil.popContext(context);
		return flag;
	}
	
	private void resetServicePartIntegrationFlag(Context context, DomainObject partObj) throws Exception {
		String partType = partObj.getInfo(context, DomainConstants.SELECT_TYPE);
		if ("SPLM_Part,SPLM_ColorPart".contains(partType)) {
			partObj.setAttributeValue(context, "SPLM_ServicePart_ERP310", "Complete");
			partObj.setAttributeValue(context, "SPLM_ServicePart_ERP300", "Complete");
			partObj.setAttributeValue(context, "SPLM_ServicePart_DMS", "Complete");
		}
	}
	
	private void resetSOServicePartIntegrationFlag(Context context, DomainObject partObj) throws Exception {
		String partType = partObj.getInfo(context, DomainConstants.SELECT_TYPE);
		if ("SPLM_Part,SPLM_ColorPart".contains(partType)) {
			partObj.setAttributeValue(context, "SPLM_DMS_Sync", "False");
			partObj.setAttributeValue(context, "SPLM_ERP1300_Sync", "False");
			partObj.setAttributeValue(context, "SPLM_ERP9000_Sync", "False");
			partObj.setAttributeValue(context, "SPLM_ERP9001_Sync", "False");
		}
	}
	
	public HashMap<String, String> setSendERP_DMSFlagFromWeb(Context context, String[] args) throws Exception {
		HashMap<String, String> returnMap = new HashMap<String, String>();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		ContextUtil.pushContext(context);
		this.setSendERP_DMSFlag(context, new String[] {objectId});
		ContextUtil.popContext(context);
		return returnMap;
	}
	
	public int setSendERP_DMSFlag(Context context, String[] args)
			throws Exception {
		int flag = 0;
		String objId = args[0];
		DomainObject soObj = new DomainObject(objId);
		soObj.setAttributeValue(context, "SPLM_SendToERP_DMS", "Processing");
		return flag;
	}
	
	public int setSOExcelFlag(Context context, String[] args)
			throws Exception {
		int flag = 0;
		String objId = args[0];
		DomainObject soObj = new DomainObject(objId);
		soObj.setAttributeValue(context, "SPLM_SOExcelFlag", "Processing");
		return flag;
	}
	
	public MapList getRelatedSOReleaseSODrawing(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Document",
				"SPLM_RelatedSOReleaseItem", null, "attribute[SPLM_SOReleaseItemType]=='Vendor'", "from", (short) 1);
		return objectList;
	}
	
	public MapList getRelatedSOReleaseVendor(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Vendor",
				"SPLM_RelatedSOReleaseItem", null, null, "from", (short) 1);
		return objectList;
	}
	
	public MapList getRelatedSOReleasePBDrawing(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Document",
				"SPLM_RelatedSOReleaseItem", null, "attribute[SPLM_SOReleaseItemType]=='Dealer'", "from", (short) 1);
		return objectList;
	}
	
	public MapList getRelatedSOReleaseDealer(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Dealer",
				"SPLM_RelatedSOReleaseItem", null, null, "from", (short) 1);
		return objectList;
	}
	
	public StringList includeSOReleaseItem (Context context, String[] args) throws Exception {
		StringList includeList = new StringList();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Document",
				"SPLM_ReferenceDoc", "", null, "from", (short) 1);
		String id;
		for (Object object : objectList) {
			id = (String)((Map) object).get(DomainConstants.SELECT_ID);
			if (includeList.contains(id)) {
				continue;
			}
			includeList.add(id);
		}
		return includeList;
	}
	
	public StringList includeSOReleaseVendor (Context context, String[] args) throws Exception {
		StringList includeList = new StringList();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Vendor",
				"SPLM_RelatedVendor", null, null, "from", (short) 1);
		String id;
		for (Object object : objectList) {
			id = (String)((Map) object).get(DomainConstants.SELECT_ID);
			if (includeList.contains(id)) {
				continue;
			}
			includeList.add(id);
		}
		return includeList;
	}
	
	public StringList includeSOReleaseDealer (Context context, String[] args) throws Exception {
		StringList includeList = new StringList();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objId = (String) programMap.get("objectId");
		DomainObject obj = new DomainObject(objId);
		MapList glnoList = obj.getRelatedObjects(context, "SPLM_SBOM,SPLM_MBOM",
				"SPLM_Part,SPLM_PLVC,SPLM_ModelCode,SPLM_GLNO", new StringList(DomainConstants.SELECT_ID), null, true,
				false, (short) -1, "", null, (short) 0, new Pattern("SPLM_GLNO"), null, null);
		if (glnoList.isEmpty()) {
			return includeList;
		}
		
		String glnoId;
		DomainObject glnoObj = new DomainObject();
		StringList partsCatalogueIdList = new StringList();
		for (Object temp : glnoList) {
			glnoId = (String)((Map) temp).get(DomainConstants.SELECT_ID);
			glnoObj.setId(glnoId);
			StringList tempPartsCatalogueIdList = glnoObj.getInfoList(context, "from[SPLM_RelatedPartsCatalogue].to[SPLM_PartsCatalogue].id");
			for (String partsCatalogueId : tempPartsCatalogueIdList) {
				if (partsCatalogueIdList.contains(partsCatalogueId)) {
					continue;
				}
				partsCatalogueIdList.add(partsCatalogueId);
			}
		}
		DomainObject partsCatalogueObj = new DomainObject();
		StringList dealerIdList;
		for (String partsCatalogueId : partsCatalogueIdList) {
			partsCatalogueObj.setId(partsCatalogueId);
			dealerIdList = partsCatalogueObj.getInfoList(context, "from[SPLM_RelatedDealer].to[SPLM_Dealer].id");
			for (String dealerId : dealerIdList) {
				if (includeList.contains(dealerId)) {
					continue;
				}
				includeList.add(dealerId);
			}
		}
		return includeList;
	}
	
	public HashMap<String, String> connectSOReleaseDealerItem(Context context, String[] args) throws Exception {
		return this.connectSOReleaseItem(context, args, "Dealer");
	}	
	
	public HashMap<String, String> connectSOReleaseVendorItem(Context context, String[] args) throws Exception {
		return this.connectSOReleaseItem(context, args, "Vendor");
	}
	
	private HashMap<String, String> connectSOReleaseItem(Context context, String[] args, String itemType) throws Exception {
		
		HashMap<String, String> returnMap = new HashMap<String, String>();
		Map programMap = JPO.unpackArgs(args);
		String partId = (String) programMap.get("objectId");
		String[] emxTableRowIds = (String[]) programMap.get("emxTableRowIds");
		try {
			String language = context.getSession().getLanguage();
			
			DomainObject partObj = new DomainObject(partId);
			MapList soList = CMC_Method_JPO_mxJPO.expandObject(context, partId, "SPLM_SO", "SPLM_AffectedItem",
					"current!='Complete'", "", "to", (short) 0);
			if (soList.isEmpty()) {
				throw new Exception("\u7f3a\u5c11SO");
			}
			String soId = (String) ((Map) soList.get(0)).get(DomainConstants.SELECT_ID);
			
			DomainObject soObj = new DomainObject(soId);
			String soName = soObj.getInfo(context, DomainConstants.SELECT_NAME);
			
			DomainObject selectedObj = new DomainObject();
			String selectedObjId, selectedObjType, selectedObjName;
			String selectedObjTypeI18n;
			MapList relatedSelectedList;
			StringBuffer errMsg = new StringBuffer();
			for (String rowId : emxTableRowIds) {
				selectedObjId = CMC_Method_JPO_mxJPO.getTableRowObjectId(rowId);
				selectedObj.setId(selectedObjId);
				
				selectedObjType = selectedObj.getInfo(context, DomainConstants.SELECT_TYPE);
				selectedObjName = selectedObj.getInfo(context, DomainConstants.SELECT_NAME);
				
				relatedSelectedList = CMC_Method_JPO_mxJPO.expandObject(context, partId, selectedObjType, "SPLM_RelatedSOReleaseItem",
						"id=='" + selectedObjId + "'", "attribute[SPLM_SO]=='" + soName + "' && attribute[SPLM_SOReleaseItemType]=='" + itemType + "'", "from", (short) 0);
				
				if (!relatedSelectedList.isEmpty()) {
					selectedObjTypeI18n = i18nNow.getTypeI18NString(selectedObjType, language);
					errMsg.append(selectedObjTypeI18n + ":" + selectedObjName + " SO:" + soName + " \u5df2\u5b58\u5728").append("\n");
					continue;
				}
				
				DomainRelationship relObj = DomainRelationship.connect(context, partObj, "SPLM_RelatedSOReleaseItem", selectedObj);
				relObj.setAttributeValue(context, "SPLM_SO", soName);
				relObj.setAttributeValue(context, "SPLM_SOReleaseItemType", itemType);
			}
			if (errMsg.length() != 0) {
				throw new Exception(errMsg.toString());
			}
		} catch (Exception e) {
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	
	public Vector getEBOMSpecCount(Context context, String args[])
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		Vector columnVals = new Vector();
		Map objectMap;
		Iterator objectListIterator = objectList.iterator();
		String objectId;
		DomainObject obj = new DomainObject();
		StringBuffer sb;
		int specCount;
		String url = "../common/emxIndentedTable.jsp?sortColumnName=Name&SuiteDirectory=engineeringcentral&showPageHeader=true&emxSuiteDirectory=engineeringcentral&program=emxCommonDocumentUI:getDocuments&portalMode=true&StringResourceFileId=emxEngineeringCentralStringResource&showRMB=false&sortDirection=ascending&freezePane=Name%2CTitle&selection=multiple&"
				+ "parentRelName=relationship_PartSpecification&header=emxEngineeringCentral.Category.SpecsAndDocuments&suiteKey=EngineeringCentral&portal=ENCSpecsAndDocsPortal&table=ENCDocumentSummary&objectId=";
		while (objectListIterator.hasNext()) {
			objectMap = (Map) objectListIterator.next();
			objectId = (String) objectMap.get(DomainConstants.SELECT_ID);
			obj.setId(objectId);
			specCount = obj.getInfoList(context, "from[Part Specification].id").size();
			sb = new StringBuffer();
			sb.append("<p>");
			sb.append("<a href=\"JavaScript:emxTableColumnLinkClick('" + XSSUtil.encodeForURL(context, url + objectId)  + "', '700', '600', 'false', 'popup', '', '" + specCount+ "', 'false', '', '')\" class=\"object\">");
			sb.append(specCount);
			sb.append("</a>");	
			sb.append("</p>");
			
			
			columnVals.add(sb.toString());
		}
		return columnVals;
	}
	
	public HashMap getServicePartLocationRange(Context context, String[] args)
			throws Exception {
		HashMap viewMap = new HashMap();
		
		StringList displayList = new StringList();
		StringList valueList = new StringList();
		
		String[] locations = {"1300", "9000", "9001"};
		
		Map info;
		for (String location : locations) {
			valueList.add(location);
			displayList.add(location);
		}
		
		viewMap.put("field_choices", valueList);
		viewMap.put("field_display_choices", displayList);
		return viewMap;
	}
	
	public void updateServicePartLocationRange(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
	    Map paramMap = (Map) programMap.get("paramMap");
	    String[] newValues = (String[]) paramMap.get("New Values");
	    String objectId = (String) paramMap.get("objectId");
	    DomainObject object = new DomainObject(objectId);
	    object.setAttributeValue(context, "SPLM_Location", newValues == null ? "" : String.join(",", newValues));
	}
	
	public MapList getSubDealer(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Dealer",
				"SPLM_SubDealer", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public Map getPNCAutoName(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		Map fieldValues   = (Map) programMap.get("fieldValues");
		String from = (String) fieldValues.get("SPLM_PNC_From");
		String codeType = (String) fieldValues.get("SPLM_PNC_CodeType");

		if (!"CMC".equals(from) || StringUtils.isEmpty(codeType)) {
			Map resultMap = new HashMap();
			resultMap.put("SelectedValues", "");
			resultMap.put("SelectedDisplayValues", "");
			return resultMap;
		}
		String name = "";
		
		if ("Four-wheel".equals(codeType)) {
			name = (String) fieldValues.get("SPLM_UPG");
		} else if ("Two-wheel".equals(codeType)) {
			name = (String) fieldValues.get("SPLM_EUPG");
		} else if ("Not Finished Car".equals(codeType)) {
			name = (String) fieldValues.get("SPLM_CommissionGroup");
		}
		
		String where = "attribute[SPLM_PNC_CodeType]=='" + codeType + "'";
		MapList pncList = DomainObject.findObjects(context, "SPLM_PNC", name + "*",
				"-", "*", PRODUCTION_VAULT, where, null, false, new StringList(DomainConstants.SELECT_ID),
				Short.parseShort("0"));
		int nextPNCIndex = pncList.size() + 1;
		
		if ("Four-wheel".equals(codeType)) {
			name = name + String.format("%04d", nextPNCIndex);
		} else if ("Two-wheel".equals(codeType)) {
			name = name + String.format("%03d", nextPNCIndex);
		} else if ("Not Finished Car".equals(codeType)) {
			name = name + String.format("%05d", nextPNCIndex);
		}
		
		Map resultMap = new HashMap();
		resultMap.put("SelectedValues", name);
		resultMap.put("SelectedDisplayValues", name);
		return resultMap;
	}
	
	public HashMap<String, String> getRedirectToEPCURL(Context context, String[] args) throws Exception {
		
		Map programMap = JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		String ip = (String) programMap.get("IP");
		String[] emxTableRowIds = (String[]) programMap.get("emxTableRowIds");
		
		String errMsg = objectId + ":" + ip;
		HashMap<String, String> returnMap = new HashMap<String, String>();
		returnMap.put("Redirect", "https://www.china-motor.com.tw/");
		returnMap.put("Message", errMsg);
		return returnMap;
	}
	
	public String getTYPE0002URL(Context context, String[] args)
			throws Exception {
		String env = System.getenv("PLM_ENV");
		if (StringUtils.isEmpty(env) || !"PRD,UAT".contains(env)) {
			return "";
		}
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = (String) paramMap.get("objectId");
		DomainObject obj = new DomainObject(objectId);
		String objName = obj.getInfo(context, DomainConstants.SELECT_NAME);
		return this.getTYPEURL(context, objName, "TYPE_0002");
	}
	
	public String getTYPE0020URL(Context context, String[] args)
			throws Exception {
		String env = System.getenv("PLM_ENV");
		if (StringUtils.isEmpty(env) || !"PRD,UAT".contains(env)) {
			return "";
		}
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = (String) paramMap.get("objectId");
		DomainObject obj = new DomainObject(objectId);
		String objName = obj.getInfo(context, DomainConstants.SELECT_NAME);
		return this.getTYPEURL(context, objName, "TYPE_0020");
	}
	
	private String getTYPEURL(Context context, String partNo, String urlType)
			throws Exception {
		String env = System.getenv("PLM_ENV");
		if (!"PRD,UAT".contains(env)) {
			return "";
		}
		String url = i18nNow.getI18nString(ERP_CAQS + "." + env, "emxSPLM", "");
		
		String input = "{ \"MATNR\":\"" + partNo + "\" }";
		StringHolder message = new StringHolder();
		StringHolder output = new StringHolder();
		StringHolder status = new StringHolder();
		YQM_SPLM_GET_ISIR_DOC_ServiceLocator locator = new YQM_SPLM_GET_ISIR_DOC_ServiceLocator();
		locator.getYQM_SPLM_GET_ISIR_DOC(new URL(url)).yqmSplmGetIsirDoc(input, message, output, status);
		JSONObject outputObj = new JSONObject(output.value);

		
		String urlCAQS = outputObj.getString(urlType);
		if (StringUtils.isEmpty(urlCAQS)) {
			return "";
		}
		
		String downloadURL = "../JCI/SPLM_CAQSDownload.jsp?url=";
		urlCAQS = Base64.getEncoder().encodeToString(urlCAQS.getBytes());
		
		StringBuffer result = new StringBuffer();
		result.append("<a href=\"" + XSSUtil.encodeForHTML(context, downloadURL + urlCAQS) + "\" ");
		result.append("style=\"color:blue;\" ");
		result.append("target=\"_blank\" ");
		result.append(">");
		result.append("URL");
		result.append("</a>");
		
		return result.toString();
	}
	
	public MapList expandPartsCatalogueData(Context context, String[] args)
			throws Exception {
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		String relWhere = "";
		
		String objectId = (String) paramMap.get("objectId");
		String expandRel = (String) paramMap.get("relType");
		String sExpandLevels = (String) paramMap.get("emxExpandFilter");
		int expandLevel = "All".equals(sExpandLevels) ? 0 : Integer
				.parseInt(sExpandLevels);
		DomainObject partObj = new DomainObject(objectId);
		
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		
		StringList relSelects = new StringList();
		relSelects.add(DomainRelationship.SELECT_LEVEL);
		relSelects.add(DomainRelationship.SELECT_ID);
		
		MapList retList = partObj.getRelatedObjects(context,
				expandRel, "*", busSelects, relSelects,
				false,
				true,
				(short) expandLevel, "", relWhere, 0);
		return retList;
	}
	
	public Vector getPartsCatalogueDataName(Context context, String[] args)
			throws Exception {
		Vector result = new Vector();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		if (objectList.size() <= 0) {
			return result;
		}
		StringList objSelects = new StringList();
		objSelects.add(DomainConstants.SELECT_TYPE);
		objSelects.add(DomainConstants.SELECT_NAME);
		objSelects.add("attribute[SPLM_GroupCode]");
		
		Map dataInfo;
		String dataId, dataType, dataName, dataGroupCode, sortKey, treeLink;
		DomainObject obj = new DomainObject();
		for (Object temp : objectList) {
			dataInfo = (Map) temp;
			dataId = (String) dataInfo.get(DomainConstants.SELECT_ID);
			obj.setId(dataId);
			
			dataInfo = obj.getInfo(context, objSelects);
			dataType = (String) dataInfo.get(DomainConstants.SELECT_TYPE);
			dataName = (String) dataInfo.get(DomainConstants.SELECT_NAME);
			dataGroupCode = (String) dataInfo.get("attribute[SPLM_GroupCode]");
			
			if ("SPLM_GroupDrawing".equals(dataType)) {
				dataGroupCode = StringUtils.isEmpty(dataGroupCode) ? "\u7f3a\u5c11\u55ae\u5143\u4ee3\u78bc" : dataGroupCode;
				dataName = dataGroupCode;
			}
			sortKey = "<div style=\"display: none\">" + dataName + "</div>";
			treeLink = CMC_Method_JPO_mxJPO.getTableTreeLink(dataId, dataName);
			
			result.add(sortKey + treeLink);
		}
		return result;
	}
	
	public Vector getPartsCatalogueDataGroupDrawing(Context context, String[] args)
			throws Exception {
		Vector result = new Vector();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		if (objectList.size() <= 0) {
			return result;
		}
		
		StringList objSelects = new StringList();
		objSelects.add(DomainConstants.SELECT_TYPE);
		objSelects.add(DomainConstants.SELECT_NAME);
		
		Map dataInfo;
		String dataId, dataType, dataName;
		DomainObject obj = new DomainObject();
		for (Object temp : objectList) {
			dataInfo = (Map) temp;
			dataId = (String) dataInfo.get(DomainConstants.SELECT_ID);
			obj.setId(dataId);
			
			dataInfo = obj.getInfo(context, objSelects);
			dataType = (String) dataInfo.get(DomainConstants.SELECT_TYPE);
			dataName = (String) dataInfo.get(DomainConstants.SELECT_NAME);
			
			if (!"SPLM_GroupDrawing".equals(dataType)) {
				dataName = "";
			}
			
			result.add(dataName);
		}
		return result;
	}
	
	public Vector getPartsCatalogueDataRevision(Context context, String[] args)
			throws Exception {
		Vector result = new Vector();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		if (objectList.size() <= 0) {
			return result;
		}
		
		StringList objSelects = new StringList();
		objSelects.add(DomainConstants.SELECT_TYPE);
		objSelects.add(DomainConstants.SELECT_REVISION);
		
		Map dataInfo;
		String dataId, dataType, dataRev;
		DomainObject obj = new DomainObject();
		for (Object temp : objectList) {
			dataInfo = (Map) temp;
			dataId = (String) dataInfo.get(DomainConstants.SELECT_ID);
			obj.setId(dataId);
			
			dataInfo = obj.getInfo(context, objSelects);
			dataType = (String) dataInfo.get(DomainConstants.SELECT_TYPE);
			dataRev = (String) dataInfo.get(DomainConstants.SELECT_REVISION);
			
			if (!"SPLM_GroupDrawing".equals(dataType)) {
				dataRev = "";
			}
			
			result.add(dataRev);
		}
		return result;
	}
	
	public Vector getAffectedChildNo(Context context, String args[]) throws Exception {
		return this.getAffectedChildInfo(context, args, DomainConstants.SELECT_NAME);
	}
	
	public Vector getAffectedChildEO(Context context, String args[]) throws Exception {
		return this.getAffectedChildInfo(context, args, "attribute[SPLM_RelatedEO]");
	}
	
	public Vector getAffectedChildPO(Context context, String args[]) throws Exception {
		return this.getAffectedChildInfo(context, args, "attribute[SPLM_RelatedPO]");
	}
	
	private Vector getAffectedChildInfo(Context context, String args[], String info) throws Exception {
		Vector result = new Vector();
		Map programMap = (Map) JPO.unpackArgs(args);	
		Map paramList = (Map) programMap.get("paramList");
		String soId = (String) paramList.get("objectId");
		DomainObject soObj = new DomainObject(soId);
		StringList affectedItemList = soObj.getInfoList(context, "from[SPLM_AffectedItem].to.id");
		StringBuffer expandWhere = new StringBuffer();
		for (String affectedItemId : affectedItemList) {
			if (expandWhere.length() > 0) {
				expandWhere.append(" || ");
			}
			expandWhere.append("(id == '" + affectedItemId + "')");
		}
		
		MapList objectList = (MapList) programMap.get("objectList");
		Map affectedItemMap;
		String affectedItemId;
		MapList affectedChildList;
		
		StringList relSelects = new StringList();
		relSelects.add("attribute[SPLM_RelatedEO]");
		relSelects.add("attribute[SPLM_RelatedPO]");

		for (Object temp : objectList) {
			affectedItemMap = (Map) temp;
			affectedItemId = (String) affectedItemMap.get(DomainConstants.SELECT_ID);
			affectedChildList = CMC_Method_JPO_mxJPO.expandObject(context, affectedItemId, "*", "SPLM_SBOM,SPLM_MBOM", expandWhere.toString(), "", "from", (short) 1, null, relSelects);
			if (affectedChildList.isEmpty()) {
				result.add("");
				continue;
			}
			StringBuffer affectedChildInfo = new StringBuffer();
			for (Object object : affectedChildList) {
				affectedChildInfo.append("<p>").append((String) ((Map) object).get(info)).append("</p>");
			}
			result.add(affectedChildInfo.toString());
		}
		return result;
	}
	
	public Vector getSpecAndReferenceDocCount(Context context, String[] args) throws Exception
    {
		Vector result = new Vector();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		String objId;
		StringList referenceList;
		DomainObject partObj = new DomainObject();
		for (Object object : objectList) {
			objId = (String) ((Map) object).get(DomainConstants.SELECT_ID);
			partObj.setId(objId);
			referenceList = partObj.getInfoList(context, "from[SPLM_ReferenceDoc].to.id");
			result.add(String.valueOf(referenceList.size()));
		}
		return result;
    }
	
	public Vector getGroupDrawingName(Context context, String[] args) throws Exception
    {
		Vector result = new Vector();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		String plvcId;
		MapList specificationList;
		StringList groupDrawingNameList;
		Map specInfo;
		String specId, specName, specSerialNumber;
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add(DomainConstants.SELECT_NAME);
		
		DomainObject plvcObj = new DomainObject();
		DomainObject specObj = new DomainObject();
		
		for (Object plvcInfo : objectList) {
			plvcId = (String) ((Map) plvcInfo).get(DomainConstants.SELECT_ID);
			plvcObj.setId(plvcId);
			
			specificationList =  plvcObj.getRelatedObjects(context,
					"SPLM_PartSpecification", "SPLM_GroupDrawing", busSelects, null,
					false,
					true,
					(short) 1, "", "", 0);
			
			if (specificationList.isEmpty()) {
				result.add("");
				continue;
			}
			
			groupDrawingNameList = new StringList();
			
			for (Object specObject : specificationList) {
				specInfo = (Map) specObject;
				specId = (String) specInfo.get(DomainConstants.SELECT_ID);
				specName = (String) specInfo.get(DomainConstants.SELECT_NAME);
				
				specObj.setId(specId);
			
				specSerialNumber = specObj.getAttributeValue(context, "SPLM_SerialNumber");
				
				groupDrawingNameList.add("<p>" + CMC_Method_JPO_mxJPO.getTableTreeLink(specId, specName + "(" + specSerialNumber + ")") + "</p>");
			}
			result.add(String.join("", groupDrawingNameList));
		}
		return result;
    }
	
	public HashMap getMaterielKDTypeRange(Context context, String[] args)
			throws Exception {
		HashMap viewMap = new HashMap();
		StringList strListDisplay = new StringList();
		StringList strListValue = new StringList();
		String[] attRangeSort = { "K-", "D-", "*-" };
		for (String range : attRangeSort) {
			strListValue.add(range);
			strListDisplay.add(range);
		}
		viewMap.put("field_choices", strListValue);
		viewMap.put("field_display_choices", strListDisplay);
		return viewMap;
	}
	
	public HashMap getValidRange(Context context, String[] args)
			throws Exception {
		String language = context.getSession().getLanguage();
		HashMap viewMap = new HashMap();
		StringList strListDisplay = new StringList();
		StringList strListValue = new StringList();
		String[] attRangeSort = { "Y", "N", "*-" };
		for (String range : attRangeSort) {
			strListValue.add(range);
			strListDisplay.add(i18nNow.getRangeI18NString("SPLM_Valid", range, language));
		}
		viewMap.put("field_choices", strListValue);
		viewMap.put("field_display_choices", strListDisplay);
		return viewMap;
	}
	
	public HashMap<String, String> connectPNC(Context context, String[] args) throws Exception {
		
		HashMap<String, String> returnMap = new HashMap<String, String>();
		Map programMap = JPO.unpackArgs(args);
		String groupDrawingId = (String) programMap.get("objectId");
		String[] emxTableRowIds = (String[]) programMap.get("emxTableRowIds");
		try {
			DomainObject groupDrawingObj = new DomainObject(groupDrawingId);
			StringList relatedPNCList = groupDrawingObj.getInfoList(context, "from[SPLM_Related_PNC].to.id");
			DomainObject selectedObj = new DomainObject();
			String selectedObjId, selectedObjName;
			MapList partSOList;
			for (String rowId : emxTableRowIds) {
				selectedObjId = CMC_Method_JPO_mxJPO.getTableRowObjectId(rowId);
				selectedObj.setId(selectedObjId);
				selectedObjName = selectedObj.getInfo(context, DomainConstants.SELECT_NAME);
				if (relatedPNCList.contains(selectedObjId)) {
					throw new Exception(selectedObjName + " \u5df2\u5b58\u5728");
				}
				DomainRelationship.connect(context, groupDrawingObj, "SPLM_Related_PNC", selectedObj);
			}
		} catch (Exception e) {
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	public HashMap<String, String> copyPartAttributesForClone(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String sourceId = (String) requestMap.get("bomObjectId");
		String targetId = (String) paramMap.get("newObjectId");
		
		String[] copyAttributes = { "SPLM_MaterialType", "SPLM_MaterialGroup", "SPLM_Location", "SPLM_Location_DMS",
				"SPLM_PurchasingGroup", "SPLM_OrderType", "SPLM_OverseaSalesOnly", "SPLM_DTAT_Only",
				"SPLM_ModelSeries", "SPLM_ModelSeriesID", "SPLM_ByGLNO", "SPLM_CMC_Code", "SPLM_CommissionGroup", "SPLM_Unit" };
		return copyPartAttributesAndVendor(context, sourceId, targetId, copyAttributes);
	}
	
	public HashMap<String, String> copyPartAttributesForNew(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String sourceId = (String) requestMap.get("bomObjectId");
		String targetId = (String) paramMap.get("newObjectId");
		
		DomainObject sourceObj = new DomainObject(sourceId);
		DomainObject targetObj = new DomainObject(targetId);
		if (!"SPLM_Part".equals(sourceObj.getInfo(context, DomainConstants.SELECT_TYPE))
				|| !"SPLM_Part".equals(targetObj.getInfo(context, DomainConstants.SELECT_TYPE))) {
			return new HashMap<String, String>();
		}
		String[] copyAttributes = { "SPLM_ModelSeries",  "SPLM_ModelSeriesID", "SPLM_ByGLNO", "SPLM_CMC_Code" };
		return copyPartAttributesAndVendor(context, sourceId, targetId, copyAttributes);
	}
	
	private HashMap<String, String> copyPartAttributesAndVendor(Context context, String sourceId, String targetId, String[] copyAttributes) throws Exception {
		HashMap<String, String> returnMap = new HashMap<String, String>();
		
		try {
			DomainObject bomObject = new DomainObject(sourceId);
			String bomObjDesc = bomObject.getInfo(context, DomainConstants.SELECT_DESCRIPTION);

			DomainObject newObject = new DomainObject(targetId);
			newObject.setDescription(context, bomObjDesc);
			
			
			for (String att : copyAttributes) {
				newObject.setAttributeValue(context, att, bomObject.getAttributeValue(context, att));
			}
			
			this.copyValidVendor(context, sourceId, targetId);
			
		} catch (Exception e) {
			returnMap.put("Action", "stop");
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	
	private void copyValidVendor(Context context, String sourceId, String targetId) throws Exception {
		DomainObject targetObj = new DomainObject(targetId);
		
		MapList vendorList = CMC_Method_JPO_mxJPO.expandObject(context, sourceId, "SPLM_Vendor",
				"SPLM_RelatedVendor", null, "attribute[SPLM_Valid]=='Y'", "from", (short) 1);
		
		Map vendorMap;
		String vendorId, relatedVendorId;
		DomainRelationship oldRelObj, newRelObj;
		DomainObject vendorObj = new DomainObject();
		for (Object tempVendor : vendorList) {
			vendorMap = (Map) tempVendor;
			vendorId = (String) vendorMap.get(DomainConstants.SELECT_ID);
			vendorObj.setId(vendorId);
			
			relatedVendorId = (String) vendorMap.get(DomainRelationship.SELECT_ID);
			oldRelObj = new DomainRelationship(relatedVendorId);
			
			newRelObj = DomainRelationship.connect(context, targetObj, "SPLM_RelatedVendor", vendorObj);
			newRelObj.setAttributeValues(context, oldRelObj.getAttributeMap(context));
		}
	}
	
	public String showSOSendDept(Context context, String[] args)
			throws Exception {
		String env = System.getenv("PLM_ENV");
		String language = context.getSession().getLanguage();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = (String) paramMap.get("objectId");
		boolean viewMode = "view".equalsIgnoreCase(((String) requestMap
				.get("mode")));

		DomainObject obj = new DomainObject(objectId);
		List<String> depts = Arrays.asList(obj.getAttributeValue(context,
				"SPLM_SendDept").split(SPLIT_KEY));
		StringBuffer result = new StringBuffer();
		try {
			StringList busSelects = new StringList();
			busSelects.add(DomainConstants.SELECT_NAME);
			busSelects.add(DomainConstants.SELECT_DESCRIPTION);
			
			MapList sendDeptList = DomainObject.findObjects(context, "SPLM_SendDept", "*",
					"*", "*", PRODUCTION_VAULT, "", null, false, busSelects,
					Short.parseShort("0"));
			sendDeptList.addSortKey(DomainConstants.SELECT_NAME, "ascending", "string");
			sendDeptList.sort();
			
			int index  = 0;
			result.append("<div style=\"display:flex;flex-direction:row;\">");
			Map sendDeptInfo;
			String dept, deptName;
			for (Object object : sendDeptList) {
				sendDeptInfo = (Map) object;
				dept = (String) sendDeptInfo.get(DomainConstants.SELECT_NAME);
				deptName = (String) sendDeptInfo.get(DomainConstants.SELECT_DESCRIPTION);

				if (index == 3) {
					index = 0;
					result.append("</div>");
					result.append("<div style=\"display:flex;flex-direction:row;\">");
				}
				
				result.append("<div style=\"min-width:220px;\">");
				result.append("<input type=\"checkbox\" "
						+ (viewMode ? " onclick=\"return false\" " : " ")
						+ " name=\"" + "SPLM_SendDept" + "\" id=\""
						+ "SPLM_SendDept" + "\" value=\"" + dept
						+ "\" " + (depts.contains(dept) ? " checked " : " ")
						+ ">" + dept + "(" + deptName + ")" + "</input>");
				result.append("</div>");
				index++;
			}
			result.append("</div>");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return result.toString();
	}

	public void updateSOSendDept(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		
		String objectId = (String) paramMap.get("objectId");
		List<String> valueList = Arrays.asList((String[]) paramMap.get("New Values"));

		DomainObject obj = new DomainObject(objectId);
		obj.setAttributeValue(context, "SPLM_SendDept", String.join(SPLIT_KEY, valueList));
	}
	

	public HashMap<String, String> checkEPC_PICImageExists(Context context, String[] args) throws Exception {
		HashMap<String, String> returnMap = new HashMap<String, String>();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		try {
			DomainObject epcPicObj = new DomainObject(objectId);
			StringList imageNoList = epcPicObj.getInfoList(context, "from[SPLM_EPC_PIC_Image].to.name");
			
			Map<String, Integer> imageNoCount = new HashMap<String, Integer>();
			int count;
			for (String imageNo : imageNoList) {
				if (imageNoCount.containsKey(imageNo)) {
					count = imageNoCount.get(imageNo) + 1;
				} else {
					count = 1;
				}
				imageNoCount.put(imageNo, count);
			}
			
			List<String> existsImageNoList = imageNoCount.entrySet()
			.stream()
			.filter(streamObj -> streamObj.getValue() > 1)
			.map(streamObj -> streamObj.getKey())
			.collect(Collectors.toList());
			
			if (!existsImageNoList.isEmpty()) {
				String errMsg = MessageFormat.format("\u4e0b\u5217\u5716\u6a94\u5df2\u5b58\u5728 {0} \u8acb\u91cd\u65b0\u78ba\u8a8d\u5f8c\u518d\u4e0a\u50b3", String.join(", ", existsImageNoList));
				throw new Exception(errMsg);
			}
		} catch (Exception e) {
			returnMap.put("Action", "stop");
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	
	public HashMap<String, String> updateEPCDataFlag(Context context, String[] args) throws Exception {
		HashMap<String, String> returnMap = new HashMap<String, String>();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		
		try {
			DomainObject groupDrawingObj = new DomainObject(objectId);
			groupDrawingObj.setAttributeValue(context, "SPLM_UpdateEPCDataFlag", "Processing");
			returnMap.put("Notify", "\u5df2\u52a0\u5165\u6392\u7a0b\u7b49\u5f85\u8655\u7406");
		} catch (Exception e) {
			returnMap.put("Action", "stop");
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	
	public String getAllEOPO(Context context, String[] args)
			throws Exception {
				HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = (String) paramMap.get("objectId");
		DomainObject partObj = new DomainObject(objectId);
		
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		
		StringList relSelects = new StringList();
		relSelects.add("attribute[SPLM_RelatedEO]");
		relSelects.add("attribute[SPLM_RelatedPO]");
		
		MapList bomList = partObj.getRelatedObjects(context, "SPLM_SBOM,SPLM_MBOM",
				"*", 
				busSelects, 
				relSelects, 
				true,
				false, (short) 1, "", "", (short) 0);
		
		Map bomInfo;
		String parentNo, relatedEO, relatedPO;
		String[] relatedEOs, relatedPOs;
		StringBuffer eoURLList, poURLList;
		StringBuffer allURLList = new StringBuffer();
		for (Object temp : bomList) {
			bomInfo = (Map) temp;
			parentNo = (String) bomInfo.get(DomainConstants.SELECT_NAME);
			relatedEO = (String) bomInfo.get("attribute[SPLM_RelatedEO]");
			relatedPO = (String) bomInfo.get("attribute[SPLM_RelatedPO]");
			
			if (StringUtils.isEmpty(relatedEO)
					&& StringUtils.isEmpty(relatedPO)) {
				continue;
			}
			
			eoURLList = new StringBuffer();
			relatedEOs = relatedEO.split(",");
			for (String eo : relatedEOs) {
				eoURLList.append(this.buildEOUrl(context, eo.trim()));
				eoURLList.append(" ");
			}
			
			relatedPOs = relatedPO.split(",");
			poURLList = new StringBuffer();
			
			for (String po : relatedPOs) {
				poURLList.append(this.buildPOUrl(po.trim()));
				poURLList.append(" ");
			}
			
			allURLList.append("<p>");
			allURLList.append(parentNo);
			allURLList.append(" EO:" + eoURLList.toString());
			allURLList.append(" PO:" + poURLList.toString());
			allURLList.append("</p>");
		}
		
		return allURLList.toString();
	}
	
	public Map<String, String> changePartToPLVC (Context context, String[] args) throws Exception {
		Map<String, String> returnMap = new HashMap<String, String>();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String partId = (String) programMap.get("objectId");
		try {
			MqlUtil.mqlCommand(context, "mod bus '" + partId + "' type SPLM_PLVC");
		} catch (Exception e) {
			returnMap.put("Message", e.getMessage());
		}
		returnMap.put("Notify", "\u5b8c\u6210");
		return returnMap;
	}
	
	public Map<String, String> changePLVCToPart (Context context, String[] args) throws Exception {
		Map<String, String> returnMap = new HashMap<String, String>();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String partId = (String) programMap.get("objectId");
		try {
			MqlUtil.mqlCommand(context, "mod bus '" + partId + "' type SPLM_Part");
		} catch (Exception e) {
			returnMap.put("Message", e.getMessage());
		}
		returnMap.put("Notify", "\u5b8c\u6210");
		return returnMap;
	}
	
	public Map<String, String> deleteServicePart (Context context, String[] args) throws Exception {
		Map<String, String> returnMap = new HashMap<String, String>();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String partId = (String) programMap.get("objectId");
		try {
			DomainObject partObj = new DomainObject(partId);
			if (!"Preliminary".equals(partObj.getInfo(context, DomainConstants.SELECT_CURRENT))) {
				throw new Exception("\u72c0\u614b\u4e0d\u518d\u5de5\u4f5c\u4e2d\u7121\u6cd5\u522a\u9664");
			}
			if (!partObj.getInfoList(context, "to[SPLM_AffectedItem].from.id").isEmpty()) {
				throw new Exception("\u6709\u76f8\u95dcSO\u5b58\u5728\u7121\u6cd5\u522a\u9664");
			}
			partObj.deleteObject(context);
		} catch (Exception e) {
			returnMap.put("Message", e.getMessage());
		}
		returnMap.put("Notify", "\u5b8c\u6210");
		return returnMap;
	}
	
	public HashMap getServicePartDest(Context context, String[] args)
			throws Exception {
		HashMap levelMap = new HashMap(2);
		
		StringList valueList = new StringList();
		StringList displayList = new StringList();
		
		valueList.add("-");
		displayList.add("\u570b\u7522");
		
		valueList.add("1400");
		displayList.add("\u9032\u53e3");
		
		levelMap.put("field_choices", valueList);
		levelMap.put("field_display_choices", displayList);

		return levelMap;
	}
	
	public StringList includeMyNotCheckServicePart (Context context, String[] args) throws Exception {
		MapList servicePartList = this.getMyNotCheckServicePart(context, args);
		
		StringList includeList = new StringList();
		String id;
		for (Object servicePart : servicePartList) {
			id = (String)((Map) servicePart).get(DomainConstants.SELECT_ID);
			if (includeList.contains(id)) {
				continue;
			}
			includeList.add(id);
		}
		return includeList;
	}
	
	public Map<String, String> publishSOPBExcel(Context context, String[] args) throws Exception {
		Map<String, String> returnMap = new HashMap<String, String>();
		try {
			SPLM_SOPB_Excel_JPO_mxJPO soPb = new SPLM_SOPB_Excel_JPO_mxJPO();
			returnMap.putAll(soPb.publishSOPBExcel(context, args));
			if (returnMap.containsKey("FilePath")) {
				File file = new File(String.valueOf(returnMap.get("FilePath")));
				if (file.exists()) {
					HashMap programMap = (HashMap) JPO.unpackArgs(args);
					String soId = (String) programMap.get("objectId");
					DomainObject soObj = new DomainObject(soId);
					soObj.setAttributeValue(context, "SPLM_SOExcelFlag", "Complete");
				}
			}
		} catch (Exception e) {
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	
	public HashMap<String, String> connectReferenceDoc(Context context, String[] args) throws Exception {
		
		HashMap<String, String> returnMap = new HashMap<String, String>();
		Map programMap = JPO.unpackArgs(args);
		String partId = (String) programMap.get("objectId");
		String[] emxTableRowIds = (String[]) programMap.get("emxTableRowIds");
		try {
			DomainObject partObj = new DomainObject(partId);
			StringList referenceDocIdList = partObj.getInfoList(context, "from[SPLM_ReferenceDoc].to.id");
			DomainObject selectedDocObj = new DomainObject();
			String selectedDocId, selectedDocNo;
			MapList partSOList;
			for (String rowId : emxTableRowIds) {
				selectedDocId = CMC_Method_JPO_mxJPO.getTableRowObjectId(rowId);
				selectedDocObj.setId(selectedDocId);
				selectedDocNo = selectedDocObj.getInfo(context, DomainConstants.SELECT_NAME);
				if (referenceDocIdList.contains(selectedDocId)) {
					throw new Exception(selectedDocNo + " \u5df2\u5b58\u5728");
				}
				
				DomainRelationship.connect(context, partObj, "SPLM_ReferenceDoc", selectedDocObj);
				
			}
		} catch (Exception e) {
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}
	
	public MapList getAllLibrary(Context context, String[] args) throws Exception {
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		busSelects.addElement(DomainConstants.SELECT_NAME);
		busSelects.addElement(DomainConstants.SELECT_MODIFIED);
		MapList objectList = DomainObject.findObjects(context, "SPLM_Library", "*",
				"*", "*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		objectList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
		objectList.sort();
		return objectList;
	}
		
	public MapList getSubLibrary(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = (String) paramMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_SubLibrary,SPLM_Feature",
				"SPLM_RelatedSubLibrary,SPLM_RelatedFeature", null, null, "from", (short) 1, null, null);
		return objectList;
	}
	
	public MapList getRelatedSubLibrary(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_SubLibrary",
				"SPLM_RelatedSubLibrary", null, null, "from", (short) 1, null, null);
		return objectList;
	}
		
	public MapList getRelatedFeature(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		MapList objectList = CMC_Method_JPO_mxJPO.expandObject(context, objectId, "SPLM_Feature",
				"SPLM_RelatedFeature", null, null, "from", (short) 1, null, null);
		return objectList;
	}
		
	public boolean showFeatureCreateField(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);

		String objectId = (String) programMap.get("objectId");
		DomainObject obj = new DomainObject(objectId);
		String objType = obj.getInfo(context, DomainConstants.SELECT_TYPE);
		String infoSelect = !"SPLM_SubLibrary".equals(objType) ? "attribute[SPLM_LibraryType]" : "to[SPLM_RelatedSubLibrary].from[SPLM_Library].attribute[SPLM_LibraryType]";
		String libraryType = obj.getInfo(context, infoSelect);
		
		HashMap settings = (HashMap) programMap.get("SETTINGS");
		String fieldLibraryType = (String) settings.get("LibraryType");
		
		return StringUtils.isNotEmpty(libraryType) && fieldLibraryType.contains(libraryType);
	}
	
	public Map reloadLibraryType(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");
		String parentId = (String) requestMap.get("objectId");
		DomainObject parentObj = new DomainObject(parentId);
		String libraryType = parentObj.getAttributeValue(context, "SPLM_LibraryType");
		
		Map resultMap = new HashMap();
		resultMap.put("SelectedValues", libraryType);
		resultMap.put("SelectedDisplayValues", libraryType);
		return resultMap;
	}
	
	public Map reloadCommissionGroup(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");
		String parentId = (String) requestMap.get("objectId");
		DomainObject parentObj = new DomainObject(parentId);
		String commissionGroup = parentObj.getAttributeValue(context, "SPLM_LibraryCommissionGroup");
		
		Map resultMap = new HashMap();
		resultMap.put("SelectedValues", commissionGroup);
		resultMap.put("SelectedDisplayValues", commissionGroup);
		return resultMap;
	}
	
	private String getFeatureTemplateId(Context context, String featureAutoName, String commissionGroup) throws Exception {
		String templateName = commissionGroup + featureAutoName + "0000";
		StringList busSelects = new StringList();
		busSelects.addElement(DomainConstants.SELECT_ID);
		MapList objectList = DomainObject.findObjects(context, "SPLM_Feature", templateName,
				"1", "*", PRODUCTION_VAULT, "", null, false, busSelects,
				Short.parseShort("0"));
		return objectList.isEmpty() ? null : (String) ((Map) objectList.get(0)).get(DomainConstants.SELECT_ID);
	}
	
	public Map reloadFeatureName(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		Map fieldValues   = (Map) programMap.get("fieldValues");
		String featureAutoName = (String) fieldValues.get("SPLM_FeatureAutoName");
		String commissionGroup = (String) fieldValues.get("SPLM_LibraryCommissionGroup");
		String featureName = commissionGroup + featureAutoName;
		String featureTemplateId = this.getFeatureTemplateId(context, featureAutoName, commissionGroup);
		boolean hasTemplate = StringUtils.isNotEmpty(featureTemplateId);
		MapList objectList = DomainObject.findObjects(context, "SPLM_Feature", featureName + "*",
				"1", "*", PRODUCTION_VAULT, "", null, false, new StringList(DomainConstants.SELECT_ID),
				Short.parseShort("0"));
		int nextIndex = hasTemplate ? objectList.size()  : objectList.size() + 1;
		featureName += String.format("%04d", nextIndex);
		
		Map resultMap = new HashMap();
		resultMap.put("SelectedValues", featureName);
		resultMap.put("SelectedDisplayValues", featureName);
		return resultMap;
	}
	
	public Map reloadSubLibraryTypeName(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		Map fieldValues   = (Map) programMap.get("fieldValues");
		String featureAutoName = (String) fieldValues.get("SPLM_FeatureAutoName");
		String commissionGroup = (String) fieldValues.get("SPLM_LibraryCommissionGroup");
		String featureTemplateId = this.getFeatureTemplateId(context, featureAutoName, commissionGroup);
		String subLibraryTypeName = StringUtils.isEmpty(featureTemplateId) ? "" : new DomainObject(featureTemplateId).getAttributeValue(context, "SPLM_SubLibraryTypeName");
		Map resultMap = new HashMap();
		resultMap.put("SelectedValues", subLibraryTypeName);
		resultMap.put("SelectedDisplayValues", subLibraryTypeName);
		return resultMap;
	}
	
	public HashMap getLibraryCommissionGroupRange(Context context, String[] args)
			throws Exception {
		String language = context.getSession().getLanguage();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");
		String parentId = (String) requestMap.get("objectId");
		DomainObject parentObj = new DomainObject(parentId);
		String libraryType = parentObj.getAttributeValue(context, "SPLM_LibraryType");
		String libraryCommissionGroupAtt = "SPLM_LibraryCommissionGroup_" + libraryType;
		
		HashMap viewMap = new HashMap();
		StringList strListStateDisplay = new StringList();
		StringList strListStateValue = new StringList();
		
		ContextUtil.pushContext(context);
		try {
			AttributeType var2 = new AttributeType(libraryCommissionGroupAtt);
			var2.open(context);
			strListStateValue.addAll(var2.getChoices(context));
			var2.close(context);
		} catch (Exception e) {
		} finally {
			ContextUtil.popContext(context);
		}

		for (String value : strListStateValue) {
			strListStateDisplay.add(i18nNow.getRangeI18NString("SPLM_LibraryCommissionGroup", value, language));
		}
		
		viewMap.put("field_choices", strListStateValue);
		viewMap.put("field_display_choices", strListStateDisplay);
		return viewMap;
	}
	
	public String getCPNO(Context context, String[] args)
			throws Exception {
				HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = (String) paramMap.get("objectId");
		DomainObject partObj = new DomainObject(objectId);
		return partObj.getAttributeValue(context, "SPLM_CP_NO").replace("~QWE~", ", ");
	}
}