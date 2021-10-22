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

import matrix.db.Context;
import matrix.util.StringList;

public class SPLM_ImageRecognition_JPO_mxJPO {
	
	// java called python for Image recognition
	private static final String PICTURES_PATH = "E:\\temp\\morga\\Pic";
	private final String exe = "python"; // 編譯器
	private final String codingCommand = "E:\\temp\\morga\\CMC_Recognize_V99.py";
	
	// java connect oracle with JDBC setting
	// This is for juichi 10.0.0.24 enviroment
	// private final String EPC_DB_URL = "jdbc:oracle:thin:@10.0.0.24:1521/cmc3dxdb";
	// private final String EPC_DB_USERNAME = "EPC";
	// private final String EPC_DB_PASSWORD = "Qwerty12345";
	
	// java connect oracle with JDBC setting
	// This is for CMC 172.20.24.91 enviroment
	private final String EPC_DB_URL = "jdbc:oracle:thin:@172.20.24.91:1521/plmtst";
	private final String EPC_DB_USERNAME = "EPC";
	private final String EPC_DB_PASSWORD = "splm@CMC2021";

	/**
	 * Expand Image and Gain bus Image to CheckoutFile with format generic
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public void scanGroupDrawing(Context context, String[] args) throws Exception {

		StringList relSelects = new StringList();
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID); // "id"
		busSelects.add(DomainConstants.SELECT_NAME); // "name"
		busSelects.add(DomainConstants.SELECT_REVISION); // "revision"
		busSelects.add(DomainConstants.SELECT_FORMAT_GENERIC); // "format[generic].name"

		// 1. find groupDrawingObj which has Scanning or null for
		// attribute[SPLM_ScanStatus]
		MapList groupDrawingList = DomainObject.findObjects(context, "SPLM_GroupDrawing", // type
				"*", // name
				"*", // revision
				"*", // owner
				"eService Production", // vault
				"attribute[SPLM_ScanStatus]=='' or attribute[SPLM_ScanStatus]=='Scanning'", // where
				null, false, busSelects, (short) 0);

		Iterator groupDrawingItr = groupDrawingList.iterator();
		DomainObject groupDrawingObj = new DomainObject();
		while (groupDrawingItr.hasNext()) {

			Map groupDrawingMap = (Map) groupDrawingItr.next();
			String groupDrawingId = (String) groupDrawingMap.get(DomainConstants.SELECT_ID);
			String groupDrawingName = (String) groupDrawingMap.get(DomainConstants.SELECT_NAME);
			groupDrawingObj.setId(groupDrawingId);

			// 2. download each groupDrawing which has Image picture on it
			MapList imageList = groupDrawingObj.getRelatedObjects(context, "SPLM_GroupDrawingImage", // relationshipPattern,
					"Image", // typePattern,
					busSelects, // StringList objectSelects,
					relSelects, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					1); // int limit)

			// checkout file and doing image recognize in same time / each picture
			Iterator imageItr = imageList.iterator();
			while (imageItr.hasNext()) {
				ArrayList<PncCoordinates> pncArrayList = new ArrayList<PncCoordinates>();
				Map imageMap = (Map) imageItr.next();
				String imageName = (String) imageMap.get(DomainConstants.SELECT_NAME);
				String imageId = (String) imageMap.get(DomainConstants.SELECT_ID);
				String imageRevision = (String) imageMap.get(DomainConstants.SELECT_REVISION);
				String imageGeneric = (String) imageMap.get(DomainConstants.SELECT_FORMAT_GENERIC);

				if (!imageGeneric.isEmpty()) {
					System.out.println("imageName : " + imageName + " exist. Downloading now.");
					DomainObject imageObj = new DomainObject(imageId);
					
					MqlUtil.mqlCommand(context, "trigger off");
					imageObj.checkoutFile(context, false, "generic", imageGeneric, PICTURES_PATH);
					MqlUtil.mqlCommand(context, "trigger on");

					// 3. image recognize
					pncArrayList = run(context, imageGeneric);
					// 4. uploadPnc
					uploadPncCoordinatesToEPC(pncArrayList, imageName, imageRevision);
				}

				// 5. connect groupDrawing / pnc with relationship[SPLM_Related_PNC]
				StringList pncList = groupDrawingObj.getInfoList(context, "from[SPLM_Related_PNC].to.id");
				for (PncCoordinates pncData : pncArrayList) {
					String pnc = pncData.pnc;
					String pncId = pncData.pncId;
					if (pncList.contains(pncId)) {
						continue;
					}

					System.out.println("pic Name: " + groupDrawingName + " ,pnc id : " + pncId + " ,pnc : " + pnc);
					DomainRelationship.connect(context, groupDrawingObj, "SPLM_Related_PNC", new DomainObject(pncId));
					pncList.add(pncId);
				}
				System.out.println("PNC / GroupDrawing relationship[SPLM_RelatedGroupDrawing] Connect succeed.");
			}
			
			// 6. change groupDrawing attribute[ScanStatus] to ScanComplete
			groupDrawingObj.setAttributeValue(context, "SPLM_ScanStatus", "ScanComplete");
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
	public ArrayList<PncCoordinates> run(Context context, String imageGeneric) throws Exception {
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
		String[] cmdImportData = new String[] { exe, codingCommand, picturePath };
		
		try {
			proc = Runtime.getRuntime().exec(cmdImportData);
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
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
	private void uploadPncCoordinatesToEPC(ArrayList<PncCoordinates> pncArrayLists, String imageName, String imageRevision) {
		System.out.println("uploading...");
		Connection connection = null;
		Statement statement = null;
		ResultSet result = null;
		PreparedStatement pStatement = null;
		try {
			connection = DriverManager.getConnection(EPC_DB_URL, EPC_DB_USERNAME, EPC_DB_PASSWORD);
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

				pStatement.setString(1, imageName); // GROUP_DRAWING_NO
				pStatement.setString(2, pncNams); // PNC_NO
				pStatement.setInt(3, x); // X_AXIS
				pStatement.setInt(4, y); // Y_AXIS
				pStatement.setInt(5, width); // X_LENGTH
				pStatement.setInt(6, height); // Y_LENGTH
				pStatement.setString(7, imageRevision); // GROUP_DRAWING_REV
				pStatement.setString(8, pncNams); // PNC_NO
				pStatement.setString(9, imageName); // GROUP_DRAWING_NO
				pStatement.setString(10, imageRevision); // GROUP_DRAWING_REV
				pStatement.setInt(11, x); // X_AXIS
				pStatement.setInt(12, y); // Y_AXIS
				pStatement.addBatch();
			}
			int[] count = pStatement.executeBatch();
			connection.commit();
			System.out.println("Inserted records into the table EPC_PNC_COORDINATES now... ");
		} catch (SQLException e) {
			e.printStackTrace();
			if (connection != null) {
				try {
					System.err.println("Transaction is being rolled back");
					connection.rollback();
				} catch (SQLException e2) {
					e2.printStackTrace();
				}
			}
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			if (result != null) {
				try {
					result.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (pStatement != null) {
				try {
					pStatement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("uploading succesful");
	}

	public class PncCoordinates {
		public String pnc; // PNC_NO
		public int width; // X_LENGTH
		public int height; // Y_LENGTH
		public int x; // X_AXIS
		public int y; // Y_AXIS
		public String pncId;
	}
}