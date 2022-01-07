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
import java.util.Optional;
import java.util.UUID;
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
import com.matrixone.apps.common.Person;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.i18nNow;

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
	private static final String INPORT_SO_EXCEL_LOCATION = "C:\\DassaultSystemesBatchFile\\SOExcelTemplate\\SO - 20211230.xlsx";
	private static final String INPORT_PB_EXCEL_LOCATION = "C:\\DassaultSystemesBatchFile\\SOExcelTemplate\\PB - 20211211.xlsx";

	private static final String TEMP_FOLDER = "PLMTemp\\createSOPBExcel\\";
	private static final String TEMP_FOLDER_DEVICE = "D:\\";
	private static final String VAULT = "eService Production";

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

	private boolean isPictureFile(String fileLocation) throws Exception {
		byte[] stream = Files.readAllBytes(Paths.get(fileLocation));
		String picHexString = bytesToHexString(stream, 6).replaceAll(" ", "");
		// jpg File | png File | gif File HexString data, avoid other File input
		if (picHexString.startsWith("FFD8FF") || picHexString.startsWith("89504E47")
				|| picHexString.startsWith("47494638")) {
			return true;
		}
		// tiff File HexString data, avoid other File input
		if (picHexString.startsWith("492049") || picHexString.startsWith("49492A00")
				|| picHexString.startsWith("4D4D002A") || picHexString.startsWith("4D4D002B")) {
			return true;
		}
		return false;
	}

	private void checkUnZipFilesIfnotDeleteAllpic(List<String> unzipFileList, String fileTempFolder) throws Exception {
		for (String unZipFile : unzipFileList) {
			if (!isPictureFile(unZipFile)) {
				deleteDirectoryExistFiles(new File(fileTempFolder).toString());
				throw new Exception("\u5716\u6a94\u53ea\u63a5\u53d7 jpeg / png / gif / tiff \u683c\u5f0f");
			}
		}
	}

	/* ---Dassault API package simple using--- */
	private MapList getIsServicePartAI(Context context, DomainObject soObj, StringList busSelect) throws Exception {
		return getSOAI(context, soObj, busSelect, "attribute[SPLM_IsServicePart]=='True'");
	}

	private MapList getHaveSubPartAI(Context context, DomainObject soObj, StringList busSelect) throws Exception {
		return getSOAI(context, soObj, busSelect, "attribute[SPLM_HaveSubPart]=='True'");
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

	/////////////////////////////////////////////////////////////////////// task
	public void searchSOPBExcel(Context context, String[] args) throws Exception {
		StringList busSelectSo = new StringList();
		busSelectSo.add(DomainConstants.SELECT_ID);
		busSelectSo.add(DomainConstants.SELECT_NAME);

		MapList soMapList = DomainObject.findObjects(context, "SPLM_SO", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"current=='Complete'&& attribute[SPLM_BatchFlag]=='Processing'", // where
				null, false, busSelectSo, (short) 0);

		DomainObject soDomObj = new DomainObject();
		for (Object soObj : soMapList) {
			Map soMap = (Map) soObj;
			String soId = (String) soMap.get(DomainConstants.SELECT_ID);
			String soName = (String) soMap.get(DomainConstants.SELECT_ID);
			soDomObj.setId(soId);

			SOPBTitle soPBTitle = getSOPBExcelTitleInfo(context, soDomObj);

			// prepare Files for restore
			String outputSoFileName = getTempFolderPath() + soPBTitle.titleName;
			String fileTempFolder = outputSoFileName + "\\tempPic";

			File picTempFile = new File(fileTempFolder);
			if (!picTempFile.exists()) {
				picTempFile.mkdirs();
			}

			System.out.println(soName + " \u6b63\u5728\u57f7\u884c\u4e2d, \u767c\u9001SO:" + soPBTitle.releaseSO
					+ " ,\u767c\u9001PB:" + soPBTitle.releasePB);

			ArrayList<String> soFileArrayList = null;
			ArrayList<String> pbFileArrayList = null;
			// Data processing
			if (soPBTitle.releaseSO) {
				ArrayList<SOPublish> soPublishArrayList = getAffectedItemInfo(context, soDomObj, fileTempFolder);
				soFileArrayList = createSOExcel(context, soPublishArrayList, soPBTitle, outputSoFileName);
				if (soFileArrayList.size() == 0) {
					throw new Exception("\u6c92\u6709\u6a94\u6848\u53ef\u4ee5\u4e0b\u8f09");
				}
			}

			if (soPBTitle.releasePB) {
				ArrayList<PBPublish> pbPublishArrayList = getPartBulletinInfo(context, soDomObj, fileTempFolder);
				pbFileArrayList = createPBExcel(context, pbPublishArrayList, soPBTitle, outputSoFileName);
				if (pbFileArrayList.size() == 0) {
					throw new Exception("\u6c92\u6709\u6a94\u6848\u53ef\u4ee5\u4e0b\u8f09");
				}
			}

			System.out.println(soName + " \u5df2\u57f7\u884c\u5b8c\u7562");
			soDomObj.setAttributeValue(context, "SPLM_BatchFlag", "Complete");

			if (Optional.ofNullable(soFileArrayList).isPresent()) {

			}

			if (Optional.ofNullable(pbFileArrayList).isPresent()) {

			}

			this.deleteDirectoryExistFiles(fileTempFolder);
			this.deleteDirectoryExistFiles(outputSoFileName);
		}
	}

	/////////////////////////////////////////////////////////////////////// web
	public HashMap<String, String> publishSOPBExcel(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String) programMap.get("objectId");
		DomainObject soObj = new DomainObject(objectId);

		HashMap<String, String> returnMap = new HashMap<String, String>();

		try {
			SOPBTitle soPBTitle = getSOPBExcelTitleInfo(context, soObj);
			if (!soPBTitle.releaseSO && !soPBTitle.releasePB) {
				throw new Exception("\u8acb\u52fe\u9078 \u767c\u4f48SO / \u767c\u4f48PB");
			}

			// prepare Files for restore
			String outputSoFileName = getTempFolderPath() + soPBTitle.titleName;
			String outputSoFileZip = outputSoFileName + ".zip";
			String fileTempFolder = outputSoFileName + "\\tempPic";

			File picTempFile = new File(fileTempFolder);
			if (!picTempFile.exists()) {
				picTempFile.mkdirs();
			}

			// Data processing
			if (soPBTitle.releaseSO) {
				ArrayList<SOPublish> soPublishArrayList = getAffectedItemInfo(context, soObj, fileTempFolder);
				ArrayList<String> soFileArrayList = createSOExcel(context, soPublishArrayList, soPBTitle,
						outputSoFileName);
				if (soFileArrayList.size() == 0) {
					throw new Exception("\u6c92\u6709\u6a94\u6848\u53ef\u4ee5\u4e0b\u8f09");
				}
			}

			if (soPBTitle.releasePB) {
				ArrayList<PBPublish> pbPublishArrayList = getPartBulletinInfo(context, soObj, fileTempFolder);
				ArrayList<String> pbFileArrayList = createPBExcel(context, pbPublishArrayList, soPBTitle,
						outputSoFileName);
				if (pbFileArrayList.size() == 0) {
					throw new Exception("\u6c92\u6709\u6a94\u6848\u53ef\u4ee5\u4e0b\u8f09");
				}
			}

			this.deleteDirectoryExistFiles(fileTempFolder);
			CMC_Method_JPO_mxJPO.makeZip(new File(outputSoFileName), new File(outputSoFileZip));
			this.deleteDirectoryExistFiles(outputSoFileName);
			returnMap.put("FilePath", outputSoFileZip);
		} catch (Exception e) {
			returnMap.put("Message", e.getMessage());
		}

		return returnMap;
	}

	/* assemble Info and publish Excel*/
	private ArrayList<SOPublish> getAffectedItemInfo(Context context, DomainObject soObj, String fileTempFolder)
			throws Exception {

		StringList busSelectsPart = new StringList();
		busSelectsPart.add(DomainConstants.SELECT_ID);
		busSelectsPart.add(DomainConstants.SELECT_NAME);
		busSelectsPart.add("attribute[SPLM_Name_TC]"); // PART_NAME_C
		busSelectsPart.add("attribute[SPLM_Name_EN]"); // PART_NAME_E
		busSelectsPart.add("attribute[SPLM_MediumPackingQuantity]"); // MEDIUNM_PACKINGNUMS
		busSelectsPart.add("attribute[SPLM_Unit]"); // UNIT
		busSelectsPart.add("attribute[SPLM_MaterialType]"); // MATERIAL_TYPE
		busSelectsPart.add("attribute[SPLM_MaterialGroup]"); // MATERIAL_GROUP
		busSelectsPart.add("attribute[SPLM_PurchasingGroup]"); // PURCHASING_GROUP
		busSelectsPart.add("attribute[SPLM_ItemSubType]"); // PART_TYPE
		busSelectsPart.add("attribute[SPLM_AC_NO]"); // ACNO
		busSelectsPart.add("attribute[SPLM_CP_NO]"); // ACNO
		busSelectsPart.add("attribute[SPLM_SO_Note]"); // REMARK

		StringList busSelectAltOpt = new StringList();
		busSelectAltOpt.add(DomainConstants.SELECT_NAME);
		StringList relSelectAltOpt = new StringList();
		relSelectAltOpt.add("attribute[SPLM_OptionalExchangeable]");
		relSelectAltOpt.add("attribute[SPLM_OptionalType]");

		StringList parentBusSelects = new StringList();
		parentBusSelects.add(DomainConstants.SELECT_ID);
		parentBusSelects.add(DomainConstants.SELECT_NAME);
		StringList parentRelSelects = new StringList();
		parentRelSelects.add("attribute[SPLM_RelatedEO]");
		parentRelSelects.add("attribute[SPLM_RelatedPO]");

		StringList busSelectSBom = new StringList();
		busSelectSBom.add(DomainConstants.SELECT_NAME);
		busSelectSBom.add(DomainConstants.SELECT_ID);
		StringList relSelectSBom = new StringList();
		relSelectSBom.add("attribute[Quantity]");
		relSelectSBom.add("attribute[SPLM_GroupCode]");
		relSelectSBom.add("attribute[SPLM_PNC]"); // pnc
		relSelectSBom.add("attribute[SPLM_Materiel_KD_Type]"); // K-/D-
		relSelectSBom.add("attribute[SPLM_EnableDate]"); // Start Date
		relSelectSBom.add("attribute[SPLM_DisableDate]"); // End Date
		relSelectSBom.add("attribute[SPLM_BOM_Note]"); // Bom remark

		StringList busSelectSoDocumentVendor = new StringList();
		busSelectSoDocumentVendor.add(DomainConstants.SELECT_NAME);
		busSelectSoDocumentVendor.add(DomainConstants.SELECT_ID);
		busSelectSoDocumentVendor.add("attribute[SPLM_DocType]");
		
		StringList busSelectAllVendor = new StringList();
		busSelectAllVendor.add(DomainConstants.SELECT_NAME);
		StringList relSelectAllVendor = new StringList();
		relSelectAllVendor.add("attribute[SPLM_VendorPartNo]");
		relSelectAllVendor.add("attribute[SPLM_Valid]");

		String soName = soObj.getInfo(context, DomainConstants.SELECT_NAME);
		String forceBreakLine = "\r\n";

		MapList servicesPartMapList = getIsServicePartAI(context, soObj, busSelectsPart);
		if (servicesPartMapList.isEmpty()) {
			throw new Exception("\u7f3a\u5c11 \u5f71\u97ff\u4ef6\u865f");
		}

		StringList soAffectedParentPartIdList = soObj.getInfoList(context, "from[SPLM_ReferenceParentPart].to.id");

		ArrayList<SOPublish> soPublishArrayList = new ArrayList<SOPublish>();
		DomainObject partDomObj = new DomainObject();

		for (Object partInfo : servicesPartMapList) {
			// AI
			Map partMap = (Map) partInfo;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partNameCh = (String) partMap.get("attribute[SPLM_Name_TC]");
			String partNameEn = (String) partMap.get("attribute[SPLM_Name_EN]");
			String partMediumPackingNums = "1";
			String partUnit = (String) partMap.get("attribute[SPLM_Unit]");
			String partMaterialType = (String) partMap.get("attribute[SPLM_MaterialType]");
			String partMaterialGroup = (String) partMap.get("attribute[SPLM_MaterialGroup]");
			String partItemSubType = ((String) partMap.get("attribute[SPLM_ItemSubType]"));
			String partPuchasingGroup = ((String) partMap.get("attribute[SPLM_PurchasingGroup]"));
			String colorPartACNO = (String) partMap.get("attribute[SPLM_AC_NO]");
			String colorPartCPNO = (String) partMap.get("attribute[SPLM_CP_NO]");
			String partRemark = (String) partMap.get("attribute[SPLM_SO_Note]");
			partDomObj.setId(partId);
			String relatedEO = "";
			String relatedPO = "";
			String parentPart = "";
			String altOptPart = "";

			// partParent data processing
			MapList parentPartMapList = partDomObj.getRelatedObjects(context, "SPLM_SBOM", // relationshipPattern,
					"*", // typePattern,
					parentBusSelects, // StringList objectSelects,
					parentRelSelects, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			parentPart = (String) parentPartMapList.stream().filter(
					obj -> soAffectedParentPartIdList.contains((String) ((Map) obj).get(DomainConstants.SELECT_ID)))
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME))
					.collect(Collectors.joining(forceBreakLine));
			
			if(!parentPart.isEmpty()) {
				relatedEO = (String) parentPartMapList.stream().filter(
						obj -> soAffectedParentPartIdList.contains((String) ((Map) obj).get(DomainConstants.SELECT_ID)))
						.map(obj -> (String) ((Map) obj).get("attribute[SPLM_RelatedEO]"))
						.collect(Collectors.joining(forceBreakLine));
				
				relatedPO = (String) parentPartMapList.stream().filter(
						obj -> soAffectedParentPartIdList.contains((String) ((Map) obj).get(DomainConstants.SELECT_ID)))
						.map(obj -> (String) ((Map) obj).get("attribute[SPLM_RelatedPO]"))
						.collect(Collectors.joining(forceBreakLine));
			}

			// vendor/document data processing
			MapList vendorMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedSOReleaseItem", // relationshipPattern,
					"*", // typePattern,
					busSelectSoDocumentVendor, // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_SO]=='" + soName + "' &&  attribute[SPLM_SOReleaseItemType]=='Vendor'", // String
																											// relationshipWhere
					0); // int limit)

			List<String> vendorList = (List<String>) vendorMapList.stream().filter(
					obj -> ((String) ((Map) obj).get(DomainConstants.SELECT_TYPE)).equalsIgnoreCase("SPLM_Vendor"))
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME)).collect(Collectors.toList());

			if (vendorList.isEmpty()) {
				throw new Exception("\u552e\u670d\u96f6\u4ef6 " + partName + " \u586b\u9078 \u767c\u9001\u5ee0\u5546");
			}

			// documnetId Map collection -> structure <documentName , documentId> , filter by attribute[SPLM_DocType]=='Part Drawing'
			Map<String, String> soDocumentIdList = (Map<String, String>) vendorMapList.stream()
					.filter(obj -> ((String) ((Map) obj).get("attribute[SPLM_DocType]")).equalsIgnoreCase("Part Drawing"))
					.collect(Collectors.toMap(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME),
							obj -> (String) ((Map) obj).get(DomainConstants.SELECT_ID)));

			// groupName data processing
			String groupName = (String) vendorMapList.stream()
					.filter(obj -> ((String) ((Map) obj).get("attribute[SPLM_DocType]")).equalsIgnoreCase("Part Drawing"))
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME))
					.collect(Collectors.joining(forceBreakLine));

			// partType data processing
			if (!partItemSubType.isEmpty()) {
				partItemSubType = i18nNow.getRangeI18NString("SPLM_ItemSubType", partItemSubType, "zh");
			}

			// altPart/optPart data processing
			MapList optAltMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedOptionalPart", // relationshipPattern,
					"SPLM_Part,SPLM_ColorPart", // typePattern,
					busSelectAltOpt, // StringList objectSelects,
					relSelectAltOpt, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			ArrayList<String> optAltArrayList = new ArrayList<String>();
			for (Object optAltObj : optAltMapList) {
				Map optAltMap = (Map) optAltObj;
				String optAltName = (String) optAltMap.get(DomainConstants.SELECT_NAME);
				String optAltOptionType = (String) optAltMap.get("attribute[SPLM_OptionalType]");
				String optAltExchageable = (String) optAltMap.get("attribute[SPLM_OptionalExchangeable]");
				String optAltRel = optAltExchageable.isEmpty() ? optAltName + "(" + optAltOptionType + ")"
						: optAltName + "(" + optAltExchageable + ")";
				optAltArrayList.add(optAltRel);
			}
			altOptPart = String.join(forceBreakLine, optAltArrayList);

			// colorPart ACNO/CPNO
			String partACNO = colorPartACNO.isEmpty() ? "" : (colorPartACNO + "_" + forceBreakLine + colorPartCPNO);

			/* --- BOM Part --- */
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
			MapList allVenderMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedVendor", // relationshipPattern,
					"SPLM_Vendor", // typePattern,
					busSelectAllVendor, // StringList objectSelects,
					relSelectAllVendor, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)
			
			Map<String, String> vendorPartNoMap = (Map<String, String>) allVenderMapList.stream()
//					.map(obj -> (String) ((Map) obj).get("attribute[SPLM_VendorPartNo]"))
//					.collect(Collectors.joining(forceBreakLine));
					.collect(Collectors.toMap(obj-> (String) ((Map) obj).get(DomainConstants.SELECT_NAME), obj-> (String) ((Map) obj).get("attribute[SPLM_VendorPartNo]")));
			
			ArrayList<PartBom> partBomArrayList = new ArrayList<PartBom>();
			for (Object subPartObj : subPartMapList) {
				Map subPartMap = (Map) subPartObj;

				String subPartId = (String) subPartMap.get(DomainConstants.SELECT_ID);
				String subPartStartDate = (String) subPartMap.get("attribute[SPLM_EnableDate]");
				String subPartEndDate = (String) subPartMap.get("attribute[SPLM_DisableDate]");
				String subPartKDType = (String) subPartMap.get("attribute[SPLM_Materiel_KD_Type]");
				String subPartVendor = "";

				// Date format change
				subPartStartDate = subPartStartDate.isEmpty() ? "" : convertDateFormat(subPartStartDate, "yyyyMMdd");
				subPartEndDate = subPartEndDate.isEmpty() ? "" : convertDateFormat(subPartEndDate, "yyyyMMdd");

				if(subPartKDType.equalsIgnoreCase("*-")) {
					DomainObject subPartDomObj = new DomainObject(subPartId);
					MapList validMapList = subPartDomObj.getRelatedObjects(context, "SPLM_RelatedVendor", // relationshipPattern,
							"SPLM_Vendor", // typePattern,
							busSelectAllVendor, // StringList objectSelects,
							relSelectAllVendor, // StringList relationshipSelects,
							false, // boolean getTo,
							true, // boolean getFrom,
							(short) 1, // short recurseToLevel,
							"", // String objectWhere,
							"", // String relationshipWhere
							0); // int limit)
	
					subPartVendor = (String) validMapList.stream()
							.filter(obj -> ((String) ((Map) obj).get("attribute[SPLM_Valid]")).equalsIgnoreCase("*-"))
							.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME))
							.collect(Collectors.joining(forceBreakLine));
				}
				
				PartBom partBom = new PartBom();
				partBom.part = partName;
				partBom.subPart = (String) subPartMap.get(DomainConstants.SELECT_NAME);
				partBom.quantity = (String) subPartMap.get("attribute[Quantity]");
				partBom.groupCode = (String) subPartMap.get("attribute[SPLM_GroupCode]");
				partBom.pnc = (String) subPartMap.get("attribute[SPLM_PNC]");
				partBom.typeKD = subPartKDType.equalsIgnoreCase("*-") ? "" : subPartKDType;
				partBom.vendor = subPartKDType.equalsIgnoreCase("*-") ? subPartVendor : "";
				partBom.startDate = subPartStartDate;
				partBom.endDate = subPartEndDate;
				partBom.bomRemark = (String) subPartMap.get("attribute[SPLM_BOM_Note]");
				partBomArrayList.add(partBom);
			}

			/* --- Pic download --- */
			ContextUtil.pushContext(context);
			MqlUtil.mqlCommand(context, "trigger off");
			DomainObject documentDomObj = new DomainObject();
			ArrayList<Document> documentArrayList = new ArrayList<Document>();
			try {
				for (String documentName : soDocumentIdList.keySet()) {
					String documentId = soDocumentIdList.get(documentName);
					documentDomObj.setId(documentId);
					ArrayList<String> picFileArrayList = new ArrayList<String>();
					for (matrix.db.File documentFile : documentDomObj.getFiles(context)) {
						String fileName = documentFile.getName();
						String fileLocation = fileTempFolder + "\\" + fileName;
						documentDomObj.checkoutFile(context, false, documentFile.getFormat(), fileName, fileTempFolder);
						if (isPictureFile(fileLocation)) {
							picFileArrayList.add(fileLocation);
						}
					}
					Document document = new Document();
					document.picName = documentName;
					document.picFileLocations = picFileArrayList;
					documentArrayList.add(document);
				}
			} catch (Exception e) {
				throw e;
			} finally {
				MqlUtil.mqlCommand(context, "trigger on");
				ContextUtil.popContext(context);
			}
			
			/* --- AffectedItem Part ---*/
			AffectedItem affectedItem = new AffectedItem();
			affectedItem.part = parentPart;
			affectedItem.affectedItemPart = partName;
			affectedItem.partName = partNameCh + forceBreakLine + partNameEn;
			affectedItem.documentFile = groupName;
			affectedItem.mediumPackingNums = partMediumPackingNums;
			affectedItem.unit = partUnit;
			affectedItem.materialType = partMaterialType;
			affectedItem.materialGroup = partMaterialGroup;
			affectedItem.partType = partItemSubType;
			affectedItem.purchasingGroup = partPuchasingGroup;
			affectedItem.referenceEO = relatedEO;
			affectedItem.referencePO = relatedPO;
			affectedItem.altOptPart = altOptPart;
			affectedItem.acNo = partACNO;
			affectedItem.remark = partRemark;

			/* --- assemble All data --- */
			for (String vendorName : vendorList) {
				Optional soOptional = soPublishArrayList.stream()
						.filter(obj -> obj.fileName.equalsIgnoreCase(vendorName)).findFirst();

				SOPublish so = (SOPublish) soOptional.orElse(new SOPublish());
				if (so.isVendorNameEmpty()) {
					so.fileName = vendorName;
					so.vendorAI = new ArrayList<AffectedItem>();
					so.vendorBom = new ArrayList<PartBom>();
					so.vendorDocument = new ArrayList<Document>();
					soPublishArrayList.add(so);
				}
				
				AffectedItem affectedItemClone = (AffectedItem) affectedItem.clone();
				affectedItemClone.vendor = vendorPartNoMap.get(vendorName);
				so.vendorAI.add(affectedItemClone);
				so.vendorBom.addAll(partBomArrayList);
				so.vendorDocument.addAll(documentArrayList);
			}
		}
		return soPublishArrayList;
	}

	private ArrayList<String> createSOExcel(Context context, ArrayList<SOPublish> soPublishArrayList,
			SOPBTitle soPBTitle, String outputSoFileName) throws Exception {
		String soName = soPBTitle.titleName;
		ArrayList<String> soFileArrayList = new ArrayList<String>();

		FileInputStream fis = null;
		try {
			for (SOPublish soPublish : soPublishArrayList) {
				fis = new FileInputStream(INPORT_SO_EXCEL_LOCATION);
				XSSFWorkbook soWorkBook = (XSSFWorkbook) WorkbookFactory.create(fis);
				fis.close();

				// Home Page
				Sheet homePageSheet = soWorkBook.getSheet("\u5c01\u9762\u53caAI");

				/* homePage Sending title */
				CellStyle homePageCellStyleForDelivery = cloneCellStyle(homePageSheet, 3, 0);
				homePageCellStyleForDelivery.setWrapText(true);
				Row row4 = homePageSheet.getRow(4);

				Cell soNumberCell = row4.createCell(0);
				soNumberCell.setCellStyle(homePageCellStyleForDelivery);
				soNumberCell.setCellValue(soPBTitle.titleName);

				Cell soIssuerCell = row4.createCell(1);
				soIssuerCell.setCellStyle(homePageCellStyleForDelivery);
				soIssuerCell.setCellValue(soPBTitle.titleIssuer);

				Cell soVaultCell = row4.createCell(2);
				soVaultCell.setCellStyle(homePageCellStyleForDelivery);
				soVaultCell.setCellValue(soPBTitle.titleVault);

				Cell soOwnerCell = row4.createCell(3);
				soOwnerCell.setCellStyle(homePageCellStyleForDelivery);
				soOwnerCell.setCellValue(soPBTitle.titleOwner);

				Cell soSendDeptCell = row4.createCell(5);
				soSendDeptCell.setCellStyle(homePageCellStyleForDelivery);
				soSendDeptCell.setCellValue(soPBTitle.titleSendDept);

				Cell soReleaseDateCell = row4.createCell(14);
				soReleaseDateCell.setCellStyle(homePageCellStyleForDelivery);
				soReleaseDateCell.setCellValue(soPBTitle.titleReleaseDate);

				/* AI */
				CellStyle homePageCellStyleForAIDetail = cloneCellStyle(homePageSheet, 12, 0);
				homePageCellStyleForAIDetail.setWrapText(true);
				int aiLocationRow = 13;
				if (!soPublish.isVendorAIEmpty()) {
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
						groupDrawingCell.setCellValue(affectedItem.documentFile);

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

						Cell purchasingGroupCell = row.createCell(9);
						purchasingGroupCell.setCellStyle(homePageCellStyleForAIDetail);
						purchasingGroupCell.setCellValue(affectedItem.purchasingGroup);

						Cell vendorCell = row.createCell(10);
						vendorCell.setCellStyle(homePageCellStyleForAIDetail);
						vendorCell.setCellValue(affectedItem.vendor);

						Cell referenceEOCell = row.createCell(11);
						referenceEOCell.setCellStyle(homePageCellStyleForAIDetail);
						referenceEOCell.setCellValue(affectedItem.referenceEO);

						Cell referencePOCell = row.createCell(12);
						referencePOCell.setCellStyle(homePageCellStyleForAIDetail);
						referencePOCell.setCellValue(affectedItem.referencePO);

						Cell altOptPartCell = row.createCell(13);
						altOptPartCell.setCellStyle(homePageCellStyleForAIDetail);
						altOptPartCell.setCellValue(affectedItem.altOptPart);

						Cell acNoCell = row.createCell(14);
						acNoCell.setCellStyle(homePageCellStyleForAIDetail);
						acNoCell.setCellValue(affectedItem.acNo);

						Cell remarkCell = row.createCell(15);
						remarkCell.setCellStyle(homePageCellStyleForAIDetail);
						remarkCell.setCellValue(affectedItem.remark);
					}
				}

				// BOM
				Sheet BOMSheet = soWorkBook.getSheet("BOM");
				CellStyle BOMCellStyleForSBOM = cloneCellStyle(BOMSheet, 1, 0);
				BOMCellStyleForSBOM.setWrapText(true);
				int sbomRowLocation = 2;
				if (!soPublish.isVendorBomEmpty()) {
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
						groupCell.setCellValue(partBom.groupCode);

						Cell pncCell = row.createCell(4);
						pncCell.setCellStyle(BOMCellStyleForSBOM);
						pncCell.setCellValue(partBom.pnc);

						Cell KDTypeCell = row.createCell(5);
						KDTypeCell.setCellStyle(BOMCellStyleForSBOM);
						KDTypeCell.setCellValue(partBom.typeKD);

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

				/* past normal/ZIP Picture */
				if (!soPublish.isVendorDocumentEmpty()) {
					ArrayList<Document> documentArrayList = soPublish.vendorDocument;
					for (Document document : documentArrayList) {
						XSSFClientAnchor preImage = null;
						Sheet picSheet = soWorkBook.createSheet(document.picName);
						XSSFDrawing drawing = (XSSFDrawing) picSheet.createDrawingPatriarch();
						for (String picLocation : document.picFileLocations) {
							preImage = addImage(soWorkBook, drawing, picLocation, preImage);
						}
					}
				}
				
				protectSheets(soWorkBook);
				String soFile = outputSoFileName + "\\" + soName + "_" + soPublish.fileName + ".xlsx";
				this.output(soWorkBook, soFile);
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

	private ArrayList<PBPublish> getPartBulletinInfo(Context context, DomainObject soObj, String fileTempFolder)
			throws Exception {

		StringList busSelectsPart = new StringList();
		busSelectsPart.add(DomainConstants.SELECT_ID);
		busSelectsPart.add(DomainConstants.SELECT_NAME);
		busSelectsPart.add("attribute[SPLM_Name_TC]");
		busSelectsPart.add("attribute[SPLM_Name_EN]");
		busSelectsPart.add("attribute[SPLM_PB_Note]");
		busSelectsPart.add("attribute[SPLM_MaterialGroup]");



//		StringList busSelectSBom = new StringList();
//		busSelectSBom.add(DomainConstants.SELECT_NAME);
//		busSelectSBom.add("attribute[SPLM_Name_TC]");
//		busSelectSBom.add("attribute[SPLM_Name_EN]");
//		busSelectSBom.add("attribute[SPLM_PB_Note]");
//		StringList relSelectSBom = new StringList();
//		relSelectSBom.add("attribute[Quantity]");
//		relSelectSBom.add("attribute[SPLM_GroupCode]");
//		relSelectSBom.add("attribute[SPLM_PNC]"); // pnc
//		relSelectSBom.add("attribute[SPLM_Materiel_KD_Type]"); // K-/D-
//		relSelectSBom.add("attribute[SPLM_EnableDate]"); // Start Date
//		relSelectSBom.add("attribute[SPLM_DisableDate]"); // End Date

		StringList busSelectSoDocumentVendor = new StringList();
		busSelectSoDocumentVendor.add(DomainConstants.SELECT_NAME);
		busSelectSoDocumentVendor.add(DomainConstants.SELECT_ID);
		busSelectSoDocumentVendor.add("attribute[SPLM_DocType]");

		
		String soName = soObj.getInfo(context, DomainConstants.SELECT_NAME);
		String soPartCatalogue = soObj.getAttributeValue(context, "SPLM_PartsCatalogue");
		String forceBreakLine = "\r\n";

		MapList servicesPartMapList = getIsServicePartAI(context, soObj, busSelectsPart);
		if (servicesPartMapList.isEmpty()) {
			throw new Exception("\u7f3a\u5c11 \u5f71\u97ff\u4ef6\u865f");
		}
		
		String adaptMC = "";
		MapList partCatalogueIdMapList = DomainObject.findObjects(context, "SPLM_PartsCatalogue",
				soPartCatalogue, // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"", // where
				null, false, new StringList(DomainConstants.SELECT_ID), (short) 0);
		
		for(Object partCatalogueObj : partCatalogueIdMapList){
			Map partCatalogueMap = (Map) partCatalogueObj;
			String partCatalogueId = (String) ((Map) partCatalogueMap).get(DomainConstants.SELECT_ID);
			DomainObject partCatalogueDomObj = new DomainObject(partCatalogueId);
		
			MapList adaptMCMapList = partCatalogueDomObj.getRelatedObjects(context, "SPLM_RelatedSOReleaseItem", // relationshipPattern,
					"SPLM_GroupDrawing", // typePattern,
					new StringList("attribute[SPLM_ModelCode]"), // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_ModelCode]!=''", // String
					0);
		
		    adaptMC = (String) adaptMCMapList.stream()
		    		.filter(obj -> ((String) ((Map) obj).get("attribute[SPLM_ModelCode]")).isEmpty())
		    		.map(obj -> (String) ((Map) obj).get("attribute[SPLM_ModelCode]"))
		    		.collect(Collectors.joining(forceBreakLine));
		}

		ArrayList<PBPublish> pbPublishArrayList = new ArrayList<PBPublish>();
		DomainObject partDomObj = new DomainObject();

		for (Object servicePartInfo : servicesPartMapList) {
			// PB
			Map partMap = (Map) servicePartInfo;
			String partId = (String) partMap.get(DomainConstants.SELECT_ID);
			String partName = (String) partMap.get(DomainConstants.SELECT_NAME);
			String partNameCh = (String) partMap.get("attribute[SPLM_Name_TC]");
			String partNameEn = (String) partMap.get("attribute[SPLM_Name_EN]");
			String partAttPBNote = (String) partMap.get("attribute[SPLM_PB_Note]");
			String partAttMaterialGroup = (String) partMap.get("attribute[SPLM_MaterialGroup]");
			partDomObj.setId(partId);
			
			// materialGroup data processing
			partAttMaterialGroup = (partAttMaterialGroup.equalsIgnoreCase("KD") || partAttMaterialGroup.equalsIgnoreCase("KDY"))
					? "K"
					: "D";

			// dealer/document data processing
			MapList dealerMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedSOReleaseItem", // relationshipPattern,
					"*", // typePattern,
					busSelectSoDocumentVendor, // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"attribute[SPLM_SO]=='" + soName + "' &&  attribute[SPLM_SOReleaseItemType]=='Dealer'", // String
																											// relationshipWhere
					0); // int limit)

			List<String> dealerList = (List<String>) dealerMapList.stream().filter(
					obj -> ((String) ((Map) obj).get(DomainConstants.SELECT_TYPE)).equalsIgnoreCase("SPLM_Dealer"))
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME)).collect(Collectors.toList());

			if (dealerList.isEmpty()) {
				throw new Exception(
						"\u552e\u670d\u96f6\u4ef6 " + partName + " \u586b\u9078 \u767c\u9001\u7d93\u92b7\u5546");
			}

			// documnetId Map collection -> structure <documentName , documentId> , filter by attribute[SPLM_DocType]=='Part Drawing'
			Map<String, String> soDocumentIdList = (Map<String, String>) dealerMapList.stream()
					.filter(obj -> ((String) ((Map) obj).get("attribute[SPLM_DocType]")).equalsIgnoreCase("Part Drawing"))
					.collect(Collectors.toMap(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME),
							obj -> (String) ((Map) obj).get(DomainConstants.SELECT_ID)));

			// groupName data processing
			String groupName = (String) dealerMapList.stream()
					.filter(obj -> ((String) ((Map) obj).get("attribute[SPLM_DocType]")).equalsIgnoreCase("Part Drawing"))
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME))
					.collect(Collectors.joining(forceBreakLine));

			/* --- BOM Part --- */
			MapList subPartMapList = partDomObj.getRelatedObjects(context, "SPLM_SBOM,SPLM_MBOM", // relationshipPattern,
					"*", // typePattern,
					null, // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			ArrayList<PartBulletin> partBulletinArrayList = new ArrayList<PartBulletin>();
			for (Object subPartObj : subPartMapList) {
				Map subPartMap = (Map) subPartObj;

				String subPartNameCh = (String) subPartMap.get("attribute[SPLM_Name_TC]");
				String subPartNameEn = (String) subPartMap.get("attribute[SPLM_Name_EN]");
				String subPartStartDate = (String) subPartMap.get("attribute[SPLM_EnableDate]");
				String subPartEndDate = (String) subPartMap.get("attribute[SPLM_DisableDate]");

				// Date format change
				subPartStartDate = subPartStartDate.isEmpty() ? "" : convertDateFormat(subPartStartDate, "yyyyMMdd");
				subPartEndDate = subPartEndDate.isEmpty() ? "" : convertDateFormat(subPartEndDate, "yyyyMMdd");

				PartBulletin partBulletin = new PartBulletin();
				partBulletin.part = (String) subPartMap.get(DomainConstants.SELECT_NAME);
				partBulletin.partName = subPartNameCh + forceBreakLine + subPartNameEn;
				partBulletin.documentFile = groupName;
				partBulletin.groupCode = (String) subPartMap.get("attribute[SPLM_GroupCode]");
				partBulletin.pnc = (String) subPartMap.get("attribute[SPLM_PNC]");
				partBulletin.quantity = (String) subPartMap.get("attribute[Quantity]");
				partBulletin.typeKD = (String) subPartMap.get("attribute[SPLM_Materiel_KD_Type]");
				partBulletin.startDate = subPartStartDate;
				partBulletin.endDate = subPartEndDate;
				partBulletin.pbRemark = (String) subPartMap.get("attribute[SPLM_PB_Note]");
//				partBulletin.adaptableMS = ((String) subPartMap.get("attribute[SPLM_ModelSeries]")).replaceAll(",",
//						forceBreakLine);
				partBulletin.adaptableMS = adaptMC;
				partBulletinArrayList.add(partBulletin);
			}

			/* --- Pic download --- */
			ContextUtil.pushContext(context);
			MqlUtil.mqlCommand(context, "trigger off");
			DomainObject documentDomObj = new DomainObject();
			ArrayList<Document> documentArrayList = new ArrayList<Document>();
			try {
				for (String documentName : soDocumentIdList.keySet()) {
					String documentId = soDocumentIdList.get(documentName);
					documentDomObj.setId(documentId);
					ArrayList<String> picFileArrayList = new ArrayList<String>();
					for (matrix.db.File documentFile : documentDomObj.getFiles(context)) {
						String fileName = documentFile.getName();
						String fileLocation = fileTempFolder + "\\" + fileName;
						documentDomObj.checkoutFile(context, false, documentFile.getFormat(), fileName, fileTempFolder);
						if (isPictureFile(fileLocation)) {
							picFileArrayList.add(fileLocation);
						}
					}
					Document document = new Document();
					document.picName = documentName;
					document.picFileLocations = picFileArrayList;
					documentArrayList.add(document);
				}
			} catch (Exception e) {
				throw e;
			} finally {
				MqlUtil.mqlCommand(context, "trigger on");
				ContextUtil.popContext(context);
			}

			/* --- assemble Data --- */
			PBPublish pbPublish;
			for (String dealerName : dealerList) {
				Optional<PBPublish> pbOptional = pbPublishArrayList.stream()
						.filter(obj -> obj.dealerName.equalsIgnoreCase(dealerName)).findFirst();
				pbPublish = pbOptional.orElse(new PBPublish());
				if (pbPublish.isDealerNameEmpty()) {
					pbPublish.dealerName = dealerName;
					pbPublish.dealerPB = new ArrayList<PartBulletin>();
					pbPublish.dealerDocument = new ArrayList<Document>();
					pbPublishArrayList.add(pbPublish);
				}
				pbPublish.dealerPB.addAll(partBulletinArrayList);
				pbPublish.dealerDocument.addAll(documentArrayList);
			}
		}
		return pbPublishArrayList;
	}

	private ArrayList<String> createPBExcel(Context context, ArrayList<PBPublish> pbPublishArrayList,
			SOPBTitle soPBTitle, String outputSoFileName) throws Exception {
		String soName = soPBTitle.titleName;
		ArrayList<String> pbFileArrayList = new ArrayList<String>();

		FileInputStream fis = null;
		try {
			for (PBPublish pbPublish : pbPublishArrayList) {
				fis = new FileInputStream(INPORT_PB_EXCEL_LOCATION);
				XSSFWorkbook pbWorkBook = (XSSFWorkbook) WorkbookFactory.create(fis);
				fis.close();

				// Home Page
				Sheet homePageSheet = pbWorkBook.getSheet("PB");

				/* homePage Sending title */
				CellStyle homePageCellStyleForDelivery = cloneCellStyle(homePageSheet, 3, 0);
				homePageCellStyleForDelivery.setWrapText(true);
				Row row4 = homePageSheet.getRow(4);

				Cell soNumberCell = row4.createCell(0);
				soNumberCell.setCellStyle(homePageCellStyleForDelivery);
				soNumberCell.setCellValue(soPBTitle.titleName);

				Cell soIssuerCell = row4.createCell(1);
				soIssuerCell.setCellStyle(homePageCellStyleForDelivery);
				soIssuerCell.setCellValue(soPBTitle.titleIssuer);

				Cell soVaultCell = row4.createCell(3);
				soVaultCell.setCellStyle(homePageCellStyleForDelivery);
				soVaultCell.setCellValue(soPBTitle.titleVault);

				Cell soOwnerCell = row4.createCell(6);
				soOwnerCell.setCellStyle(homePageCellStyleForDelivery);
				soOwnerCell.setCellValue(soPBTitle.titleOwner);

				Cell soSendDeptCell = row4.createCell(8);
				soSendDeptCell.setCellStyle(homePageCellStyleForDelivery);
				soSendDeptCell.setCellValue(soPBTitle.titleSendDept);

				Cell soReleaseDateCell = row4.createCell(13);
				soReleaseDateCell.setCellStyle(homePageCellStyleForDelivery);
				soReleaseDateCell.setCellValue(soPBTitle.titleReleaseDate);

				/* PB */
				CellStyle homePageCellStyleForPBDetail = cloneCellStyle(homePageSheet, 9, 0);
				CellStyle homePageCellStyleForPBDetail1 = cloneCellStyle(homePageSheet, 9, 1);
				CellStyle homePageCellStyleForPBDetail11 = cloneCellStyle(homePageSheet, 9, 11);
				homePageCellStyleForPBDetail.setWrapText(true);
				int aiLocationRow = 10;
				if (!pbPublish.isDealerPBEmpty()) {
					ArrayList<PartBulletin> partBullentinArrayList = pbPublish.dealerPB;
					int partBullentinLength = partBullentinArrayList.size();

					Cell detailCell = homePageSheet.getRow(7).getCell(0);
					detailCell.setCellValue(
							detailCell.getStringCellValue().replace("  ", String.valueOf(partBullentinLength)));
					Cell dealerDetailCell = homePageSheet.getRow(8).getCell(0);
					dealerDetailCell
							.setCellValue(dealerDetailCell.getStringCellValue().replace(" ", pbPublish.dealerName));

					for (int i = 0; i < partBullentinLength; i++) {
						Row row = homePageSheet.createRow(i + aiLocationRow);
						PartBulletin partBulletin = partBullentinArrayList.get(i);

						Cell partCell = row.createCell(0);
						partCell.setCellStyle(homePageCellStyleForPBDetail);
						partCell.setCellValue(partBulletin.part);

						Cell partNameCell = row.createCell(1);
						partNameCell.setCellStyle(homePageCellStyleForPBDetail);
						partNameCell.setCellValue(partBulletin.partName);

						Cell nullCell = row.createCell(2);
						nullCell.setCellStyle(homePageCellStyleForPBDetail);
						
						Cell groupDrawingCell = row.createCell(3);
						groupDrawingCell.setCellStyle(homePageCellStyleForPBDetail);
						groupDrawingCell.setCellValue(partBulletin.documentFile);

						Cell groupCodeCell = row.createCell(4);
						groupCodeCell.setCellStyle(homePageCellStyleForPBDetail);
						groupCodeCell.setCellValue(partBulletin.groupCode);

						Cell pncCell = row.createCell(5);
						pncCell.setCellStyle(homePageCellStyleForPBDetail);
						pncCell.setCellValue(partBulletin.pnc);

						Cell quantityCell = row.createCell(6);
						quantityCell.setCellStyle(homePageCellStyleForPBDetail);
						quantityCell.setCellValue(partBulletin.quantity);

						Cell KDTypeCell = row.createCell(7);
						KDTypeCell.setCellStyle(homePageCellStyleForPBDetail);
						KDTypeCell.setCellValue(partBulletin.typeKD);

						Cell startDateCell = row.createCell(8);
						startDateCell.setCellStyle(homePageCellStyleForPBDetail);
						startDateCell.setCellValue(partBulletin.startDate);

						Cell endDateCell = row.createCell(9);
						endDateCell.setCellStyle(homePageCellStyleForPBDetail);
						endDateCell.setCellValue(partBulletin.endDate);

						Cell bomRemarkCell = row.createCell(10);
						bomRemarkCell.setCellStyle(homePageCellStyleForPBDetail);
						bomRemarkCell.setCellValue(partBulletin.pbRemark);

						Cell adaptMSCell = row.createCell(11);
						adaptMSCell.setCellStyle(homePageCellStyleForPBDetail);
						adaptMSCell.setCellValue(partBulletin.adaptableMS);
						
						Cell nullCell1 = row.createCell(12);
						nullCell1.setCellStyle(homePageCellStyleForPBDetail);
						Cell nullCell2 = row.createCell(13);
						nullCell2.setCellStyle(homePageCellStyleForPBDetail);

					}
				}

				/* past normal/ZIP Picture */
				if (!pbPublish.isDealerDocumentEmpty()) {
					ArrayList<Document> documentArrayList = pbPublish.dealerDocument;
					for (Document document : documentArrayList) {
						XSSFClientAnchor preImage = null;
						Sheet picSheet = pbWorkBook.createSheet(document.picName);
						XSSFDrawing drawing = (XSSFDrawing) picSheet.createDrawingPatriarch();
						for (String picLocation : document.picFileLocations) {
							preImage = addImage(pbWorkBook, drawing, picLocation, preImage);
						}
					}
				}

				protectSheets(pbWorkBook);
				String pbFile = outputSoFileName + "\\PB_" + pbPublish.dealerName + ".xlsx";
				this.output(pbWorkBook, pbFile);
				pbFileArrayList.add(pbFile);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
		return pbFileArrayList;
	}

	private SOPBTitle getSOPBExcelTitleInfo(Context context, DomainObject soObj) throws Exception {
		String soOwner = (String) soObj.getInfo(context, DomainConstants.SELECT_OWNER);
		String soReleaseDate = soObj.getInfo(context, "state[Complete].actual");
		soReleaseDate = soReleaseDate.isEmpty() ? "" : convertDateFormat(soReleaseDate, "yyyyMMdd");

		Map<String, String> soSendDeptInfo = getSoSendDeptInfo(context);
		ArrayList<String> soSendDeptArrayList = new ArrayList<String>();
		for (String sendDeptStr : soObj.getAttributeValue(context, "SPLM_SendDept").split("~QWE~")) {
			soSendDeptArrayList.add(soSendDeptInfo.get(sendDeptStr));
		}
		SOPBTitle soPBTitle = new SOPBTitle();
		soPBTitle.titleName = soObj.getInfo(context, DomainConstants.SELECT_NAME);
		soPBTitle.titleIssuer = "\u6559\u80b2\u8a13\u7df4\u8cc7\u6599\u7d44";
		soPBTitle.titleVault = Person.getPerson(context, soOwner).getInfo(context, Person.SELECT_FIRST_NAME);
		soPBTitle.titleOwner = Person.getPerson(context, soOwner).getInfo(context, Person.SELECT_FIRST_NAME);
		soPBTitle.titleSendDept = String.join(",", soSendDeptArrayList);
		soPBTitle.titleReleaseDate = soReleaseDate;
		soPBTitle.releaseSO = Boolean.valueOf(soObj.getAttributeValue(context, "SPLM_ReleaseSO"));
		soPBTitle.releasePB = Boolean.valueOf(soObj.getAttributeValue(context, "SPLM_ReleasePB"));

		return soPBTitle;
	}

	private Map<String, String> getSoSendDeptInfo(Context context) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add(DomainConstants.SELECT_DESCRIPTION);

		MapList sendDeptMapList = DomainObject.findObjects(context, "SPLM_SendDept", "*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"", // where
				null, false, busSelects, (short) 0);

		return (Map<String, String>) sendDeptMapList.stream()
				.collect(Collectors.toMap(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME),
						obj -> (String) ((Map) obj).get(DomainConstants.SELECT_DESCRIPTION)));
	}

	private void protectSheets(XSSFWorkbook xswb) {
		int sheetTotal = xswb.getNumberOfSheets();
	    for (int i = 0; i < sheetTotal; i++) {
	        Sheet sh = xswb.getSheetAt(i);
	        if ( !Optional.ofNullable(sh).isPresent() ) {
	            continue;
	        }
	        sh.protectSheet(UUID.randomUUID().toString());
	    }
	}
	
	/* --- Class --- */
	class SOPublish {
		private String fileName;
		private ArrayList<AffectedItem> vendorAI;
		private ArrayList<PartBom> vendorBom;
		private ArrayList<Document> vendorDocument;

		public boolean isVendorNameEmpty() {
			return !Optional.ofNullable(this.fileName).isPresent();
		}

		public boolean isVendorAIEmpty() {
			return !Optional.ofNullable(this.vendorAI).isPresent();
		}

		public boolean isVendorBomEmpty() {
			return !Optional.ofNullable(this.vendorBom).isPresent();
		}

		public boolean isVendorDocumentEmpty() {
			return !Optional.ofNullable(this.vendorDocument).isPresent();
		}
	}

	class AffectedItem implements Cloneable{
		private String part;
		private String affectedItemPart;
		private String partName;
		private String documentFile;
		private String mediumPackingNums;
		private String unit;
		private String materialType;
		private String materialGroup;
		private String partType;
		private String purchasingGroup;
		private String vendor;
		private String referenceEO;
		private String referencePO;
		private String altOptPart;
		private String acNo;
		private String remark;
		
		@Override
		protected Object clone() throws CloneNotSupportedException {
			Object cloneObj = super.clone();
			return cloneObj;
		}
	}

	class PartBom {
		private String part;
		private String subPart;
		private String quantity;
		private String groupCode;
		private String pnc;
		private String typeKD;
		private String vendor;
		private String startDate;
		private String endDate;
		private String bomRemark;
	}

	class Document {
		private String picName;
		private ArrayList<String> picFileLocations;
	}

	class PBPublish {
		private String dealerName;
		private ArrayList<PartBulletin> dealerPB;
		private ArrayList<Document> dealerDocument;

		public boolean isDealerNameEmpty() {
			return !Optional.ofNullable(this.dealerName).isPresent();
		}

		public boolean isDealerPBEmpty() {
			return !Optional.ofNullable(this.dealerPB).isPresent();
		}

		public boolean isDealerDocumentEmpty() {
			return !Optional.ofNullable(this.dealerDocument).isPresent();
		}
	}

	class PartBulletin {
		private String part;
		private String partName;
		private String documentFile;
		private String groupCode;
		private String pnc;
		private String quantity;
		private String typeKD;
		private String startDate;
		private String endDate;
		private String pbRemark;
		private String adaptableMS;
	}

	class SOPBTitle {
		private String titleName;
		private String titleIssuer;
		private String titleVault;
		private String titleOwner;
		private String titleSendDept;
		private String titleReleaseDate;
		private boolean releaseSO;
		private boolean releasePB;
	}
}
