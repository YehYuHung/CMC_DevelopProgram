import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
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
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;

import com.google.gson.GsonBuilder;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.i18nNow;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;

public class SPLM_SOPB_Excel_JPO_mxJPO {

	private static final String INPORT_SO_EXCEL_LOCATION = "C:\\DassaultSystemesBatchFile\\SOExcelTemplate\\SO - 20211230.xlsx";
	private static final String INPORT_PB_EXCEL_LOCATION = "C:\\DassaultSystemesBatchFile\\SOExcelTemplate\\PB - 20211211.xlsx";

	private static final String TEMP_FOLDER = "PLMTemp\\createSOPBExcel\\";
	private static final String TEMP_FOLDER_DEVICE = "D:\\";
	private static final String VAULT = "eService Production";
	private static final String FORCE_BREAKLINE = "\r\n";
	private static final String PLM_ENV = System.getenv("PLM_ENV");
	private static final String SO_EXCEL = "SO.EXCEL.FTP";
	private static final String SO_PICLOCATION = "\u5716\u6a94";
	
	/* --- FTP Connect --- */
	private void sendExeclToFTP(File file) throws Exception{
		String ftpHost = i18nNow.getI18nString(SO_EXCEL + ".HOST." + PLM_ENV, "emxSPLM", ""); 
		String ftpFolder = i18nNow.getI18nString(SO_EXCEL + ".FOLDER." + PLM_ENV, "emxSPLM", ""); 
		String ftpUser = i18nNow.getI18nString(SO_EXCEL + ".USER." + PLM_ENV, "emxSPLM", ""); 
		String ftpPsd = i18nNow.getI18nString(SO_EXCEL + ".PASSWORD." + PLM_ENV, "emxSPLM", "");
		
		boolean successLogin = false;
		InputStream is = null;
		FTPClient ftpclient = new FTPClient();
		try {
			ftpclient.connect(ftpHost);
			successLogin = ftpclient.login(ftpUser, ftpPsd);
			if (!successLogin) {
				throw new Exception("Login Fail");
			}
			boolean successChange = ftpclient.changeWorkingDirectory(ftpFolder);
			if (!successChange) {
				throw new Exception("Change Folder Fail");
			}
			ftpclient.setFileType(FTPClient.BINARY_FILE_TYPE);
			is = Files.newInputStream(Paths.get(file.getPath()));
			ftpclient.storeFile(file.getName(), is);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw e;
		} finally {
			if (is != null) {
				is.close();
			}
			if (successLogin) {
				ftpclient.logout();
			}
			if (ftpclient.isConnected()) {
				ftpclient.disconnect();
			}
		}
	}

	/* --- can using method - Testing OK --- */
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
	
	/**
	 * Lock Excel All Sheet with UUID
	 * @param context
	 * @param soObj
	 * @param busSelect
	 * @return
	 * @throws Exception
	 */
	private void protectAllSheets(XSSFWorkbook xswb) {
		int sheetTotal = xswb.getNumberOfSheets();
		for (int i = 0; i < sheetTotal; i++) {
			Sheet sh = xswb.getSheetAt(i);
			if (!Optional.ofNullable(sh).isPresent()) {
				continue;
			}
			sh.protectSheet(UUID.randomUUID().toString());
		}
	}

	private String fillPrefixZero(String value , int fillLength) {
		int valueLength = value.length();
		if( valueLength < fillLength ) {			
			int left = fillLength - valueLength;
			value = String.format("%0" + left +"d%s", 0 , value);
		}
		return value;
	}
	
	private void makeZip(File srcFile, File targetZip)
            throws IOException, FileNotFoundException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
                targetZip));
        String dir = "";
        recurseFiles(srcFile, zos, dir);
        zos.close();
    }

    private void recurseFiles(File file, ZipOutputStream zos, String dir)
            throws IOException, FileNotFoundException {
    
        if (file.isDirectory()) {
            System.out.println("find Directory : " + file.getName());
            dir += file.getName() + File.separator;
            String[] fileNames = file.list();
            if (fileNames != null) {
                for (int i = 0; i < fileNames.length; i++) {
                    recurseFiles(new File(file, fileNames[i]), zos, dir);
                }
            }
        }
        // Otherwise, a file so add it as an entry to the Zip file.
        else {
            System.out.println("to zip : " + file.getName());

            byte[] buf = new byte[1024];
            int len;

            // Create a new Zip entry with the file's name.
            dir = dir.substring(dir.indexOf(File.separator) + 1);
            ZipEntry zipEntry = new ZipEntry(dir + file.getName());
            // Create a buffered input stream out of the file
            // we're trying to add into the Zip archive.
            FileInputStream fin = new FileInputStream(file);
            BufferedInputStream in = new BufferedInputStream(fin);
            zos.putNextEntry(zipEntry);
            // Read bytes from the file and write into the Zip archive.

            while ((len = in.read(buf)) >= 0) {
                zos.write(buf, 0, len);
            }

            // Close the input stream.
            in.close();

            // Close this entry in the Zip stream.
            zos.closeEntry();
        }

    }
	
	/* ---Dassault API package simple using--- */
	private MapList getSOAI(Context context, DomainObject soObj, StringList busSelect) throws Exception {
		return getSOAI(context, soObj, busSelect, "");
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

	private MapList getRelatedVendor(Context context, DomainObject partObj, StringList busSelects,
			StringList relSelects) throws Exception {
		return partObj.getRelatedObjects(context, "SPLM_RelatedVendor", // relationshipPattern,
				"SPLM_Vendor", // typePattern,
				busSelects, // StringList objectSelects,
				relSelects, // StringList relationshipSelects,
				false, // boolean getTo,
				true, // boolean getFrom,
				(short) 1, // short recurseToLevel,
				"", // String objectWhere,
				"", // String relationshipWhere
				0); // int limit)
	}

	private MapList getSOReleaseDocumentForDealer(Context context, DomainObject partObj, DomainObject soObj,
			StringList busSelects) throws Exception {
		return getSORelease(context, partObj, busSelects, "SPLM_Document",
				"attribute[SPLM_SO]=='" + soObj.getInfo(context, DomainConstants.SELECT_NAME)
						+ "' && attribute[SPLM_SOReleaseItemType]=='Dealer'");
	}

	private MapList getSOReleaseDocumentForVendor(Context context, DomainObject partObj, DomainObject soObj,
			StringList busSelects) throws Exception {
		return getSORelease(context, partObj, busSelects, "SPLM_Document",
				"attribute[SPLM_SO]=='" + soObj.getInfo(context, DomainConstants.SELECT_NAME)
						+ "' && attribute[SPLM_SOReleaseItemType]=='Vendor'");
	}

	private MapList getSOReleaseDealer(Context context, DomainObject partObj, DomainObject soObj, StringList busSelects)
			throws Exception {
		return getSORelease(context, partObj, busSelects, "SPLM_Dealer",
				"attribute[SPLM_SO]=='" + soObj.getInfo(context, DomainConstants.SELECT_NAME) + "'");
	}

	private MapList getSOReleaseVendor(Context context, DomainObject partObj, DomainObject soObj, StringList busSelects)
			throws Exception {
		return getSORelease(context, partObj, busSelects, "SPLM_Vendor",
				"attribute[SPLM_SO]=='" + soObj.getInfo(context, DomainConstants.SELECT_NAME) + "'");
	}

	private MapList getSORelease(Context context, DomainObject partObj, StringList busSelects, String typePattern,
			String relWhere) throws Exception {
		return partObj.getRelatedObjects(context, "SPLM_RelatedSOReleaseItem", // relationshipPattern,
				typePattern, // typePattern,
				busSelects, // StringList objectSelects,
				null, // StringList relationshipSelects,
				false, // boolean getTo,
				true, // boolean getFrom,
				(short) 1, // short recurseToLevel,
				"", // String objectWhere,
				relWhere, // String relationshipWhere
				0); // int limit)
	}

	private DomainObject createVersionObject(Context context, DomainObject obj, String objType, String fileName)
			throws Exception {
		String docOwner = (String) obj.getInfo(context, DomainConstants.SELECT_OWNER);

		DomainObject versionObj = new DomainObject();

		versionObj.createObject(context, objType, UUID.randomUUID().toString(), "1", "Version", VAULT);

		Map<String, String> versionValues = new HashMap<String, String>();
		versionValues.put("Title", fileName);
		versionValues.put("Is Version Object", "True");
		versionValues.put("Originator", docOwner);
		versionObj.setAttributeValues(context, versionValues);
		versionObj.setOwner(context, docOwner);

		MqlUtil.mqlCommand(context, "modify bus " + versionObj.getId(context) + " organization '"
				+ obj.getInfo(context, "organization") + "' project '" + obj.getInfo(context, "project") + "'");

		DomainRelationship.connect(context, obj, "Active Version", versionObj);

		DomainRelationship.connect(context, obj, "Latest Version", versionObj);
		return versionObj;
	}

	private void prechekcAndUploadSoDocument(Context context, DomainObject soObj, ArrayList<String> execlFileArrayList) throws Exception {
		StringList docIdStringList = soObj.getInfoList(context, "from[SPLM_ReferenceDoc].to.id");
		DomainObject docDomObj = new DomainObject();
		DomainObject docVerDomObj = new DomainObject();
		for(String docId : docIdStringList) {
			docDomObj.setId(docId);
			StringList docVerIdList = docDomObj.getInfoList(context, "from[Active Version].to.id");
			for(String docVerId : docVerIdList) {
				docVerDomObj.setId(docVerId);
				docVerDomObj.deleteObject(context);
			}
			docDomObj.deleteObject(context);
		}
		
		for (String fileLocation : execlFileArrayList) {
			File file = new File(fileLocation);
			String fileFullName = file.getName();
			String fileName = fileFullName.substring(0, fileFullName.lastIndexOf('.'));
			
			DomainObject soDocumentObj = new DomainObject();
			soDocumentObj.createObject(context, "SPLM_SODocument", // String Type
					fileName, // String name
					"-", // String revision
					"SPLM_SODocument", // String policy
					VAULT); // String vault
			soDocumentObj.setOwner(context, soObj.getInfo(context, DomainConstants.SELECT_OWNER));

			MqlUtil.mqlCommand(context,
					"modify bus " + soDocumentObj.getId(context) + " organization '"
							+ soObj.getInfo(context, "organization") + "' project '"
							+ soObj.getInfo(context, "project") + "'");

			DomainRelationship.connect(context, soObj, "SPLM_ReferenceDoc", soDocumentObj);

			soDocumentObj.checkinFile(context, false, true, InetAddress.getLocalHost().getHostName(),
					DomainConstants.FORMAT_GENERIC, fileFullName, file.getParent());

			createVersionObject(context, soDocumentObj, "SPLM_SODocument", fileFullName);
		}
		soObj.setAttributeValue(context, "SPLM_SOExcelFlag", "Complete");
	}

	private void uploadSoDocumentToFTP(Context context, DomainObject soDomObj, ArrayList<String> soFileArrayList) throws Exception{
		String sendVendorFTPSuffix = soDomObj.getInfo(context, DomainConstants.SELECT_NAME) + "SO.zip";
		for (String soLocation : soFileArrayList) {
			File oldFile = new File(soLocation);
			String soFileName = oldFile.getName();
			String vendorNo = soFileName.substring(soFileName.indexOf("_")+1, soFileName.lastIndexOf("."));
			String newFileStr = oldFile.getParent() + "\\V" + fillPrefixZero(vendorNo, 5) + sendVendorFTPSuffix;
			File newFile = new File(newFileStr);
			oldFile.renameTo(newFile);
			sendExeclToFTP(newFile);;
		}
	}
	
	private String getLatestEO(Context context, String physicalid) throws Exception {
		String proposed = MqlUtil.mqlCommand(context, "query path type 'Proposed Activity.Where' endswithany '" + physicalid + "' select owner dump ' '");
		String objName = "";
		if (StringUtils.isEmpty(proposed)) {
			return MqlUtil.mqlCommand(context, "print bus '" + physicalid + "' select to[Affected Item].from.name dump");
		}
		StringList proposedList = new StringList();
		if (proposed.contains("\n")) {
			String[] tempProposedArray = proposed.split("\n");
			for (String tempProposed : tempProposedArray) {
				if (StringUtils.isEmpty(tempProposed)) {
					continue;
				}
				if (proposedList.contains(tempProposed)) {
					continue;
				}
				proposedList.add(tempProposed);
			}
		} else {
			proposedList.add(proposed);
		}
		if (proposedList.isEmpty()) {
			return objName;
		}
		StringList eoIdList = new StringList();
		String eoId;
		DomainObject eoObj = new DomainObject();
		for (String tempProposed : proposedList) {
			eoId = MqlUtil.mqlCommand(context, "print bus " + tempProposed.replace("Where Proposed Activity", "'Proposed Activity'") + " select to[Proposed Activities].from.id dump");
			if (StringUtils.isEmpty(eoId)) {
				continue;
			}
			if (eoIdList.contains(eoId)) {
				continue;
			}
			eoIdList.add(eoId);
			eoObj.setId(eoId);
			objName = eoObj.getInfo(context, DomainConstants.SELECT_CURRENT).equalsIgnoreCase("Complete")? eoObj.getInfo(context, DomainConstants.SELECT_NAME) : objName;
		}
		return objName;
	}
	
	/* --- package ArrayList Location --- */
	private ArrayList<AffectedItem> getAffectedItemArrayList(Context context, MapList servicesPartMapList,
			DomainObject soDomObj) throws Exception {
		StringList altOptBusSelects = new StringList();
		altOptBusSelects.add(DomainConstants.SELECT_NAME);
		StringList altOptRelSelects = new StringList();
		altOptRelSelects.add("attribute[SPLM_OptionalExchangeable]");
		altOptRelSelects.add("attribute[SPLM_OptionalType]");

		StringList parentBusSelects = new StringList();
		parentBusSelects.add(DomainConstants.SELECT_NAME);
		parentBusSelects.add(DomainConstants.SELECT_ID);
		StringList parentRelSelects = new StringList();
		parentRelSelects.add("attribute[SPLM_RelatedEO]");
		parentRelSelects.add("attribute[SPLM_RelatedPO]");

		StringList soSendVendorBusSelects = new StringList();
		soSendVendorBusSelects.add(DomainConstants.SELECT_NAME);

		StringList soSendDocumentForVendorBusSelects = new StringList();
		soSendDocumentForVendorBusSelects.add(DomainConstants.SELECT_NAME);
		soSendDocumentForVendorBusSelects.add("attribute[SPLM_DocType]");

		StringList vendorBusSelects = new StringList();
		vendorBusSelects.add(DomainConstants.SELECT_NAME);
		StringList vendorRelSelects = new StringList();
		vendorRelSelects.add("attribute[SPLM_VendorPartNo]");
		
		StringList eoPartBusSelects = new StringList();
		eoPartBusSelects.add("physicalid");

		ArrayList<AffectedItem> affectedItemArrayList = new ArrayList<AffectedItem>();
		DomainObject partDomObj = new DomainObject();

		String soName = soDomObj.getInfo(context, DomainConstants.SELECT_NAME);
		StringList soAffectedParentPartIdList = soDomObj.getInfoList(context, "from[SPLM_ReferenceParentPart].to.id");

		for (Object partInfo : servicesPartMapList) {
			Map<String, String> partMap = (Map) partInfo;
			String partId = partMap.get(DomainConstants.SELECT_ID);
			String partName = partMap.get(DomainConstants.SELECT_NAME);
			String partNameCh = partMap.get("attribute[SPLM_Name_TC]");
			String partNameEn = partMap.get("attribute[SPLM_Name_EN]");
			String partMediumPackingNums = "1";
			String partUnit = partMap.get("attribute[SPLM_Unit]");
			String partMaterialType = partMap.get("attribute[SPLM_MaterialType]");
			String partMaterialGroup = partMap.get("attribute[SPLM_MaterialGroup]");
			String partItemSubType = partMap.get("attribute[SPLM_ItemSubType]");
			String partPuchasingGroup = partMap.get("attribute[SPLM_PurchasingGroup]");
			String colorPartACNO = partMap.get("attribute[SPLM_AC_NO]");
			String colorPartCPNO = partMap.get("attribute[SPLM_CP_NO]");
			String partRemark = partMap.get("attribute[SPLM_SO_Note]");
			String partPhysicalId = partMap.get("physicalid");
			partDomObj.setId(partId);
			String relatedEO = "";
			String relatedPO = "";
			String parentPart = "";
			String altOptPart = "";

			// partParent data processing
			MapList parentPartMapList = partDomObj.getRelatedObjects(context, "SPLM_SBOM,SPLM_MBOM", // relationshipPattern,
					"*", // typePattern,
					parentBusSelects, // StringList objectSelects,
					parentRelSelects, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			if (!parentPartMapList.isEmpty()) {
				parentPart = (String) parentPartMapList.stream().filter(
						obj -> soAffectedParentPartIdList.contains((String) ((Map) obj).get(DomainConstants.SELECT_ID)))
						.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME))
						.collect(Collectors.joining(FORCE_BREAKLINE));

				relatedPO = (String) parentPartMapList.stream().filter(
						obj -> soAffectedParentPartIdList.contains((String) ((Map) obj).get(DomainConstants.SELECT_ID)))
						.map(obj -> (String) ((Map) obj).get("attribute[SPLM_RelatedPO]"))
						.collect(Collectors.joining(FORCE_BREAKLINE));
			}
			
			// ServicePart -> Part -> EO -> Latest EO  data processing
			MapList eoPartMapList = DomainObject.findObjects(context, "Part,CMC_ColorPart",
					partName, 
					"*",
					"*",
					VAULT,
					"current=='Release' && (revision==last || next.current!='Release')",
					null, false, eoPartBusSelects, (short)1);
			for(Object eoPartObj : eoPartMapList) {
				Map<String, String> eoPartMap = (Map) eoPartObj;
				relatedEO = getLatestEO(context, eoPartMap.get("physicalid"));
			}

			// vendor/document data processing
			MapList sendVendorMapList = getSOReleaseVendor(context, partDomObj, soDomObj, soSendVendorBusSelects);

			if (sendVendorMapList.isEmpty()) {
				throw new Exception("\u552e\u670d\u96f6\u4ef6 " + partName + " \u586b\u9078 \u767c\u9001\u5ee0\u5546");
			}

			List<String> sendVendorList = (List<String>) sendVendorMapList.stream()
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME)).collect(Collectors.toList());

			// groupName data processing
			MapList sendDocumentForVendorMapList = getSOReleaseDocumentForVendor(context, partDomObj, soDomObj,
					soSendDocumentForVendorBusSelects);

			String groupName = (String) sendDocumentForVendorMapList.stream().filter(
					obj -> ((String) ((Map) obj).get("attribute[SPLM_DocType]")).equalsIgnoreCase("Part Drawing"))
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME))
					.collect(Collectors.joining(FORCE_BREAKLINE));

			// altPart/optPart data processing
			MapList optAltMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedOptionalPart", // relationshipPattern,
					"SPLM_Part,SPLM_ColorPart", // typePattern,
					altOptBusSelects, // StringList objectSelects,
					altOptRelSelects, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			ArrayList<String> optAltArrayList = new ArrayList<String>();
			for (Object optAltObj : optAltMapList) {
				Map<String, String> optAltMap = (Map) optAltObj;
				String optAltName = optAltMap.get(DomainConstants.SELECT_NAME);
				String optAltOptionType = optAltMap.get("attribute[SPLM_OptionalType]");
				String optAltExchageable = optAltMap.get("attribute[SPLM_OptionalExchangeable]");
				String optAltRel = optAltExchageable.isEmpty() ? optAltName + "(" + optAltOptionType + ")"
						: optAltName + "(" + optAltExchageable + ")";
				optAltArrayList.add(optAltRel);
			}
			altOptPart = String.join(FORCE_BREAKLINE, optAltArrayList);

			// colorPart ACNO/CPNO
			String partACNO = colorPartACNO.isEmpty() ? "" : (colorPartACNO + "_" + FORCE_BREAKLINE + colorPartCPNO);

			// vendor all through rel[SPLM_RelatedVendor].vendor.attribute[]==TRUE
			MapList vendorPartNoMapList = getRelatedVendor(context, partDomObj, vendorBusSelects, vendorRelSelects);

			Map<String, String> vendorPartNoMap = (Map<String, String>) vendorPartNoMapList.stream()
					.collect(Collectors.toMap(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME),
							obj -> (String) ((Map) obj).get("attribute[SPLM_VendorPartNo]")));

			/* --- AffectedItem Part --- */
			for (String sendVendor : sendVendorList) {
				AffectedItem affectedItem = new AffectedItem();
				affectedItem.setSendVendorName(sendVendor);
				affectedItem.setPart(parentPart);
				affectedItem.setAffectedItemPart(partName);
				affectedItem.setPartName(partNameCh + FORCE_BREAKLINE + partNameEn);
				affectedItem.setDocumentFile(groupName);
				affectedItem.setMediumPackingNums(partMediumPackingNums);
				affectedItem.setUnit(partUnit);
				affectedItem.setMaterialType(partMaterialType);
				affectedItem.setMaterialGroup(partMaterialGroup);
				affectedItem.setPartType(partItemSubType);
				affectedItem.setPurchasingGroup(partPuchasingGroup);
				affectedItem.setVendorPartNo(vendorPartNoMap.get(sendVendor));
				affectedItem.setRelatedEO(relatedEO);
				affectedItem.setRelatedPO(relatedPO);
				affectedItem.setAltOptPart(altOptPart);
				affectedItem.setAcNo(partACNO);
				affectedItem.setRemark(partRemark);
				affectedItemArrayList.add(affectedItem);
			}
		}
		return affectedItemArrayList;
	}

	private ArrayList<PartBom> getAIPartBomArrayList(Context context, MapList servicesPartMapList,
			DomainObject soDomObj) throws Exception {
		DomainObject partDomObj = new DomainObject();

		StringList sBomBusSelects = new StringList();
		sBomBusSelects.add(DomainConstants.SELECT_NAME);
		sBomBusSelects.add(DomainConstants.SELECT_ID);
		StringList sBomRelSelects = new StringList();
		sBomRelSelects.add("attribute[Quantity]");
		sBomRelSelects.add("attribute[SPLM_GroupCode]");
		sBomRelSelects.add("attribute[SPLM_PNC]"); // pnc
		sBomRelSelects.add("attribute[SPLM_Materiel_KD_Type]"); // K-/D-
		sBomRelSelects.add("attribute[SPLM_EnableDate]"); // Start Date
		sBomRelSelects.add("attribute[SPLM_DisableDate]"); // End Date
		sBomRelSelects.add("attribute[SPLM_BOM_Note]"); // Bom remark

		StringList subPartVendorBusSelects = new StringList();
		subPartVendorBusSelects.add(DomainConstants.SELECT_NAME);
		StringList subPartVendorRelSelects = new StringList();
		subPartVendorRelSelects.add("attribute[SPLM_Valid]");

		StringList partVendorBusSelects = new StringList();
		partVendorBusSelects.add(DomainConstants.SELECT_NAME);

		String soName = soDomObj.getInfo(context, DomainConstants.SELECT_NAME);

		ArrayList<PartBom> partBomArrayList = new ArrayList<PartBom>();

		for (Object partInfo : servicesPartMapList) {
			Map<String, String> partMap = (Map) partInfo;
			String partId = partMap.get(DomainConstants.SELECT_ID);
			String partName = partMap.get(DomainConstants.SELECT_NAME);

			partDomObj.setId(partId);
			MapList subPartMapList = partDomObj.getRelatedObjects(context, "SPLM_SBOM,SPLM_MBOM", // relationshipPattern,
					"*", // typePattern,
					sBomBusSelects, // StringList objectSelects,
					sBomRelSelects, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)

			MapList sendVendorMapList = getSOReleaseVendor(context, partDomObj, soDomObj, partVendorBusSelects);
			List<String> sendVendorList = (List<String>) sendVendorMapList.stream()
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME)).collect(Collectors.toList());

			DomainObject subPartDomObj = new DomainObject();

			for (Object subPartObj : subPartMapList) {
				Map<String, String> subPartMap = (Map) subPartObj;

				String subPartId = subPartMap.get(DomainConstants.SELECT_ID);
				String subPartName = subPartMap.get(DomainConstants.SELECT_NAME);
				String subPartQuantity = subPartMap.get("attribute[Quantity]");
				String subPartGroupCode = subPartMap.get("attribute[SPLM_GroupCode]");
				String subPartPNC = subPartMap.get("attribute[SPLM_PNC]");
				String subPartKDType = subPartMap.get("attribute[SPLM_Materiel_KD_Type]");
				String subPartStartDate = subPartMap.get("attribute[SPLM_EnableDate]");
				String subPartEndDate = subPartMap.get("attribute[SPLM_DisableDate]");
				String subPartBomRemark = subPartMap.get("attribute[SPLM_BOM_Note]");
				String subPartStartVendor = "";

				subPartDomObj.setId(subPartId);

				MapList subPartVendorMapList = getRelatedVendor(context, subPartDomObj, subPartVendorBusSelects,
						subPartVendorRelSelects);

				if (!subPartVendorMapList.isEmpty()) {
					subPartStartVendor = (String) subPartVendorMapList.stream()
							.filter(obj -> ((String) ((Map) obj).get("attribute[SPLM_Valid]")).equalsIgnoreCase("*-"))
							.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME))
							.collect(Collectors.joining(FORCE_BREAKLINE));
				}

				for (String Vendor : sendVendorList) {
					PartBom partBom = new PartBom();
					partBom.setSendVendorName(Vendor);
					partBom.setPart(partName);
					partBom.setSubPart(subPartName);
					partBom.setQuantity(subPartQuantity);
					partBom.setGroupCode(subPartGroupCode);
					partBom.setPnc(subPartPNC);
					partBom.setTypeKD(subPartKDType);
					partBom.setSubPartVendorStar(subPartKDType.equalsIgnoreCase("*-") ? subPartStartVendor : "");
					partBom.setStartDate(subPartStartDate);
					partBom.setEndDate(subPartEndDate);
					partBom.setBomRemark(subPartBomRemark);
					partBomArrayList.add(partBom);
				}
			}
		}
		return partBomArrayList;
	}

	private ArrayList<Document> getDocumentForVendorArrayList(Context context, MapList servicesPartMapList,
			DomainObject soDomObj, String fileTempFolder) throws Exception {

		StringList documentBusSelects = new StringList();
		documentBusSelects.add(DomainConstants.SELECT_NAME);
		documentBusSelects.add(DomainConstants.SELECT_ID);
		documentBusSelects.add("attribute[SPLM_DocType]");

		StringList partVendorBusSelects = new StringList();
		partVendorBusSelects.add(DomainConstants.SELECT_NAME);

		DomainObject partDomObj = new DomainObject();
		DomainObject documentDomObj = new DomainObject();
		File file = null;

		String soName = soDomObj.getInfo(context, DomainConstants.SELECT_NAME);
		
		ContextUtil.pushContext(context);
		MqlUtil.mqlCommand(context, "trigger off");
		ArrayList<Document> documentArrayList = new ArrayList<Document>();
		try {
			for (Object partObj : servicesPartMapList) {
				Map<String, String> partMap = (Map<String, String>) partObj;
				partDomObj.setId(partMap.get(DomainConstants.SELECT_ID));

				MapList documentMapList = getSOReleaseDocumentForVendor(context, partDomObj, soDomObj,
						documentBusSelects);

				MapList sendVendorMapList = getSOReleaseVendor(context, partDomObj, soDomObj, partVendorBusSelects);
				List<String> sendVendorList = (List<String>) sendVendorMapList.stream()
						.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME)).collect(Collectors.toList());

				for (Object documentObj : documentMapList) {
					Map<String, String> documentMap = (Map) documentObj;
					
					String documentId = documentMap.get(DomainConstants.SELECT_ID);
					String documentName = documentMap.get(DomainConstants.SELECT_NAME);
					documentDomObj.setId(documentId);
					boolean isPartDrawing = documentMap.get("attribute[SPLM_DocType]").equalsIgnoreCase("Part Drawing");
					
					for (String sendVendor : sendVendorList) {
						String picDocumentFolder = fileTempFolder + "\\" + soName + "_" + sendVendor + "\\" + SO_PICLOCATION;
						file = new File(picDocumentFolder);
						if( !file.exists() ) {
							file.mkdirs();
						}
						
						ArrayList<String> picFileArrayList = getFileLocations(context, documentDomObj, picDocumentFolder);
						
						if( !isPartDrawing ) {
							continue;
						}
						
						Document document = new Document();
						document.setSendTypeName(sendVendor);
						document.setPicName(documentName);
						document.setPicFileLocations(picFileArrayList);
						documentArrayList.add(document);
					}
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			MqlUtil.mqlCommand(context, "trigger on");
			ContextUtil.popContext(context);
		}
		return documentArrayList;
	}

	private ArrayList<PartBulletin> getPartBulletinArrayList(Context context, MapList servicesPartMapList,
			DomainObject soDomObj) throws Exception {

		StringList toGroupDrawingBusSelects = new StringList();
		toGroupDrawingBusSelects.add(DomainConstants.SELECT_ID);
		StringList toGroupDrawingRelSelects = new StringList();
		toGroupDrawingRelSelects.add("attribute[SPLM_CMC_Code]"); // adapt

		StringList fromGroupDrawingBusSelects = new StringList();
		fromGroupDrawingBusSelects.add(DomainConstants.SELECT_ID);
		StringList fromGroupDrawingRelSelects = new StringList();
		fromGroupDrawingRelSelects.add("attribute[SPLM_EnableDate]");
		fromGroupDrawingRelSelects.add("attribute[SPLM_DisableDate]");
		fromGroupDrawingRelSelects.add("attribute[Quantity]");
		fromGroupDrawingRelSelects.add("attribute[SPLM_PNC]");
		fromGroupDrawingRelSelects.add("attribute[SPLM_GroupCode]");

		String soAttPartsCatalogue = soDomObj.getAttributeValue(context, "SPLM_PartsCatalogue");
		if (soAttPartsCatalogue.isEmpty()) {
			throw new Exception("\u8acb\u8f38\u5165 \u6709\u6548 \u96f6\u4ef6\u518a");
		}

		MapList partCatalogueIdMapList = DomainObject.findObjects(context, "SPLM_PartsCatalogue", soAttPartsCatalogue, // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"", // where
				null, false, new StringList(DomainConstants.SELECT_ID), (short) 0);

		String partCatalogueId = (String) ((Map) partCatalogueIdMapList.get(0)).get(DomainConstants.SELECT_ID);
		DomainObject partCatalogueDomObj = new DomainObject(partCatalogueId);

		MapList toGroupDrawingMapList = partCatalogueDomObj.getRelatedObjects(context, "SPLM_RelatedGroupDrawing", // relationshipPattern,
				"SPLM_GroupDrawing", // typePattern,
				toGroupDrawingBusSelects, // StringList objectSelects,
				toGroupDrawingRelSelects, // StringList relationshipSelects,
				false, // boolean getTo,
				true, // boolean getFrom,
				(short) 1, // short recurseToLevel,
				"", // String objectWhere,
				"", // String
				0);

		// Map -> <Id, ModelCode>
		Map<String, String> groupDrawingMap = (Map<String, String>) toGroupDrawingMapList.stream()
				.collect(Collectors.toMap(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_ID),
						obj -> (String) ((Map) obj).get("attribute[SPLM_CMC_Code]")));
		String groupDrawingAllId = String.join(",", groupDrawingMap.keySet());

		ArrayList<PartBulletin> partBulletinArrayList = new ArrayList<PartBulletin>();
		DomainObject partDomObj = new DomainObject();

		for (Object partObj : servicesPartMapList) {
			Map<String, String> partMap = (Map) partObj;
			String partId = partMap.get(DomainConstants.SELECT_ID);
			partDomObj.setId(partId);

			MapList fromGroupDrawingMapList = partDomObj.getRelatedObjects(context, "SPLM_RelatedPart", // relationshipPattern,
					"SPLM_GroupDrawing", // typePattern,
					fromGroupDrawingBusSelects, // StringList objectSelects,
					fromGroupDrawingRelSelects, // StringList relationshipSelects,
					true, // boolean getTo,
					false, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"id smatchlist '" + groupDrawingAllId + "' ','", // String objectWhere,
					"", // String
					0);

			if (fromGroupDrawingMapList.isEmpty()) {
				continue;
			}

//			MapList dealerMapList = getSOReleaseDealer(context, partDomObj, soDomObj,
//					new StringList(DomainConstants.SELECT_NAME));
			
			MapList documentForDealerMapList = getSOReleaseDocumentForDealer(context, partDomObj, soDomObj,
					new StringList(DomainConstants.SELECT_NAME));
			String documentFiles = (String) documentForDealerMapList.stream()
					.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME))
					.collect(Collectors.joining(FORCE_BREAKLINE));

			String partName = partMap.get(DomainConstants.SELECT_NAME);
			String partNameCh = partMap.get("attribute[SPLM_Name_TC]");
			String partNameEn = partMap.get("attribute[SPLM_Name_EN]");
			String partPBRemark = partMap.get("attribute[SPLM_PB_Note]");
			String partTypeKD = partMap.get("attribute[SPLM_MaterialGroup]");

			for (Object fromGroupDrawingObj : fromGroupDrawingMapList) {
				Map<String, String> GroupDrawingMap = (Map) fromGroupDrawingObj;
				String partPnc = GroupDrawingMap.get("attribute[SPLM_PNC]");
				String partStartDate = GroupDrawingMap.get("attribute[SPLM_EnableDate]");
				String partEndDate = GroupDrawingMap.get("attribute[SPLM_DisableDate]");
				String partQuantity = GroupDrawingMap.get("attribute[Quantity]");
				String partGroupCode = GroupDrawingMap.get("attribute[SPLM_GroupCode]");
				String GroupDrawingId = GroupDrawingMap.get(DomainConstants.SELECT_ID);
				String partAdaptMC =  groupDrawingMap.get(GroupDrawingId);
				
				PartBulletin partBulletin = new PartBulletin();
				partBulletin.setPart(partName);
				partBulletin.setPartName(partNameCh + FORCE_BREAKLINE + partNameEn);
				partBulletin.setDocumentFile(documentFiles);
				partBulletin.setGroupCode(partGroupCode);
				partBulletin.setPnc(partPnc);
				partBulletin.setQuantity(partQuantity);
				partBulletin.setTypeKD(partTypeKD);
				partBulletin.setStartDate(partStartDate);
				partBulletin.setEndDate(partEndDate);
				partBulletin.setPbRemark(partPBRemark);
				partBulletin.setAdaptableMS(partAdaptMC);
				partBulletinArrayList.add(partBulletin);
			}

		}
		return partBulletinArrayList;
	}

	private ArrayList<Document> getDocumentForDealerArrayList(Context context, MapList servicesPartMapList,
			DomainObject soDomObj, String fileTempFolder) throws Exception {
		StringList documentBusSelects = new StringList();
		documentBusSelects.add(DomainConstants.SELECT_NAME);
		documentBusSelects.add(DomainConstants.SELECT_ID);
		documentBusSelects.add("attribute[SPLM_DocType]");

		StringList partDealerBusSelects = new StringList();
		partDealerBusSelects.add(DomainConstants.SELECT_NAME);

		DomainObject partDomObj = new DomainObject();
		DomainObject documentDomObj = new DomainObject();

		String soName = soDomObj.getInfo(context, DomainConstants.SELECT_NAME);
		String picDocumentFolder = fileTempFolder + "\\" + soName + "_PB\\" + SO_PICLOCATION;
		File file = new File(picDocumentFolder);
		if( !file.exists() ) {
			file.mkdirs();
		}

		ContextUtil.pushContext(context);
		MqlUtil.mqlCommand(context, "trigger off");
		ArrayList<Document> documentArrayList = new ArrayList<Document>();
		try {
			for (Object partObj : servicesPartMapList) {
				Map<String, String> partMap = (Map<String, String>) partObj;
				partDomObj.setId(partMap.get(DomainConstants.SELECT_ID));

				MapList documentMapList = getSOReleaseDocumentForDealer(context, partDomObj, soDomObj,
						documentBusSelects);

				for (Object documentObj : documentMapList) {
					Map<String, String> documentMap = (Map) documentObj;

					String documentId = documentMap.get(DomainConstants.SELECT_ID);
					String documentName = documentMap.get(DomainConstants.SELECT_NAME);
					documentDomObj.setId(documentId);

					ArrayList<String> picFileArrayList = getFileLocations(context, documentDomObj, picDocumentFolder);

					if ( !documentMap.get("attribute[SPLM_DocType]").equalsIgnoreCase("Part Drawing") ) {
						continue;
					}
					
					Document document = new Document();
					document.setPicName(documentName);
					document.setPicFileLocations(picFileArrayList);
					documentArrayList.add(document);
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			MqlUtil.mqlCommand(context, "trigger on");
			ContextUtil.popContext(context);
		}
		return documentArrayList;
	}

	/////////////////////////////////////////////////////////////////////// task
	public void batchCreateSOPBExcelAndSendToFTP(Context context, String[] args) throws Exception {
		StringList busSelectSo = new StringList();
		busSelectSo.add(DomainConstants.SELECT_ID);
		busSelectSo.add(DomainConstants.SELECT_NAME);

		MapList soMapList = DomainObject.findObjects(context, "SPLM_SO", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"current=='Complete'&& attribute[SPLM_SOExcelFlag]=='Processing'", // where
				null, false, busSelectSo, (short) 0);

		System.out.println("SO size : " + soMapList.size());
		DomainObject soDomObj = new DomainObject();
		for (Object soObj : soMapList) {
			Map soMap = (Map) soObj;
			String soId = (String) soMap.get(DomainConstants.SELECT_ID);
			String soName = (String) soMap.get(DomainConstants.SELECT_NAME);
			soDomObj.setId(soId);

			System.out.println("*******************\u958b\u59cb " + soName + " Excel \u7522\u51fa*******************");
			SOPBTitle soPBTitle = getSOPBExcelTitleInfo(context, soDomObj);

			// prepare Files for restore
			String outputSoFileLocation = getTempFolderPath() + soPBTitle.getTitleName();

			System.out.println(soName + " \u6b63\u5728\u57f7\u884c\u4e2d, \u767c\u9001SO:" + soPBTitle.isReleaseSO()
					+ " ,\u767c\u9001PB:" + soPBTitle.isReleasePB());

			ArrayList<String> soFileArrayList = null;
			ArrayList<String> pbFileArrayList = null;
			ArrayList<String> soExeclFileArrayList = new ArrayList<String>();
			// Data processing
			try {
				if (soPBTitle.isReleaseSO()) {
					ArrayList<SOPublish> soPublishArrayList = getAffectedItemInfo(context, soDomObj, outputSoFileLocation);
					soFileArrayList = createSOExcel(context, soPublishArrayList, soPBTitle, outputSoFileLocation);
					if (soFileArrayList.size() == 0) {
						throw new Exception("\u6c92\u6709\u6a94\u6848\u53ef\u4ee5\u4e0b\u8f09");
					}
					soExeclFileArrayList.addAll(soFileArrayList);
				}

				if (soPBTitle.isReleasePB()) {
					ArrayList<PBPublish> pbPublishArrayList = getPartBulletinInfo(context, soDomObj, outputSoFileLocation);
					pbFileArrayList = createPBExcel(context, pbPublishArrayList, soPBTitle, outputSoFileLocation);
					if (pbFileArrayList.size() == 0) {
						throw new Exception("\u6c92\u6709\u6a94\u6848\u53ef\u4ee5\u4e0b\u8f09");
					}
					soExeclFileArrayList.addAll(pbFileArrayList);
				}
				
				MqlUtil.mqlCommand(context, "trigger off");
				this.prechekcAndUploadSoDocument(context, soDomObj, soExeclFileArrayList);
				this.uploadSoDocumentToFTP(context, soDomObj, soFileArrayList);
				SPLM_SendSOExcel_JPO_mxJPO sendSOExcel = new SPLM_SendSOExcel_JPO_mxJPO();
				sendSOExcel.soSendMailToDept(context, soId);
				this.deleteDirectoryExistFiles(outputSoFileLocation);
			} catch (Exception e) {
				System.out.println(soName + " Error :" + e.getMessage());
				soDomObj.setAttributeValue(context, "SPLM_SOExcelFlag", "Error");
			} finally {
				MqlUtil.mqlCommand(context, "trigger on");
			}

			System.out.println("*******************" + soName + " \u5df2\u57f7\u884c\u5b8c\u7562*******************");
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
			String outputSoFileName = getTempFolderPath() + soPBTitle.getTitleName();
			String outputSoFileZip = outputSoFileName + ".zip";

			// Data processing
			if (soPBTitle.isReleaseSO()) {
				ArrayList<SOPublish> soPublishArrayList = getAffectedItemInfo(context, soObj, outputSoFileName);
				ArrayList<String> soFileArrayList = createSOExcel(context, soPublishArrayList, soPBTitle,
						outputSoFileName);
				if (soFileArrayList.size() == 0) {
					throw new Exception("\u6c92\u6709\u6a94\u6848\u53ef\u4ee5\u4e0b\u8f09");
				}
			}

			if (soPBTitle.isReleasePB()) {
				ArrayList<PBPublish> pbPublishArrayList = getPartBulletinInfo(context, soObj, outputSoFileName);
				ArrayList<String> pbFileArrayList = createPBExcel(context, pbPublishArrayList, soPBTitle,
						outputSoFileName);
				if (pbFileArrayList.size() == 0) {
					throw new Exception("\u6c92\u6709\u6a94\u6848\u53ef\u4ee5\u4e0b\u8f09");
				}
			}

			this.makeZip(new File(outputSoFileName), new File(outputSoFileZip));
			this.deleteDirectoryExistFiles(outputSoFileName);
			returnMap.put("FilePath", outputSoFileZip);
		} catch (Exception e) {
			returnMap.put("Message", e.getMessage());
		}
		return returnMap;
	}

	/* <MAIN> assemble Info and publish Excel */
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
		busSelectsPart.add("physicalid"); // physicalId

		String soName = soObj.getInfo(context, DomainConstants.SELECT_NAME);

		MapList servicesPartMapList = getSOAI(context, soObj, busSelectsPart);
		if (servicesPartMapList.isEmpty()) {
			throw new Exception("\u7f3a\u5c11 \u5f71\u97ff\u4ef6\u865f");
		}

		ArrayList<SOPublish> soPublishArrayList = new ArrayList<SOPublish>();
		ArrayList<AffectedItem> affedAffectedItemArrayList = this.getAffectedItemArrayList(context, servicesPartMapList,soObj);
		ArrayList<PartBom> partBomArrayList = this.getAIPartBomArrayList(context, servicesPartMapList, soObj);
		ArrayList<Document> documentArrayList = this.getDocumentForVendorArrayList(context, servicesPartMapList, soObj,fileTempFolder);
		HashSet<String> soSendVendorSet = this.getReleaseSoSend(context, soObj, "vendor");

		for (String soSendVendor : soSendVendorSet) {
			ArrayList<AffectedItem> aiArrayList = affedAffectedItemArrayList.stream()
					.filter(obj -> soSendVendor.equals(obj.getSendVendorName()))
					.collect(Collectors.toCollection(ArrayList::new));

			ArrayList<PartBom> pbArrayList = partBomArrayList.stream()
					.filter(obj -> soSendVendor.equals(obj.getSendVendorName()))
					.collect(Collectors.toCollection(ArrayList::new));

			ArrayList<Document> docArrayList = documentArrayList.stream()
					.filter(obj -> soSendVendor.equals(obj.getSendTypeName()))
					.collect(Collectors.toCollection(ArrayList::new));

			SOPublish soPublish = new SOPublish();
			soPublish.setSendVendorName(soSendVendor);
			soPublish.setVendorAI(aiArrayList);
			soPublish.setVendorBom(pbArrayList);
			soPublish.setVendorDocument(docArrayList);
			soPublishArrayList.add(soPublish);
		}
		return soPublishArrayList;
	}

	private ArrayList<String> createSOExcel(Context context, ArrayList<SOPublish> soPublishArrayList,
			SOPBTitle soPBTitle, String outputSoFileName) throws Exception {
		String soName = soPBTitle.getTitleName();
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
				soNumberCell.setCellValue(soPBTitle.getTitleName());

				Cell soIssuerCell = row4.createCell(1);
				soIssuerCell.setCellStyle(homePageCellStyleForDelivery);
				soIssuerCell.setCellValue(soPBTitle.getTitleIssuer());

				Cell soVaultCell = row4.createCell(2);
				soVaultCell.setCellStyle(homePageCellStyleForDelivery);
				soVaultCell.setCellValue(soPBTitle.getTitleRouter());

				Cell soOwnerCell = row4.createCell(3);
				soOwnerCell.setCellStyle(homePageCellStyleForDelivery);
				soOwnerCell.setCellValue(soPBTitle.getTitleOwner());

				Cell soSendDeptCell = row4.createCell(5);
				soSendDeptCell.setCellStyle(homePageCellStyleForDelivery);
				soSendDeptCell.setCellValue(soPBTitle.getTitleSendDept());

				Cell soReleaseDateCell = row4.createCell(14);
				soReleaseDateCell.setCellStyle(homePageCellStyleForDelivery);
				soReleaseDateCell.setCellValue(soPBTitle.getTitleReleaseDate());

				/* AI */
				CellStyle homePageCellStyleForAIDetail = cloneCellStyle(homePageSheet, 12, 0);
				homePageCellStyleForAIDetail.setWrapText(true);
				int aiLocationRow = 13;
				if (!soPublish.isVendorAIEmpty()) {
					ArrayList<AffectedItem> affectedItemArrayList = soPublish.getVendorAI();
					for (int i = 0; i < affectedItemArrayList.size(); i++) {
						Row row = homePageSheet.createRow(i + aiLocationRow);
						AffectedItem affectedItem = affectedItemArrayList.get(i);

						Cell partCell = row.createCell(0);
						partCell.setCellStyle(homePageCellStyleForAIDetail);
						partCell.setCellValue(affectedItem.getPart());

						Cell affectedItemPartCell = row.createCell(1);
						affectedItemPartCell.setCellStyle(homePageCellStyleForAIDetail);
						affectedItemPartCell.setCellValue(affectedItem.getAffectedItemPart());

						Cell partNameCell = row.createCell(2);
						partNameCell.setCellStyle(homePageCellStyleForAIDetail);
						partNameCell.setCellValue(affectedItem.getPartName());

						Cell groupDrawingCell = row.createCell(3);
						groupDrawingCell.setCellStyle(homePageCellStyleForAIDetail);
						groupDrawingCell.setCellValue(affectedItem.getDocumentFile());

						Cell mediumPackingNumsCell = row.createCell(4);
						mediumPackingNumsCell.setCellStyle(homePageCellStyleForAIDetail);
						mediumPackingNumsCell.setCellValue(affectedItem.getMediumPackingNums());

						Cell unitCell = row.createCell(5);
						unitCell.setCellStyle(homePageCellStyleForAIDetail);
						unitCell.setCellValue(affectedItem.getUnit());

						Cell materialTypeCell = row.createCell(6);
						materialTypeCell.setCellStyle(homePageCellStyleForAIDetail);
						materialTypeCell.setCellValue(affectedItem.getMaterialType());

						Cell materialGroupCell = row.createCell(7);
						materialGroupCell.setCellStyle(homePageCellStyleForAIDetail);
						materialGroupCell.setCellValue(affectedItem.getMaterialGroup());

						Cell partTypeCell = row.createCell(8);
						partTypeCell.setCellStyle(homePageCellStyleForAIDetail);
						partTypeCell.setCellValue(affectedItem.getPartType());

						Cell purchasingGroupCell = row.createCell(9);
						purchasingGroupCell.setCellStyle(homePageCellStyleForAIDetail);
						purchasingGroupCell.setCellValue(affectedItem.getPurchasingGroup());

						Cell vendorCell = row.createCell(10);
						vendorCell.setCellStyle(homePageCellStyleForAIDetail);
						vendorCell.setCellValue(affectedItem.getVendorPartNo());

						Cell referenceEOCell = row.createCell(11);
						referenceEOCell.setCellStyle(homePageCellStyleForAIDetail);
						referenceEOCell.setCellValue(affectedItem.getRelatedEO());

						Cell referencePOCell = row.createCell(12);
						referencePOCell.setCellStyle(homePageCellStyleForAIDetail);
						referencePOCell.setCellValue(affectedItem.getRelatedPO());

						Cell altOptPartCell = row.createCell(13);
						altOptPartCell.setCellStyle(homePageCellStyleForAIDetail);
						altOptPartCell.setCellValue(affectedItem.getAltOptPart());

						Cell acNoCell = row.createCell(14);
						acNoCell.setCellStyle(homePageCellStyleForAIDetail);
						acNoCell.setCellValue(affectedItem.getAcNo());

						Cell remarkCell = row.createCell(15);
						remarkCell.setCellStyle(homePageCellStyleForAIDetail);
						remarkCell.setCellValue(affectedItem.getRemark());
					}
				}

				// BOM
				Sheet BOMSheet = soWorkBook.getSheet("BOM");
				CellStyle BOMCellStyleForSBOM = cloneCellStyle(BOMSheet, 1, 0);
				BOMCellStyleForSBOM.setWrapText(true);
				int sbomRowLocation = 2;
				if (!soPublish.isVendorBomEmpty()) {
					ArrayList<PartBom> partBomArrayList = soPublish.getVendorBom();
					for (int i = 0; i < partBomArrayList.size(); i++) {
						PartBom partBom = partBomArrayList.get(i);
						Row row = BOMSheet.createRow(i + sbomRowLocation);

						Cell partCell = row.createCell(0);
						partCell.setCellStyle(BOMCellStyleForSBOM);
						partCell.setCellValue(partBom.getPart());

						Cell subPartCell = row.createCell(1);
						subPartCell.setCellStyle(BOMCellStyleForSBOM);
						subPartCell.setCellValue(partBom.getSubPart());

						Cell quantityCell = row.createCell(2);
						quantityCell.setCellStyle(BOMCellStyleForSBOM);
						quantityCell.setCellValue(partBom.getQuantity());

						Cell groupCell = row.createCell(3);
						groupCell.setCellStyle(BOMCellStyleForSBOM);
						groupCell.setCellValue(partBom.getGroupCode());

						Cell pncCell = row.createCell(4);
						pncCell.setCellStyle(BOMCellStyleForSBOM);
						pncCell.setCellValue(partBom.getPnc());

						Cell KDTypeCell = row.createCell(5);
						KDTypeCell.setCellStyle(BOMCellStyleForSBOM);
						KDTypeCell.setCellValue(partBom.getTypeKD());

						Cell subVendorStarCell = row.createCell(6);
						subVendorStarCell.setCellStyle(BOMCellStyleForSBOM);
						subVendorStarCell.setCellValue(partBom.getSubPartVendorStar());

						Cell startDateCell = row.createCell(7);
						startDateCell.setCellStyle(BOMCellStyleForSBOM);
						startDateCell.setCellValue(partBom.getStartDate());

						Cell endDateCell = row.createCell(8);
						endDateCell.setCellStyle(BOMCellStyleForSBOM);
						endDateCell.setCellValue(partBom.getEndDate());

						Cell bomRemarkCell = row.createCell(9);
						bomRemarkCell.setCellStyle(BOMCellStyleForSBOM);
						bomRemarkCell.setCellValue(partBom.getBomRemark());
					}
				}

				/* past normal/ZIP Picture */
				if (!soPublish.isVendorDocumentEmpty()) {
					ArrayList<Document> documentArrayList = soPublish.getVendorDocument();
					for (Document document : documentArrayList) {
						XSSFClientAnchor preImage = null;
						Sheet picSheet = soWorkBook.createSheet(document.getPicName());
						XSSFDrawing drawing = (XSSFDrawing) picSheet.createDrawingPatriarch();
						for (String picLocation : document.getPicFileLocations()) {
							preImage = addImage(soWorkBook, drawing, picLocation, preImage);
						}
					}
				}

				protectAllSheets(soWorkBook);
				String soVendorFile = outputSoFileName + "\\" + soName + "_" + soPublish.getSendVendorName();
				String picTempFile = soVendorFile + "\\" + SO_PICLOCATION;
				String excelFile = soVendorFile + "\\" + soName + "_" + soPublish.getSendVendorName() + ".xlsx";
				String soVendorZip = soVendorFile + ".zip";
				
				this.output(soWorkBook, excelFile);
				this.makeZip(new File(soVendorFile), new File(soVendorZip));
				this.deleteDirectoryExistFiles(picTempFile);
				this.deleteDirectoryExistFiles(soVendorFile);
				
				soFileArrayList.add(soVendorZip);
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
		busSelectsPart.add("attribute[SPLM_PB_Note]"); // remark
		busSelectsPart.add("attribute[SPLM_MaterialGroup]"); // KD

		String soName = soObj.getInfo(context, DomainConstants.SELECT_NAME);
		MapList servicesPartMapList = getSOAI(context, soObj, busSelectsPart);
		if (servicesPartMapList.isEmpty()) {
			throw new Exception("\u7f3a\u5c11 \u5f71\u97ff\u4ef6\u865f");
		}

		ArrayList<PBPublish> pbPublishArrayList = new ArrayList<PBPublish>();

		ArrayList<PartBulletin> partBullentinArrayList = this.getPartBulletinArrayList(context, servicesPartMapList,
				soObj);
		ArrayList<Document> documentArrayList = this.getDocumentForDealerArrayList(context, servicesPartMapList, soObj,
				fileTempFolder);
		
//		HashSet<String> soSendDealerSet = this.getReleaseSoSend(context, soObj, "dealer");
//		for (String soSendDealer : soSendDealerSet) {
//			ArrayList<PartBulletin> pbArrayList = partBullentinArrayList.stream()
//					.filter(obj -> soSendDealer.equals(obj.getSendDealerName()))
//					.collect(Collectors.toCollection(ArrayList::new));
//
//			ArrayList<Document> docArrayList = documentArrayList.stream()
//					.filter(obj -> soSendDealer.equals(obj.getSendTypeName()))
//					.collect(Collectors.toCollection(ArrayList::new));

		PBPublish pbPublish = new PBPublish();
		pbPublish.setDealerPB(partBullentinArrayList);
		pbPublish.setDealerDocument(documentArrayList);
		pbPublishArrayList.add(pbPublish);
		return pbPublishArrayList;
	}

	private ArrayList<String> createPBExcel(Context context, ArrayList<PBPublish> pbPublishArrayList,
			SOPBTitle soPBTitle, String outputSoFileName) throws Exception {
		String soName = soPBTitle.getTitleName();
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
				soNumberCell.setCellValue(soPBTitle.getTitleName());

				Cell soIssuerCell = row4.createCell(1);
				soIssuerCell.setCellStyle(homePageCellStyleForDelivery);
				soIssuerCell.setCellValue(soPBTitle.getTitleIssuer());

				Cell soVaultCell = row4.createCell(3);
				soVaultCell.setCellStyle(homePageCellStyleForDelivery);
				soVaultCell.setCellValue(soPBTitle.getTitleRouter());

				Cell soOwnerCell = row4.createCell(6);
				soOwnerCell.setCellStyle(homePageCellStyleForDelivery);
				soOwnerCell.setCellValue(soPBTitle.getTitleOwner());

				Cell soSendDeptCell = row4.createCell(8);
				soSendDeptCell.setCellStyle(homePageCellStyleForDelivery);
				soSendDeptCell.setCellValue(soPBTitle.getTitleSendDept());

				Cell soReleaseDateCell = row4.createCell(13);
				soReleaseDateCell.setCellStyle(homePageCellStyleForDelivery);
				soReleaseDateCell.setCellValue(soPBTitle.getTitleReleaseDate());

				/* PB */
				CellStyle homePageCellStyleForPBDetail = cloneCellStyle(homePageSheet, 9, 0);
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
							.setCellValue(dealerDetailCell.getStringCellValue().replace(" ", ""));

					for (int i = 0; i < partBullentinLength; i++) {
						Row row = homePageSheet.createRow(i + aiLocationRow);
						PartBulletin partBulletin = partBullentinArrayList.get(i);

						Cell partCell = row.createCell(0);
						partCell.setCellStyle(homePageCellStyleForPBDetail);
						partCell.setCellValue(partBulletin.getPart());

						Cell partNameCell = row.createCell(1);
						partNameCell.setCellStyle(homePageCellStyleForPBDetail);
						partNameCell.setCellValue(partBulletin.getPartName());

						Cell nullCell = row.createCell(2);
						nullCell.setCellStyle(homePageCellStyleForPBDetail);

						Cell groupDrawingCell = row.createCell(3);
						groupDrawingCell.setCellStyle(homePageCellStyleForPBDetail);
						groupDrawingCell.setCellValue(partBulletin.getDocumentFile());

						Cell groupCodeCell = row.createCell(4);
						groupCodeCell.setCellStyle(homePageCellStyleForPBDetail);
						groupCodeCell.setCellValue(partBulletin.getGroupCode());

						Cell pncCell = row.createCell(5);
						pncCell.setCellStyle(homePageCellStyleForPBDetail);
						pncCell.setCellValue(partBulletin.getPnc());

						Cell quantityCell = row.createCell(6);
						quantityCell.setCellStyle(homePageCellStyleForPBDetail);
						quantityCell.setCellValue(partBulletin.getQuantity());

						Cell KDTypeCell = row.createCell(7);
						KDTypeCell.setCellStyle(homePageCellStyleForPBDetail);
						KDTypeCell.setCellValue(partBulletin.getTypeKD());

						Cell startDateCell = row.createCell(8);
						startDateCell.setCellStyle(homePageCellStyleForPBDetail);
						startDateCell.setCellValue(partBulletin.getStartDate());

						Cell endDateCell = row.createCell(9);
						endDateCell.setCellStyle(homePageCellStyleForPBDetail);
						endDateCell.setCellValue(partBulletin.getEndDate());

						Cell bomRemarkCell = row.createCell(10);
						bomRemarkCell.setCellStyle(homePageCellStyleForPBDetail);
						bomRemarkCell.setCellValue(partBulletin.getPbRemark());

						Cell adaptMSCell = row.createCell(11);
						adaptMSCell.setCellStyle(homePageCellStyleForPBDetail);
						adaptMSCell.setCellValue(partBulletin.getAdaptableMS());

						Cell nullCell1 = row.createCell(12);
						nullCell1.setCellStyle(homePageCellStyleForPBDetail);
						Cell nullCell2 = row.createCell(13);
						nullCell2.setCellStyle(homePageCellStyleForPBDetail);

					}
				}

				/* past normal/ZIP Picture */
				if (!pbPublish.isDealerDocumentEmpty()) {
					ArrayList<Document> documentArrayList = pbPublish.getDealerDocument();
					for (Document document : documentArrayList) {
						XSSFClientAnchor preImage = null;
						Sheet picSheet = pbWorkBook.createSheet(document.getPicName());
						XSSFDrawing drawing = (XSSFDrawing) picSheet.createDrawingPatriarch();
						for (String picLocation : document.getPicFileLocations()) {
							preImage = addImage(pbWorkBook, drawing, picLocation, preImage);
						}
					}
				}

				protectAllSheets(pbWorkBook);
				
				String soDealerFile = outputSoFileName + "\\" + soName + "_PB";
				String picTempFile = soDealerFile + "\\" + SO_PICLOCATION;
				String excelFile = soDealerFile + "\\" + soName + "_PB.xlsx";
				String soDealerZip = soDealerFile + ".zip";
				
				this.output(pbWorkBook, excelFile);
				this.makeZip(new File(soDealerFile), new File(soDealerZip));
				this.deleteDirectoryExistFiles(picTempFile);
				this.deleteDirectoryExistFiles(soDealerFile);

				pbFileArrayList.add(soDealerZip);
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

	/* --- get other Info for more data --- */
	private SOPBTitle getSOPBExcelTitleInfo(Context context, DomainObject soObj) throws Exception {
		String soOwner = (String) soObj.getInfo(context, DomainConstants.SELECT_OWNER);
		String soReleaseDate = soObj.getInfo(context, "state[Complete].actual");
		String soRoute = "";
		soReleaseDate = soReleaseDate.isEmpty() ? "" : convertDateFormat(soReleaseDate, "yyyyMMdd");

		Map<String, String> soSendDeptInfo = getSoSendDeptInfo(context);
		ArrayList<String> soSendDeptArrayList = new ArrayList<String>();
		for (String sendDeptStr : soObj.getAttributeValue(context, "SPLM_SendDept").split("~QWE~")) {
			soSendDeptArrayList.add(soSendDeptInfo.get(sendDeptStr));
		}
		
		
		MapList mapList = soObj.getRelatedObjects(context, "Object Route", // relationshipPattern,
				"Route", // typePattern,
//				new StringList(DomainConstants.SELECT_OWNER), // StringList objectSelects,
				new StringList(DomainConstants.SELECT_ID), // StringList objectSelects,
				new StringList(DomainRelationship.SELECT_ORIGINATED), // StringList relationshipSelects,
				false, // boolean getTo,
				true, // boolean getFrom,
				(short) 1, // short recurseToLevel,
				"", // String objectWhere,
				"", // String relationshipWhere
				0); // int limit)
		
		if( !mapList.isEmpty() ) {
			mapList.addSortKey(DomainRelationship.SELECT_ORIGINATED, "descending", "date");
			mapList.sort();
//			String latestVault = (String) ((Map) mapList.get(0)).get(DomainConstants.SELECT_OWNER);
			String latestRouteId = (String) ((Map) mapList.get(0)).get(DomainConstants.SELECT_ID);
//			soRoute = Person.getPerson(context, latestVault).getInfo(context, Person.SELECT_FIRST_NAME);
			
			MapList RouteMapList = new DomainObject(latestRouteId).getRelatedObjects(context, "Route Node", // relationshipPattern,
					"Person", // typePattern,
					new StringList(Person.SELECT_FIRST_NAME), // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)
			soRoute = (String) ((Map) RouteMapList.get(0)).get(Person.SELECT_FIRST_NAME);
		}
		
		SOPBTitle soPBTitle = new SOPBTitle();
		soPBTitle.setTitleName(soObj.getInfo(context, DomainConstants.SELECT_NAME));
		soPBTitle.setTitleIssuer("\u6559\u80b2\u8a13\u7df4\u8cc7\u6599\u7d44");
		soPBTitle.setTitleRouter(soRoute);
		soPBTitle.setTitleOwner(Person.getPerson(context, soOwner).getInfo(context, Person.SELECT_FIRST_NAME));
		soPBTitle.setTitleSendDept(String.join(",", soSendDeptArrayList));
		soPBTitle.setTitleReleaseDate(soReleaseDate);
		soPBTitle.setReleaseSO(Boolean.valueOf(soObj.getAttributeValue(context, "SPLM_ReleaseSO")));
		soPBTitle.setReleasePB(Boolean.valueOf(soObj.getAttributeValue(context, "SPLM_ReleasePB")));

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

	private HashSet<String> getReleaseSoSend(Context context, DomainObject soObj, String trigger) throws Exception {
		trigger = trigger.equalsIgnoreCase("vendor") ? "SPLM_Vendor" : trigger.equalsIgnoreCase("dealer") ? "SPLM_Dealer" : "";
		if (trigger.isEmpty()) {
			throw new Exception("Please type vendor / dealer for trigger");
		}
		MapList adaptMCMapList = soObj.getRelatedObjects(context, "SPLM_AffectedItem,SPLM_RelatedSOReleaseItem", // relationshipPattern,
				"*", // typePattern,
				new StringList(DomainConstants.SELECT_NAME), // StringList objectSelects,
				null, // StringList relationshipSelects,
				false, // boolean getTo,
				true, // boolean getFrom,
				(short) 2, // short recurseToLevel,
				"", // String objectWhere,
				"", // String
				0, new Pattern(trigger), new Pattern("SPLM_RelatedSOReleaseItem"), null);

		return (HashSet<String>) adaptMCMapList.stream()
				.map(obj -> (String) ((Map) obj).get(DomainConstants.SELECT_NAME))
				.collect(Collectors.toCollection(HashSet::new));
	}

	private ArrayList<String> getFileLocations(Context context, DomainObject documentDomObj, String picDocumentFolder)
			throws Exception {
		ArrayList<String> picFileArrayList = new ArrayList<String>();
		for (matrix.db.File documentFile : documentDomObj.getFiles(context)) {
			String fileName = documentFile.getName();
			String fileLocation = picDocumentFolder + "\\" + fileName;
			documentDomObj.checkoutFile(context, false, documentFile.getFormat(), fileName, picDocumentFolder);
			if (!isPictureFile(fileLocation)) {
				continue;
			}
			picFileArrayList.add(fileLocation);
		}
		return picFileArrayList;
	}

	/* --- Class --- */
	class SOPublish {
		private String sendVendorName;
		private ArrayList<AffectedItem> vendorAI;
		private ArrayList<PartBom> vendorBom;
		private ArrayList<Document> vendorDocument;

		public String getSendVendorName() {
			return sendVendorName;
		}

		public void setSendVendorName(String sendVendorName) {
			this.sendVendorName = sendVendorName;
		}

		public ArrayList<AffectedItem> getVendorAI() {
			return vendorAI;
		}

		public void setVendorAI(ArrayList<AffectedItem> vendorAI) {
			this.vendorAI = vendorAI;
		}

		public ArrayList<PartBom> getVendorBom() {
			return vendorBom;
		}

		public void setVendorBom(ArrayList<PartBom> vendorBom) {
			this.vendorBom = vendorBom;
		}

		public ArrayList<Document> getVendorDocument() {
			return vendorDocument;
		}

		public void setVendorDocument(ArrayList<Document> vendorDocument) {
			this.vendorDocument = vendorDocument;
		}

		public boolean isVendorNameEmpty() {
			return !Optional.ofNullable(this.sendVendorName).isPresent();
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

	class AffectedItem {
		private String sendVendorName;
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
		private String vendorPartNo;
		private String relatedEO;
		private String relatedPO;
		private String altOptPart;
		private String acNo;
		private String remark;

		public String getSendVendorName() {
			return sendVendorName;
		}

		public void setSendVendorName(String sendVendorName) {
			this.sendVendorName = sendVendorName;
		}

		public String getPart() {
			return part;
		}

		public void setPart(String part) {
			this.part = part;
		}

		public String getAffectedItemPart() {
			return affectedItemPart;
		}

		public void setAffectedItemPart(String affectedItemPart) {
			this.affectedItemPart = affectedItemPart;
		}

		public String getPartName() {
			return partName;
		}

		public void setPartName(String partName) {
			this.partName = partName;
		}

		public String getDocumentFile() {
			return documentFile;
		}

		public void setDocumentFile(String documentFile) {
			this.documentFile = documentFile;
		}

		public String getMediumPackingNums() {
			return mediumPackingNums;
		}

		public void setMediumPackingNums(String mediumPackingNums) {
			this.mediumPackingNums = mediumPackingNums;
		}

		public String getUnit() {
			return unit;
		}

		public void setUnit(String unit) {
			this.unit = unit;
		}

		public String getMaterialType() {
			return materialType;
		}

		public void setMaterialType(String materialType) {
			this.materialType = materialType;
		}

		public String getMaterialGroup() {
			return materialGroup;
		}

		public void setMaterialGroup(String materialGroup) {
			this.materialGroup = materialGroup;
		}

		public String getPartType() {
			return partType;
		}

		public void setPartType(String partType) throws Exception {
			this.partType = transPartType(partType);
		}

		public String getPurchasingGroup() {
			return purchasingGroup;
		}

		public void setPurchasingGroup(String purchasingGroup) {
			this.purchasingGroup = purchasingGroup;
		}

		public String getVendorPartNo() {
			return vendorPartNo;
		}

		public void setVendorPartNo(String vendorPartNo) {
			this.vendorPartNo = vendorPartNo;
		}

		public String getRelatedEO() {
			return relatedEO;
		}

		public void setRelatedEO(String relatedEO) {
			this.relatedEO = relatedEO;
		}

		public String getRelatedPO() {
			return relatedPO;
		}

		public void setRelatedPO(String relatedPO) {
			this.relatedPO = relatedPO;
		}

		public String getAltOptPart() {
			return altOptPart;
		}

		public void setAltOptPart(String altOptPart) {
			this.altOptPart = altOptPart;
		}

		public String getAcNo() {
			return acNo;
		}

		public void setAcNo(String acNo) {
			this.acNo = acNo;
		}

		public String getRemark() {
			return remark;
		}

		public void setRemark(String remark) {
			this.remark = remark;
		}

		private String transPartType(String partType) throws Exception {
			return i18nNow.getRangeI18NString("SPLM_ItemSubType", partType, "zh");
		}
	}

	class PartBom {
		private String sendVendorName;
		private String part;
		private String subPart;
		private String quantity;
		private String groupCode;
		private String pnc;
		private String typeKD;
		private String subPartVendorStar;
		private String startDate;
		private String endDate;
		private String bomRemark;

		public String getSendVendorName() {
			return sendVendorName;
		}

		public void setSendVendorName(String sendVendorName) {
			this.sendVendorName = sendVendorName;
		}

		public String getPart() {
			return part;
		}

		public void setPart(String part) {
			this.part = part;
		}

		public String getSubPart() {
			return subPart;
		}

		public void setSubPart(String subPart) {
			this.subPart = subPart;
		}

		public String getQuantity() {
			return quantity;
		}

		public void setQuantity(String quantity) {
			this.quantity = quantity;
		}

		public String getGroupCode() {
			return groupCode;
		}

		public void setGroupCode(String groupCode) {
			this.groupCode = groupCode;
		}

		public String getPnc() {
			return pnc;
		}

		public void setPnc(String pnc) {
			this.pnc = pnc;
		}

		public String getTypeKD() {
			return typeKD;
		}

		public void setTypeKD(String typeKD) {
			this.typeKD = transTypeKD(typeKD);
		}

		public String getSubPartVendorStar() {
			return subPartVendorStar;
		}

		public void setSubPartVendorStar(String subPartVendorStar) {
			this.subPartVendorStar = subPartVendorStar;
		}

		public String getStartDate() {
			return startDate;
		}

		public void setStartDate(String startDate) {
			this.startDate = transDate(startDate);
		}

		public String getEndDate() {
			return endDate;
		}

		public void setEndDate(String endDate) {
			this.endDate = transDate(endDate);
		}

		public String getBomRemark() {
			return bomRemark;
		}

		public void setBomRemark(String bomRemark) {
			this.bomRemark = bomRemark;
		}

		private String transTypeKD(String typeKD) {
			return typeKD.equalsIgnoreCase("*-") ? "" : typeKD;
		}

		private String transDate(String date) {
			return date.isEmpty() ? "" : convertDateFormat(date, "yyyyMMdd");
		}
	}

	class Document {
		private String sendTypeName;
		private String picName;
		private ArrayList<String> picFileLocations;

		public String getSendTypeName() {
			return sendTypeName;
		}

		public void setSendTypeName(String sendVendorName) {
			this.sendTypeName = sendVendorName;
		}

		public String getPicName() {
			return picName;
		}

		public void setPicName(String picName) {
			this.picName = picName;
		}

		public ArrayList<String> getPicFileLocations() {
			return picFileLocations;
		}

		public void setPicFileLocations(ArrayList<String> picFileLocations) {
			this.picFileLocations = picFileLocations;
		}
	}

	class PBPublish {
		private ArrayList<PartBulletin> dealerPB;
		private ArrayList<Document> dealerDocument;

		public ArrayList<PartBulletin> getDealerPB() {
			return dealerPB;
		}

		public void setDealerPB(ArrayList<PartBulletin> dealerPB) {
			this.dealerPB = dealerPB;
		}

		public ArrayList<Document> getDealerDocument() {
			return dealerDocument;
		}

		public void setDealerDocument(ArrayList<Document> dealerDocument) {
			this.dealerDocument = dealerDocument;
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

		public String getPart() {
			return part;
		}

		public void setPart(String part) {
			this.part = part;
		}

		public String getPartName() {
			return partName;
		}

		public void setPartName(String partName) {
			this.partName = partName;
		}

		public String getDocumentFile() {
			return documentFile;
		}

		public void setDocumentFile(String documentFile) {
			this.documentFile = documentFile;
		}

		public String getGroupCode() {
			return groupCode;
		}

		public void setGroupCode(String groupCode) {
			this.groupCode = groupCode;
		}

		public String getPnc() {
			return pnc;
		}

		public void setPnc(String pnc) {
			this.pnc = pnc;
		}

		public String getQuantity() {
			return quantity;
		}

		public void setQuantity(String quantity) {
			this.quantity = quantity;
		}

		public String getTypeKD() {
			return typeKD;
		}

		public void setTypeKD(String materialGroup) {
			this.typeKD = transKDType(materialGroup);
		}

		public String getStartDate() {
			return startDate;
		}

		public void setStartDate(String startDate) {
			this.startDate = transDateFormat(startDate);
		}

		public String getEndDate() {
			return endDate;
		}

		public void setEndDate(String endDate) {
			this.endDate = transDateFormat(endDate);
		}

		public String getPbRemark() {
			return pbRemark;
		}

		public void setPbRemark(String pbRemark) {
			this.pbRemark = pbRemark;
		}

		public String getAdaptableMS() {
			return adaptableMS;
		}

		public void setAdaptableMS(String adaptableMS) {
			this.adaptableMS = adaptableMS;
		}

		public String transKDType(String materialGroup) {
			return (materialGroup.equalsIgnoreCase("KD") || materialGroup.equalsIgnoreCase("KDY")) ? "K" : "D";
		}
		
		public String transDateFormat(String Date) {
			return Date.isEmpty()? "": convertDateFormat(Date, "yyyyMMdd");
		}
	}

	class SOPBTitle {
		private String titleName;
		private String titleIssuer;
		private String titleRouter;
		private String titleOwner;
		private String titleSendDept;
		private String titleReleaseDate;
		private boolean releaseSO;
		private boolean releasePB;

		public String getTitleName() {
			return titleName;
		}

		public void setTitleName(String titleName) {
			this.titleName = titleName;
		}

		public String getTitleIssuer() {
			return titleIssuer;
		}

		public void setTitleIssuer(String titleIssuer) {
			this.titleIssuer = titleIssuer;
		}

		public String getTitleRouter() {
			return titleRouter;
		}

		public void setTitleRouter(String titleRouter) {
			this.titleRouter = titleRouter;
		}

		public String getTitleOwner() {
			return titleOwner;
		}

		public void setTitleOwner(String titleOwner) {
			this.titleOwner = titleOwner;
		}

		public String getTitleSendDept() {
			return titleSendDept;
		}

		public void setTitleSendDept(String titleSendDept) {
			this.titleSendDept = titleSendDept;
		}

		public String getTitleReleaseDate() {
			return titleReleaseDate;
		}

		public void setTitleReleaseDate(String titleReleaseDate) {
			this.titleReleaseDate = titleReleaseDate;
		}

		public boolean isReleaseSO() {
			return releaseSO;
		}

		public void setReleaseSO(boolean releaseSO) {
			this.releaseSO = releaseSO;
		}

		public boolean isReleasePB() {
			return releasePB;
		}

		public void setReleasePB(boolean releasePB) {
			this.releasePB = releasePB;
		}
	}
}