import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import com.google.gson.GsonBuilder;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

public class SPLM_MMC_PartsCatalogue_JPO_mxJPO {
	private final static String VAULT = "eService Production";
	private final static String TEMP_FOLDER = "PLMTemp\\createMMCPartCatalogue\\";
	private final static String TEMP_FOLDER_DEVICE = "D:\\";
	private final static String[] PICTURE_FORMAT = { DomainConstants.FORMAT_GENERIC,
			DomainConstants.MVL_FORMAT_MX_IMAGE, DomainConstants.MVL_FORMAT_MX_LARGE_IMAGE,
			DomainConstants.MVL_FORMAT_MX_MEDIUM_IMAGE, DomainConstants.MVL_FORMAT_MX_SMALL_IMAGE,
			DomainConstants.MVL_FORMAT_MX_THUMBNAIL_IMAGE };

	/*
	 * 檔案注意事項 1.以上零件冊文字檔與單元圖都要放在同一個目錄讓系統程式讀取 2.進口車(不會有單元圖check
	 * in)與國產車的三菱資料將仿零件冊資料結構建立，而不會建立SBOM結構。
	 * 3.單元圖的命名一率為1XX_YYYWWWWW.tif，其中單元圖檔名第一個數字1要忽略，XX為大單號，YYY為小單號，WWWWW為單元圖號
	 * 4.groupDrawing <-> part : 在單元圖與件號間將起用日、停用日、用量、單元代碼；序號與PNC填入屬性 5.pnc <-> part
	 * : 在PNC與件號間將起用日、停用日、用量、單元代碼與序號填入屬性
	 */

	/* --- Dassault API --- */
	//
	private void copyPictureAndUploadImagePicture(Context context, DomainObject imageDomObj, String oldPath,
			String newPath) throws Exception {
		File newFile = new File(newPath);
		String fileParent = newFile.getParent();
		String fileName = newFile.getName();

		try {
			BufferedImage originalImg = ImageIO.read(new FileInputStream(new File(oldPath)));
			int mxThumbnailHeight = 42;
			int mxSmallHeight = 64;
			int mxMediumHeight = 108;
			int mxLargeHeight = 480;
			BufferedImage[] newImgs = { originalImg, originalImg, scalingByHeight(originalImg, mxLargeHeight),
					scalingByHeight(originalImg, mxMediumHeight), scalingByHeight(originalImg, mxSmallHeight),
					scalingByHeight(originalImg, mxThumbnailHeight) };
			for (int i = 0; i < newImgs.length; i++) {
				ImageIO.write(newImgs[i], "jpg", newFile);
				imageDomObj.checkinFile(context, false, true, InetAddress.getLocalHost().getHostName(),
						PICTURE_FORMAT[i], fileName, fileParent);
			}
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}
	}

	private void createImageAndConnectGroupDrawing(Context context, DomainObject groupDrawingDomObj, String picturePath)
			throws Exception {
		ContextUtil.pushContext(context);
		MqlUtil.mqlCommand(context, "trigger off");
		File file = new File(picturePath);
		String fileName = file.getName().substring(0, file.getName().lastIndexOf('.'));
		String newFileName = file.getParent() + "\\" + fileName + ".jpg";

		DomainObject imageDomObj = new DomainObject();
		imageDomObj.createObject(context, "Image", // String Type
				fileName, // String name
				java.util.UUID.randomUUID().toString().replace("-", ""), // String revision
				"Image", // String policy
				VAULT); // String vault
		imageDomObj.setOwner(context, groupDrawingDomObj.getInfo(context, DomainConstants.SELECT_OWNER));

		this.copyPictureAndUploadImagePicture(context, imageDomObj, picturePath, newFileName);

		MqlUtil.mqlCommand(context,
				"modify bus " + imageDomObj.getId(context) + " organization '"
						+ groupDrawingDomObj.getInfo(context, "organization") + "' project '"
						+ groupDrawingDomObj.getInfo(context, "project") + "'");

		DomainRelationship.connect(context, groupDrawingDomObj, "SPLM_GroupDrawingImage", imageDomObj);
		MqlUtil.mqlCommand(context, "trigger on");
		ContextUtil.popContext(context);
	}

	// DomainObject -> exits->getId /
	private DomainObject createPartCatalogueDomObj(Context context, Map<String, String> partCatalogueMap,
			String partCatalogueName, Map<String, String> attributeMap) throws Exception {
		DomainObject partsCatalogueDomObj;
//		if(partCatalogueMap.containsKey(partCatalogueName)) {
//			partsCatalogueDomObj = new DomainObject(partCatalogueMap.get(partCatalogueName));
//			
//		} else {
			partsCatalogueDomObj = createDomObj(context, partCatalogueMap, "SPLM_PartsCatalogue", "1", "SPLM_PartsCatalogue",partCatalogueName, attributeMap, "");
//		}
		return partsCatalogueDomObj;
	}

	private DomainObject createGroupDrawingDomObj(Context context, Map<String, String> groupDrawingMap,
			String groupDrawingName, Map<String, String> attributeMap) throws Exception {
		return createDomObj(context, groupDrawingMap, "SPLM_GroupDrawing", "-", "SPLM_GroupDrawing", groupDrawingName,
				attributeMap, "");
	}

	// map[0] => name_TC , name_EN
	// map[1] => SPLM_Location , SPLM_MaterialType , SPLM_MaterialGroup , SPLM_Location_DMS, SPLM_IsServicePart
	//           SPLM_ModelSeries, SPLM_PB_Note, SPLM_CMC_Code
	private DomainObject createPartDomObj(Context context, Map<String, String> partMap, String partNo,Map<String, String>... attributeMap) throws Exception {
		DomainObject partDomObj;
		StringList partSelect = new StringList();
		partSelect.add("SPLM_ModelSeries");
		partSelect.add("SPLM_CMC_Code");

		if (partMap.containsKey(partNo)) {
			partDomObj = new DomainObject(partMap.get(partNo));
			if (attributeMap.length == 2) {
				AttributeList partAttributeList = partDomObj.getAttributeValues(context, partSelect);
				Set<String> newMs = Arrays.stream(attributeMap[1].get("SPLM_ModelSeries").split(",")).collect(Collectors.toSet());
				newMs.addAll(Arrays.stream(partAttributeList.get(0).getValue().split(",")).collect(Collectors.toSet()));
				Set<String> newMc = Arrays.stream(attributeMap[1].get("SPLM_CMC_Code").split(",")).collect(Collectors.toSet());
				newMc.addAll(Arrays.stream(partAttributeList.get(1).getValue().split(",")).collect(Collectors.toSet()));
				
				Map<String, String> existMap = new HashMap<String, String>();
				existMap.put("SPLM_ModelSeries", String.join(",", newMs));
				existMap.put("SPLM_CMC_Code", String.join(",", newMc));
				
				partDomObj.setAttributeValues(context, existMap);
			}
		} else {
			partDomObj = createDomObj(context, partMap, "SPLM_Part", "-", "SPLM_ServicePart", partNo, attributeMap[0],"");
			if (attributeMap.length == 2) {
				partDomObj.setAttributeValues(context, attributeMap[1]);
			}
		}

		return partDomObj;
	}

	private DomainObject createPncDomObj(Context context, Map<String, String> pncMap, String pnc,
			Map<String, String> attributeMap) throws Exception {
		return createDomObj(context, pncMap, "SPLM_PNC", "-", "SPLM_PNC", pnc, attributeMap, "");
	}

	/**
	 * 主要建立 / 判斷 bus物件存在與否
	 * 
	 * @param context
	 * @param map
	 * @param typePattern
	 * @param revision
	 * @param policy
	 * @param name
	 * @param attributeMap
	 * @param description
	 * @return
	 * @throws Exception
	 */
	private DomainObject createDomObj(Context context, Map<String, String> map, String typePattern, String revision,
			String policy, String name, Map<String, String> attributeMap, String description) throws Exception {
		DomainObject domObj = new DomainObject();
		if (!map.containsKey(name)) {
			domObj.createObject(context, typePattern, // String Type
					name, // String name
					revision, // String revision
					policy, // String policy
					VAULT); // String vault
			domObj.setDescription(context, description);
			if (Optional.ofNullable(attributeMap).isPresent()) {
				domObj.setAttributeValues(context, attributeMap);
			}
			map.put(name, domObj.getId(context));
			return domObj;
		}

		domObj.setId(map.get(name));
		return domObj;
	}

	// Map -> structure <key>Name = <Value>Id
	private Map<String, String> getGroupDrawingInfo(Context context) throws Exception {
		return getDataInfo(context, "SPLM_GroupDrawing");
	}

	private Map<String, String> getPNCInfo(Context context) throws Exception {
		return getDataInfo(context, "SPLM_PNC");
	}

	private Map<String, String> getPartInfo(Context context) throws Exception {
		return getDataInfo(context, "SPLM_Part");
	}

	private Map<String, String> getPartCatalogueInfo(Context context) throws Exception {
		return getDataInfo(context, "SPLM_PartsCatalogue");
	}

	/**
	 * 前置作業 搜索該type的全部物件 name/Id
	 * 
	 * @param context
	 * @param typePattern
	 * @return
	 * @throws Exception
	 */
	private Map<String, String> getDataInfo(Context context, String typePattern) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_ID);

		MapList mapList = DomainObject.findObjects(context, typePattern, // type
				"*", // Name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"", // where
				null, false, busSelects, (short) 0);

		return (HashMap<String, String>) mapList.stream()
				.collect(Collectors.toMap(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME),
						obj -> (String) ((Map) obj).get(DomainConstants.SELECT_ID), (oldName, newName) -> oldName));
	}

	private MapList getSpecificRelFrom(Context context,DomainObject mainDomObj, String relationship, String specificDomId) throws Exception {
		return 	mainDomObj.getRelatedObjects(context, relationship, // relationshipPattern,
				"*", // typePattern,
				null, // StringList objectSelects,
				null, // StringList relationshipSelects,
				false, // boolean getTo,
				true, // boolean getFrom,
				(short) 1, // short recurseToLevel,
				"id=='" + specificDomId + "'",
				"", // String relationshipWhere
				0); // int limit)
	}
	
	/* --- Start Line --- */
	/**
	 * 網頁進入點程式 ->  三菱零件冊(txtPath), 圖檔zip(dwgZipPath), 售服零件ERP屬性(location)
	 * @param context
	 * @param args
	 * @return logPath (log檔)
	 * @throws Exception
	 */
	public String createMMCPartsCatalogue(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		String txtPath = (String) programMap.get("txtPath"); // attribute Import/Domestic
		String zipPath = (String) programMap.get("dwgZipPath"); // Location
		String location = (String) programMap.get("location"); // Location
		String logPath = "";

//		String owner = "splmuser"; // 匯入的人
		String owner = context.getUser(); // 匯入的人

		String tempFolder = this.getTempFolderPath();
		File file = new File(tempFolder);
		if (!file.exists()) {
			file.mkdirs();
		}

		StringBuffer sbError = new StringBuffer();
		long startCurrent = System.currentTimeMillis();
		Map<String, String> pictureDetailMap = new HashMap<String, String>();
		ArrayList<PCData> splArrayList = new ArrayList<PCData>();
		String tempFileName = new File(txtPath).getName().toUpperCase();
		tempFileName = tempFileName.startsWith("SPL")? "SPL" : tempFileName.startsWith("PC")? "PC" : "";
		try {
			if (!zipPath.isEmpty()) {
				pictureDetailMap = this.getZipPictureInfo(zipPath, tempFolder, sbError);
			}

			if( !tempFileName.isEmpty()) {
				splArrayList = getSPLMMCPartsCatalogueInfo(txtPath, sbError);
				if( !splArrayList.isEmpty() ) {
					this.createSPLAndPCPartsCatalogue(context, pictureDetailMap, owner, splArrayList, location, sbError);
				}				
			} else {
				tempFileName = "catalogue";
				sbError.append("\u96f6\u4ef6\u518a\u4e0d\u662f SPL / PC \u958b\u982d  \u8acb\u8a73\u67e5\u662f\u5426\u6b63\u78ba");
			}
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			logPath = this.outputFile("log_" + convertNowDateFormat("yyyyMMddHHmmss") + "_" + tempFileName + ".txt", sbError.toString());
		}

		for (String dwgPath : pictureDetailMap.values()) {
			new File(dwgPath).delete();
		}

		System.out.println("total : " + (System.currentTimeMillis() - startCurrent));
		return logPath;
	}

	/**
	 * 創建零件冊資料 / 連接資料 (多處判斷需看清楚)
	 * @param context
	 * @param pictureDetailMap
	 * @param owner
	 * @param splArrayList
	 * @param location
	 * @param sbError
	 * @throws Exception
	 */
	private void createSPLAndPCPartsCatalogue(Context context, Map<String, String> pictureDetailMap, String owner,
			ArrayList<PCData> splArrayList, String location, StringBuffer sbError) throws Exception {
		// location -> 進口 erp 9000, 國產 erp 1300 (需轉換成空)
//		ArrayList<PCData> splArrayList = getSPLMMCPartsCatalogueInfo(txtPath, sbError);
		Map<String, String> pncMap = getPNCInfo(context);
		Map<String, String> partMap = getPartInfo(context);
		Map<String, String> partCatalogueMap = getPartCatalogueInfo(context);
		Map<String, String> groupDrawingMap = getGroupDrawingInfo(context);
		Map<String, Integer> relPartCatalogueGroupDrawingmap = new HashMap<String, Integer>();
		Map<String, Integer> relGroupDrawingPNCmap = new HashMap<String, Integer>();
		Map<String, Integer> relGroupDrawingPartmap = new HashMap<String, Integer>();
		Map<String, Integer> relPNCPartmap = new HashMap<String, Integer>();

		Map<String, String> partAttForNameMap = new HashMap<String, String>();
		Map<String, String> partAttributesMap = new HashMap<String, String>();// InportMMC
		boolean is1300 = location.equals("1300");
		String erpLocation = is1300? "": location;
		String dmsLocation = is1300? "": "SDM"; // 1300 -> CMC暫時拿掉
		String typeKD = is1300? "": "KD"; // 1300 -> KD 暫時拿掉
		String isServicePart = is1300? "FALSE": "TRUE";
		partAttributesMap.put("SPLM_Location", erpLocation);
		partAttributesMap.put("SPLM_MaterialType", "ZHAL");
		partAttributesMap.put("SPLM_MaterialGroup", typeKD);
		partAttributesMap.put("SPLM_Location_DMS", dmsLocation);
		partAttributesMap.put("SPLM_IsServicePart", isServicePart);

		DomainObject partCatalogueDomObj = null;
		DomainObject groupDrawingDomObj = null;
		DomainObject partDomObj = null;
		DomainObject replacePartDomObj = null;
		DomainObject pncDomObj = null;
		DomainRelationship relreplacePart = null;
		DomainRelationship relRelatedGroupDrawing = null;
		DomainRelationship relRelatedPartFromPNC = null;
		DomainRelationship relRelatedPartFromGroupDrawing = null;
		Map<String, String> catalogueAttributesMap = new HashMap<String, String>();
		Map<String, String> relatedPartAttributesMap = new HashMap<String, String>();
		Map<String, String> pncAttributesMap = new HashMap<String, String>();
		String partCatalogueName, groupCode, importGroupDrawingName, domesticGroupDrawingName, partNo, pnc, pncName,
				partMC, startDate, endDate, quantity;
		String partCatalogueAttMc, partCatalogueAttMs, partExchange, partAlter, partRemark;
		String groupDrawingName = "";
		
		String vendorMMCId = getVenderMMCId(context);
		
		ArrayList<AlterPart> alterPartArray;

		StringList pncSelect = new StringList();
		pncSelect.add("SPLM_Name_EN");
		pncSelect.add("SPLM_Name_TC");

		AttributeList tempAttributeList = new AttributeList();
		MapList tempMapList = new MapList();
		
		Set<String> catalogueMsSet = splArrayList.stream().filter(obj -> !obj.getMs().isEmpty()).map(obj -> obj.getMs()).collect(Collectors.toSet());
		Set<String> catalogueMcSet = new HashSet<String>();
		splArrayList.stream().filter(obj -> !obj.getMc().isEmpty()).forEach(obj -> catalogueMcSet.addAll(obj.getMc()));;

		catalogueAttributesMap.put("SPLM_ModelSeries", String.join(",",catalogueMsSet));
		catalogueAttributesMap.put("SPLM_ModelCode", String.join(",",catalogueMcSet));

//		ContextUtil.startTransaction(context, true);
		int i = 0;
		for (PCData spl : splArrayList) {
			/* 創建新的零件冊 */
			partCatalogueName = spl.getPartsCatalogueNo();
			groupCode = spl.getGroupCode();
			importGroupDrawingName = spl.getImportGroupDrawingName();
			domesticGroupDrawingName = spl.getDomesticGroupDrawingName();
			partNo = spl.getPartNo();
			pnc = spl.getPncNo();
			pncName = spl.getPncName();
			startDate = spl.getEnableDate();
			endDate = spl.getDisableDate();
			quantity = spl.getQuantity();
			partCatalogueAttMc = String.join(",",spl.getMc());
			partCatalogueAttMs = spl.getMs();
			alterPartArray = spl.getAlterPartArrayList();
			partRemark = spl.getRemark();
			partMC = String.join(",", spl.getMc());

			relatedPartAttributesMap.put("SPLM_EnableDate", startDate);
			relatedPartAttributesMap.put("SPLM_DisableDate", endDate);
			relatedPartAttributesMap.put("Quantity", quantity);
			relatedPartAttributesMap.put("SPLM_GroupCode", groupCode);
			relatedPartAttributesMap.put("SPLM_PNC", pnc);

			pncAttributesMap.put("SPLM_Name_EN", pncName);
			pncAttributesMap.put("SPLM_Name_TC", pncName);

			partAttributesMap.put("SPLM_PB_Note", partRemark);
			partAttributesMap.put("SPLM_ModelSeries", partCatalogueAttMs);
			partAttributesMap.put("SPLM_CMC_Code", partCatalogueAttMc);
			
			if(!is1300) {				
				partAttributesMap.put("SPLM_GroupCode", groupCode); // groupCode 3/27 需求
				partAttributesMap.put("SPLM_PNC", pnc); // pnc 3/27 需求
				partAttributesMap.put("SPLM_ModelCode", spl.getMc().stream().findFirst().orElse("")); // ModelCode 3/27 需求
			}

			/* partCatalogue object */
			try {
				partCatalogueDomObj = createPartCatalogueDomObj(context, partCatalogueMap, partCatalogueName,catalogueAttributesMap);
			} catch (Exception e2) {
				sbError.append("(Error) \u96f6\u4ef6\u518a\u5efa\u7acb : (" + partCatalogueName + ")" + e2.getMessage() + '\n');
				sbError.append("\u539f\u59cb\u8cc7\u6599 \u7b2c" + spl.getLineIndex() + "\u884c : " + spl.toString() + '\n');
			}

			/* groupDrawing object */
			try {
				if (!pictureDetailMap.isEmpty() && pictureDetailMap.containsKey(groupCode)) {
					domesticGroupDrawingName = new File(pictureDetailMap.get(groupCode)).getName();
					groupDrawingName = domesticGroupDrawingName.substring(1, domesticGroupDrawingName.lastIndexOf("."));

					if (groupDrawingMap.containsKey(groupDrawingName)) {
						groupDrawingDomObj = new DomainObject(groupDrawingMap.get(groupDrawingName));
					} else {
						groupDrawingDomObj = createGroupDrawingDomObj(context, groupDrawingMap, groupDrawingName,Collections.singletonMap("SPLM_GroupCode", groupCode));
						this.createImageAndConnectGroupDrawing(context, groupDrawingDomObj,pictureDetailMap.get(groupCode));
					}
				} else {
					groupDrawingName = importGroupDrawingName;
					groupDrawingDomObj = createGroupDrawingDomObj(context, groupDrawingMap, groupDrawingName,Collections.singletonMap("SPLM_GroupCode", groupCode));
				}
			} catch (Exception e2) {
				sbError.append("(Error) \u55ae\u5143\u5716\u5efa\u7acb : (" + groupDrawingName + ")" + e2.getMessage() + '\n');
				sbError.append("\u539f\u59cb\u8cc7\u6599 \u7b2c" + spl.getLineIndex() + "\u884c : " + spl.toString() + '\n');
			}
 
			
			/* pnc object */
			try {
				pncDomObj = createPncDomObj(context, pncMap, pnc, pncAttributesMap);
			} catch (Exception e2) {
				sbError.append("(Error) PNC\u5efa\u7acb : (" + pnc + ")" + e2.getMessage() + '\n');
				sbError.append("\u539f\u59cb\u8cc7\u6599 \u7b2c" + spl.getLineIndex() + "\u884c : " + spl.toString() + '\n');
			}

			/* extract pnc Attribute for part */
			tempAttributeList = pncDomObj.getAttributeValues(context, pncSelect);
			for (Attribute attribute : tempAttributeList.getIterator()) {
				partAttForNameMap.put(attribute.getAttributeType().getName(), attribute.getValue());
			}

			/* part object */
			try {
				boolean existPart = partMap.containsKey(partNo);
				partDomObj = createPartDomObj(context, partMap, partNo, partAttForNameMap, partAttributesMap);
				// 進口車 廠商 添加 MMC
				if(!existPart && !is1300 && !partDomObj.getInfoList(context, "from[SPLM_RelatedVendor].to.id").contains(vendorMMCId)) {
					DomainRelationship.connect(context, partDomObj, "SPLM_RelatedVendor",new DomainObject(vendorMMCId));
				}

				for (AlterPart alterPart : alterPartArray) {
					tempAttributeList = partDomObj.getAttributeValues(context, pncSelect);
					for (Attribute attribute : tempAttributeList.getIterator()) {
						partAttForNameMap.put(attribute.getAttributeType().getName(), attribute.getValue());
					}
					
					boolean existAltPart = partMap.containsKey(alterPart.getAlterPart());
					replacePartDomObj = createPartDomObj(context, partMap, alterPart.getAlterPart(), partAttForNameMap, partAttributesMap);
					tempMapList = getSpecificRelFrom(context, partDomObj, "SPLM_RelatedOptionalPart", replacePartDomObj.getId(context));
					if(tempMapList.isEmpty()) {
						relreplacePart = DomainRelationship.connect(context, partDomObj, "SPLM_RelatedOptionalPart",replacePartDomObj);
						relreplacePart.setAttributeValue(context, "SPLM_OptionalExchangeable", alterPart.getExchangeAble());
					}
					
					if(!existAltPart && !is1300 && !replacePartDomObj.getInfoList(context, "from[SPLM_RelatedVendor].to.id").contains(vendorMMCId)) {
						DomainRelationship.connect(context, replacePartDomObj, "SPLM_RelatedVendor",new DomainObject(vendorMMCId));
					}
				}
			} catch (Exception e2) {
				sbError.append("(Error) \u552e\u670d\u96f6\u4ef6\u5efa\u7acb : (" + partNo + ")" + e2.getMessage() + '\n');
				sbError.append("\u539f\u59cb\u8cc7\u6599 \u7b2c" + spl.getLineIndex() + "\u884c : " + spl.toString() + '\n');
			}

			/* <Relationship> partCatalogue -> groupDrawing */
			if (!checkExistKey(relPartCatalogueGroupDrawingmap, partCatalogueName, groupCode)) {
				try {
					tempMapList = getSpecificRelFrom(context, partCatalogueDomObj, "SPLM_RelatedGroupDrawing", groupDrawingDomObj.getId(context));
					if(tempMapList.isEmpty()) {						
						relRelatedGroupDrawing = DomainRelationship.connect(context, partCatalogueDomObj,"SPLM_RelatedGroupDrawing", groupDrawingDomObj);
						relRelatedGroupDrawing.setAttributeValue(context, "SPLM_CMC_Code", partCatalogueAttMc);
					}
				} catch (Exception e2) {
					sbError.append("(Error) \u96f6\u4ef6\u518a-\u55ae\u5143\u5716 \u95dc\u806f : " + e2.getMessage() + '\n');
					sbError.append("\u539f\u59cb\u8cc7\u6599 \u7b2c" + spl.getLineIndex() + "\u884c : " + spl.toString() + '\n');
				}
			}

			/* <Relationship> groupDrawing -> PNC */
			if (!checkExistKey(relGroupDrawingPNCmap, groupDrawingName, pnc)) {
				try {
					tempMapList = getSpecificRelFrom(context, groupDrawingDomObj, "SPLM_Related_PNC", pncDomObj.getId(context));
					if(tempMapList.isEmpty()) {						
						DomainRelationship.connect(context, groupDrawingDomObj, "SPLM_Related_PNC", pncDomObj);
					}
				} catch (Exception e2) {
					sbError.append("(Error) \u55ae\u5143\u5716-PNC \u95dc\u806f : " + e2.getMessage() + '\n');
					sbError.append("\u539f\u59cb\u8cc7\u6599 \u7b2c" + spl.getLineIndex() + "\u884c : " + spl.toString() + '\n');
				}
			}

			/* <Relationship> groupDrawing -> Part/ColorPart */
			if (!checkExistKey(relGroupDrawingPartmap, groupDrawingName, partNo, startDate, endDate, quantity,groupCode, pnc)) {
				try {
					tempMapList = getSpecificRelFrom(context, groupDrawingDomObj, "SPLM_RelatedPart", partDomObj.getId(context));
					if(tempMapList.isEmpty()) {						
						relRelatedPartFromGroupDrawing = DomainRelationship.connect(context, groupDrawingDomObj,"SPLM_RelatedPart", partDomObj);
						relRelatedPartFromGroupDrawing.setAttributeValues(context, relatedPartAttributesMap);
					}
				} catch (Exception e2) {
					sbError.append("(Error) \u55ae\u5143\u5716-\u552e\u670d\u96f6\u4ef6 \u95dc\u806f : " + e2.getMessage() + '\n');
					sbError.append("\u539f\u59cb\u8cc7\u6599 \u7b2c" + spl.getLineIndex() + "\u884c : " + spl.toString() + '\n');
				}
			}

			/* <Relationship> PNC -> Part/ColorPart */
			if (!checkExistKey(relPNCPartmap, pnc, partNo, startDate, endDate, quantity, groupCode, pnc)) {
				try {
					tempMapList = getSpecificRelFrom(context, pncDomObj, "SPLM_RelatedPart", partDomObj.getId(context));
					if(tempMapList.isEmpty()) {						
						relRelatedPartFromPNC = DomainRelationship.connect(context, pncDomObj, "SPLM_RelatedPart",partDomObj);
						relRelatedPartFromPNC.setAttributeValues(context, relatedPartAttributesMap);
					}
				} catch (Exception e2) {
					sbError.append("(Error) PNC-\u552e\u670d\u96f6\u4ef6 \u95dc\u806f : " + e2.getMessage() + '\n');
					sbError.append("\u539f\u59cb\u8cc7\u6599 \u7b2c" + spl.getLineIndex() + "\u884c : " + spl.toString() + '\n');
				}
			}
			
			/* <Relationship> ImportPart -> Vendor */			

//			if (++i % 100 == 0) {
//				System.out.println("done " + i);
//			}
		}
//		ContextUtil.abortTransaction(context);
	}
	
	private void setVenderImportMMC(Context context, DomainObject partObj, String vendorMMCId) throws Exception {
		StringList vendorList = partObj.getInfoList(context, "from[SPLM_Vendor].to.id");
		if(partObj.getInfoList(context, "from[SPLM_Vendor].to.id").contains(vendorMMCId)) {
			return;
		}
		
	}
	
	private String getVenderMMCId(Context context) throws Exception {
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		
		MapList vendorMapList = DomainObject.findObjects(context, "SPLM_Vendor", // type
				"MMC", // Name
				"-", // revision
				"*", // owner
				VAULT, // vault
				"", // where
				null, false, busSelect, (short) 0);
		
		if(vendorMapList.isEmpty()) {
			throw new Exception("\u8acb\u65b0\u589e MMC \u5ee0\u5546\u4ee3\u865f \u624d\u53ef\u6dfb\u52a0");
		}
		
		return (String) ((Map) vendorMapList.get(0)).get(DomainConstants.SELECT_ID);
	}
		
	/**
	 * 三菱零件冊解析 (SPL/PC) 
	 * @param splFileLocation
	 * @param sbError
	 * @return
	 * @throws Exception
	 */
	private ArrayList<PCData> getSPLMMCPartsCatalogueInfo(String splFileLocation, StringBuffer sbError) throws Exception {
		String fileName = new File(splFileLocation).getName().toUpperCase();
		boolean isSPL = fileName.startsWith("SPL");
		ArrayList<PCData> splArrayList = new ArrayList<PCData>();
		PCData splLatestVersion = null;
		List<String> lineSet = new ArrayList<String>();
		String line = "";

		try {
			lineSet = Files.readAllLines(Paths.get(splFileLocation));
		} catch (Exception e) {
			System.out.println(fileName + " : " + e.getMessage());
			sbError.append("\u8acb\u8f38\u5165 UTF-8\u683c\u5f0f " + fileName + " \u6a94\u6848\n");
			sbError.append("(\u64cd\u4f5c : txt\u6a94\u958b\u555f -> \u6a94\u6848 -> \u53e6\u5b58\u65b0\u6a94 -> <\u53f3\u4e0b\u89d2>\u7de8\u78bc -> UTF-8 -> \u5b58\u6a94\u5373\u53ef)");
		}
		
		try {
			sbError.append("\u8cc7\u6599\u89e3\u6790\u6709\u8aa4 - \u4e0d\u8655\u7406 \u7b46\u6578 : \n");
			for (int counter = 1 ; counter <= lineSet.size(); ++counter) {
				line = lineSet.get(counter-1).trim();	
				splLatestVersion = isSPL ? new SPL(line, counter) : new PC(line, counter);
				
				// 解析時有問題直接寫到error裡
				if (splLatestVersion.getStates().equals(States.Warring)) {
					sbError.append(splLatestVersion.getErrorMessages().get(0) + "\n");
					continue;
				}
	
				// 泛指各行 01 的 row
				if (splLatestVersion.getRow().startsWith("01")) {
					splArrayList.add(splLatestVersion);
				} else {
					PCData splPreVersion = splArrayList.get(splArrayList.size() - 1);
					// O 覆蓋 Y 產工
					if (!splPreVersion.equals(splLatestVersion)) {
						splLatestVersion.setStates(States.Warring);
						splLatestVersion.addErrorMessages(States.Warring.toString());
						splLatestVersion.addErrorMessages((counter - 1) + " \u884c ,Data : " + splPreVersion.toString());
						splLatestVersion.addErrorMessages(counter + " \u884c ,Data : " + splLatestVersion.toString());
						splLatestVersion.addErrorMessages("------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
					}
					splPreVersion.setMc(splLatestVersion.getMc());
					splPreVersion.setRemark(splLatestVersion.getRemark());
					splPreVersion.setStates(splLatestVersion.getStates());
					splPreVersion.setAlterPartArrayList(splLatestVersion);
					splPreVersion.setErrorMessages(splLatestVersion.getErrorMessages());
				}			
			}
			sbError.append("------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n\n");
		} catch (Exception e) {
			sbError.append("------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
			sbError.append("\u5efa\u6a94\u932f\u8aa4\n");
			sbError.append("(Error) " + splLatestVersion.getLineIndex() + "\u884c : " + e.getMessage() + "\n");
			sbError.append("\u539f\u59cb\u8cc7\u6599 : " + line + "\n");
			sbError.append("------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n\n");
		}
		
		if (!splArrayList.isEmpty()) {
			sbError.append("\u5408\u4f75\u898f\u5247\uff1aREMARK\u5408\u4f75, \u65b0\u820a\u66ff\u63db/\u4e92\u63db \u95dc\u806f, MC\u5408\u4f75\u9664\u5916\n");
			sbError.append("(\u9664\u4e86\u5408\u4f75\u898f\u5247\u5916\uff0c\u4e0d\u76f8\u540c\u7684\u8cc7\u6599\u7b46\u6578\u5982\u4e0b)\n");
			
			String lineEnd = "\n";
			sbError.append((String) splArrayList.stream().filter(obj -> !obj.getStates().equals(States.Success))
					.map(obj -> String.join(lineEnd, obj.getErrorMessages())).collect(Collectors.joining(lineEnd)));
		}

		return splArrayList.stream().filter(obj -> !obj.getStates().equals(States.Error))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	// Map -> structure <pictureName = absolutePath>
	private Map<String, String> getZipPictureInfo(String zipLocation, String tempFolder, StringBuffer sbError)
			throws Exception {
		sbError.append("zip\u58d3\u7e2e\u6a94 \u5716\u7247\u958b\u59cb\u89e3\u6790\n");
		if (!isZipFile(zipLocation)) {
			throw new Exception("\u8acb\u78ba\u8a8d\u662f\u5426\u70bazip\u6a94");
		}
		List<File> pictureList = this.unZip(new File(zipLocation), tempFolder);

		Map<String, String> mmcMap = new HashMap<String, String>();
		for (File tempPicture : pictureList) {
			String absoluteFile = tempPicture.getAbsolutePath();
			if (!isTiffPictureFile(absoluteFile)) {
				sbError.append("\u5716\u6a94 " + absoluteFile + " \u4e0d\u662f tiff/tif\u6a94\u6848 \u683c\u5f0f \n");
				continue;
			}
			String pictureFile = tempPicture.getName();
			pictureFile = pictureFile.substring(0, pictureFile.lastIndexOf('.'));
			if (pictureFile.length() != 13) {
				sbError.append("\u5716\u6a94 " + absoluteFile
						+ " \u540d\u7a31 \u4e0d\u7b26\u5408\u898f\u5b9a (1XX_YYYFWWWWW) \n");
				continue;
			}
			mmcMap.put(pictureFile.substring(1, 7).replace("_", ""), absoluteFile);
		}
		sbError.append("zip\u58d3\u7e2e\u6a94 \u5716\u7247\u89e3\u6790\u5b8c\u7562\n");
		sbError.append(
				"------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n\n");

		return mmcMap;
	}

	/* --- my library --- */
	private void createDirectory(String directory, String subDirectory) {
		String dir[];
		File fl = new File(directory);
		try {
			if (subDirectory == "" && fl.exists() != true)
				fl.mkdir();
			else if (subDirectory != "") {
				dir = subDirectory.replace('\\', '/').split("/");
				for (int i = 0; i < dir.length; i++) {
					File subFile = new File(directory + File.separator + dir[i]);
					if (subFile.exists() == false)
						subFile.mkdir();
					directory += File.separator + dir[i];
				}
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	private String outputFile(String fileName, String dataStr) {
		return outputDataFile(fileName, dataStr, true);
	}

	private String outputOverWriteFile(String fileName, String dataStr) {
		return outputDataFile(fileName, dataStr, false);
	}

	private String outputDataFile(String fileName, String dataStr, boolean overwrite) {
		String tempPath = this.getTempFolderPath();
		File dataFile = new File(tempPath + fileName);
		File dataRootFile = new File(tempPath);
		try {
			if (dataRootFile.exists() != true) {
				dataRootFile.mkdirs();
			}
			if (dataFile.exists() != true) {
				System.out.println("We had to make " + fileName + " file.");
				dataFile.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(dataFile, overwrite), StandardCharsets.UTF_8));
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
		return dataFile.toString();
	}

	private String convertNowDateFormat(String formatType) {
		return convertDateFormat(new Date().toGMTString(), formatType);
	}

	private String convertDateFormat(String targetDate, String dateFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		Date d = new Date(targetDate);
		return sdf.format(d);
	}

	private String translateJsonIntoString(Object obj) {
		return new GsonBuilder().disableHtmlEscaping().create().toJson(obj);
	}

	private String getTempFolderPath() {
		if (new File(TEMP_FOLDER_DEVICE).exists()) {
			return TEMP_FOLDER_DEVICE + TEMP_FOLDER;
		}
		return "C:\\" + TEMP_FOLDER;
	}

	private String bytesToHexString(byte[] src) {
		return bytesToHexString(src, 0);
	}

	private String bytesToHexString(byte[] src, int size) {
		if (src == null || src.length <= 0) {
			return null;
		}
		if (size == 0) {
			size = src.length;
		}
		StringBuilder stringBuilder = new StringBuilder("");
		for (int i = 0; i < size; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv + " ");
		}
		return stringBuilder.toString().toUpperCase();
	}

	private boolean isZipFile(String fileLocation) throws Exception {
		byte[] stream = Files.readAllBytes(Paths.get(fileLocation));
		String zipHexString = bytesToHexString(stream, 6).replaceAll(" ", "");
		String fileExtension = fileLocation.replaceAll("^.*\\.(.*)$", "$1");
		if ((zipHexString.startsWith("504B03040A") || zipHexString.startsWith("504B030414"))
				&& fileExtension.equalsIgnoreCase("zip")) {
			return true;
		}
		return false;
	}

	private boolean isTiffPictureFile(String fileLocation) throws Exception {
		byte[] stream = Files.readAllBytes(Paths.get(fileLocation));
		String picHexString = bytesToHexString(stream, 6).replaceAll(" ", "");
		// tiff File HexString data, avoid other File input
		if (picHexString.startsWith("492049") || picHexString.startsWith("49492A00")
				|| picHexString.startsWith("4D4D002A") || picHexString.startsWith("4D4D002B")) {
			return true;
		}
		return false;
	}

	private boolean checkExistKey(Map<String, Integer> map, String... keys) {
		String key = String.join("_", keys);
//		boolean exist = map.containsKey(key);
//		if ( !exist ) {
//			map.put(key, 1);
//		} else {
//			map.put(key, map.get(key) + 1);
//		}
//		return exist;
		if (map.containsKey(key)) {
			return true;
		}
		map.put(key, 1);
		return false;
	}

	private BufferedImage scalingByHeight(BufferedImage img, int newHeight) {
		BigDecimal width = new BigDecimal(img.getWidth());
		BigDecimal height = new BigDecimal(img.getHeight());
		int newWidth = width.multiply(new BigDecimal(newHeight)).divide(height, 10, RoundingMode.HALF_UP)
				.setScale(5, RoundingMode.HALF_UP).intValue();
		BufferedImage newImg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		newImg.getGraphics().drawImage(img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
		return newImg;
	}

	// unZip Code
	private List<File> unZip(File ZIPFile, String outputDirectory) throws Exception {
		List<File> unZipFile = new ArrayList<File>();
		try {
			ZipFile zipFile = new ZipFile(ZIPFile);
			Enumeration<ZipEntry> e = zipFile.getEntries();
			ZipEntry zipEntry = null;
			createDirectory(outputDirectory, "");

			while (e.hasMoreElements()) {
				zipEntry = (org.apache.tools.zip.ZipEntry) e.nextElement();
				System.out.println("unziping " + zipEntry.getName());
				if (zipEntry.isDirectory()) {
					String name = zipEntry.getName();
					name = name.substring(0, name.length() - 1);
					File f = new File(outputDirectory + File.separator + name);
					f.mkdir();
					System.out.println("Create Directory " + outputDirectory + File.separator + name);
				} else {
					String fileName = zipEntry.getName();
					fileName = fileName.replace('\\', '/');

					if (fileName.indexOf("/") != -1) {
						createDirectory(outputDirectory, fileName.substring(0, fileName.lastIndexOf("/")));
						fileName = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length());
					}

					File f = new File(outputDirectory + File.separator + zipEntry.getName());

					unZipFile.add(f);

					f.createNewFile();
					InputStream in = zipFile.getInputStream(zipEntry);
					FileOutputStream out = new FileOutputStream(f);

					byte[] by = new byte[1024];
					int c;
					while ((c = in.read(by)) != -1) {
						out.write(by, 0, c);
					}
					out.close();
					in.close();
				}
			}
			zipFile.close();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		return unZipFile;
	}
	
	/* --- Class Part --- */
	public class SPL extends PCData {

		public SPL() {
		}

		@Override
		public String toString() {
			return "SPL {\"PART_CATEGORY_NO\":\"" + super.partsCatalogueNo + "\", \"BIG_GROUP\":\"" + super.bigGroupCode
					+ "\", \"SMALL_GROUP\":\"" + super.smallGroupCode + "\", \"PNC_NO\":\"" + super.pncNo
					+ "\", \"MODELSERIES\":\"" + super.ms + "\", \"START_DATE\":\"" + super.enableDate
					+ "\", \"END_DATE\":\"" + super.disableDate + "\", \"PART_NO\":\"" + super.partNo
					+ "\", \"QUANTITY\":\"" + super.quantity + "\", \"MODELCODE\":" + super.mc + ", \"row\":\""
					+ super.row + "\", \"GROUP_NAME_E\":\"" + super.smallGroupCodeName + "\", \"PNC_NO_E\":\""
					+ super.pncName + "\", \"ns\":\"" + super.ns + "\", \"REMARK\":\"" + super.remark
					+ "\", \"exchangeable\":\"" + super.exchangeable + "\", \"alterPart\":\"" + super.alterPart + "\"}";
		}

		public SPL(String line, int lineIndex) throws Exception {
			if (line.isEmpty() || line.startsWith("@")) {
				super.states = States.Warring;
				super.addErrorMessages("(@\u8a3b\u89e3 \\ \u7a7a\u767d\u884c) \u7b2c " + lineIndex + " \u884c : " + line);
			} else if(line.length() > 320 || line.length() < 316) {
				super.states = States.Warring;
				super.addErrorMessages("(\u8cc7\u6599\u9577\u5ea6 \u6709\u554f\u984c) \u7b2c "  + lineIndex + " \u884c : " + line);
			} else {
				this.splitDefault(line, lineIndex);				
			}
		}

		private void splitDefault(String line, int lineIndex) throws Exception {
			super.lineIndex = lineIndex;
			super.partsCatalogueNo = line.substring(4, 14).trim();
			super.bigGroupCode = line.substring(17, 20).trim();
			super.smallGroupCode = line.substring(20, 23).trim();
			super.pncNo = line.substring(23, 30).trim();
			super.ms = line.substring(30, 37).trim();

			String[] dates = line.substring(37, 67).trim().split("-"); // startDay[0] / EnableDay[1]
			super.enableDate = transDate(dates[0]);
			super.disableDate = dates.length == 2 ? transDate(dates[1]) : "12/31/9999";
			super.partNo = line.substring(67, 84).trim();

			String tempQuantity = line.substring(84, 88).trim().replace("N", "");
			super.quantity = tempQuantity.chars().allMatch(Character::isDigit) ? tempQuantity : "00";

			super.mc = Arrays.stream(line.substring(88, 178).trim().split("  | ")).filter(obj -> !obj.isEmpty()).collect(Collectors.toSet());;
			super.row = line.substring(178, 182).trim();
			super.smallGroupCodeName = line.substring(182, 212).trim();
			super.pncName = line.substring(212, 272).trim();
			super.ns = line.substring(272, 284).trim();
			super.remark = line.substring(284, 314).trim();
			super.exchangeable = line.substring(314, 315).trim();
			super.alterPart = "";
			super.alterPartArrayList = new ArrayList<AlterPart>();

			if (!super.exchangeable.isEmpty()) {
				AlterPart alter = new AlterPart();
				alter.setExchangeAble(super.exchangeable);
				alter.setAlterPart(super.remark);
				super.alterPartArrayList.add(alter);
				super.remark = "";
				super.exchangeable = "";
			}
		}
		
		private String transDate(String date) throws Exception {
			try {
				if (date.length() != 6 || !date.contains(".")) {
					throw new Exception("Date Format Error:" + date);
				}

				String year = "20" + date.substring(0, 2);
				String month = date.substring(2, 4);
				String day = date.substring(5);

				switch (day) {
				case "1":
					break;
				case "2":
					day = "11";
					break;
				case "3":
					day = "21";
					break;
				default:
					throw new Exception("Date Format Error:" + date);
				}
				return month + "/" + day + "/" + year;
			} catch (Exception e) {
				super.addErrorMessages(super.lineIndex + " : " + e.getMessage());
				super.addErrorMessages(
						"------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
				super.setStates(States.Error);
				return "";
			}
		}

		// 欄:5-15 零件冊編碼
		// 欄:18-20 大單元號
		// 欄:21-23 小單元號
		// 欄:24-30 PNC號
		// 欄:31-37 三菱M/S GK1W
		// 欄:38-67 年月旬，1710.1- 只有起用日 1710.1-1710.3 有起用日與停用日
		// 上旬的1對應到1號
		// 中旬的2對應到11號
		// 下旬的3對應到21號
		// 欄:68-84 件號
		// 欄:85-88 用量，01N或CCN ，N要去掉 ，01或CC
		// 欄:89-178 三菱細車型號
		// 欄:179-182 換列標示， 0101 代表資料只有一列， 0102 代表資料有兩列，目前是第一列， 0203代表有三列，目前是第二列
		// 欄:183-212 小單元名稱
		// 欄:213-272 PNC名稱
		// 欄:273-284 三菱M/S 對外稱呼，如NS
		// 欄:285-315 備註欄，若315 是Y OR O，則代表這個欄位記錄的是替代件，Y為新建可替代舊件、O為新舊件互為替代
	}

	public class PC extends PCData {

		public PC() {
		}

		@Override
		public String toString() {
			return "PC {\"partsCatalogueNo\":\"" + super.partsCatalogueNo + "\", \"bigGroupCode\":\""
					+ super.bigGroupCode + "\", \"smallGroupCode\":\"" + super.smallGroupCode + "\", \"pncNo\":\""
					+ super.pncNo + "\", \"enableDate\":\"" + super.enableDate + "\", \"disableDate\":\""
					+ super.disableDate + "\", \"ms\":\"" + super.ms + "\", \"mc\":" + super.mc + ", \"partNo\":\""
					+ super.partNo + "\", \"exchangeable\":\"" + super.exchangeable + "\", \"alterPart\":\""
					+ super.alterPart + "\", \"quantity\":\"" + super.quantity + "\", \"row\":\"" + super.row
					+ "\", \"remark\":\"" + super.remark + "\", \"groupCodeName\":\"" + super.bigGroupCodeName
					+ "\", \"pncName\":\"" + super.pncName + "\"}";
		}

		private String transDate(String date) throws Exception {
			try {
				if (date.length() > 6) {
					throw new Exception("Date Format Error:" + date);
				}

				String year = "20" + date.substring(0, 2);
				String month = date.substring(2, 4);
				String day = "1";

				return month + "/" + day + "/" + year;
			} catch (Exception e) {
				super.addErrorMessages(super.lineIndex + " : " + e.getMessage());
				super.addErrorMessages(
						"------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
				super.setStates(States.Error);
				return "";
			}
		}

		public PC(String line, int lineIndex) throws Exception {
			// ANSI讀檔以外 只剩UTF  用最後一個次判斷 / 長度判斷 / (待決定)
			if (line.isEmpty() || line.startsWith("@")) {
				super.states = States.Warring;
				super.addErrorMessages("(@\u8a3b\u89e3 \\ \u7a7a\u767d\u884c) \u7b2c " + lineIndex + " \u884c : " + line);
			} else if(line.length() <  298 || line.length() > 299) { // 長度
				super.states = States.Warring;
				super.addErrorMessages("(\u8cc7\u6599\u9577\u5ea6 \u6709\u554f\u984c) \u7b2c "  + lineIndex + " \u884c : " + line);
			} else if(line.charAt(296) != ' ' || line.charAt(297) == ' ') { // 297, 298有值
				super.states = States.Warring;
				super.addErrorMessages("(\u8cc7\u6599\u9577\u5ea6 \u6709\u554f\u984c) \u7b2c "  + lineIndex + " \u884c : " + line);
			} else if(line.charAt(37) != ' ') {
				super.states = States.Warring;
				super.addErrorMessages("(\u65e5\u671f\u683c\u5f0f \u6709\u554f\u984c) \u7b2c "  + lineIndex + " \u884c : " + line);
			} else if(line.substring(70, 87).contains("*")) {
				super.states = States.Warring;
				super.addErrorMessages("(\u4ef6\u865f\u661f\u865f \u4e0d\u8655\u7406) \u7b2c "  + lineIndex + " \u884c : " + line);				
			} else { // 正常
				splitDefault(line, lineIndex);
			}			
		}
		
		private void splitDefault(String line, int lineIndex) throws Exception {
			super.lineIndex = lineIndex;
			
			super.partsCatalogueNo = line.substring(3, 13).trim();
			super.bigGroupCode = line.substring(13, 15).trim();
			super.smallGroupCode = line.substring(16, 19).trim();
			super.pncNo = line.substring(19, 26).trim();
	
			String[] dates = line.substring(26, 38).trim().split("-"); // startDay[0] / EnableDay[1]
			super.enableDate = transDate(dates[0]);
			super.disableDate = dates.length == 2 ? transDate(dates[1]) : "12/31/9999";
	
			super.ms = line.substring(38, 45).trim();
			super.mc = Arrays.stream(line.substring(45, 70).trim().split("  | ")).filter(obj -> !obj.isEmpty()).collect(Collectors.toSet());
			if (super.ms.toUpperCase().equals("ALL")) {
				super.mc.add(super.ms);
			}
	
			super.partNo = line.substring(70, 87).trim();
			super.exchangeable = line.substring(87, 88).trim();
			super.alterPart = line.substring(88, 105).trim();
	
			super.quantity = line.substring(105, 107).chars().allMatch(Character::isDigit) ? line.substring(105, 107) : "00";
	
			super.row = line.substring(107, 111).trim();
			super.remark = line.substring(111, 126).trim();
			super.bigGroupCodeName = line.substring(126, 156).trim();
			super.smallGroupCodeName = line.substring(156, 186).trim();
			super.pncName = line.substring(186, 246).trim();
			super.alterPartArrayList = new ArrayList<AlterPart>();
	
			if (!super.exchangeable.isEmpty() && !super.alterPart.isEmpty() ) {
				AlterPart alter = new AlterPart();
				alter.setExchangeAble(super.exchangeable);
				alter.setAlterPart(super.alterPart);
				super.alterPartArrayList.add(alter);
				super.alterPart = "";
				super.exchangeable = "";
			}
		}

		// 欄:4-13 零件冊編碼
		// 欄:14-16 大單元號
		// 欄:17-19 小單元號
		// 欄:20-26 PNC號
		// 欄:27-38 年月旬，1411?-
		// 欄:39-45 三菱 M/S
		// 欄:46-70 三菱 細車型
		// 欄:71-87 件號
		// 欄:88 替代關係 Y、O、#…. # 不處理
		// 欄:89-105 替代件號
		// 欄:106-107 用量，01、CC、AR
		// 欄:108-111 換列標示， 0101 代表資料只有一列， 0102 代表資料有兩列，目前是第一列， 0203代表有三列，目前是第二列
		// 欄:112-126 備註欄
		// 欄:127-156 大單元名稱
		// 欄:157-186 小單元名稱
		// 欄:187-246 PNC名稱
	}

	public class GroupDrawingFile {
		private String groupCode;
		private String groupDrawingName;
		private String groupDrawingLocation;

		public String getGroupCode() {
			return groupCode;
		}

		public void setGroupCode(String groupCode) {
			this.groupCode = groupCode;
		}

		public String getGroupDrawingName() {
			return groupDrawingName;
		}

		public void setGroupDrawingName(String groupDrawingName) {
			this.groupDrawingName = groupDrawingName;
		}

		public String getGroupDrawingLocation() {
			return groupDrawingLocation;
		}

		public void setGroupDrawingLocation(String groupDrawingLocation) {
			this.groupDrawingLocation = groupDrawingLocation;
		}
	}

	public class MMCFolder {
		private String catalogueLocation;
		private Map<String, String> groupCodeInfo;

		public String getCatalogueLocation() {
			return catalogueLocation;
		}

		public void setCatalogueLocation(String catalogueLocation) {
			this.catalogueLocation = catalogueLocation;
		}

		public Map<String, String> getGroupCodeInfo() {
			return groupCodeInfo;
		}

		public void setGroupCodeInfo(Map<String, String> groupCodeInfo) {
			this.groupCodeInfo = groupCodeInfo;
		}
	}

	private enum States {
		Error, Warring, Success
	}

	public abstract class PCData {
		private String partsCatalogueNo;
		private String bigGroupCode;
		private String smallGroupCode;
		private String pncNo;
		private String ms;
		private String enableDate;
		private String disableDate;
		private String partNo;
		private String quantity;
		private Set<String> mc;
		private String row;
		private String pncName;
		private String ns;
		private String remark;
		private String exchangeable;
		private String alterPart;
		private String bigGroupCodeName;
		private String smallGroupCodeName;
		private ArrayList<AlterPart> alterPartArrayList;

		private States states = States.Success;
		private StringList errorMessages;
		private int lineIndex;

		public String getPartsCatalogueNo() {
			return partsCatalogueNo;
		}

		public String getBigGroupCode() {
			return bigGroupCode;
		}

		public String getSmallGroupCode() {
			return smallGroupCode;
		}

		public String getPncNo() {
			return pncNo;
		}

		public String getMs() {
			return ms;
		}

		public String getEnableDate() {
			return enableDate;
		}

		public String getDisableDate() {
			return disableDate;
		}

		public String getPartNo() {
			return partNo;
		}

		public String getQuantity() {
			return quantity;
		}

		public Set<String> getMc() {
			return mc;
		}

		public void setMc(Set<String> mc) {
			this.mc.addAll(mc);
		}

		public String getRow() {
			return row;
		}

		public String getAlterPart() {
			return alterPart;
		}

		public String getBigGroupCodeName() {
			return bigGroupCodeName;
		}

		public String getSmallGroupCodeName() {
			return smallGroupCodeName;
		}

		public String getPncName() {
			return pncName;
		}

		public String getNs() {
			return ns;
		}

		public void setRemark(String remark) {
			this.remark += remark;
		}

		public String getRemark() {
			return remark;
		}

		public String getExchangeable() {
			return exchangeable;
		}

		public String getGroupCode() {
			return this.bigGroupCode + this.smallGroupCode;
		}

		public String getImportGroupDrawingName() {
			return this.partsCatalogueNo + "-" + getGroupCode();
		}

		public String getDomesticGroupDrawingName() {
			return "1" + this.bigGroupCode + "_" + this.smallGroupCode + "F" + this.pncNo;
		}

		public ArrayList<AlterPart> getAlterPartArrayList() {
			return alterPartArrayList;
		}

		public int getLineIndex() {
			return lineIndex;
		}

		public void setAlterPartArrayList(PCData newData) {
			this.alterPartArrayList.addAll(newData.getAlterPartArrayList());
		}

		public void setPncName(String pncName) {
			this.pncName = pncName;
		}

		public States getStates() {
			return states;
		}

		public void setStates(States states) {
			if (this.states.equals(States.Error)) {
				return;
			} else if (this.states.equals(States.Warring) && states.equals(States.Success)) {
				return;
			}
			this.states = states;
		}

		public StringList getErrorMessages() {
			return errorMessages;
		}

		public void setErrorMessages(StringList errorMessageList) {
			if (!Optional.ofNullable(errorMessages).isPresent()) {
				this.errorMessages = new StringList();
			}
			this.errorMessages.addAll(errorMessageList);
		}

		public void addErrorMessages(String errorMessage) {
			if (!Optional.ofNullable(this.errorMessages).isPresent()) {
				this.errorMessages = new StringList();
			}
			if (errorMessage.isEmpty() || this.errorMessages.contains(errorMessage)) {
				return;
			}
			this.errorMessages.add(errorMessage);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PCData other = (PCData) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
				return false;
			return Objects.equals(bigGroupCode, other.bigGroupCode) && Objects.equals(disableDate, other.disableDate)
					&& Objects.equals(enableDate, other.enableDate)
					&& Objects.equals(smallGroupCodeName, other.smallGroupCodeName)
					&& Objects.equals(bigGroupCodeName, other.bigGroupCodeName) && Objects.equals(ms, other.ms)
					&& Objects.equals(ns, other.ns) && Objects.equals(partNo, other.partNo)
					&& Objects.equals(partsCatalogueNo, other.partsCatalogueNo)
					&& Objects.equals(pncName, other.pncName) && Objects.equals(pncNo, other.pncNo)
					&& Objects.equals(quantity, other.quantity) && Objects.equals(smallGroupCode, other.smallGroupCode);
		}

		private SPLM_MMC_PartsCatalogue_JPO_mxJPO getEnclosingInstance() {
			return SPLM_MMC_PartsCatalogue_JPO_mxJPO.this;
		}

	}

	public class AlterPart {
		private String exchangeAble;
		private String alterPart;

		public String getExchangeAble() {
			return exchangeAble;
		}

		public void setExchangeAble(String exchangeAble) {
			this.exchangeAble = exchangeAble;
		}

		public String getAlterPart() {
			return alterPart;
		}

		public void setAlterPart(String alterPart) {
			this.alterPart = alterPart;
		}
	}

	// ------------- test ------------------
	/**
	 * 測試用 - 零件冊 - 日期格式
	 * @throws Exception
	 */
	public void checkMmcDateFormatTest() throws Exception {
		String filePath = "C:/temp/Morga/PC/TTT.txt";
		PC pc = new PC();
		BufferedReader dateli = new BufferedReader(
				new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8));
		String temp = dateli.readLine();
		String[] dataefor = temp.split("-");
		System.out.println(dataefor[0]);
		System.out.println(dataefor[1]);
		dataefor[1] = dataefor[1].replace("\uFFFD", "?");
		System.out.println(dataefor[1]);
		System.out.println(dataefor[1].length());
		System.out.println(pc.transDate(dataefor[0]));
		System.out.println(pc.transDate(dataefor[1]));
	}

	/**
	 * 測試用 - 對原始txt檔進行清洗，獲取資個元件資料並輸出txt檔 (路徑可自己調整)
	 * @throws Exception
	 */
	public void partCatalogueDataTest() throws Exception {
		String splFileLocation = "C:/temp/Morga/Catalogue/PC-CDB3M006D1.txt";
//		String splFileLocation = "C:/temp/Morga/Catalogue/PC-DB-BDBAB503D1.txt";
//		String splFileLocation = "C:/temp/Morga/Catalogue/PC-DB-BDBAB503D144.txt";
//		String splFileLocation = "C:/temp/Morga/Catalogue/PC-DB-BDBAB503D188.txt";
//		String splFileLocation = "C:/temp/Morga/Catalogue/SPL-B80AH538A_DB(171113).txt";
//		String splFileLocation = "C:/temp/Morga/Catalogue/SPL-B80AH550A_DB(171113).txt";
		StringBuffer sb = new StringBuffer();
		ArrayList<PCData> data = this.getSPLMMCPartsCatalogueInfo(splFileLocation,sb);
		System.out.println(sb.toString());
		outputFile("data.txt", translateJsonIntoString(data));
	}
}