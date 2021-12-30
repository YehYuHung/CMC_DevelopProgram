import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.gson.GsonBuilder;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

public class SPLM_SOPB_Excel_JPO_mxJPO {

//	private static final String EXPORT_EXCEL_LOCATION = "D:\\eclipse-workspaceCompany\\morgaJian\\src\\MMC_data\\temp\\";
//	private static final String EXPORT_EXCEL_LOCATIONZIP = "D:\\eclipse-workspaceCompany\\morgaJian\\src\\MMC_data\\123.zip";
//	private static final String INPORT_SO_EXCEL_LOCATION = "D:\\eclipse-workspaceCompany\\morgaJian\\src\\MMC_data\\SO格式 - 20211211.xlsx";
//	private static final String INPORT_PB_EXCEL_LOCATION = "D:\\eclipse-workspaceCompany\\morgaJian\\src\\MMC_data\\PB格式 - 20211211.xlsx";
//	private static final String EXPORT_EXCEL_LOCATION = "C:\\temp\\morga\\temp\\";
//	private static final String EXPORT_EXCEL_LOCATIONZIP = "C:\\temp\\morga\\123.zip";
	private static final String INPORT_SO_EXCEL_LOCATION = "C:\\DassaultSystemesBatchFile\\SOExcelTemplate\\SO - 20211211.xlsx";
	private static final String INPORT_PB_EXCEL_LOCATION = "C:\\DassaultSystemesBatchFile\\SOExcelTemplate\\PB - 20211211.xlsx";

	private static final String TEMP_FOLDER = "PLMTemp";
	private static final String TEMP_FOLDER_DEVICE = "D:\\";

	/* can using method - Testing OK */
	private String getTempFolderPath() throws Exception {
		if (new File(TEMP_FOLDER_DEVICE).exists()) {
			return TEMP_FOLDER_DEVICE + TEMP_FOLDER;
		}
		return "C:\\" + TEMP_FOLDER;
	}

	private void deleteDirectoryExistFiles(String directoryPath) {
		File file = new File(directoryPath);
		if (file.isDirectory()) {
			for (File deleteFile : file.listFiles()) {
				if (deleteFile.delete()) {
					System.out.println("File delete : " + deleteFile.toString());
				} else {
					System.err.println("Check the File can be delete or not!");
				}
			}
			file.delete();
		} else {
			System.err.println("The File Path isn't Directory, PLEASE CHECK THE ROOT WHICH YOU WANNA DELETE !");
		}
	}

	private String translateJsonIntoString(Object obj) {
		return new GsonBuilder().disableHtmlEscaping().create().toJson(obj);
	}

	private String convertDateFormat(String targetDate, String dateFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		Date d = new Date(targetDate);
		return sdf.format(d);
	}

	private XSSFClientAnchor addImage(Workbook wb, XSSFDrawing drawing, String imagePath, XSSFClientAnchor preImage)
			throws Exception {
		byte[] stream = Files.readAllBytes(Paths.get(imagePath));
		int imageId = wb.addPicture(stream, Workbook.PICTURE_TYPE_PNG);

		int startRow = preImage == null ? 0 : preImage.getRow2() + 1;
		int startCol = 0;
		int endRow = preImage == null ? 0 : preImage.getRow2() + 2;
		int endCol = 1;

		XSSFClientAnchor imageAnchor = new XSSFClientAnchor();
		imageAnchor.setRow1(startRow);
		imageAnchor.setCol1(startCol);
		imageAnchor.setRow2(endRow);
		imageAnchor.setCol2(endCol);

		XSSFPicture picture = drawing.createPicture(imageAnchor, imageId);
		picture.resize();

		return imageAnchor;
	}

	private void countRowTitleValuePlace(Sheet sh, int rowIndex, ArrayList<Integer> recordArray) {
		Row row = sh.getRow(rowIndex);
		if (row == null) {
			System.err.println(
					"Method countRowTitleValuePlace : The selected Row is Empty, please check / create new row for count!");
			return;
		}
		Iterator<Cell> cellItr = row.cellIterator();
		int i = 0;
		while (cellItr.hasNext()) {
			if (!cellItr.next().getStringCellValue().isEmpty()) {
				recordArray.add(i);
			}
			++i;
		}
	}

	private CellStyle cloneCellStyle(Sheet sh, int rowIndex, int cellIndex) {
		Row row = sh.getRow(rowIndex);
		if (row == null) {
			throw new IllegalArgumentException(
					"Method cloneCellStyleInSameSheet : The selected Row is Empty, please check / create new row for count!");
		}
		try {
			Cell cell = row.getCell(cellIndex);
			return cell.getCellStyle();
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Method cloneCellStyleInSameSheet : The selected Cell is Empty, please check / create new Cell!");
		}
	}

	private void output(Workbook wb, String exportLocation) throws IOException {
		File file = new File(exportLocation);
		if (file.getParentFile().exists() != true) {
			file.getParentFile().mkdirs();
		}
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(exportLocation);
			wb.write(out);
		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/* ---Dassault API package simple using--- */
	private MapList getIsServicePartAI(Context context, DomainObject soObj, StringList busSelect) throws Exception {
		return getSOAI(context, soObj, busSelect, "attribute[SPLM_IsServicePart]=='TRUE'");
	}

	private MapList getHaveSubPartAI(Context context, DomainObject soObj, StringList busSelect) throws Exception {
		return getSOAI(context, soObj, busSelect, "attribute[SPLM_HaveSubPart]=='TRUE'");
	}

	private MapList getSOAI(Context context, DomainObject soObj, StringList busSelect, String objectWhere)
			throws Exception {
		return soObj.getRelatedObjects(context, "SPLM_AffectedItem", // relationshipPattern,
				"SPLM_Part,SPLM_ColorPart", // typePattern,
				busSelect, // StringList objectSelects,
				null, // StringList relationshipSelects,
				false, // boolean getTo,
				true, // boolean getFrom,
				(short) 1, // short recurseToLevel,
				objectWhere, // String objectWhere,
				"", // String relationshipWhere
				0); // int limit)
	}

	/* ---SO PB select--- */
	private String publishSOExcel(Context context) throws Exception {
		StringList busSelectSo = new StringList();
		busSelectSo.add(DomainConstants.SELECT_ID);
		busSelectSo.add(DomainConstants.SELECT_NAME);
		busSelectSo.add(DomainObject.SELECT_VAULT);
		busSelectSo.add(DomainObject.SELECT_OWNER);
		busSelectSo.add("current.actual");
		busSelectSo.add("attribute[SPLM_SendDept]");

		StringList busSelectForAI = new StringList();
		busSelectForAI.add(DomainConstants.SELECT_ID);
		busSelectForAI.add(DomainConstants.SELECT_NAME);
		busSelectForAI.add("attribute[SPLM_Name_TC]"); // PART_NAME_C
		busSelectForAI.add("attribute[SPLM_Name_EN]"); // PART_NAME_E
		busSelectForAI.add("attribute[SPLM_MediumPackingQuantity]"); // MEDIUNM_PACKINGNUMS
		busSelectForAI.add("attribute[SPLM_Unit]"); // UNIT
		busSelectForAI.add("attribute[SPLM_MaterialType]"); // MATERIAL_TYPE
		busSelectForAI.add("attribute[SPLM_MaterialGroup]"); // MATERIAL_GROUP
		busSelectForAI.add("attribute[SPLM_ItemSubType]"); // PART_TYPE
		busSelectForAI.add("attribute[SPLM_AC_NO]"); // ACNO
		busSelectForAI.add("attribute[SPLM_SO_Note]"); // ACNO

		StringList busSelectForSBOM = new StringList();
		busSelectForSBOM.add(DomainObject.SELECT_ID);
		busSelectForSBOM.add(DomainObject.SELECT_NAME);

		MapList soMapList = DomainObject.findObjects(context, "SPLM_SO", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"current=='Complete'&& attribute[SPLM_BatchFlag]=='Processing'", // where
				null, false, busSelectSo, (short) 0);

		String outputZipFile = "";

		ArrayList<String> soTitle = new ArrayList<String>();
		DomainObject soDomObj = new DomainObject();
		for (Object soObj : soMapList) {
			String soId = (String) ((Map) soObj).get(DomainConstants.SELECT_ID);
			String soName = (String) ((Map) soObj).get(DomainConstants.SELECT_NAME);
			String soVault = (String) ((Map) soObj).get(DomainConstants.SELECT_VAULT);
			String soOwner = (String) ((Map) soObj).get(DomainConstants.SELECT_OWNER);
			String soSendDept = (String) ((Map) soObj).get("attribute[SPLM_SendDept]");
			String soReleaseDate = (String) ((Map) soObj).get("current.actual");
			String soSendSpec = "";
			String soCreate = "";

			// Data processing
			soSendDept = soSendDept.isEmpty() ? "" : soSendDept.replaceAll("~QWE~", ",");
			soReleaseDate = soReleaseDate.isEmpty() ? "" : convertDateFormat(soReleaseDate, "yyyyMMdd");
			soTitle.add(soName);
			soTitle.add(soSendSpec);
			soTitle.add(soVault);
			soTitle.add(soCreate);
			soTitle.add(soSendDept);
			soTitle.add(soReleaseDate);

			soDomObj.setId(soId);

			MapList partMapListForAI = getIsServicePartAI(context, soDomObj, busSelectForAI);
			MapList partMapListForSBOM = getHaveSubPartAI(context, soDomObj, busSelectForSBOM);
			Map<String, ArrayList<ArrayList<String>>> AffectedItemMap = getAffectedItemInfo(context, partMapListForAI);
			Map<String, ArrayList<ArrayList<String>>> sbomMap = getSBOMInfo(context, partMapListForSBOM);

			createSOExcel(soTitle, AffectedItemMap, sbomMap);
			outputZipFile = getTempFolderPath() + File.separator + soName + ".zip";
		}
		return outputZipFile;
//		createPBExcel(context, soMapList);
	}

	/* ---Start Line : API--- */
	private Map<String, ArrayList<ArrayList<String>>> getAffectedItemInfo(Context context, MapList partMapList)
			throws Exception {

		StringList busSelectAltOpt = new StringList();
		busSelectAltOpt.add(DomainConstants.SELECT_NAME);
		StringList relSelectAltOpt = new StringList();
		relSelectAltOpt.add("attribute[SPLM_OptionalType]");
		relSelectAltOpt.add("attribute[SPLM_OptionalExchangeable]");

		List<String> partNameArray = (List<String>) partMapList.stream()
				.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME)).collect(Collectors.toList());

		Map<String, ArrayList<ArrayList<String>>> affectedItemMap = new HashMap<String, ArrayList<ArrayList<String>>>();

		DomainObject partDomObj = new DomainObject();
		for (Object partObj : partMapList) {
			Map partMap = (Map) partObj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partNameCh = (String) partMap.get("attribute[SPLM_Name_TC]");
			String partNameEn = (String) partMap.get("attribute[SPLM_Name_EN]");
			String partMediumPackingNums = (String) partMap.get("attribute[SPLM_MediumPackingQuantity]");
			String partUnit = (String) partMap.get("attribute[SPLM_Unit]");
			String partMaterialType = (String) partMap.get("attribute[SPLM_MaterialType]");
			String partMaterialGroup = (String) partMap.get("attribute[SPLM_MaterialGroup]");
			String partItemSubType = ((String) partMap.get("attribute[SPLM_ItemSubType]"));
			String partACNO = (String) partMap.get("attribute[SPLM_AC_NO]");
			String partRemark = (String) partMap.get("attribute[SPLM_SO_Note]");
			partDomObj.setId(partId);
			String relatedEO = partDomObj.getInfo(context, "from[SPLM_SBOM].attribute[SPLM_RelatedEO]");
			String relatedPO = partDomObj.getInfo(context, "from[SPLM_SBOM].attribute[SPLM_RelatedPO]");
			String partParent = "";

			// parent Part data processing
			MapList partParentMapList = partDomObj.getRelatedObjects(context, "SPLM_SBOM", // relationshipPattern,
					"SPLM_Part,SPLM_ColorPart", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					null, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"attribute[SPLM_IsServicePart]=='True'", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			for (Object partParentObj : partParentMapList) {
				String part = (String) ((Map) partParentObj).get(DomainConstants.SELECT_NAME);
				if (partNameArray.contains(part)) {
					partParent = part;
				}
			}

			// groupName data processing
			StringList groupCodeList = partDomObj.getInfoList(context,
					"to[SPLM_RelatedPart].from[SPLM_GroupDrawing].name");
			groupCodeList = groupCodeList.size() > 0 ? groupCodeList : new StringList("");

			// partType data processing
			if (!partItemSubType.isEmpty()) {
				partItemSubType = partItemSubType.substring(0, 1);
			}

			// vendorNo data processing
			MapList vendorMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedVendor", // relationshipPattern,
					"SPLM_Vendor", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_Valid]==Y", // String relationshipWhere
					0); // int limit)

			// altPart/optPart data processing
			MapList optAltMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedOptionalPart", // relationshipPattern,
					"SPLM_Part,SPLM_ColorPart", // typePattern,
					busSelectAltOpt, // StringList objectSelects,
					relSelectAltOpt, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			ArrayList<String> optAltArray = new ArrayList<String>();
			for (Object optAltObj : optAltMapList) {
				Map optAltMap = (Map) optAltObj;
				String optAltName = (String) optAltMap.get(DomainConstants.SELECT_NAME);
//				String optAltType = (String) optAltMap.get("attribute[SPLM_OptionalType]");
				String optAltExchageable = (String) optAltMap.get("attribute[SPLM_OptionalExchangeable]");
				String optAltRel = optAltName + "(" + optAltExchageable + ")";
				optAltArray.add(optAltRel);
			}
//			System.out.println("Packing Start");
			// Packing AI Info
			for (Object vendorObj : vendorMapList) {
				Map vendorMap = (Map) vendorObj;
				String vendorName = (String) vendorMap.get(DomainConstants.SELECT_NAME);
				ArrayList<ArrayList<String>> affectedItemInfoArray = new ArrayList<ArrayList<String>>();
				for (String groupCode : groupCodeList) {
					for (String optAltRel : optAltArray) {

						ArrayList<String> affectedItemDataArray = new ArrayList<String>();
						affectedItemDataArray.add(partParent); // part
						affectedItemDataArray.add(partName); // affectedItemPart
						affectedItemDataArray.add(partNameCh + "/" + partNameEn); // partName
						affectedItemDataArray.add(groupCode); // groupDrawing
						affectedItemDataArray.add(partMediumPackingNums); // mediumPackingNums
						affectedItemDataArray.add(partUnit); // unit
						affectedItemDataArray.add(partMaterialType); // materialType
						affectedItemDataArray.add(partMaterialGroup); // materialGroup
						affectedItemDataArray.add(partItemSubType); // partType
						affectedItemDataArray.add(vendorName); // vendor
						affectedItemDataArray.add(relatedEO); // referenceEO
						affectedItemDataArray.add(relatedPO); // referencePO
						affectedItemDataArray.add(optAltRel); // altOptPart
						affectedItemDataArray.add(partACNO); // acNo
						affectedItemDataArray.add(partRemark); // remark

//						System.out.println(affectedItemDataArray);
						affectedItemInfoArray.add(affectedItemDataArray);
					}
				}
				affectedItemMap.put(vendorName, affectedItemInfoArray);
			}
		}
		return affectedItemMap;
	}

	private Map<String, ArrayList<ArrayList<String>>> getSBOMInfo(Context context, MapList partMapList)
			throws Exception {

		StringList busSelectSBom = new StringList();
		busSelectSBom.add(DomainConstants.SELECT_NAME);
		StringList relSelectSBom = new StringList();
		relSelectSBom.add("attribute[Quantity]");
		relSelectSBom.add("attribute[SPLM_GroupCode]");
		relSelectSBom.add("attribute[SPLM_PNC]"); // pnc
		relSelectSBom.add("attribute[SPLM_Materiel_KD_Type]"); // K-/D-
		relSelectSBom.add("attribute[SPLM_EnableDate]"); // Start Date
		relSelectSBom.add("attribute[SPLM_DisableDate]"); // End Date
		relSelectSBom.add("attribute[SPLM_BOM_Note]"); // Bom remark

		Map<String, ArrayList<ArrayList<String>>> sbomMap = new HashMap<String, ArrayList<ArrayList<String>>>();

		DomainObject partDomObj = new DomainObject();
		for (Object partObj : partMapList) {
			Map partMap = (Map) partObj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			partDomObj.setId(partId);

			// vendorNo data processing
			MapList vendorMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedVendor", // relationshipPattern,
					"SPLM_Vendor", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_Valid]==Y", // String relationshipWhere
					0); // int limit)

			MapList subPartMapList = partDomObj.getRelatedObjects(context, "SPLM_SBOM", // relationshipPattern,
					"SPLM_Part,SPLM_ColorPart", // typePattern,
					busSelectSBom, // StringList objectSelects,
					relSelectSBom, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			for (Object vendorObj : vendorMapList) {
				Map vendorMap = (Map) vendorObj;
				String vendorName = (String) vendorMap.get(DomainConstants.SELECT_NAME);
				ArrayList<ArrayList<String>> sbomInfoArray = new ArrayList<ArrayList<String>>();

				for (Object subPartObj : subPartMapList) {
					Map subPartMap = (Map) subPartObj;

					String subPartName = (String) subPartMap.get(DomainConstants.SELECT_NAME);
					String subPartQuantity = (String) subPartMap.get("attribute[Quantity]");
					String subPartGroup = (String) subPartMap.get("attribute[SPLM_GroupCode]");
					String subPartPnc = (String) subPartMap.get("attribute[SPLM_PNC]");
					String subPartKDType = (String) subPartMap.get("attribute[SPLM_Materiel_KD_Type]");
					String subPartStartDate = (String) subPartMap.get("attribute[SPLM_EnableDate]");
					String subPartEndDate = (String) subPartMap.get("attribute[SPLM_DisableDate]");
					String subPartBomRemark = (String) subPartMap.get("attribute[SPLM_BOM_Note]");

					// Date format change
					subPartStartDate = subPartStartDate.isEmpty() ? ""
							: convertDateFormat(subPartStartDate, "yyyyMMdd");
					subPartEndDate = subPartEndDate.isEmpty() ? "" : convertDateFormat(subPartEndDate, "yyyyMMdd");

					ArrayList<String> sbomDataArray = new ArrayList<String>();
					sbomDataArray.add(partName); // part
					sbomDataArray.add(subPartName); // subPart
					sbomDataArray.add(subPartQuantity); // quantity
					sbomDataArray.add(subPartGroup); // group
					sbomDataArray.add(subPartPnc); // pnc
					sbomDataArray.add(subPartKDType); // KD-type
					sbomDataArray.add(vendorName); // vendor
					sbomDataArray.add(subPartStartDate); // startDate
					sbomDataArray.add(subPartEndDate); // endDate
					sbomDataArray.add(subPartBomRemark); // bomRemark

					sbomInfoArray.add(sbomDataArray);
					sbomMap.put(vendorName, sbomInfoArray);
				}
			}
		}
		return sbomMap;
	}

	/* --- Create Excel --- */
	private void createPBExcel(Context context, MapList soMapList, MapList sbomMapt) throws Exception {

//		CMC_Method_JPO_mxJPO.makeZip(null, );
	}

	private void createSOExcel(ArrayList<String> soTitle, Map<String, ArrayList<ArrayList<String>>> AffectedItemMap,
			Map<String, ArrayList<ArrayList<String>>> sbomMap) throws Exception {
		String soName = soTitle.get(0);
		String tempFolder = getTempFolderPath() + File.separator;
		String outputSoFileName = tempFolder + "temp" + File.separator;
		String outputZipFile = tempFolder + soName + ".zip";

		for (String vendorName : AffectedItemMap.keySet()) {
			FileInputStream fis = new FileInputStream(INPORT_SO_EXCEL_LOCATION);
			XSSFWorkbook SOWorkBook = (XSSFWorkbook) WorkbookFactory.create(fis);
			fis.close();

			// Home Page
			Sheet homePageSheet = SOWorkBook.getSheet("封面及AI");
			CellStyle homePageCellStyleForDelivery = cloneCellStyle(homePageSheet, 3, 0);
			CellStyle homePageCellStyleForAIDetail = cloneCellStyle(homePageSheet, 10, 0);

			/* homePage Sending title */
			ArrayList<Integer> inputValuePlaceForDeliveryArray = new ArrayList<Integer>();
			countRowTitleValuePlace(homePageSheet, 3, inputValuePlaceForDeliveryArray);

			Row row4 = homePageSheet.getRow(4);
			int deliveryLength = inputValuePlaceForDeliveryArray.size();
			for (int i = 0; i < deliveryLength; ++i) {
				Cell cell = row4.createCell(inputValuePlaceForDeliveryArray.get(i));
				cell.setCellStyle(homePageCellStyleForDelivery);
				cell.setCellValue(soTitle.get(i));
			}

			/* AI */
			ArrayList<Integer> inputValuePlaceForAIArray = new ArrayList<Integer>();
			countRowTitleValuePlace(homePageSheet, 10, inputValuePlaceForAIArray);

			ArrayList<ArrayList<String>> aiOuterArray = AffectedItemMap.get(vendorName);
			int aiFinalSite = 11 + aiOuterArray.size();
			for (int i = 11; i < aiFinalSite; i++) {
				ArrayList<String> aiArray = aiOuterArray.get(i - 11);
				Row row = homePageSheet.createRow(i);
				for (Integer j : inputValuePlaceForAIArray) {
					Cell cell = row.createCell(j);
					cell.setCellStyle(homePageCellStyleForAIDetail);
					cell.setCellValue(aiArray.get(j));
				}
			}

			// BOM
			Sheet BOMSheet = SOWorkBook.getSheet("BOM");
			CellStyle BOMCellStyleForSBOM = cloneCellStyle(BOMSheet, 1, 0);
			ArrayList<Integer> inputValuePlaceForSBOMArray = new ArrayList<Integer>();
			countRowTitleValuePlace(BOMSheet, 1, inputValuePlaceForSBOMArray);

			ArrayList<ArrayList<String>> sbomOuterArray = new ArrayList<ArrayList<String>>();
			if (sbomMap.containsKey(vendorName)) {
				sbomOuterArray = sbomMap.get(vendorName);
			}
			if (sbomOuterArray.size() > 0) {
				int sbomFinalSite = 2 + sbomOuterArray.size();
				for (int i = 2; i < sbomFinalSite; i++) {
					ArrayList<String> sbomArray = sbomOuterArray.get(i - 2);
					Row row = BOMSheet.createRow(i);
					for (Integer j : inputValuePlaceForSBOMArray) {
						Cell cell = row.createCell(j);
						cell.setCellStyle(BOMCellStyleForSBOM);
						cell.setCellValue(sbomArray.get(j));
					}
				}
			}

			/* past ZIP Picture */
			Sheet picSheet_1 = SOWorkBook.getSheet("圖號1");
			Sheet picSheet_2 = SOWorkBook.getSheet("圖號2");

			String soFile = outputSoFileName + soName + "_" + vendorName + ".xlsx";
			this.output(SOWorkBook, soFile);
		}
		CMC_Method_JPO_mxJPO.makeZip(new File(outputSoFileName), new File(outputZipFile));
		deleteDirectoryExistFiles(outputSoFileName);
	}

	private void getSOTitle(Context context, DomainObject soObj) throws Exception {
		String soName = soObj.getName(context);
		String soVault = soObj.getVault(context);
		String soOwner = soObj.getOwner(context).toString();
		String soSendDept = soObj.getAttributeValue(context, "attribute[SPLM_SendDept]").replace("~QWE~", ",");
		String soReleaseDate = soObj.getCurrentState(context).getActualDate();
		String soIssuer = "";
	}

	/////////////////////////////////////////////////////////////////////// new
	public HashMap<String, String> publishSOPBExcel(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		DomainObject soObj = new DomainObject(objectId);

		HashMap<String, String> returnMap = new HashMap<String, String>();

		HashMap<String, String> soTitleMap = new HashMap<String, String>();
		String soName = soObj.getName(context);
		String soVault = soObj.getVault(context);
		String soOwner = soObj.getOwner(context).toString();
		String soSendDept = soObj.getAttributeValue(context, "attribute[SPLM_SendDept]").replace("~QWE~", ",");
		String soReleaseDate = soObj.getCurrentState(context).getActualDate();
		String soIssuer = "";
		String releaseSO = soObj.getAttributeValue(context, "attribute[SPLM_ReleaseSO]");
		String releasePB = soObj.getAttributeValue(context, "attribute[SPLM_ReleasePB]");

		String outputSoFileName = getTempFolderPath() + "\\createSOExcel\\" + soName;
		String outputSoFileZip = outputSoFileName + ".zip";
		try {
			if (releaseSO.equalsIgnoreCase("False") && releasePB.equalsIgnoreCase("False")) {
				throw new Exception("\u8acb\u52fe\u9078 \u767c\u4f48SO / \u767c\u4f48PB");
			}
			// Data processing
			soSendDept = soSendDept.length() == 0 ? "" : soSendDept.replaceAll("~QWE~", ",");
			soReleaseDate = soReleaseDate.length() == 0 ? "" : convertDateFormat(soReleaseDate, "yyyyMMdd");

			soTitleMap.put("soName", soName);
			soTitleMap.put("soIssuer", soIssuer);
			soTitleMap.put("soVault", soVault);
			soTitleMap.put("soOwner", soOwner);
			soTitleMap.put("soSendDept", soSendDept);
			soTitleMap.put("soReleaseDate", soReleaseDate);

			ArrayList<SoPublish> soPublishArrayList = new ArrayList<SoPublish>();
			
			HashMap<String, Object> soExcelInfo = SOExcel(context, soObj, soPublishArrayList);
			soExcelInfo.put("soTitle", soTitleMap);
			ArrayList<String> fileArrayList = createSOExcel123(soExcelInfo, outputSoFileName);

			if (fileArrayList.size() == 0) {
				throw new Exception("\u6c92\u6709\u6a94\u6848\u53ef\u4ee5\u4e0b\u8f09");
			}
			
			CMC_Method_JPO_mxJPO.makeZip(new File(outputSoFileName), new File(outputSoFileZip));
			this.deleteDirectoryExistFiles(outputSoFileName);

			returnMap.put("FilePath", outputSoFileZip);
		} catch (Exception e) {
			returnMap.put("Message", e.getMessage());
		}

		return returnMap;
	}

	private HashMap<String, Object> SOExcel(Context context, DomainObject soObj,ArrayList<SoPublish> soPublishArrayList) throws Exception {
		StringList busSelectsPart = new StringList();
		busSelectsPart.add(DomainConstants.SELECT_ID);
		busSelectsPart.add(DomainConstants.SELECT_NAME);
		busSelectsPart.add("attribute[SPLM_Name_TC]"); // PART_NAME_C
		busSelectsPart.add("attribute[SPLM_Name_EN]"); // PART_NAME_E
		busSelectsPart.add("attribute[SPLM_MediumPackingQuantity]"); // MEDIUNM_PACKINGNUMS
		busSelectsPart.add("attribute[SPLM_Unit]"); // UNIT
		busSelectsPart.add("attribute[SPLM_MaterialType]"); // MATERIAL_TYPE
		busSelectsPart.add("attribute[SPLM_MaterialGroup]"); // MATERIAL_GROUP
		busSelectsPart.add("attribute[SPLM_ItemSubType]"); // PART_TYPE
		busSelectsPart.add("attribute[SPLM_AC_NO]"); // ACNO
		busSelectsPart.add("attribute[SPLM_SO_Note]"); // REMARK

		StringList busSelectAltOpt = new StringList();
		busSelectAltOpt.add(DomainConstants.SELECT_NAME);
		StringList relSelectAltOpt = new StringList();
		relSelectAltOpt.add("attribute[SPLM_OptionalExchangeable]");

		StringList parentBusSelects = new StringList();
		parentBusSelects.add(DomainConstants.SELECT_ID);
		parentBusSelects.add(DomainConstants.SELECT_NAME);
		
		StringList busSelectSBom = new StringList();
		busSelectSBom.add(DomainConstants.SELECT_NAME);
		StringList relSelectSBom = new StringList();
		relSelectSBom.add("attribute[Quantity]");
		relSelectSBom.add("attribute[SPLM_GroupCode]");
		relSelectSBom.add("attribute[SPLM_PNC]"); // pnc
		relSelectSBom.add("attribute[SPLM_Materiel_KD_Type]"); // K-/D-
		relSelectSBom.add("attribute[SPLM_EnableDate]"); // Start Date
		relSelectSBom.add("attribute[SPLM_DisableDate]"); // End Date
		relSelectSBom.add("attribute[SPLM_BOM_Note]"); // Bom remark
		
		StringList busSelectVendor = new StringList();
		busSelectVendor.add(DomainConstants.SELECT_NAME);
		busSelectVendor.add(DomainConstants.SELECT_ID);

		String soName = soObj.getName(context);
		String forceBreakLine = "\r\n";

		MapList partMapList = getIsServicePartAI(context, soObj, busSelectsPart);
		if (partMapList.isEmpty()) {
			throw new Exception("\u7f3a\u5c11 \u5f71\u97ff\u4ef6\u865f");
		}
		StringList soAffectedPartIdList = soObj.getInfoList(context, "from[SPLM_AffectedItem].to.id");

		HashMap<String, Object> map = new HashMap<String, Object>();
		DomainObject partDomObj = new DomainObject();
		
		for (Object partInfo : partMapList) {
			// AI
			Map partMap = (Map) partInfo;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partNameCh = (String) partMap.get("attribute[SPLM_Name_TC]");
			String partNameEn = (String) partMap.get("attribute[SPLM_Name_EN]");
			String partMediumPackingNums = (String) partMap.get("attribute[SPLM_MediumPackingQuantity]");
			String partUnit = (String) partMap.get("attribute[SPLM_Unit]");
			String partMaterialType = (String) partMap.get("attribute[SPLM_MaterialType]");
			String partMaterialGroup = (String) partMap.get("attribute[SPLM_MaterialGroup]");
			String partItemSubType = ((String) partMap.get("attribute[SPLM_ItemSubType]"));
			String partACNO = (String) partMap.get("attribute[SPLM_AC_NO]");
			String partRemark = (String) partMap.get("attribute[SPLM_SO_Note]");
			partDomObj.setId(partId);
			String relatedEO = partDomObj.getInfo(context, "from[SPLM_SBOM].attribute[SPLM_RelatedEO]");
			String relatedPO = partDomObj.getInfo(context, "from[SPLM_SBOM].attribute[SPLM_RelatedPO]");
			String parentPart = "";
			String altOptPart = "";

			// partParent data processing
			MapList partParentMapList = partDomObj.getRelatedObjects(context, "SPLM_SBOM", // relationshipPattern,
					"*", // typePattern,
					parentBusSelects, // StringList objectSelects,
					null, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			for (Object parentPartInfo : partParentMapList) {
				String parentPartId = (String) ((Map) parentPartInfo).get(DomainConstants.SELECT_ID);
				if (soAffectedPartIdList.contains(parentPartId)) {
					parentPart = (String) ((Map) parentPartInfo).get(DomainConstants.SELECT_NAME);
					break;
				}
			}

			// vendor/document data processing
			MapList vendorMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedSOReleaseItem", // relationshipPattern,
					"*", // typePattern,
					busSelectVendor, // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_SO]=='" + soName + "' &&  attribute[SPLM_SOReleaseItemType]=='Vendor'", // String
																											// relationshipWhere
					0); // int limit)

			List<String> vendorList = (List<String>) vendorMapList.stream()
					.filter(obj -> ((String) ((Map) obj).get(DomainConstants.SELECT_TYPE)).equalsIgnoreCase("SPLM_Vendor"))
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME))
					.collect(Collectors.toList());
			
			if (vendorList.isEmpty()) {
				throw new Exception("\u552e\u670d\u96f6\u4ef6 " + partName + " \u586b\u9078 \u767c\u9001\u5ee0\u5546");
			}
			
			List<String> soDocumentIdList = (List<String>) vendorMapList.stream()
					.filter(obj -> ((String) ((Map) obj).get(DomainConstants.SELECT_TYPE)).equalsIgnoreCase("SPLM_Document"))
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_ID))
					.collect(Collectors.toList());


			// groupName data processing
			StringList groupCodeList = partDomObj.getInfoList(context,
					"to[SPLM_RelatedPart].from[SPLM_GroupDrawing].name");
			groupCodeList = groupCodeList.size() > 0 ? groupCodeList : new StringList("");
			String groupCode = groupCodeList.size() > 1 ? String.join("/n", groupCodeList) : "";

			// partType data processing
			if (partItemSubType.length() > 2) {
				partItemSubType = partItemSubType.substring(0, 1);
			}

			// altPart/optPart data processing
			MapList optAltMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedOptionalPart", // relationshipPattern,
					"SPLM_Part,SPLM_ColorPart", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					new StringList("attribute[SPLM_OptionalExchangeable]"), // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			ArrayList<String> optAltArrayList = new ArrayList<String>();
			for (Object optAltObj : optAltMapList) {
				Map optAltMap = (Map) optAltObj;
				String optAltName = (String) optAltMap.get(DomainConstants.SELECT_NAME);
				String optAltExchageable = (String) optAltMap.get("attribute[SPLM_OptionalExchangeable]");
				String optAltRel = optAltExchageable.isEmpty() ? optAltName
						: optAltName + "(" + optAltExchageable + ")";
				optAltArrayList.add(optAltRel);
			}

			altOptPart = optAltArrayList.isEmpty()?  altOptPart : String.join(forceBreakLine, optAltArrayList);

			// BOM
			MapList subPartMapList = partDomObj.getRelatedObjects(context, "SPLM_SBOM,SPLM_MBOM", // relationshipPattern,
					"*", // typePattern,
					busSelectSBom, // StringList objectSelects,
					relSelectSBom, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)
			
			// vendor all through rel[SPLM_RelatedVendor].vendor.attribute[]==TRUE
			MapList validVenderMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedVendor", // relationshipPattern,
					"SPLM_Vendor", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_Valid]=='Y'", // String relationshipWhere
					0); // int limit)
			List<String> affectedItemVendorList = (List<String>) validVenderMapList.stream().map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME)).collect(Collectors.toList());


			SoPublish so;
			ArrayList<AffectedItem> affectedItemArrayList;
			ArrayList<PartBom> partBomArrayList;
			for (String vendorName : vendorList) {

				if (map.containsKey(vendorName)) {
					so = (SoPublish) map.get(vendorName);
				} else {
					so = new SoPublish();
					map.put(vendorName, so);
				}

				if (so.vendorAI != null) {
					affectedItemArrayList = so.vendorAI;
				} else {
					affectedItemArrayList = new ArrayList<AffectedItem>();
					so.vendorAI = affectedItemArrayList;
				}

				if (so.vendorBom != null) {
					partBomArrayList = so.vendorBom;
				} else {
					partBomArrayList = new ArrayList<PartBom>();
					so.vendorBom = partBomArrayList;
				}

				AffectedItem affectedItem = new AffectedItem();
				affectedItem.part = parentPart;
				affectedItem.affectedItemPart = partName;
				affectedItem.partName = partNameCh + forceBreakLine + partNameEn;
				affectedItem.groupDrawing = groupCode;
				affectedItem.mediumPackingNums = partMediumPackingNums;
				affectedItem.unit = partUnit;
				affectedItem.materialType = partMaterialType;
				affectedItem.materialGroup = partMaterialGroup;
				affectedItem.partType = partItemSubType;
				affectedItem.vendor = vendorName;
				affectedItem.referenceEO = relatedEO;
				affectedItem.referencePO = relatedPO;
				affectedItem.altOptPart = altOptPart;
				affectedItem.acNo = partACNO;
				affectedItem.remark = partRemark;
				affectedItemArrayList.add(affectedItem);

				for (Object subPartObj : subPartMapList) {
					Map subPartMap = (Map) subPartObj;

					String subPartStartDate = (String) subPartMap.get("attribute[SPLM_EnableDate]");
					String subPartEndDate = (String) subPartMap.get("attribute[SPLM_DisableDate]");
					String subPartKDType = (String) subPartMap.get("attribute[SPLM_Materiel_KD_Type]");

					// Date format change
					subPartStartDate = subPartStartDate.isEmpty() ? ""
							: convertDateFormat(subPartStartDate, "yyyyMMdd");
					subPartEndDate = subPartEndDate.isEmpty() ? "" : convertDateFormat(subPartEndDate, "yyyyMMdd");

					// subPartKDType data processing
					subPartKDType = subPartKDType.equals("*-") ? "" : subPartKDType;

					PartBom partBom = new PartBom();
					partBom.part = partName;
					partBom.subPart = (String) subPartMap.get(DomainConstants.SELECT_NAME);
					partBom.quantity = (String) subPartMap.get("attribute[Quantity]");
					partBom.group = (String) subPartMap.get("attribute[SPLM_GroupCode]");
					partBom.pnc = (String) subPartMap.get("attribute[SPLM_PNC]");
					partBom.KDType = subPartKDType;
					partBom.vendor = subPartKDType.length() > 0 ? "" : vendorName;
					partBom.startDate = subPartStartDate;
					partBom.endDate = subPartEndDate;
					partBom.bomRemark = (String) subPartMap.get("attribute[SPLM_BOM_Note]");

					partBomArrayList.add(partBom);
				}
			}
		}
		return map;
	}

	private ArrayList<String> createSOExcel123(HashMap<String, Object> soExcelInfo, String outputSoFileName)
			throws Exception {
		Map<String, String> soTitleMap = (Map<String, String>) soExcelInfo.get("soTitle");
		soExcelInfo.remove("soTitle");
		String soName = soTitleMap.get("soName");

		ArrayList<String> soFileArrayList = new ArrayList<String>();

		FileInputStream fis = null;
		try {
			for (String vendorName : soExcelInfo.keySet()) {
				fis = new FileInputStream(INPORT_SO_EXCEL_LOCATION);
				XSSFWorkbook SOWorkBook = (XSSFWorkbook) WorkbookFactory.create(fis);
				fis.close();

				SoPublish soPublish = (SoPublish) soExcelInfo.get(vendorName);

				// Home Page
				Sheet homePageSheet = SOWorkBook.getSheet("封面及AI");

				/* homePage Sending title */
				CellStyle homePageCellStyleForDelivery = cloneCellStyle(homePageSheet, 3, 0);
				homePageCellStyleForDelivery.setWrapText(true);
				Row row4 = homePageSheet.getRow(4);

				Cell soNumberCell = row4.createCell(0);
				soNumberCell.setCellStyle(homePageCellStyleForDelivery);
				soNumberCell.setCellValue(soName);

				Cell soIssuerCell = row4.createCell(1);
				soIssuerCell.setCellStyle(homePageCellStyleForDelivery);
				soIssuerCell.setCellValue(soTitleMap.get("soIssuer"));

				Cell soOwnerCell = row4.createCell(2);
				soOwnerCell.setCellStyle(homePageCellStyleForDelivery);
				soOwnerCell.setCellValue(soTitleMap.get("soOwner"));

				Cell soVaultCell = row4.createCell(3);
				soVaultCell.setCellStyle(homePageCellStyleForDelivery);
				soVaultCell.setCellValue(soTitleMap.get("soVault"));

				Cell soSendDeptCell = row4.createCell(5);
				soSendDeptCell.setCellStyle(homePageCellStyleForDelivery);
				soSendDeptCell.setCellValue(soTitleMap.get("soSendDept"));

				Cell soReleaseDateCell = row4.createCell(13);
				soReleaseDateCell.setCellStyle(homePageCellStyleForDelivery);
				soReleaseDateCell.setCellValue(soTitleMap.get("soReleaseDate"));

				/* AI */
				CellStyle homePageCellStyleForAIDetail = cloneCellStyle(homePageSheet, 10, 0);
				homePageCellStyleForAIDetail.setWrapText(true);
				int aiLocationRow = 11;
				if (soPublish.vendorAI != null) {
					ArrayList<AffectedItem> affectedItemArrayList = soPublish.vendorAI;
					for (int i = 0; i < affectedItemArrayList.size(); i++) {
						Row row = homePageSheet.createRow(i + aiLocationRow);
						AffectedItem affectedItem = affectedItemArrayList.get(i);

						Cell partCell = row.createCell(0);
						partCell.setCellStyle(homePageCellStyleForAIDetail);
						partCell.setCellValue(affectedItem.part);

						Cell affectedItemPartCell = row.createCell(1);
						affectedItemPartCell.setCellStyle(homePageCellStyleForAIDetail);
						affectedItemPartCell.setCellValue(affectedItem.affectedItemPart);

						Cell partNameCell = row.createCell(2);
						partNameCell.setCellStyle(homePageCellStyleForAIDetail);
						partNameCell.setCellValue(affectedItem.partName);

						Cell groupDrawingCell = row.createCell(3);
						groupDrawingCell.setCellStyle(homePageCellStyleForAIDetail);
						groupDrawingCell.setCellValue(affectedItem.groupDrawing);

						Cell mediumPackingNumsCell = row.createCell(4);
						mediumPackingNumsCell.setCellStyle(homePageCellStyleForAIDetail);
						mediumPackingNumsCell.setCellValue(affectedItem.mediumPackingNums);

						Cell unitCell = row.createCell(5);
						unitCell.setCellStyle(homePageCellStyleForAIDetail);
						unitCell.setCellValue(affectedItem.unit);

						Cell materialTypeCell = row.createCell(6);
						materialTypeCell.setCellStyle(homePageCellStyleForAIDetail);
						materialTypeCell.setCellValue(affectedItem.materialType);

						Cell materialGroupCell = row.createCell(7);
						materialGroupCell.setCellStyle(homePageCellStyleForAIDetail);
						materialGroupCell.setCellValue(affectedItem.materialGroup);

						Cell partTypeCell = row.createCell(8);
						partTypeCell.setCellStyle(homePageCellStyleForAIDetail);
						partTypeCell.setCellValue(affectedItem.partType);

						Cell vendorCell = row.createCell(9);
						vendorCell.setCellStyle(homePageCellStyleForAIDetail);
						vendorCell.setCellValue(affectedItem.vendor);

						Cell referenceEOCell = row.createCell(10);
						referenceEOCell.setCellStyle(homePageCellStyleForAIDetail);
						referenceEOCell.setCellValue(affectedItem.referenceEO);

						Cell referencePOCell = row.createCell(11);
						referencePOCell.setCellStyle(homePageCellStyleForAIDetail);
						referencePOCell.setCellValue(affectedItem.referencePO);

						Cell altOptPartCell = row.createCell(12);
						altOptPartCell.setCellStyle(homePageCellStyleForAIDetail);
						altOptPartCell.setCellValue(affectedItem.altOptPart);

						Cell acNoCell = row.createCell(13);
						acNoCell.setCellStyle(homePageCellStyleForAIDetail);
						acNoCell.setCellValue(affectedItem.acNo);

						Cell remarkCell = row.createCell(14);
						remarkCell.setCellStyle(homePageCellStyleForAIDetail);
						remarkCell.setCellValue(affectedItem.remark);
					}
				}

				// BOM
				Sheet BOMSheet = SOWorkBook.getSheet("BOM");
				CellStyle BOMCellStyleForSBOM = cloneCellStyle(BOMSheet, 1, 0);
				BOMCellStyleForSBOM.setWrapText(true);
				int sbomRowLocation = 2;
				if (soPublish.vendorBom != null) {
					ArrayList<PartBom> partBomArrayList = soPublish.vendorBom;
					for (int i = 0; i < partBomArrayList.size(); i++) {
						PartBom partBom = partBomArrayList.get(i);
						Row row = BOMSheet.createRow(i + sbomRowLocation);

						Cell partCell = row.createCell(0);
						partCell.setCellStyle(BOMCellStyleForSBOM);
						partCell.setCellValue(partBom.part);

						Cell subPartCell = row.createCell(1);
						subPartCell.setCellStyle(BOMCellStyleForSBOM);
						subPartCell.setCellValue(partBom.subPart);

						Cell quantityCell = row.createCell(2);
						quantityCell.setCellStyle(BOMCellStyleForSBOM);
						quantityCell.setCellValue(partBom.quantity);

						Cell groupCell = row.createCell(3);
						groupCell.setCellStyle(BOMCellStyleForSBOM);
						groupCell.setCellValue(partBom.group);

						Cell pncCell = row.createCell(4);
						pncCell.setCellStyle(BOMCellStyleForSBOM);
						pncCell.setCellValue(partBom.pnc);

						Cell KDTypeCell = row.createCell(5);
						KDTypeCell.setCellStyle(BOMCellStyleForSBOM);
						KDTypeCell.setCellValue(partBom.KDType);

						Cell vendorCell = row.createCell(6);
						vendorCell.setCellStyle(BOMCellStyleForSBOM);
						vendorCell.setCellValue(partBom.vendor);

						Cell startDateCell = row.createCell(7);
						startDateCell.setCellStyle(BOMCellStyleForSBOM);
						startDateCell.setCellValue(partBom.startDate);

						Cell endDateCell = row.createCell(8);
						endDateCell.setCellStyle(BOMCellStyleForSBOM);
						endDateCell.setCellValue(partBom.endDate);

						Cell bomRemarkCell = row.createCell(9);
						bomRemarkCell.setCellStyle(BOMCellStyleForSBOM);
						bomRemarkCell.setCellValue(partBom.bomRemark);
					}
				}

				/* past ZIP Picture */
				Sheet picSheet_1 = SOWorkBook.getSheet("圖號1");
				Sheet picSheet_2 = SOWorkBook.getSheet("圖號2");

				String soFile = outputSoFileName + "\\" + soName + "_" + vendorName + ".xlsx";
				this.output(SOWorkBook, soFile);
				soFileArrayList.add(soFile);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
		return soFileArrayList;
	}

	private HashMap<String, Object> PBExcel(Context context, DomainObject soObj) throws Exception {
		StringList busSelectsPart = new StringList();
		busSelectsPart.add(DomainConstants.SELECT_ID);
		busSelectsPart.add(DomainConstants.SELECT_NAME);
		busSelectsPart.add("attribute[SPLM_Name_TC]"); // PART_NAME_C
		busSelectsPart.add("attribute[SPLM_Name_EN]"); // PART_NAME_E
		busSelectsPart.add("attribute[SPLM_MediumPackingQuantity]"); // MEDIUNM_PACKINGNUMS
		busSelectsPart.add("attribute[SPLM_Unit]"); // UNIT
		busSelectsPart.add("attribute[SPLM_MaterialType]"); // MATERIAL_TYPE
		busSelectsPart.add("attribute[SPLM_MaterialGroup]"); // MATERIAL_GROUP
		busSelectsPart.add("attribute[SPLM_ItemSubType]"); // PART_TYPE
		busSelectsPart.add("attribute[SPLM_AC_NO]"); // ACNO
		busSelectsPart.add("attribute[SPLM_SO_Note]"); // REMARK

		StringList busSelectAltOpt = new StringList();
		busSelectAltOpt.add(DomainConstants.SELECT_NAME);
		StringList relSelectAltOpt = new StringList();
		relSelectAltOpt.add("attribute[SPLM_OptionalExchangeable]");
		String soName = soObj.getName(context);

		MapList partMapList = getIsServicePartAI(context, soObj, busSelectsPart);
		List<String> partNameList = (List<String>) partMapList.stream()
				.map(obj -> ((Map) obj).get(DomainConstants.SELECT_NAME).toString()).collect(Collectors.toList());

		HashMap<String, Object> map = new HashMap<String, Object>();
		DomainObject partDomObj = new DomainObject();
		String forceBreakLine = "\r\n";
		for (Object obj : partMapList) {
			// AI
			Map partMap = (Map) obj;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partNameCh = (String) partMap.get("attribute[SPLM_Name_TC]");
			String partNameEn = (String) partMap.get("attribute[SPLM_Name_EN]");
			String partMediumPackingNums = (String) partMap.get("attribute[SPLM_MediumPackingQuantity]");
			String partUnit = (String) partMap.get("attribute[SPLM_Unit]");
			String partMaterialType = (String) partMap.get("attribute[SPLM_MaterialType]");
			String partMaterialGroup = (String) partMap.get("attribute[SPLM_MaterialGroup]");
			String partItemSubType = ((String) partMap.get("attribute[SPLM_ItemSubType]"));
			String partACNO = (String) partMap.get("attribute[SPLM_AC_NO]");
			String partRemark = (String) partMap.get("attribute[SPLM_SO_Note]");
			partDomObj.setId(partId);
			String relatedEO = partDomObj.getInfo(context, "from[SPLM_SBOM].attribute[SPLM_RelatedEO]");
			String relatedPO = partDomObj.getInfo(context, "from[SPLM_SBOM].attribute[SPLM_RelatedPO]");
			String partParent = "";
			String altOptPart = "";

			// partParent data processing
			MapList partParentMapList = partDomObj.getRelatedObjects(context, "SPLM_SBOM", // relationshipPattern,
					"SPLM_Part,SPLM_ColorPart", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					null, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			for (Object partParentObj : partParentMapList) {
				String part = (String) ((Map) partParentObj).get(DomainConstants.SELECT_NAME);
				if (partNameList.contains(part)) {
					partParent = part;
				}
			}

			// vendor data processing
			MapList vendorMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedSOReleaseItem", // relationshipPattern,
					"SPLM_Vendor", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_SO]=='" + soName + "'", // String relationshipWhere
					0); // int limit)

			// groupName data processing
			StringList groupCodeList = partDomObj.getInfoList(context,
					"to[SPLM_RelatedPart].from[SPLM_GroupDrawing].name");
			groupCodeList = groupCodeList.size() > 0 ? groupCodeList : new StringList("");
			String groupCode = groupCodeList.size() > 1 ? String.join("/n", groupCodeList) : "";

			// partType data processing
			if (partItemSubType.length() > 2) {
				partItemSubType = partItemSubType.substring(0, 1);
			}

			// altPart/optPart data processing
			MapList optAltMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedOptionalPart", // relationshipPattern,
					"SPLM_Part,SPLM_ColorPart", // typePattern,
					new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
					new StringList("attribute[SPLM_OptionalExchangeable]"), // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			ArrayList<String> optAltArray = new ArrayList<String>();
			for (Object optAltObj : optAltMapList) {
				Map optAltMap = (Map) optAltObj;
				String optAltName = (String) optAltMap.get(DomainConstants.SELECT_NAME);
				String optAltExchageable = (String) optAltMap.get("attribute[SPLM_OptionalExchangeable]");
				String optAltRel = optAltExchageable.isEmpty() ? optAltName
						: optAltName + "(" + optAltExchageable + ")";
				optAltArray.add(optAltRel);
			}

			altOptPart = optAltArray.size() > 1 ? String.join(forceBreakLine, optAltArray) : altOptPart;

			// BOM
			StringList busSelectSBom = new StringList();
			busSelectSBom.add(DomainConstants.SELECT_NAME);
			StringList relSelectSBom = new StringList();
			relSelectSBom.add("attribute[Quantity]");
			relSelectSBom.add("attribute[SPLM_GroupCode]");
			relSelectSBom.add("attribute[SPLM_PNC]"); // pnc
			relSelectSBom.add("attribute[SPLM_Materiel_KD_Type]"); // K-/D-
			relSelectSBom.add("attribute[SPLM_EnableDate]"); // Start Date
			relSelectSBom.add("attribute[SPLM_DisableDate]"); // End Date
			relSelectSBom.add("attribute[SPLM_BOM_Note]"); // Bom remark

			MapList subPartMapList = partDomObj.getRelatedObjects(context, "SPLM_SBOM", // relationshipPattern,
					"SPLM_Part,SPLM_ColorPart", // typePattern,
					busSelectSBom, // StringList objectSelects,
					relSelectSBom, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			SoPublish so;
			ArrayList<AffectedItem> affectedItemArrayList;
			ArrayList<PartBom> partBomArrayList;
			for (Object vendorObj : vendorMapList) {
				String vendorName = (String) ((Map) vendorObj).get(DomainConstants.SELECT_NAME);

				if (map.containsKey(vendorName)) {
					so = (SoPublish) map.get(vendorName);
				} else {
					so = new SoPublish();
					map.put(vendorName, so);
				}

				if (so.vendorAI != null) {
					affectedItemArrayList = so.vendorAI;
				} else {
					affectedItemArrayList = new ArrayList<AffectedItem>();
					so.vendorAI = affectedItemArrayList;
				}

				if (so.vendorBom != null) {
					partBomArrayList = so.vendorBom;
				} else {
					partBomArrayList = new ArrayList<PartBom>();
					so.vendorBom = partBomArrayList;
				}

				AffectedItem affectedItem = new AffectedItem();
				affectedItem.part = partParent;
				affectedItem.affectedItemPart = partName;
				affectedItem.partName = partNameCh + forceBreakLine + partNameEn;
				affectedItem.groupDrawing = groupCode;
				affectedItem.mediumPackingNums = partMediumPackingNums;
				affectedItem.unit = partUnit;
				affectedItem.materialType = partMaterialType;
				affectedItem.materialGroup = partMaterialGroup;
				affectedItem.partType = partItemSubType;
				affectedItem.vendor = vendorName;
				affectedItem.referenceEO = relatedEO;
				affectedItem.referencePO = relatedPO;
				affectedItem.altOptPart = altOptPart;
				affectedItem.acNo = partACNO;
				affectedItem.remark = partRemark;
				affectedItemArrayList.add(affectedItem);

				for (Object subPartObj : subPartMapList) {
					Map subPartMap = (Map) subPartObj;

					String subPartStartDate = (String) subPartMap.get("attribute[SPLM_EnableDate]");
					String subPartEndDate = (String) subPartMap.get("attribute[SPLM_DisableDate]");
					String subPartKDType = (String) subPartMap.get("attribute[SPLM_Materiel_KD_Type]");

					// Date format change
					subPartStartDate = subPartStartDate.isEmpty() ? ""
							: convertDateFormat(subPartStartDate, "yyyyMMdd");
					subPartEndDate = subPartEndDate.isEmpty() ? "" : convertDateFormat(subPartEndDate, "yyyyMMdd");

					// subPartKDType data processing
					subPartKDType = subPartKDType.equals("*-") ? "" : subPartKDType;

					PartBom partBom = new PartBom();
					partBom.part = partName;
					partBom.subPart = (String) subPartMap.get(DomainConstants.SELECT_NAME);
					partBom.quantity = (String) subPartMap.get("attribute[Quantity]");
					partBom.group = (String) subPartMap.get("attribute[SPLM_GroupCode]");
					partBom.pnc = (String) subPartMap.get("attribute[SPLM_PNC]");
					partBom.KDType = subPartKDType;
					partBom.vendor = subPartKDType.length() > 0 ? "" : vendorName;
					partBom.startDate = subPartStartDate;
					partBom.endDate = subPartEndDate;
					partBom.bomRemark = (String) subPartMap.get("attribute[SPLM_BOM_Note]");

					partBomArrayList.add(partBom);
				}
			}
		}
		return map;
	}

	private String createPBSOExcel123(HashMap<String, Object> soExcelInfo) throws Exception {
		Map<String, String> soTitleMap = (Map<String, String>) soExcelInfo.get("soTitle");
		soExcelInfo.remove("soTitle");
		String soName = soTitleMap.get("soName");
		String tempFolder = getTempFolderPath() + File.separator + "createSOExcel" + File.separator;
		String outputSoFileName = tempFolder + soName + File.separator;
		String outputZipFile = tempFolder + soName + ".zip";

		for (String vendorName : soExcelInfo.keySet()) {
			FileInputStream fis = new FileInputStream(INPORT_SO_EXCEL_LOCATION);
			XSSFWorkbook SOWorkBook = (XSSFWorkbook) WorkbookFactory.create(fis);
			fis.close();

			SoPublish soPublish = (SoPublish) soExcelInfo.get(vendorName);

			// Home Page
			Sheet homePageSheet = SOWorkBook.getSheet("封面及AI");

			/* homePage Sending title */
			CellStyle homePageCellStyleForDelivery = cloneCellStyle(homePageSheet, 3, 0);
			homePageCellStyleForDelivery.setWrapText(true);
			Row row4 = homePageSheet.getRow(4);

			Cell soNumberCell = row4.createCell(0);
			soNumberCell.setCellStyle(homePageCellStyleForDelivery);
			soNumberCell.setCellValue(soName);

			Cell soIssuerCell = row4.createCell(1);
			soIssuerCell.setCellStyle(homePageCellStyleForDelivery);
			soIssuerCell.setCellValue(soTitleMap.get("soIssuer"));

			Cell soOwnerCell = row4.createCell(2);
			soOwnerCell.setCellStyle(homePageCellStyleForDelivery);
			soOwnerCell.setCellValue(soTitleMap.get("soOwner"));

			Cell soVaultCell = row4.createCell(3);
			soVaultCell.setCellStyle(homePageCellStyleForDelivery);
			soVaultCell.setCellValue(soTitleMap.get("soVault"));

			Cell soSendDeptCell = row4.createCell(5);
			soSendDeptCell.setCellStyle(homePageCellStyleForDelivery);
			soSendDeptCell.setCellValue(soTitleMap.get("soSendDept"));

			Cell soReleaseDateCell = row4.createCell(13);
			soReleaseDateCell.setCellStyle(homePageCellStyleForDelivery);
			soReleaseDateCell.setCellValue(soTitleMap.get("soReleaseDate"));

			/* AI */
			CellStyle homePageCellStyleForAIDetail = cloneCellStyle(homePageSheet, 10, 0);
			homePageCellStyleForAIDetail.setWrapText(true);
			int aiLocationRow = 11;
			if (soPublish.vendorAI != null) {
				ArrayList<AffectedItem> affectedItemArrayList = soPublish.vendorAI;
				for (int i = 0; i < affectedItemArrayList.size(); i++) {
					Row row = homePageSheet.createRow(i + aiLocationRow);
					AffectedItem affectedItem = affectedItemArrayList.get(i);

					Cell partCell = row.createCell(0);
					partCell.setCellStyle(homePageCellStyleForAIDetail);
					partCell.setCellValue(affectedItem.part);

					Cell affectedItemPartCell = row.createCell(1);
					affectedItemPartCell.setCellStyle(homePageCellStyleForAIDetail);
					affectedItemPartCell.setCellValue(affectedItem.affectedItemPart);

					Cell partNameCell = row.createCell(2);
					partNameCell.setCellStyle(homePageCellStyleForAIDetail);
					partNameCell.setCellValue(affectedItem.partName);

					Cell groupDrawingCell = row.createCell(3);
					groupDrawingCell.setCellStyle(homePageCellStyleForAIDetail);
					groupDrawingCell.setCellValue(affectedItem.groupDrawing);

					Cell mediumPackingNumsCell = row.createCell(4);
					mediumPackingNumsCell.setCellStyle(homePageCellStyleForAIDetail);
					mediumPackingNumsCell.setCellValue(affectedItem.mediumPackingNums);

					Cell unitCell = row.createCell(5);
					unitCell.setCellStyle(homePageCellStyleForAIDetail);
					unitCell.setCellValue(affectedItem.unit);

					Cell materialTypeCell = row.createCell(6);
					materialTypeCell.setCellStyle(homePageCellStyleForAIDetail);
					materialTypeCell.setCellValue(affectedItem.materialType);

					Cell materialGroupCell = row.createCell(7);
					materialGroupCell.setCellStyle(homePageCellStyleForAIDetail);
					materialGroupCell.setCellValue(affectedItem.materialGroup);

					Cell partTypeCell = row.createCell(8);
					partTypeCell.setCellStyle(homePageCellStyleForAIDetail);
					partTypeCell.setCellValue(affectedItem.partType);

					Cell vendorCell = row.createCell(9);
					vendorCell.setCellStyle(homePageCellStyleForAIDetail);
					vendorCell.setCellValue(affectedItem.vendor);

					Cell referenceEOCell = row.createCell(10);
					referenceEOCell.setCellStyle(homePageCellStyleForAIDetail);
					referenceEOCell.setCellValue(affectedItem.referenceEO);

					Cell referencePOCell = row.createCell(11);
					referencePOCell.setCellStyle(homePageCellStyleForAIDetail);
					referencePOCell.setCellValue(affectedItem.referencePO);

					Cell altOptPartCell = row.createCell(12);
					altOptPartCell.setCellStyle(homePageCellStyleForAIDetail);
					altOptPartCell.setCellValue(affectedItem.altOptPart);

					Cell acNoCell = row.createCell(13);
					acNoCell.setCellStyle(homePageCellStyleForAIDetail);
					acNoCell.setCellValue(affectedItem.acNo);

					Cell remarkCell = row.createCell(14);
					remarkCell.setCellStyle(homePageCellStyleForAIDetail);
					remarkCell.setCellValue(affectedItem.remark);
				}
			}

			// BOM
			Sheet BOMSheet = SOWorkBook.getSheet("BOM");
			CellStyle BOMCellStyleForSBOM = cloneCellStyle(BOMSheet, 1, 0);
			BOMCellStyleForSBOM.setWrapText(true);
			int sbomRowLocation = 2;
			if (soPublish.vendorBom != null) {
				ArrayList<PartBom> partBomArrayList = soPublish.vendorBom;
				for (int i = 0; i < partBomArrayList.size(); i++) {
					PartBom partBom = partBomArrayList.get(i);
					Row row = BOMSheet.createRow(i + sbomRowLocation);

					Cell partCell = row.createCell(0);
					partCell.setCellStyle(BOMCellStyleForSBOM);
					partCell.setCellValue(partBom.part);

					Cell subPartCell = row.createCell(1);
					subPartCell.setCellStyle(BOMCellStyleForSBOM);
					subPartCell.setCellValue(partBom.subPart);

					Cell quantityCell = row.createCell(2);
					quantityCell.setCellStyle(BOMCellStyleForSBOM);
					quantityCell.setCellValue(partBom.quantity);

					Cell groupCell = row.createCell(3);
					groupCell.setCellStyle(BOMCellStyleForSBOM);
					groupCell.setCellValue(partBom.group);

					Cell pncCell = row.createCell(4);
					pncCell.setCellStyle(BOMCellStyleForSBOM);
					pncCell.setCellValue(partBom.pnc);

					Cell KDTypeCell = row.createCell(5);
					KDTypeCell.setCellStyle(BOMCellStyleForSBOM);
					KDTypeCell.setCellValue(partBom.KDType);

					Cell vendorCell = row.createCell(6);
					vendorCell.setCellStyle(BOMCellStyleForSBOM);
					vendorCell.setCellValue(partBom.vendor);

					Cell startDateCell = row.createCell(7);
					startDateCell.setCellStyle(BOMCellStyleForSBOM);
					startDateCell.setCellValue(partBom.startDate);

					Cell endDateCell = row.createCell(8);
					endDateCell.setCellStyle(BOMCellStyleForSBOM);
					endDateCell.setCellValue(partBom.endDate);

					Cell bomRemarkCell = row.createCell(9);
					bomRemarkCell.setCellStyle(BOMCellStyleForSBOM);
					bomRemarkCell.setCellValue(partBom.bomRemark);
				}
			}

			/* past ZIP Picture */
			Sheet picSheet_1 = SOWorkBook.getSheet("圖號1");
			Sheet picSheet_2 = SOWorkBook.getSheet("圖號2");

			String soFile = outputSoFileName + soName + "_" + vendorName + ".xlsx";
			this.output(SOWorkBook, soFile);
		}
		CMC_Method_JPO_mxJPO.makeZip(new File(outputSoFileName), new File(outputZipFile));
		this.deleteDirectoryExistFiles(outputSoFileName);
		return outputZipFile;
	}

//	private  getSOPBTitleInfo(Context context, DomainObject soObj){
//		String soName = soObj.getName(context);
//		String soVault = soObj.getVault(context);
//		String soOwner = soObj.getOwner(context).toString();
//		String soSendDept = soObj.getAttributeValue(context, "attribute[SPLM_SendDept]").replace("~QWE~", ",");
//		String soReleaseDate = soObj.getCurrentState(context).getActualDate();
//		String soIssuer = "";	
//	}
	
	class SoPublish {
		private String vendorName;
		private ArrayList<AffectedItem> vendorAI;
		private ArrayList<PartBom> vendorBom;
		private List<String> vendorDocument;
	}

	class AffectedItem {
		private String part;
		private String affectedItemPart;
		private String partName;
		private String groupDrawing;
		private String mediumPackingNums;
		private String unit;
		private String materialType;
		private String materialGroup;
		private String partType;
		private String vendor;
		private String referenceEO;
		private String referencePO;
		private String altOptPart;
		private String acNo;
		private String remark;
	}

	class PartBom {
		private String part;
		private String subPart;
		private String quantity;
		private String group;
		private String pnc;
		private String KDType;
		private String vendor;
		private String startDate;
		private String endDate;
		private String bomRemark;
	}

//	class SOPBTitle{
//		private String titleName;
//		private String soVault;
//		private String soOwner;
//		private String soOwner;
//		private String soReleaseDate;
//		private String soIssuer;
//		
//		String soName = soObj.getName(context);
//		String soVault = soObj.getVault(context);
//		String soOwner = soObj.getOwner(context).toString();
//		String soSendDept = soObj.getAttributeValue(context, "attribute[SPLM_SendDept]").replace("~QWE~", ",");
//		String soReleaseDate = soObj.getCurrentState(context).getActualDate();
//		String soIssuer = "";	
//
//	}
}
