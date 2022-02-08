import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.i18nNow;

import matrix.db.Context;
import matrix.util.StringList;

public class SPLM_ImageRecognition_JPO_mxJPO {
	
	// java called python for Image recognition
	private static final String PICTURES_PATH = "C:\\TEMP\\Morga\\python\\Pic";
	private final String exe = "python"; // 編譯器
	private final String command = "C:\\TEMP\\Morga\\python\\CMC_Recognize_V99.py";
	
	// java connect oracle with JDBC setting
	// This is for juichi 10.0.0.24 enviroment
	// private final String EPC_DB_URL = "jdbc:oracle:thin:@10.0.0.24:1521/cmc3dxdb";
	// private final String EPC_DB_USERNAME = "EPC";
	// private final String EPC_DB_PASSWORD = "Qwerty12345";
	
	// java connect oracle with JDBC setting
	// This is for CMC 172.20.24.91 enviroment
	// private final String EPC_DB_URL = "jdbc:oracle:thin:@172.20.24.91:1521/plmtst";
	// private final String EPC_DB_USERNAME = "EPC";
	// private final String EPC_DB_PASSWORD = "splm@CMC2021";
	
	private final String PLM_ENV = System.getenv("PLM_ENV");
	private final String EPC_DB_IP = "EPC.DB.IP";
	private final String EPC_DB_PORT = "EPC.DB.Port";
	private final String EPC_DB_SID = "EPC.DB.SID";
	private final String EPC_DB_USER = "EPC.DB.User";
	private final String EPC_DB_PASSWORD = "EPC.DB.Password";
	
	/* DB connect */
	private String getERC_DB_URL() throws Exception {
		return "jdbc:oracle:thin:@" + getProperty(EPC_DB_IP)+ ":" + getProperty(EPC_DB_PORT) + "/" + getProperty(EPC_DB_SID);
	}

	private String getERC_DB_USER() throws Exception {
		return getProperty(EPC_DB_USER);
	}

	private String getERC_DB_PASSWORD() throws Exception {
		return getProperty(EPC_DB_PASSWORD);
	}
	
	private String getProperty(String keyStr) throws Exception {
		if (!"PRD,UAT".contains(PLM_ENV)) {
			throw new IllegalArgumentException(PLM_ENV + " is not PRD / UAT Enviroment");
		}
		String url = i18nNow.getI18nString(keyStr + "." + PLM_ENV, "emxSPLM", "");
		if (url.equals(keyStr + "." + PLM_ENV)) {
			throw new IllegalArgumentException("emxSPLM Property not found : " + keyStr + "." + PLM_ENV);
		}
		return url;
	}
	
	/*--------------------------------------------------*/
	/**
	 * Expand Image and Gain bus Image to CheckoutFile with format generic
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public void scanGroupDrawing(Context context, String[] args) throws Exception {
		
		File picDownloadTempFile = new File(PICTURES_PATH);
		if (picDownloadTempFile.exists() != true) {
			picDownloadTempFile.mkdirs();
		}

		StringList relSelects = new StringList();
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID); // "id"
		busSelects.add(DomainConstants.SELECT_NAME); // "name"
		busSelects.add(DomainConstants.SELECT_REVISION); // "revision"
		busSelects.add(DomainConstants.SELECT_FORMAT_GENERIC); // "format[generic].name"

		// 1. find groupDrawingObj which has Scanning or null for
		// attribute[SPLM_ScanStatus]
		MapList groupDrawingMapList = DomainObject.findObjects(context, "SPLM_GroupDrawing", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"attribute[SPLM_ScanStatus]=='' or attribute[SPLM_ScanStatus]=='Scanning'", // where
				null, false, busSelects, (short) 0);

		Iterator groupDrawingItr = groupDrawingMapList.iterator();
		DomainObject groupDrawingDomObj = new DomainObject();
		while (groupDrawingItr.hasNext()) {
			Map groupDrawingMap = (Map) groupDrawingItr.next();
			String groupDrawingId = (String) groupDrawingMap.get(DomainConstants.SELECT_ID);
			String groupDrawingName = (String) groupDrawingMap.get(DomainConstants.SELECT_NAME);
			String groupDrawingRevision = (String) groupDrawingMap.get(DomainConstants.SELECT_REVISION);
			groupDrawingDomObj.setId(groupDrawingId);

			// 2. download each groupDrawing which has Image picture on it
			MapList imageMapList = groupDrawingDomObj.getRelatedObjects(context, "SPLM_GroupDrawingImage", // relationshipPattern,
					"Image", // typePattern,
					busSelects, // StringList objectSelects,
					relSelects, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					1); // int limit)
		
			if(imageMapList.size() < 1) {
				continue;
			}

			// checkout file and doing image recognize in same time / each picture
			DomainObject imageDomObj = new DomainObject();
			Iterator imageItr = imageMapList.iterator();
			while (imageItr.hasNext()) {
				ArrayList<PncCoordinates> pncCoordinatesArray = new ArrayList<PncCoordinates>();
				Map<String, String> imageMap = (Map) imageItr.next();
				String imageName = imageMap.get(DomainConstants.SELECT_NAME);
				String imageId = imageMap.get(DomainConstants.SELECT_ID);
				String imageGeneric = imageMap.get(DomainConstants.SELECT_FORMAT_GENERIC);

				if (!imageGeneric.isEmpty()) {
					System.out.println("imageName : " + imageName + " exist. Downloading now.");
					imageDomObj.setId(imageId);
					
					// download Picture
					MqlUtil.mqlCommand(context, "trigger off");
					imageDomObj.checkoutFile(context, false, "generic", imageGeneric, PICTURES_PATH);
					MqlUtil.mqlCommand(context, "trigger on");

					// 3. image recognize
					pncCoordinatesArray = this.run(context, imageGeneric);
					// 4. upload PNC coordinates data
					this.uploadPncCoordinatesToEPC(pncCoordinatesArray, groupDrawingName, groupDrawingRevision);
				}

				// 5. connect groupDrawing / pnc with relationship[SPLM_Related_PNC]
				StringList pncList = groupDrawingDomObj.getInfoList(context, "from[SPLM_Related_PNC].to.id");
				for (PncCoordinates pncCoordinates : pncCoordinatesArray) {
					String pnc = pncCoordinates.pnc;
					String pncId = pncCoordinates.pncId;
					if (pncList.contains(pncId)) {
						continue;
					}

					System.out.println("GroupDrawing Name: " + groupDrawingName + " ,pnc id : " + pncId + " ,pnc : " + pnc);
					DomainRelationship.connect(context, groupDrawingDomObj, "SPLM_Related_PNC", new DomainObject(pncId));
					pncList.add(pncId);
				}
				System.out.println("PNC / GroupDrawing relationship[SPLM_RelatedGroupDrawing] Connect succeed.");
			}
			
			// 6. change groupDrawing attribute[ScanStatus] to ScanComplete
			groupDrawingDomObj.setAttributeValue(context, "SPLM_ScanStatus", "ScanComplete");
			System.out.println("SPLM_GroupDrawing.attribute[SPLM_ScanStatus] change succeed.");
			System.out.println();
		}

	}

	/**
	 * parse picture and gain datas with ArrayList
	 * 
	 * @param context
	 * @param imageGeneric
	 * @return
	 * @throws Exception
	 */
	private ArrayList<PncCoordinates> run(Context context, String imageGeneric) throws Exception {
		String picturePath = PICTURES_PATH + "\\" + imageGeneric;
		Gson gson = new Gson();
		ArrayList<PncCoordinates> pncArrayList = new ArrayList<PncCoordinates>();

		// open picture file / each one
		File file = new File(picturePath);
		if (!file.isFile() || !file.exists()) {
			System.out.println("Can't find " + picturePath + " in folder！");
			return pncArrayList;
		}

		// recognize and get from json to string type back
		String imageRecognizeStr = imageRecognition(picturePath);
		if (!imageRecognizeStr.isEmpty()) {
			
			StringList busSelects = new StringList(DomainConstants.SELECT_ID);
			ArrayList<PncCoordinates> imageRecognizeDatas = gson.fromJson(imageRecognizeStr,
					new TypeToken<ArrayList<PncCoordinates>>() {}.getType());
			
			// restore existed pnc into new ArrayList 
			for (PncCoordinates pncData : imageRecognizeDatas) {

				String pncName = pncData.pnc;
				MapList pncExistsList = DomainObject.findObjects(context, "SPLM_PNC", // type
						pncName, // name
						"-", // revision
						"*", // owner
						"eService Production", // vault
						"", // where
						null, false, busSelects, (short) 0);

				if (pncExistsList.isEmpty()) {
					continue;
				}
				
				pncData.pncId = (String) ((Map) pncExistsList.get(0)).get(DomainConstants.SELECT_ID);
				pncArrayList.add(pncData);
			}
		} else {
			System.out.println(picturePath + " can't recognize any pnc.");
		}

		if (file.delete()) {
			System.out.println("Delete " + file.toString() + " successful.");
		}

		return pncArrayList;
	}

	/**
	 * Call python and retrun result back to package datas.
	 * 
	 * @param picturePath
	 * @return sb.toString()
	 */
	private String imageRecognition(String picturePath) {
		Process proc = null;
		BufferedReader br = null;
		String line = null;
		StringBuffer sb = new StringBuffer();
		String[] cmdImportData = new String[] { exe, command, picturePath };
		
		try {
			proc = Runtime.getRuntime().exec(cmdImportData);
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (Exception e) {
			System.out.println("Method imageRecognition<Part1> : " + e.getMessage());
		} finally {
			try {
				br.close();
			} catch (IOException e2) {
				System.out.println("Method imageRecognition<Part1> : " + e2.getMessage());
			}
			proc.destroy();
		}

		return sb.toString();
	}

	/**
	 * upload PNC to table EPC_PNC_COORDINATES
	 * 
	 * @param datas
	 */
	private void uploadPncCoordinatesToEPC(ArrayList<PncCoordinates> pncArrayLists, String groupDrawingName, String groupDrawingRevision) {
		System.out.println("Oracle Data Uploading...");
		Connection connection = null;
		Statement statement = null;
		ResultSet result = null;
		PreparedStatement pStatement = null;
		
		try {
			connection = DriverManager.getConnection(getERC_DB_URL(), getERC_DB_USER(), getERC_DB_PASSWORD());
			connection.setAutoCommit(false);
			String sqlString = "INSERT INTO EPC_PNC_COORDINATES(GROUP_DRAWING_NO,PNC_NO,X_AXIS,Y_AXIS,X_LENGTH,Y_LENGTH,GROUP_DRAWING_REV)"
					+ " Select ?, ?, ?, ?, ?, ?, ? from dual"
					+ " where not exists (select * from EPC_PNC_COORDINATES where PNC_NO = ? and GROUP_DRAWING_NO =? and GROUP_DRAWING_REV = ? and X_AXIS = ?  and Y_AXIS = ? )";
			pStatement = connection.prepareStatement(sqlString);

			for (PncCoordinates pncObj : pncArrayLists) {
				String pncNams = pncObj.pnc; // PNC_NO
				int x = pncObj.x; // X_AXIS
				int y = pncObj.y; // Y_AXIS
				int width = pncObj.width; // X_LENGTH
				int height = pncObj.height; // Y_LENGTH

				pStatement.setString(1, groupDrawingName); // GROUP_DRAWING_NO
				pStatement.setString(2, pncNams); // PNC_NO
				pStatement.setInt(3, x); // X_AXIS
				pStatement.setInt(4, y); // Y_AXIS
				pStatement.setInt(5, width); // X_LENGTH
				pStatement.setInt(6, height); // Y_LENGTH
				pStatement.setString(7, groupDrawingRevision); // GROUP_DRAWING_REV
				pStatement.setString(8, pncNams); // PNC_NO
				pStatement.setString(9, groupDrawingName); // GROUP_DRAWING_NO
				pStatement.setString(10, groupDrawingRevision); // GROUP_DRAWING_REV
				pStatement.setInt(11, x); // X_AXIS
				pStatement.setInt(12, y); // Y_AXIS
				pStatement.addBatch();
			}
			int[] count = pStatement.executeBatch();
			connection.commit();
			System.out.println("Inserted data : " + count.length);
			System.out.println("Inserted records into the table EPC_PNC_COORDINATES now... ");
		} catch (Exception e) {
			System.out.println("(Error) Method uploadPncCoordinatesToEPC<Oracle> : " + e.getMessage());
			if (connection != null) {
				try {
					System.out.println("Transaction is being rolled back");
					connection.rollback();
				} catch (SQLException e2) {
					System.out.println("(Error) Method uploadPncCoordinatesToEPC<connection.rollback> : " + e2.getMessage());
				}
			}
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				System.out.println("(Error) Method uploadPncCoordinatesToEPC<connection.setAutoCommit> : " + e.getMessage());
			}
			if (result != null) {
				try {
					result.close();
				} catch (SQLException e) {
					System.out.println("(Error) Method uploadPncCoordinatesToEPC<result> : " + e.getMessage());
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					System.out.println("(Error) Method uploadPncCoordinatesToEPC<statement> : " + e.getMessage());
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					System.out.println("(Error) Method uploadPncCoordinatesToEPC<connection> : " + e.getMessage());
				}
			}
			if (pStatement != null) {
				try {
					pStatement.close();
				} catch (SQLException e) {
					System.out.println("(Error) Method uploadPncCoordinatesToEPC<pStatement> : " + e.getMessage());
				}
			}
		}
		System.out.println("uploading succesful");
	}

	public class PncCoordinates {
		private String pnc; // PNC_NO
		private String pncId;
		private int width; // X_LENGTH
		private int height; // Y_LENGTH
		private int x; // X_AXIS
		private int y; // Y_AXIS
		
		public String getPnc() {
			return pnc;
		}
		
		public void setPnc(String pnc) {
			this.pnc = pnc;
		}
		
		public String getPncId() {
			return pncId;
		}
		
		public void setPncId(String pncId) {
			this.pncId = pncId;
		}
		
		public int getWidth() {
			return width;
		}
		
		public void setWidth(int width) {
			this.width = width;
		}
		
		public int getHeight() {
			return height;
		}
		
		public void setHeight(int height) {
			this.height = height;
		}
		
		public int getX() {
			return x;
		}
		
		public void setX(int x) {
			this.x = x;
		}
		
		public int getY() {
			return y;
		}
		
		public void setY(int y) {
			this.y = y;
		}
	}
}