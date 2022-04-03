import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.collections4.ListUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.zefer.pd4ml.PD4PageMark;

import com.google.gson.GsonBuilder;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.pd4ml.PD4ML;
import com.pd4ml.PageMargins;
import com.pd4ml.PageSize;

import matrix.db.Context;
import matrix.db.RelationshipWithSelect;
import matrix.db.RelationshipWithSelectList;
import matrix.util.StringList;

public class SPLM_CataloguePDF_mxJPO {

	// A4 比例 210mm*297mm 比例為 0.70707070707071
	// 1839px*0.7070707070707071 = 1300px (高度)
	// scale = (pageFormat.width - pageInsets.left - pageInsets.right) / htmlWidth
	// (文件文字縮放比算法)
	// (第一種高度) 1300 已經扣掉Inset
	// (第二種高度) 1340 補上Inset
	// div 已設定 height:1220px ， header 已設定 height: 12px -> 共使用 1232px
	// 所以會多 68px 或是 108px? (待測試)

	//

	protected final int topPageInset = 20;
	protected final int leftPageInset = 20;
	protected final int rightPageInset = 20;
	protected final int bottomPageInset = 20;
	protected final int userSpaceWidth = 1839;
	protected final String A4PAGE_HEIGHT = "100%";
	protected final String A4PAGE_WIDTH = "100%";
	protected final String OUTPUT_PATH = "C:/TEMP/Morga/MORTEST5-2.pdf";
	protected final String OUTPUT_PATH2 = "C:/TEMP/Morga/MORPIC6.pdf";

	private final String TEMP_PICTURE_2 = "C:/temp/Morga/CP02115103A-3.jpg";
	private final String ALTER_FORMATSTR = "%s -> %s(%s)";

//	 htmlWidth value defines "virtual web browser" frame width (in screen pixels), and by default is set to 640.
//	 scale = (pageFormat.width - pageInsets.left - pageInsets.right) / htmlWidth

	/* 獲取資料 */
	private ArrayList<Abbre> getAbbreCSV(String csvPath) throws Exception {
		FileInputStream fis = null;
		Workbook wb = null;
		ArrayList<Abbre> abbreArray = new ArrayList<Abbre>();
		try {
			fis = new FileInputStream(csvPath);
			wb = new XSSFWorkbook(fis);
			Sheet abbreSheet = wb.getSheetAt(0);
			for (int abbreStart = abbreSheet.getFirstRowNum() + 1; abbreStart <= abbreSheet
					.getLastRowNum(); ++abbreStart) {
				String abbre = abbreSheet.getRow(abbreStart).getCell(0).getStringCellValue().trim();
				String value = abbreSheet.getRow(abbreStart).getCell(1).getStringCellValue().trim();
				Abbre abbreObj = new Abbre();
				abbreObj.setAbbreName(abbre);
				abbreObj.setAbbreWhole(value);
				abbreArray.add(abbreObj);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			// TODO: handle exception
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
		return abbreArray;
	}

	class Abbre {
		private String abbreName;
		private String abbreWhole;

		public String getAbbreName() {
			return abbreName;
		}

		public void setAbbreName(String abbreName) {
			this.abbreName = abbreName;
		}

		public String getAbbreWhole() {
			return abbreWhole;
		}

		public void setAbbreWhole(String abbreWhole) {
			this.abbreWhole = abbreWhole;
		}
	}

	/* 假資料用 ----------------------------------------------------------- */
	private ArrayList<GroupDrawingRelatedPart> getIndexInfo() {
		ArrayList<GroupDrawingRelatedPart> IndexArray = new ArrayList<GroupDrawingRelatedPart>();
		for (int i = 0; i < 100; ++i) {
			GroupDrawingRelatedPart index = new GroupDrawingRelatedPart();
			index.setPart("1003A093");
			index.setGroupCode("15210");
			index.setPnc("01144");
			IndexArray.add(index);

			GroupDrawingRelatedPart index2 = new GroupDrawingRelatedPart();
			index2.setPart("1003A126D");
			index.setGroupCode("11110");
			index2.setPnc("11384");
			IndexArray.add(index2);
		}
		return IndexArray;
	}

	/*-------------------------------------------------------------------------------------*/
	/* PDF Setting And Generate */
	private void generatePDFNoLiscense(File outputPDFPath, StringReader strReader, String cssStyle, PD4PageMark header)
			throws Exception {
		FileOutputStream fos = new FileOutputStream(outputPDFPath);

//		pd4ml.adjustHtmlWidth();
//		pd4ml.fitPageVertically();
//		pd4ml.outputFormat(PD4Constants.PDF, 1136, 803);
//		PageMargins margin = new PageMargins(topPageInset, rightPageInset, leftPageInset, bottomPageInset);

//		PD4ML pd4ml = new PD4ML(); // com.zefer.pd4ml.PD4ML
//		pd4ml.setHtmlWidth(userSpaceWidth); // unit : px
//		 選擇目標檔案的格式 - A4
//		pd4ml.setPageSize(PD4Constants.A4); // 豎立顯示
//		pd4ml.setPageSize(pd4ml.changePageOrientation(PD4Constants.A4)); // 橫躺顯示
//		pd4ml.setPageInsets(new Insets(topPageInset, leftPageInset, bottomPageInset, rightPageInset));
//		 原來的html文件也有邊距，可以通過這個方式壓縮
//		pd4ml.addStyle(cssStyle, true);
//		pd4ml.useTTF("C:/eclipse/eclipse-workspace/morgaCMC/jars", true);
//		pd4ml.useTTF("C:/DassaultSystemes/R2020x/3DSpace/win_b64/docs/custom", true);
//		pd4ml.setDefaultTTFs("kaiu", "kaiu", "kaiu");
//		pd4ml.enableDebugInfo();
//		pd4ml.generateOutlines(true);
		// 根據 H1-H6 自動製作bookRemark 效果
//		System.out.println("seting");
//		header.setPageNumberTemplate("page $[page] of $[total]");
//		System.out.println(header.getAreaHeight());
//		pd4ml.setPageHeader(header, 12);
//		if(!cssStyle.isEmpty()) {
//			pd4ml.addStyle(cssStyle, true);
//		}
//		pd4ml.render(strReader, fos);
	}

	private void generatePDFPro(File outputPDFPath, String htmlStr, String cssStyle, String headerStr)
			throws Exception {
		FileOutputStream fos = new FileOutputStream(outputPDFPath);

		String licensePath = "C:/DassaultSystemes/R2020x/3DSpace/win_b64/docs/custom/pd4ml.lic";
		PD4ML pd4ml = new PD4ML(); // com.pd4ml.PD4ML
		pd4ml.setHtmlWidth(userSpaceWidth); // unit : px
		pd4ml.applyLicense(new URI(licensePath).toString());

		pd4ml.setPageSize(PageSize.A4.rotate()); // 橫躺顯示
		pd4ml.setPageMargins(new PageMargins(topPageInset, rightPageInset, leftPageInset, bottomPageInset));
		pd4ml.addStyle(cssStyle, true);
		pd4ml.setPageHeader(headerStr, 30, "2+");
//		System.out.println(pd4ml.predictPageHeight(new PageMargins(topPageInset, rightPageInset, leftPageInset, bottomPageInset), PageSize.A4.rotate(), userSpaceWidth));
		pd4ml.enableHyperlinks(false);
		// 如果內建的基本pdf字型不夠用，可以設定成non-Latin，TTF能夠做到這一點
		pd4ml.useTTF("C:/Windows/Fonts", true);

		/*
		 * support 3 types 1.<H1>-<H6> headings hierarchy 2.named anchors <a
		 * name="chapter1">Chapter 1</a> 3.structure of <pd4ml:bookmark> tags
		 */
//		pd4ml.generateBookmarksFromHeadings(true);
		System.out.println("seting");
		pd4ml.readHTML(new ByteArrayInputStream(htmlStr.getBytes()));
		pd4ml.writePDF(fos);
		System.out.println("Done");
	}

	private void name1PD4ML(String abbrePath, String catalogueMsName, String catalogueStartDate,
			ArrayList<GroupDrawingRelatedPart> indexPartList, Map<String, ArrayList<GroupCode>> indexGroupNoMap,
			Map<String, ArrayList<String>> groupDrawingPicMap, Map<String, ArrayList<DetailPart>> detailPartMap,
			Map<String, ArrayList<String>> allReplacePartMap) throws Exception {
		/*-----------------------------------------------------------------------*/
		StringBuffer css = new StringBuffer();
		/* 樣式一 設定 */
		css.append("div.groupIndex_outer_div{display: flex; justify-content: space-between; flex-wrap: wrap;}");
		css.append("div.groupIndex_inner_div{width:49%; max-height:95%; height:95%;}");
		css.append("table.groupIndex_table{width:100%; border-collapse: collapse;}");
		css.append("tr.groupIndex_tr_text{font-size : 30px;}");
		css.append("tr.groupIndex_tr_title{font-size : 50px; border: 1px solid black; border-left:none; border-right: none;}");
		css.append("td.groupIndex_td_fixHeight{height: 70px;}");
		css.append("td.groupIndex_td_1{width: 7%;}");
		css.append("td.groupIndex_td_2{width: 35%;}");
		css.append("td.groupIndex_td_3{width: 57%;}");
		css.append("div.groupIndex_div_split{height: 70px; margin:0px;}");

		/* 樣式二 設定 partDetail */
		css.append("table.partDetail_outer_table{width: 100%; height:95%}");
		css.append("table.partDetail_innerTitle_table{width: 100%; font-size: 60px;}");
		css.append("td.partDetail_innerTitle_td_1{width: 15%; text-align: center; vertical-align:middle; font-size: 70px;}");
		css.append("td.partDetail_innerTitle_td_2{width: 18%; text-align: right;vertical-align:middle; }");
		css.append("td.partDetail_innerTitle_td_3{width: 65%; text-align: left;}");
		
		css.append("td.table_detail_inner_picture{vertical-align: top; height: 990px; max-height: 990px; border: 5px solid black;}");

		css.append("table.partDetail_inner_table{width: 100%;border-collapse : collapse; font-size:20px;}");
		css.append("table.partDetail_inner_table tr {border: 1px solid black;}");
		css.append("tr.partDetail_innerTable_tr_titleMid{text-align: center; vertical-align: middle;}");
		css.append("tr.partDetail_innerTable_tr_textMid{ vertical-align: middle;}");
		css.append("td.partDetail_innerTable_td_1{width: 6%;}");
		css.append("td.partDetail_innerTable_td_2{width: 10%;}");
		css.append("td.partDetail_innerTable_td_3{width: 25%;}");
		css.append("td.partDetail_innerTable_td_4{width: 2%;text-align: center;}");
		css.append("td.partDetail_innerTable_td_5{width: 3%;text-align: center;}");
		css.append("td.partDetail_innerTable_td_6{width: 24%;}");
		css.append("td.partDetail_innerTable_td_7{width: 6%;text-align: center;}");
		css.append("td.partDetail_innerTable_td_8{width: 6%;text-align: center;}");
		css.append("td.partDetail_innerTable_td_9{width: 4%;}");
		css.append("td.partDetail_innerTable_td_10{width: 14%;}");

		/* 通用格式 */
		css.append("td.td_text_center{text-align: center;}");
		css.append("td {vertical-align: text-top;}");
		css.append("p {margin: 0px; padding-left: 3px;}");
		css.append("img {margin: 0px; padding: 0px;}");
		css.append("div.div_page_boxer {margin: 0px; padding : 0px; font-family: DFKai-sb; height:1220px;max-height:1220px; width:100%; word-break: break-all;}");

		/* 封面 */
		css.append("div.div_cover {display:flex; align-items: center;justify-content: center;}");
		css.append("div.div_cover_inner{font-size:70px;}");
		css.append("div.div_cover_inner p{line-height: 70px;}");

		/* 樣式三 Index groupNo */
		css.append("div.div_bigGroupCode{font-size: 300px; width: 49%;}");
//		css.append("div, table { border: 1 solid tomato;}");
//		PD4PageMark header = new PD4PageMark();
//		header.setPageNumberTemplate("P.$[page]");
//		header.setAreaHeight(9); // unit : pt (3pt = 4px)
//		header.setFontSize(25);
//		header.setPagesToSkip(10);
//		header.getPageNumberTemplate();
		String headerStr = "<div style='width: 100%; height: 12px; text-align: right; font-size: 20px; font-family: DFKai-sb;'>P.$[page]</div>";

		StringBuffer html = new StringBuffer();
		ArrayList<String> htmlList = new ArrayList<String>();
		ArrayList<String> tagList = new ArrayList<String>();

		html.append("<html>");
		html.append("<body>");

		this.setPageFont(css, html, catalogueMsName, catalogueStartDate);
		this.setTitleBookRemark(tagList);

		this.setPageAbbreviationCover(html);
		this.setPageAbbreviation(css, html, abbrePath);

		this.setPagePartIndexCover(html);
		this.setPagePartIndex(css, html, indexPartList);

		this.setPageGroupIndexCover(html);
		this.setPageGroupIndex(css, html, indexGroupNoMap);

		int counterPic = 0;
		for (ArrayList<GroupCode> groupCodeList : indexGroupNoMap.values()) {
			String bookmark = "";
			if (groupCodeList.get(0).getGroupCode().length() < 3) {
				bookmark = String.join(" ", groupCodeList.get(0).getBigGroupCode(), groupCodeList.get(0).getNameCh(),
						groupCodeList.get(0).getNameEn());
			}
			this.setPageGroupIndexHasBigCode(html, groupCodeList, bookmark);

			html.append("<div>");
			tagList.add(String.format("<pd4ml:bookmark name=\"%s\" href=\"#%s\">", bookmark, bookmark));
			for (GroupCode groupCodeObj : groupCodeList) {
				if (groupDrawingPicMap.containsKey(groupCodeObj.getGroupCode())) {
					this.setPagePicture(html, groupDrawingPicMap.get(groupCodeObj.getGroupCode()), groupCodeObj,
							catalogueMsName);
				}
				if( detailPartMap.containsKey(groupCodeObj.getGroupCode())) {
					this.setPageServicePartDeatail(html ,tagList ,allReplacePartMap ,detailPartMap.get(groupCodeObj.getGroupCode()), groupCodeObj, catalogueMsName);					
				}
			}
			tagList.add("</pd4ml:bookmark>"); // 暫時關閉
			html.append("</div>");
		}

		html.append("</body>");
		html.append("</html>");

		this.outputFile("htmlStructure.txt", String.join("", tagList) + html.toString());
		this.generatePDFPro(new File(OUTPUT_PATH), String.join("", tagList) + html.toString(), css.toString(),headerStr);
		System.out.println("Generate");
	}

	/* Title */
	private void setTitleBookRemark(ArrayList<String> tagList) {
		tagList.add("<pd4ml:bookmark name=\"封面 Font\" href=\"#CoverFont\">");
		tagList.add("</pd4ml:bookmark>");
		tagList.add("<pd4ml:bookmark name=\"縮寫表 Abbreviations\" href=\"#AbbreviationsCover\">");
		tagList.add("</pd4ml:bookmark>");
		tagList.add("<pd4ml:bookmark name=\"件號索引目錄 Part No. Index\" href=\"#partIndexCover\">");
		tagList.add("</pd4ml:bookmark>");
		tagList.add("<pd4ml:bookmark name=\"目錄編號索引 Group No. Index\" href=\"#groupIndexCover\">");
		tagList.add("</pd4ml:bookmark>");
	}

	/*-------------------------------------------------------------------------------------*/
	/* C. 書本封面 OK */
	private void setPageFont(StringBuffer css, StringBuffer html, String catalogueMsName, String catalogueStartDate) {
		css.append("div.div_font{display: flex; justify-content: flex-end;}");
		css.append("div.div_font_innerBox {width: 80%;}");
		css.append("div.div_font_innerBox_1 {border-bottom: 3px solid black; font-size: 70px; height:10%;display: flex; align-items: flex-end; padding-left: 100px;}");
		css.append("div.div_font_innerBox_2 {font-size: 70px; height:15%;display: flex; align-items: flex-end; padding-left: 100px;}");
		css.append("div.div_font_innerBox_3 {font-size: 50px; height:10%;display: flex; justify-content: flex-end; padding-right: 100px;}");

		html.append("<div class=\"div_page_boxer div_font\">");
		html.append("<div class=\"div_font_innerBox\">");
		html.append("<div class=\"div_font_innerBox_1\">");
		html.append("Parts Catalog" + setTag("CoverFont"));
		html.append("</div>");
		html.append("<div class=\"div_font_innerBox_2\">");
		html.append(catalogueMsName + " " + catalogueStartDate.substring(0, 4) + "年型零件冊");
		html.append("</div>");
		html.append("<div class=\"div_font_innerBox_3\">");
		html.append("版本日期 : " + catalogueStartDate);
		html.append("</div>");
		html.append("</div>");
		html.append("</div>");
	}

	/*-------------------------------------------------------------------------------------*/
	/* 1. 縮寫表 OK */
	private void setPageAbbreviation(StringBuffer css, StringBuffer html, String abbrePath) {
		css.append("td.Abbre_innerTable_td_1{width: 32%;}");
		css.append("td.Abbre_innerTable_td_2{width: 2%;}");
		css.append("td.Abbre_innerTable_td_3{width: 66%;}");
		css.append("td.Abbre_outerTable_td_border{border: 3px solid black;}");
		css.append("td.Abbre_outerTable_td_width{width: 25%}");
		css.append("th.Abbre_outerTable_th{font-size:50px;}");
		css.append("table.Abbre_outer_table {width:100%; border-spacing:10px;}");
		css.append("table.Abbre_inner_table tr td p {line-height:30px;}");

		int count = 0;
		try {
			ArrayList<Abbre> abbreArrayList = this.getAbbreCSV(abbrePath);
			while (count < abbreArrayList.size()) {
				html.append("<div class=\"div_page_boxer\">");
				html.append("<table class=\"Abbre_outer_table\">");
				html.append("<thead><th class=\"Abbre_outerTable_th\" colspan=\"4\">縮寫表(Abbreviations)</th></thead>");
				html.append("<tbody><tr>");
				for (int i = 0; i < 4; ++i) {
					if (abbreArrayList.size() == count) {
						html.append("<td class=\"Abbre_outerTable_td_width\">");
						html.append("</td>");
					} else {
						html.append("<td class=\"Abbre_outerTable_td_width Abbre_outerTable_td_border\">");
						count = this.setAbbreviationsTable(html, abbreArrayList, count);
						html.append("</td>");
					}
				}
				html.append("</tr></tbody></table>");
				html.append("</div>");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}

	}

	private int setAbbreviationsTable(StringBuffer html, ArrayList<Abbre> abbreArrayList, int count) {
		// 主要
		int lineLimit = 0;
		html.append("<table class=\"Abbre_inner_table\"><tbody");
		while (lineLimit < 30 && count < abbreArrayList.size()) {
			lineLimit += this.setAbbreviationsText(html, abbreArrayList.get(count));
			++count;
		}
		html.append("</tbody></table>");
		return count;
	}

	private int setAbbreviationsText(StringBuffer html, Abbre abbre) {
		html.append("<tr>");
		html.append("<td class=\"Abbre_innerTable_td_1\">");
		html.append("<p>" + abbre.getAbbreName() + "</p>");
		html.append("</td>");
		html.append("<td class=\"Abbre_innerTable_td_2\">");
		html.append("<p>:</p>");
		html.append("</td>");
		html.append("<td class=\"Abbre_innerTable_td_3\">");
		ArrayList<String> subArray = substituteWorldBreak(abbre.getAbbreWhole(), 34);
		for (String splitWord : subArray) {
			html.append("<p>" + splitWord + "</p>");
		}
		html.append("</td>");
		html.append("</tr>");
		return subArray.size();
	}

	private ArrayList<String> substituteWorldBreak(String world, int limit) {
		ArrayList<String> splitWorldArray = new ArrayList<String>();
		while (world.length() > limit) {
			splitWorldArray.add(world.substring(0, limit));
			world = world.substring(limit);
		}
		splitWorldArray.add(world);
		return splitWorldArray;
	}

	/*-------------------------------------------------------------------------------------*/
	/* 3. 目錄編號Index 第一種 OK */
	private void setPageGroupIndex(StringBuffer css, StringBuffer html, Map<String, ArrayList<GroupCode>> indexGroupNoMap) {
		ArrayList<GroupCode> groupCodeList = new ArrayList<GroupCode>();
		for (ArrayList<GroupCode> indexGroupNoValue : indexGroupNoMap.values()) {
			groupCodeList.addAll(indexGroupNoValue);
		}
		
		int count = 0;
		// 限制設定要記得相同
		while (count < groupCodeList.size()) {
			html.append("<div class=\"div_page_boxer groupIndex_outer_div\">");
			for (int j = 0; j < 2; ++j) {
				html.append("<div class=\"groupIndex_inner_div\">");
				count = this.setIndexTable(html, groupCodeList, count);
				html.append("</div>");
			}
			html.append("</div>");
		}
	}

	/* 4. 目錄編號Index 第二種 有bigCode OK */
	private void setPageGroupIndexHasBigCode(StringBuffer html, ArrayList<GroupCode> indexGroupNoValue,String bookmark) {
		boolean hasBigGroupCode = false;
		int count = 0;
		// 限制設定要記得相同
		while (count < indexGroupNoValue.size()) {
			html.append("<div class=\"div_page_boxer groupIndex_outer_div\">");
			for (int j = 0; j < 2; ++j) {
				if (!hasBigGroupCode) {
					html.append("<div class=\"groupIndex_inner_div div_bigGroupCode\">");
					this.setIndexBigGroupCode(html, indexGroupNoValue.get(0).getBigGroupCode(), setTag(bookmark));
					html.append("</div>");
					hasBigGroupCode = true;
				} else {
					html.append("<div class=\"groupIndex_inner_div\">");
					count = this.setIndexTable(html, indexGroupNoValue, count);
					html.append("</div>");
				}
			}
			html.append("</div>");
		}
	}

	/* 目錄編號Index 共用格式 */
	private void setIndexBigGroupCode(StringBuffer html, String bigGroupCode, String tagLink) {
		html.append("<table class=\"groupIndex_table\">");
		html.append("<tbody><tr>");
		html.append("<td class=\"td_text_center\">");
		html.append(bigGroupCode + tagLink);
		html.append("</td></tr></tbody>");
		html.append("</table>");
	}

	private int setIndexTable(StringBuffer html, ArrayList<GroupCode> allGroupCode, int count) {
		int lineLimit = 0;
		
		html.append("<table class=\"groupIndex_table\">");
		while (lineLimit < 16 && count < allGroupCode.size()) {
			GroupCode groupCodeObj = allGroupCode.get(count);
			if (groupCodeObj.getGroupCode().length() < 3) {
				lineLimit += this.setIndexTitle(html, groupCodeObj);
			} else {
				lineLimit += this.setIndexText(html, groupCodeObj);
			}
			++count;
		}
		html.append("</table>");

		return count;
	}

	private int setIndexText(StringBuffer html, GroupCode groupCodeObj) {
		return _setIndexText(html, groupCodeObj.getSmallGroupCode(), groupCodeObj.getNameCh(),
				groupCodeObj.getNameEn());
	}

	// 底層 <可Test ， test尚未撰寫>
	private int _setIndexText(StringBuffer html, String smallGroupCode, String nameCh, String nameEn) {
		ArrayList<String> nameChArray = this.substituteWorldBreak(nameCh, 10);
		ArrayList<String> nameEnArray = this.substituteWorldBreak(nameEn, 31);
		
		html.append("<tr class=\"groupIndex_tr_text\">");
		html.append("<td class=\"groupIndex_td_1 groupIndex_td_fixHeight\">");
		html.append("<p>" + smallGroupCode + "</p>");
		html.append("</td><td class=\"groupIndex_td_2 groupIndex_td_fixHeight\">");
		for (String tempWord : nameChArray) {
			html.append("<div class=\"groupIndex_div_split\">" + tempWord + "</div>");
		}
		html.append("</td><td class=\"groupIndex_td_3 groupIndex_td_fixHeight\">");
		for (String tempWord : nameEnArray) {
			html.append("<div class=\"groupIndex_div_split\">" + tempWord + "</div>");
		}
		html.append("</td>");
		html.append("</tr>");
		return nameEnArray.size() > nameChArray.size() ? nameEnArray.size() : nameChArray.size();
	}

	private int setIndexTitle(StringBuffer html, GroupCode groupCodeObj) {
		return _setIndexTitle(html, groupCodeObj.getBigGroupCode(), groupCodeObj.getNameCh(), groupCodeObj.getNameEn());
	}

	// 底層 <可Test ， test尚未撰寫>
	private int _setIndexTitle(StringBuffer html, String bigGroupCode, String nameCh, String nameEn) {
		ArrayList<String> nameChArray = this.substituteWorldBreak(nameCh, 10);
		ArrayList<String> nameEnArray = this.substituteWorldBreak(nameEn, 31);
		
		html.append("<tr class=\"groupIndex_tr_title\">");
		html.append("<td class=\"groupIndex_td_1 groupIndex_td_fixHeight\">");
		html.append("<p>" + bigGroupCode + "</p>");
		html.append("</td>");
		html.append("<td class=\"groupIndex_td_2 groupIndex_td_fixHeight td_text_center\">");
		for (String tempWord : nameChArray) {
			html.append("<div class=\"groupIndex_div_split\">" + tempWord + "</div>");
		}
		html.append("</td>");
		html.append("<td class=\"groupIndex_td_3 groupIndex_td_fixHeight\">");
		for (String tempWord : nameEnArray) {
			html.append("<div class=\"groupIndex_div_split\">" + tempWord + "</div>");
		}
		html.append("</td>");
		html.append("</tr>");
		return nameEnArray.size() > nameChArray.size() ? nameEnArray.size() : nameChArray.size();
	}

	/*-------------------------------------------------------------------------------------*/
	/* 2. 件號索引目錄 OK */
	private void setPagePartIndex(StringBuffer css, StringBuffer html,
			ArrayList<GroupDrawingRelatedPart> GroupDrawingRelatedPartArrayList) {
		/* 樣式二 設定 -> partNo/Group/PNC 組成 */
		css.append("table.partIndex_outer_table {width: 100%;}");
		css.append("table.partIndex_inner_table {border-collapse : collapse; border: 3px solid black; width : 98%;}");
		css.append("table.partIndex_inner_table tr td{border: 1px solid black;}");
		css.append("th.partIndex_outerTable_th{font-size:50px;}");
		css.append("td.partIndex_outerTable_td{width:25%;}");
		css.append("table.partIndex_inner_table tr td p{line-height:30px;}");

		int count = 0;
		while (GroupDrawingRelatedPartArrayList.size() > count) {
			html.append("<div class=\"div_page_boxer\">");
			html.append("<table class=\"partIndex_outer_table\">");
			html.append("<thead><th class=\"partIndex_outerTable_th\" colspan=\"4\">件號索引目錄</th></thead>");
			html.append("<tbody><tr>");
			for (int i = 0; i < 4; ++i) {
				html.append("<td class=\"partIndex_outerTable_td\">");
				if (GroupDrawingRelatedPartArrayList.size() != count) {
					count = this.setServicePartIndexTable(html, GroupDrawingRelatedPartArrayList, count);
				}
				html.append("</td>");
			}
			html.append("</tr></tbody></table>");
			html.append("</div>");
		}
	}

	private int setServicePartIndexTable(StringBuffer html,
			ArrayList<GroupDrawingRelatedPart> GroupDrawingRelatedPartArrayList, int count) {
		int limit = 36;
		int finalCount = limit + count;
		if (GroupDrawingRelatedPartArrayList.size() < finalCount) {
			finalCount = GroupDrawingRelatedPartArrayList.size();
		}

		String bigCodeParam = "";
		String smallCodeParam = "";
		String pnc = "";
		String part = "";
		while (finalCount > count) {
			GroupDrawingRelatedPart relatedPart = GroupDrawingRelatedPartArrayList.get(count++);
			bigCodeParam = bigCodeParam + "<p>" + relatedPart.getBigGroupCode() + "</p>";
			smallCodeParam = smallCodeParam + "<p>" + relatedPart.getSmallGroupCode() + "</p>";
			pnc = pnc + "<p>" + relatedPart.getPnc() + "</p>";
			part = part + "<p>" + relatedPart.getPart() + "</p>";
		}

		// 輸出部分
		if (finalCount != GroupDrawingRelatedPartArrayList.size()) {
			html.append("<table class=\"partIndex_inner_table\"><tbody>");
			this.setServicePartIndexTableTile(html);

			html.append("<tr>");
			/* Part No */
			html.append("<td class=\"inner_table_td\">");
			html.append(part);
			html.append("</td>");
			/* big GroupCode */
			html.append("<td class=\"inner_table_td td_text_center\">");
			html.append(bigCodeParam);
			html.append("</td>");
			/* small GroupCode */
			html.append("<td class=\"inner_table_td td_text_center\">");
			html.append(smallCodeParam);
			html.append("</td>");
			/* PNC */
			html.append("<td class=\"inner_table_td\">");
			html.append(pnc);
			html.append("</td>");
			html.append("</tr>");
			html.append("</tbody></table>");
		}

		return count;
	}

	private void setServicePartIndexTableTile(StringBuffer html) {
		html.append("<tr>");
		html.append("<td class=\"td_text_center\">");
		html.append("<p>Part No</p>");
		html.append("</td><td class=\"td_text_center\"colspan=\"2\">");
		html.append("<p>Group</p>");
		html.append("</td><td class=\"td_text_center\">");
		html.append("<p>PNC</p>");
		html.append("</td>");
		html.append("</tr>");
	}

	/*
	 * 尚未完成
	 * -----------------------------------------------------------------------------
	 * --------
	 */
	/* 5.ServicePart部分 含圖 */
	private void setPagePicture(StringBuffer html, ArrayList<String> picPathList, GroupCode groupCodeObj,
			String catalogueMsName) throws Exception {
//		byte[] b = Files.readAllBytes(Paths.get("C:\\temp\\morga\\testing.png"));
//		  String b64 = Base64.getEncoder().encodeToString(b);
		for (String picPath : picPathList) {
			html.append("<div class=\"div_page_boxer\">");
			html.append("<table class=\"partDetail_outer_table\">");
			html.append("<thead><tr><td>");
			this.setPageServicePartTitle(html, groupCodeObj, catalogueMsName);
			html.append("</td></tr></thead>");
			html.append("<tbody><tr>");
			html.append("<td class=\"table_detail_inner_picture\">");
//			html.append("<div class=\"imgBlock\" style=\"width:80%; height:80%; max-width:100%; background-color:blue;\">");
			html.append("<img src=\"file://" + picPath + "\" />");
//			html.append("<img src=\"data:image/png;base64,"+ b64 +"\" />");
//			html.append("</div>");
			html.append("</td></tr></tbody>");
			html.append("</table>");
			html.append("</div>");
		}
	}

	/* 6.ServicePart部分 不包含圖 */
	private void setPageServicePartDeatail(StringBuffer html, ArrayList<String> tagList,
			Map<String, ArrayList<String>> allReplacePartMap, ArrayList<DetailPart> detailPartList,
			GroupCode groupCodeObj, String catalogueMsName) {

		// tag Sample -> "15 010 空氣濾清器及管 AIR CLEANER & INTAKE";
		String bookmark = String.join(" ", groupCodeObj.getBigGroupCode(), groupCodeObj.getSmallGroupCode(),
				groupCodeObj.getNameCh(), groupCodeObj.getNameEn());
		// tag 有 / 代表視自己為獨立一行 不用close的意思

		tagList.add("<pd4ml:bookmark name=\"" + bookmark + "\" href=\"#" + bookmark + "\"/>");
		int count = 0;
		while (count < detailPartList.size()) {
			html.append("<div class=\"div_page_boxer\">");
			html.append("<table class=\"partDetail_outer_table\">");
			html.append("<thead><tr><td>");
			if (count == 0) {
				this.setPageServicePartTitle(html, groupCodeObj, catalogueMsName, bookmark);
			} else {
				this.setPageServicePartTitle(html, groupCodeObj, catalogueMsName);
			}
			html.append("</td></tr></thead>");
			html.append("<tbody><tr>");
			count = this.setPageServicePartTable(html, allReplacePartMap, detailPartList, count);
			html.append("</tr></tbody>");
			html.append("</table>");
			html.append("</div>");
		}
	}

	private int setPageServicePartTable(StringBuffer html, Map<String, ArrayList<String>> allReplacePartMap,
			ArrayList<DetailPart> detailPartList, int count) {
		int lineLimit = 0;
		html.append("<table class=\"partDetail_inner_table\">");
		while (count < detailPartList.size() && lineLimit < 35) {
			if (lineLimit == 0) {
				this.setServicePartTitle(html);
			}
			lineLimit += this.setServicePartText(html, allReplacePartMap, detailPartList.get(count));
			++count;
		}
		html.append("</table>");
		return count;
	}

	private void setServicePartTitle(StringBuffer html) {
		html.append("<tr class=\"partDetail_innerTable_tr_titleMid\">");
		html.append("<td class=\"partDetail_innerTable_td_1\">");
		html.append("<p>PNC</p>");
		html.append("</td><td class=\"partDetail_innerTable_td_2\">");
		html.append("<p>件號</p>");
		html.append("<p>PARTNO</p>");
		html.append("</td><td class=\"partDetail_innerTable_td_3\">");
		html.append("<p>件名</p>");
		html.append("<p>PART NAME</p>");
		html.append("</td><td class=\"partDetail_innerTable_td_4\">");
		html.append("<p>KD</p>");
		html.append("</td><td class=\"partDetail_innerTable_td_5\">");
		html.append("<p>QTY</p>");
		html.append("</td><td class=\"partDetail_innerTable_td_6\">");
		html.append("<p>車型</p>");
		html.append("<p>MODEL NO</p>");
		html.append("</td><td class=\"partDetail_innerTable_td_7\">");
		html.append("<p>開始</p>");
		html.append("<p>FROM</p>");
		html.append("</td><td class=\"partDetail_innerTable_td_8\">");
		html.append("<p>結束</p>");
		html.append("<p>TO</p>");
		html.append("</td><td class=\"partDetail_innerTable_td_9\">");
		html.append("<p>替代</p>");
		html.append("<p>RP</p>");
		html.append("</td><td class=\"partDetail_innerTable_td_10\">");
		html.append("<p>備註</p>");
		html.append("<p>REMARK</p>");
		html.append("</td></tr>");
	}

	// 替代件 OK
	private int setServicePartText(StringBuffer html, Map<String, ArrayList<String>> allReplacePartMap,
			DetailPart detailPartObj) {
		List<List<String>> subLists = ListUtils.partition(detailPartObj.getAdaptMc(), 7);
		String replace = allReplacePartMap.containsKey(detailPartObj.getPartNo())
				? String.join("<br>", allReplacePartMap.get(detailPartObj.getPartNo()))
				: "";

//		String[] modelNo = { "RM301", "RM30V", "RM30V", "RM357", "RM351", "RM381", "RM381" };
		html.append("<tr class=\"partDetail_innerTable_tr_textMid\">");
		html.append("<td class=\"partDetail_innerTable_td_1\">");
		html.append("<span>" + detailPartObj.getPnc() + "</span>");

		html.append("</td><td class=\"partDetail_innerTable_td_2\">");
		html.append("<span>" + detailPartObj.getPartNo() + "</span>");

		html.append("</td><td class=\"partDetail_innerTable_td_3\">");
		html.append("<p>" + detailPartObj.getPartNameCh() + "</p>");
		html.append("<p>" + detailPartObj.getPartNameEn() + "</p>");

		html.append("</td><td class=\"partDetail_innerTable_td_4\">");
		html.append("<span>" + detailPartObj.getTypeKD() + "</span>");

		html.append("</td><td class=\"partDetail_innerTable_td_5\">");
		html.append("<span>" + detailPartObj.getQuantity() + "</span>");

		html.append("</td><td class=\"partDetail_innerTable_td_6\">");
		// limit 8
		for (List<String> subList : subLists) {
			html.append("<p>");
			html.append("<span>" + String.join(" ", subList) + "</span>");
			html.append("</p>");
		}
		html.append("</td><td class=\"partDetail_innerTable_td_7\">");
		html.append("<span>" + detailPartObj.getStartDate() + "</span>");

		html.append("</td><td class=\"partDetail_innerTable_td_8\">");
		html.append("<span>" + detailPartObj.getEndDate() + "</span>");

		html.append("</td><td class=\"partDetail_innerTable_td_9\">");
//		html.append("<span>" + replace + "</span>");
		html.append("<span></span>");

		html.append("</td><td class=\"partDetail_innerTable_td_10\">");
		html.append("<span>" + detailPartObj.getRemark() + "</span>");

		html.append("</td></tr>");
		return subLists.size() < 3 ? 2 : subLists.size();
	}

	/* ServicePart Page Title共用 */
	private void setPageServicePartTitle(StringBuffer html, GroupCode groupCodeObj, String catalogueMsName) {
		_setPageServicePartTitle(html, groupCodeObj, catalogueMsName, "");
	}

	private void setPageServicePartTitle(StringBuffer html, GroupCode groupCodeObj, String catalogueMsName,
			String tag) {
		_setPageServicePartTitle(html, groupCodeObj, catalogueMsName, setTag(tag));
	}

	private void _setPageServicePartTitle(StringBuffer html, GroupCode groupCodeObj, String catalogueMsName,
			String tagLink) {
		String bigGroupCode = groupCodeObj.getBigGroupCode();
		String smallGroupCode = groupCodeObj.getSmallGroupCode();
		String nameCh = groupCodeObj.getNameCh();
		String nameEn = groupCodeObj.getNameEn();

		html.append("<table class=\"partDetail_innerTitle_table\">");
		html.append("<tr>");
		html.append("<td class=\"partDetail_innerTitle_td_1\">");
		html.append("<span>" + catalogueMsName + "</span>");
		html.append("</td>");
		html.append("<td class=\"partDetail_innerTitle_td_2\">");
		html.append("<span>" + String.join(" ", bigGroupCode, smallGroupCode) + "</span>");
		html.append("</td>");
		html.append("<td class=\"partDetail_innerTitle_td_3\">");
		html.append("<p>" + nameCh + tagLink + "<br>");
		html.append(nameEn + "</p>");
		html.append("</td>");
		html.append("</tr></table>");
	}
	/*-------------------------------------------------------------------------------------*/

	/* Cover 設計 */
	private void setPageAbbreviationCover(StringBuffer html) {
		setPageCover(html, "縮  寫  表", "abbreviations", "AbbreviationsCover");
	}

	private void setPagePartIndexCover(StringBuffer html) {
		setPageCover(html, "件 號 索 引 目 錄", "part no. index", "partIndexCover");
	}

	private void setPageGroupIndexCover(StringBuffer html) {
		setPageCover(html, "目 錄 編 號 索 引", "group no. index", "groupIndexCover");
	}

	private void setPageCover(StringBuffer html, String coverNameCh, String coverNameEn, String tags) {
		html.append("<div class=\"div_page_boxer div_cover\">");
		html.append("<div class=\"div_cover_inner\">");
		// 塞 a tags 用於BookRemark
		html.append("<p>" + coverNameCh + "<a name=\"" + tags + "\">&nbsp;&nbsp;&nbsp;&nbsp;</a>" + "</p>");
		html.append("<p>" + coverNameEn.toUpperCase() + "</p>");
		html.append("</div>");
		html.append("</div>");
		html.append("<pd4ml-page-break/>");
	}

	/* bookMark使用 */
	private String setTag(String tag) {
		return "<a name=\"" + tag + "\">&nbsp;</a>";
	}

	// -----------------------------------------------------------------------------------------------------------
	// 抓資料------------------------------------------------------------------------------------------------------
	// -----------------------------------------------------------------------------------------------------------

	private final String VAULT = "eService Production";
	/* 事先改寫圖片大小 */

	// 圖片暫存位置 C:\PLMTemp\createPDFPicture
	private final String TEMP_FOLDER = "PLMTemp\\createPDFPicture\\";
	private final String TEMP_FOLDER_DEVICE = "D:\\";

	private String getTempFolderPath() {
		if (new File(TEMP_FOLDER_DEVICE).exists()) {
			return TEMP_FOLDER_DEVICE + TEMP_FOLDER;
		}
		return "C:\\" + TEMP_FOLDER;
	}

	public void test(Context context, String[] args) throws Exception {
//			String partCatalogueName = "NB80AH531B";
//			System.out.println(getAbbrePath(context, partCatalogueName));
		createPDF(context);
	}

	/**
	 * 縮寫表下載並獲得路徑
	 * 
	 * @param context
	 * @param partCatalogueName
	 * @return
	 * @throws Exception
	 */
	private String getAbbrePath(Context context, String partCatalogueId, String storePath) throws Exception {
		StringList abbreBusStringList = new StringList();
		abbreBusStringList.add(DomainConstants.SELECT_ID);
		abbreBusStringList.add(DomainConstants.SELECT_MODIFIED);

		StringList excelBusStringList = new StringList();
		excelBusStringList.add("attribute[Title]");
		excelBusStringList.add(DomainConstants.SELECT_MODIFIED);
		StringList excelRelStringList = new StringList();
		String excelLatestName = "";

		try {
			// 網頁已受限綁定只能連接一樣，所以不用排序取最新的
			// 零件冊(要的) -> 縮寫表(generic)(最新的)
			MapList abbreMapList = new DomainObject(partCatalogueId).getRelatedObjects(context, "SPLM_RelatedAbbrev", // relationshipPattern,
					"SPLM_Abbrev", // typePattern,
					abbreBusStringList, // StringList objectSelects,
					null, // StringList relationshipSelects,
					false, // boolean getTo,
					true, // boolean getFrom,
					(short) 1, // short recurseToLevel,
					"", // String objectWhere,
					"", // String relationshipWhere
					0); // int limit)
			if (!abbreMapList.isEmpty()) {
				// 縮寫表 -> 最新的
				abbreMapList.addSortKey(DomainConstants.SELECT_MODIFIED, "descending", "date");
				String abbreId = (String) ((Map) abbreMapList.get(0)).get(DomainConstants.SELECT_ID);
				DomainObject abbreDomObj = new DomainObject(abbreId);
				MapList excelLatestMapList = abbreDomObj.getRelatedObjects(context, "Active Version", // relationshipPattern,
						"SPLM_Abbrev", // typePattern,
						excelBusStringList, // StringList objectSelects,
						excelRelStringList, // StringList relationshipSelects,
						false, // boolean getTo,
						true, // boolean getFrom,
						(short) 1, // short recurseToLevel,
						"", // String objectWhere,
						"", // String relationshipWhere
						0); // int limit)

				excelLatestName = (String) ((Map) excelLatestMapList.get(0)).get("attribute[Title]");

				// 下載Excel路徑
				MqlUtil.mqlCommand(context, "trigger off");
				abbreDomObj.checkoutFile(context, false, DomainConstants.FORMAT_GENERIC, excelLatestName, storePath);
				MqlUtil.mqlCommand(context, "trigger on");
			}
		} catch (Exception e) {
			System.out.println("Excel download Error : " + e.getMessage());
			// TODO: handle exception
		}
		return excelLatestName.isEmpty() ? "" : storePath + "\\" + excelLatestName;
	}

	private void createPDF(Context context) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);

		StringList catalogueSelects = new StringList();
		catalogueSelects.add(DomainConstants.SELECT_NAME);
		catalogueSelects.add(DomainConstants.SELECT_ID);
		catalogueSelects.add("attribute[SPLM_EnableDate]");

		// 初部資料載入
		String storePath = this.getTempFolderPath();
		if (new File(storePath).mkdirs()) {
			System.out.println("\u5efa\u7acb " + storePath + " \u8cc7\u6599\u593e ");
		}

		Map<String, String> allCMCMap = this.getcmcCodeInfo(context);
		long startTime = System.currentTimeMillis();
		ArrayList<GroupCode> allGroupCodeList = this.getGroupCode(context);
		ArrayList<CatalogueRelatedGroupDrawing> allRelatedGroupDrawingList = this.getCatalogueRelatedInfo(context);
		ArrayList<GroupDrawingRelatedPart> allRelatedPartList = this.getGroupDrawingRelatedPartInfo(context);
		Map<String, ArrayList<String>> allReplacePartMap = this.getAlterPartInfo(context);
		long endTime = System.currentTimeMillis() - startTime;
		System.out.println("cost time : " + endTime);

//		MapList glnoMapList = DomainObject.findObjects(context, "SPLM_GLNO", // type
//				"*", // name
//				"*", // revision
//				"*", // owner
//				VAULT, // vault
//				"from[SPLM_RelatedPartsCatalogue].to!=''", // where
//				null, false, busSelects, (short) 0);
//
//		
//		
//		for (Object glnoObj : glnoMapList) {
//			String glnoId = (String) ((Map) glnoObj).get(DomainConstants.SELECT_ID);
//			DomainObject glnoDomObj = new DomainObject(glnoId);
//
//			// 1.
//			// 零件冊名稱 起用日(比對)(同時也是版本日期)
//			// GLNO <-> partsCatalogue 為一對一
//			MapList partCatalogueMapList = glnoDomObj.getRelatedObjects(context, "SPLM_RelatedPartsCatalogue", // relationshipPattern,
//					"SPLM_PartsCatalogue", // typePattern,
//					catalogueSelects, // StringList objectSelects,
//					null, // StringList relationshipSelects,
//					false, // boolean getTo,
//					true, // boolean getFrom,
//					(short) 1, // short recurseToLevel,
//					"", // String objectWhere,
//					"", // String relationshipWhere
//					0); // int limit)
////				long startTime = System.currentTimeMillis();
//		
//			if (partCatalogueMapList.isEmpty()) {
//				continue;
//			}
//		
//			String partCatalogueId = (String) ((Map) partCatalogueMapList.get(0)).get(DomainConstants.SELECT_ID);
		String partCatalogueId = "32355.13920.48907.64138";

		// 下載 Abbre 縮寫表 Excel檔
		String excelPath = this.getAbbrePath(context, partCatalogueId, storePath);

		// 從 <指定> 零件冊下手找GroupDrawing圖片全部符合零件冊ID的資料
		List<CatalogueRelatedGroupDrawing> filterRelatedGroupDrawingList = allRelatedGroupDrawingList.stream()
				.filter(obj -> obj.getPartCatalogueId().equals(partCatalogueId)).collect(Collectors.toList());

//			if (filterCatalogueRelatedGroupDrawingArrayList.isEmpty()) {
//				continue;
//			}

		// 2.
		// 抓取全部GroupDrawing ID 搜尋使用
		Set<String> groupDrawingIdList = filterRelatedGroupDrawingList.stream().map(obj -> obj.getGroupDrawingId())
				.collect(Collectors.toSet());

		// 比對生產日期用
		String catalogueStartDate = filterRelatedGroupDrawingList.get(0).getPartCatalogueStartDate();
		String catalogueMsName = filterRelatedGroupDrawingList.get(0).getPartCatalogueMS();

		// 分類過程自動幫忙把相同Object過濾掉 -> 件號索引目錄使用 (Part GroupCode PNC 需要另外過濾)

		ArrayList<GroupDrawingRelatedPart> filterRelatedPartList = allRelatedPartList.stream()
				.filter(obj -> groupDrawingIdList.contains(obj.getGroupDrawingId())
						&& compareDateOnlyYMD(catalogueStartDate, obj.getEndDate()))
				.map(obj -> obj).collect(Collectors.toCollection(ArrayList::new));

		Map<String, Integer> indexPartMap = new HashMap<String, Integer>();
		ArrayList<GroupDrawingRelatedPart> indexPartList = filterRelatedPartList.stream()
				.filter(obj -> !isExistKey(indexPartMap, obj.getPart(), obj.getGroupCode(), obj.getPnc()))
				.map(obj -> obj).collect(Collectors.toCollection(ArrayList::new));

		// 3.
		// 附錄編號索引使用 相同GroupCode過濾
		Map<String, ArrayList<GroupCode>> indexGroupNoMap = new TreeMap<String, ArrayList<GroupCode>>();
		List<String> groupCodeSet = filterRelatedPartList.stream().map(obj -> obj.getGroupCode()).distinct().sorted()
				.collect(Collectors.toList());
		// 有機會修改這部分問題 <優化>
		for (String groupCode : groupCodeSet) {
			for (GroupCode groupCodeObj : allGroupCodeList) {
				if (!groupCodeObj.getGroupCode().equals(groupCode)) {
					continue;
				}
				filterGroupCode(indexGroupNoMap, groupCodeObj.getBigGroupCode(), groupCodeObj, allGroupCodeList);
			}
		}

		// 4.獲取圖片 + Detail資料 OK
		// GroupDrawingId 為key
		Map<String, ArrayList<GroupDrawingRelatedPart>> groupDrawingIdPartMap = new HashMap<String, ArrayList<GroupDrawingRelatedPart>>();
		filterRelatedPartList.stream().forEach(obj -> filter(groupDrawingIdPartMap, obj.getGroupDrawingId(), obj));

		// groupCode <Key> -> data
		Map<String, ArrayList<String>> groupDrawingPicMap = new HashMap<String, ArrayList<String>>();
		Map<String, ArrayList<DetailPart>> detailPartMap = new HashMap<String, ArrayList<DetailPart>>();

		for (String groupDrawingId : groupDrawingIdPartMap.keySet()) {
			CatalogueRelatedGroupDrawing relatedGroupDrawingObj = filterRelatedGroupDrawingList.stream()
					.filter(obj -> obj.getGroupDrawingId().equals(groupDrawingId)).findFirst().get();

			for (GroupDrawingRelatedPart relatedPartObj : groupDrawingIdPartMap.get(groupDrawingId)) {

				Map<String, ArrayList<String>> filterMcMap = new HashMap<String, ArrayList<String>>();
				String partStartDate = relatedPartObj.getStartDate();

				for (String adaptMcStr : relatedGroupDrawingObj.getAdaptMC()) {
					if (!allCMCMap.containsKey(adaptMcStr)) {
						continue;
					}
					String mcStartDate = allCMCMap.get(adaptMcStr);
					String finalDate = compareDateOnlyYMD(mcStartDate, partStartDate) ? partStartDate : mcStartDate;
					filter(filterMcMap, finalDate, adaptMcStr);
				}

				for (String finalDate : filterMcMap.keySet()) {
					DetailPart detailPart = new DetailPart();
					detailPart.setPnc(relatedPartObj.getPnc());
					detailPart.setPartNo(relatedPartObj.getPart());
					detailPart.setPartNameCh(relatedPartObj.getPartNameCh());
					detailPart.setPartNameEn(relatedPartObj.getPartNameEn());
					detailPart.setTypeKD(relatedPartObj.getPartKDType());
					detailPart.setQuantity(relatedPartObj.getQuantity());
					detailPart.setAdaptMc(filterMcMap.get(finalDate));
					detailPart.setStartDate(finalDate);
					detailPart.setEndDate(relatedPartObj.getEndDate());
					detailPart.setRemark(relatedPartObj.getPartRemark());
					filter(detailPartMap, relatedPartObj.getGroupCode(), detailPart);
				}
			}

			if (!relatedGroupDrawingObj.getImageFormat().isEmpty()) {
				// 下載Image路徑
				MqlUtil.mqlCommand(context, "trigger off");
				new DomainObject(relatedGroupDrawingObj.getImageId()).checkoutFile(context, false,
						DomainConstants.FORMAT_GENERIC, relatedGroupDrawingObj.getImageFormat(), storePath);
				MqlUtil.mqlCommand(context, "trigger on");

				String picPath = storePath + relatedGroupDrawingObj.getImageFormat();
				this.copyPictureAndUploadImagePicture(picPath, picPath);

				filter(groupDrawingPicMap, relatedGroupDrawingObj.getGroupDrawingGroupCode(), picPath);
			}
		}

		// final. 輸出
		this.name1PD4ML(excelPath, catalogueMsName, catalogueStartDate, indexPartList, indexGroupNoMap,groupDrawingPicMap, detailPartMap, allReplacePartMap);

			this.deleteDirectoryExistFiles(storePath);
			if(new File(excelPath).delete()) {
				System.out.println(excelPath + " delete!");
			}
//		}

	}

	public class DetailPart {
		private String pnc;
		private String partNo;
		private String partNameCh;
		private String partNameEn;
		private String typeKD;
		private String quantity;
		private ArrayList<String> adaptMc;
		private String startDate;
		private String endDate;
		private String remark;

		public String getPnc() {
			return pnc;
		}

		public void setPnc(String pnc) {
			this.pnc = pnc;
		}

		public String getPartNo() {
			return partNo;
		}

		public void setPartNo(String partNo) {
			this.partNo = partNo;
		}

		public String getPartNameCh() {
			return partNameCh;
		}

		public void setPartNameCh(String partNameCh) {
			this.partNameCh = partNameCh;
		}

		public String getPartNameEn() {
			return partNameEn;
		}

		public void setPartNameEn(String partNameEn) {
			this.partNameEn = partNameEn;
		}

		public String getTypeKD() {
			return typeKD;
		}

		public void setTypeKD(String typeKD) {
			this.typeKD = typeKD;
		}

		public String getQuantity() {
			return quantity;
		}

		public void setQuantity(String quantity) {
			this.quantity = quantity;
		}

		public ArrayList<String> getAdaptMc() {
			return adaptMc;
		}

		public void setAdaptMc(ArrayList<String> adaptMc) {
			this.adaptMc = adaptMc;
		}

		public String getStartDate() {
			return startDate;
		}

		public void setStartDate(String startDate) {
			this.startDate = startDate;
		}

		public String getEndDate() {
			return endDate;
		}

		public void setEndDate(String endDate) {
			this.endDate = endDate;
		}

		public String getRemark() {
			return remark;
		}

		public void setRemark(String remark) {
			this.remark = remark;
		}
	}

	/**
	 * 獲取 AlterPart全部資料 (已經排好格式，取出即可使用)
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private Map<String, ArrayList<String>> getAlterPartInfo(Context context) throws Exception {
		Map<String, ArrayList<AlterPart>> allMap = this.getReplacePartInfo(context);
		Map<String, ArrayList<String>> recursiveDataMap = new HashMap<String, ArrayList<String>>();

		for (String keyStr : allMap.keySet()) {
			ArrayList<String> replaceList = new ArrayList<String>();
			this.getAlterPartRecursive(new ArrayList(Arrays.asList(keyStr)), replaceList, allMap);
			recursiveDataMap.put(keyStr, replaceList);
		}
		return recursiveDataMap;
	}

	private void getAlterPartRecursive(List<String> partList, ArrayList<String> replaceList,
			Map<String, ArrayList<AlterPart>> allMap) {
		if (partList.isEmpty()) {
			return;
		}
		/* 方法一 利用ArrayList特性 仿造recursive 效能更好 */
		for (int i = 0; i < partList.size(); ++i) {
			if (!allMap.containsKey(partList.get(i))) {
				continue;
			}
			for (AlterPart alterObj : allMap.get(partList.get(i))) {
				String ans = String.format(ALTER_FORMATSTR, alterObj.getPartNo(), alterObj.getAlterPartNO(),
						alterObj.getExchangeable());
				if (replaceList.contains(ans)) {
					continue;
				}
				replaceList.add(ans);
				partList.add(alterObj.getAlterPartNO());
			}
		}
		/* 方法二 遞迴 可用，但效能較遜色 */
//		boolean breaker = false;
//		for (String part : partList) {
//			if(allMap.containsKey(part)) {
//				for(AlterPart alterObj : allMap.get(part)) {
//					String ans = String.format(ALTER_FORMATSTR, alterObj.getPartNo(),alterObj.getAlterPartNO(), alterObj.getExchangeable());
//					if(replaceList.contains(ans)) {
//						breaker = true;
//						break;
//					}
//					replaceList.add(ans);	
//				}
//				if(breaker) {
//					continue;
//				}
//				List<String> underPartList = allMap.get(part).stream().map(obj -> obj.getAlterPartNO()).collect(Collectors.toList());
//				this.getAlterPartRecursive(underPartList, replaceList, allMap);
//			}
//		}
	}

	/*
	 * json格式 { "groupCode" : "11090", "nameCh" : "", "nameEn" : "" }
	 */
	/**
	 * 單元圖資料維護 獲取全部 (已排序 : groupCode)
	 */
	private ArrayList<GroupCode> getGroupCode(Context context) throws Exception {
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_NAME);
		busSelects.add("attribute[SPLM_Name_EN]");
		busSelects.add("attribute[SPLM_Name_TC]");

		MapList groupCodeMapList = DomainObject.findObjects(context, "SPLM_GroupCode", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"", // where
				null, false, busSelects, (short) 0);

		ArrayList<GroupCode> groupCodeArrayList = new ArrayList<GroupCode>();
		for (Object groupCodeObj : groupCodeMapList) {
			Map groupCodeMap = (Map) groupCodeObj;
			GroupCode groupCode = new GroupCode();
			groupCode.setGroupCode((String) groupCodeMap.get(DomainConstants.SELECT_NAME));
			groupCode.setNameCh((String) groupCodeMap.get("attribute[SPLM_Name_TC]"));
			groupCode.setNameEn((String) groupCodeMap.get("attribute[SPLM_Name_EN]"));
			groupCodeArrayList.add(groupCode);
		}

		groupCodeArrayList.sort(Comparator.comparing(GroupCode::getGroupCode));
		return groupCodeArrayList;
	}

	public class GroupCode {
		private String groupCode;
		private String bigGroupCode;
		private String smallGroupCode;
		private String nameCh;
		private String nameEn;

		public GroupCode() {
			this.groupCode = "";
			this.bigGroupCode = "";
			this.smallGroupCode = "";
			this.nameCh = "";
			this.nameEn = "";
		}

		public String getBigGroupCode() {
			return bigGroupCode;
		}

		public String getNameCh() {
			return nameCh;
		}

		public void setNameCh(String nameCh) {
			this.nameCh = nameCh;
		}

		public String getNameEn() {
			return nameEn;
		}

		public void setNameEn(String nameEn) {
			this.nameEn = nameEn;
		}

		public String getGroupCode() {
			return groupCode;
		}

		public void setGroupCode(String groupCode) {
			this.groupCode = groupCode;
			if (groupCode.length() > 2) {
				this.bigGroupCode = groupCode.substring(0, 2);
				this.smallGroupCode = groupCode.substring(2);
			} else {
				this.bigGroupCode = groupCode;
				this.smallGroupCode = "";
			}
		}

		public String getSmallGroupCode() {
			return smallGroupCode;
		}
	}

	/* ---------------------------------- */
	/**
	 * 獲取 ModelCode -> startDate (過濾開始日空值)
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private Map<String, String> getcmcCodeInfo(Context context) throws Exception {
		StringList busSelect = new StringList();
		busSelect.add("attribute[SPLM_CMC_Code]");
		busSelect.add("attribute[SPLM_EnableDate]");

		MapList mcMapList = DomainObject.findObjects(context, "SPLM_ModelCode", // type
				"*", // name
				"*", // revision
				"*", // owner
				VAULT, // vault
				"", // where
				null, false, busSelect, (short) 0);

		return (Map<String, String>) mcMapList.stream()
				.filter(obj -> !((String) ((Map) obj).get("attribute[SPLM_EnableDate]")).isEmpty())
				.collect(Collectors.toMap(obj -> ((Map) obj).get("attribute[SPLM_CMC_Code]"),
						obj -> ((String) ((Map) obj).get("attribute[SPLM_EnableDate]")).isEmpty() ? "1900/01/01"
								: exchangeDate((String) ((Map) obj).get("attribute[SPLM_EnableDate]")),
						(o, n) -> n = o));
	}

	/* ---------------------------------- */

	/**
	 * 獲取 Catalogue -> relatedGroupDrawing -> Image 整條線上資料 (無排序)
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private ArrayList<CatalogueRelatedGroupDrawing> getCatalogueRelatedInfo(Context context) throws Exception {
		StringList relSelects = new StringList();
		relSelects.add("from.attribute[SPLM_ModelSeries]"); // partsCatalogue fontPage Name
		relSelects.add("from.attribute[SPLM_EnableDate]"); // partsCatalogue startDate
		relSelects.add("attribute[SPLM_CMC_Code]"); // adapt MC
		relSelects.add("from.name");
		relSelects.add("to.name");
		relSelects.add("from.id");
		relSelects.add("to.id");
		relSelects.add("to.attribute[SPLM_GroupCode]");
		relSelects.add("to.from[SPLM_GroupDrawingImage].to.id");
		relSelects.add("to.from[SPLM_GroupDrawingImage].to." + DomainConstants.SELECT_FORMAT_GENERIC);
		StringList sortSelects = new StringList();

		RelationshipWithSelectList relatedGroupDrawingDatas = DomainRelationship.query(context,
				"SPLM_RelatedGroupDrawing", // relationship
				VAULT, // vault
				"from.type == SPLM_PartsCatalogue", // relationship expression
				(short) 0, // limit
				relSelects, // rel StringList
				sortSelects); // sort StringList

		Map<String, Integer> mapping = new HashMap<String, Integer>();
		ArrayList<CatalogueRelatedGroupDrawing> relatedGroupDrawingArrayList = new ArrayList<CatalogueRelatedGroupDrawing>();
		for (RelationshipWithSelect relatedGroupDrawingData : relatedGroupDrawingDatas) {
			String catalogueMs = relatedGroupDrawingData.getSelectData("from.attribute[SPLM_ModelSeries]");
			String catalogueStartDate = relatedGroupDrawingData.getSelectData("from.attribute[SPLM_EnableDate]");
			String adaptMc = relatedGroupDrawingData.getSelectData("attribute[SPLM_CMC_Code]");
			String catalogue = relatedGroupDrawingData.getSelectData("from.name");
			String groupDrawing = relatedGroupDrawingData.getSelectData("to.name");
			String catalogueId = relatedGroupDrawingData.getSelectData("from.id");
			String groupDrawingId = relatedGroupDrawingData.getSelectData("to.id");
			String groupDrawingGroupCode = relatedGroupDrawingData.getSelectData("to.attribute[SPLM_GroupCode]");
			String imageId = relatedGroupDrawingData.getSelectData("to.from[SPLM_GroupDrawingImage].to.id");
			String imageFormat = relatedGroupDrawingData
					.getSelectData("to.from[SPLM_GroupDrawingImage].to." + DomainConstants.SELECT_FORMAT_GENERIC);

			if (!this.isExistKey(mapping, catalogueId, adaptMc, catalogueStartDate, groupDrawingId)) {
				CatalogueRelatedGroupDrawing relatedObj = new CatalogueRelatedGroupDrawing();
				relatedObj.setAdaptMC(adaptMc);
				relatedObj.setGroupDrawing(groupDrawing);
				relatedObj.setGroupDrawingId(groupDrawingId);
				relatedObj.setGroupDrawingGroupCode(groupDrawingGroupCode);
				relatedObj.setImageFormat(imageFormat);
				relatedObj.setImageId(imageId);
				relatedObj.setPartCatalogue(catalogue);
				relatedObj.setPartCatalogueId(catalogueId);
				relatedObj.setPartCatalogueMS(catalogueMs);
				relatedObj.setPartCatalogueStartDate(catalogueStartDate);
				relatedGroupDrawingArrayList.add(relatedObj);
			}
		}
		return relatedGroupDrawingArrayList;
	}

	/*
	 * Json 格式 { "partCatalogue": "NBDBA4106H",
	 * "partCatalogueId":"64040.25682.16707.50174", "partCatalogueStartDate":
	 * "1900/01/01", "partCatalogueMS": "DJ1A", "adaptMC": [ "SRHYLFQ", "SRLYLFQ",
	 * "SRMYLFQ", "SRPYLFQ" ], "GroupDrawing": "NBDBA4106H-11100",
	 * "GroupDrawingId":"64040.25682.6321.10402", "GroupDrawingGroupCode":"11100",
	 * "imageId": "", "imageFormat": "" }
	 */
	public class CatalogueRelatedGroupDrawing {
		private String partCatalogue;
		private String partCatalogueId;
		private String partCatalogueStartDate;
		private String partCatalogueMS;
		private ArrayList<String> adaptMC;
		private String GroupDrawing;
		private String GroupDrawingId;
		private String GroupDrawingGroupCode;
		private String imageId;
		private String imageFormat;

		public String getPartCatalogue() {
			return partCatalogue;
		}

		public void setPartCatalogue(String partCatalogue) {
			this.partCatalogue = partCatalogue;
		}

		public String getPartCatalogueId() {
			return partCatalogueId;
		}

		public void setPartCatalogueId(String partCatalogueId) {
			this.partCatalogueId = partCatalogueId;
		}

		public String getPartCatalogueStartDate() {
			return partCatalogueStartDate;
		}

		public void setPartCatalogueStartDate(String partCatalogueStartDate) {
			if (!partCatalogueStartDate.isEmpty()) {
				this.partCatalogueStartDate = exchangeDate(partCatalogueStartDate);
			} else {
				this.partCatalogueStartDate = "1900/01/01";
			}
		}

		public String getPartCatalogueMS() {
			return partCatalogueMS;
		}

		public void setPartCatalogueMS(String partCatalogueMS) {
			this.partCatalogueMS = partCatalogueMS;
		}

		public ArrayList<String> getAdaptMC() {
			return adaptMC;
		}

		public void setAdaptMC(String adaptMC) {
			this.adaptMC = Arrays.stream(adaptMC.split(",")).collect(Collectors.toCollection(ArrayList::new));
		}

		public String getGroupDrawing() {
			return GroupDrawing;
		}

		public void setGroupDrawing(String groupDrawing) {
			GroupDrawing = groupDrawing;
		}

		public String getGroupDrawingId() {
			return GroupDrawingId;
		}

		public void setGroupDrawingId(String groupDrawingId) {
			GroupDrawingId = groupDrawingId;
		}

		public String getGroupDrawingGroupCode() {
			return GroupDrawingGroupCode;
		}

		public void setGroupDrawingGroupCode(String groupDrawingGroupCode) {
			GroupDrawingGroupCode = groupDrawingGroupCode;
		}

		public String getImageId() {
			return imageId;
		}

		public void setImageId(String imageId) {
			this.imageId = imageId;
		}

		public String getImageFormat() {
			return imageFormat;
		}

		public void setImageFormat(String imageFormat) {
			this.imageFormat = imageFormat;
		}
	}

	/* ---------------------------------- */
	/**
	 * 獲取 groupDrawing -> relatedPart -> replacePart 整條線上的資料 (已排序)
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private ArrayList<GroupDrawingRelatedPart> getGroupDrawingRelatedPartInfo(Context context) throws Exception {
		StringList busSelect = new StringList();
		busSelect.add(DomainConstants.SELECT_ID);
		busSelect.add("attribute[SPLM_GroupDrawing]");
		busSelect.add("attribute[SPLM_GroupCode]");
		busSelect.add("attribute[SPLM_EnableDate]");
		busSelect.add("attribute[SPLM_DisableDate]");
		busSelect.add("attribute[SPLM_PNC]");
		busSelect.add("attribute[Quantity]");
		busSelect.add("from.name");
		busSelect.add("to.name");
		busSelect.add("from.id");
		busSelect.add("to.id");
		busSelect.add("to.attribute[SPLM_Name_TC]");
		busSelect.add("to.attribute[SPLM_Name_EN]");
		busSelect.add("to.attribute[SPLM_PB_Note]");
		busSelect.add("to.attribute[SPLM_MaterialGroup]");
		StringList orderSelect = new StringList();
		orderSelect.add("to");

		RelationshipWithSelectList relatedPartDatas = DomainRelationship.query(context, "SPLM_RelatedPart", VAULT,
				"from.type == SPLM_GroupDrawing", (short) 0, busSelect, orderSelect);

		Map<String, Integer> mapping = new HashMap<String, Integer>();
		ArrayList<GroupDrawingRelatedPart> GroupCodeRelatedPartArrayList = new ArrayList<GroupDrawingRelatedPart>();
		for (RelationshipWithSelect relatedPartData : relatedPartDatas) {
			String groupDrawing = relatedPartData.getSelectData("from.name");
			String groupDrawingId = relatedPartData.getSelectData("from.id");
			String attPnc = relatedPartData.getSelectData("attribute[SPLM_PNC]");
			String attGroupCode = relatedPartData.getSelectData("attribute[SPLM_GroupCode]");
			String attStartDate = relatedPartData.getSelectData("attribute[SPLM_EnableDate]");
			String attEndDate = relatedPartData.getSelectData("attribute[SPLM_DisableDate]");
			String attQuantity = relatedPartData.getSelectData("attribute[Quantity]");
			String part = relatedPartData.getSelectData("to.name");
			String partId = relatedPartData.getSelectData("to.id");
			String partNameCh = relatedPartData.getSelectData("to.attribute[SPLM_Name_TC]");
			String partNameEn = relatedPartData.getSelectData("to.attribute[SPLM_Name_EN]");
			String partRemark = relatedPartData.getSelectData("to.attribute[SPLM_PB_Note]");
			String partMaterialGroup = relatedPartData.getSelectData("to.attribute[SPLM_MaterialGroup]");

			if (!this.isExistKey(mapping, groupDrawing, attStartDate, attQuantity, part)) {
				GroupDrawingRelatedPart relatedObj = new GroupDrawingRelatedPart();
				relatedObj.setGroupDrawing(groupDrawing);
				relatedObj.setGroupDrawingId(groupDrawingId);
				relatedObj.setPnc(attPnc);
				relatedObj.setGroupCode(attGroupCode);
				relatedObj.setStartDate(attStartDate);
				relatedObj.setEndDate(attEndDate);
				relatedObj.setQuantity(attQuantity);
				relatedObj.setPart(part);
				relatedObj.setPartId(partId);
				relatedObj.setPartNameCh(partNameCh);
				relatedObj.setPartNameEn(partNameEn);
				relatedObj.setPartKDType(partMaterialGroup);
				relatedObj.setPartRemark(partRemark);
				GroupCodeRelatedPartArrayList.add(relatedObj);
			}
		}
		// (deseperate) 按照 part 名稱(數字, 英文) 排序 (內建有了)
		// GroupCodeRelatedPartArrayList.sort(Comparator.comparing(GroupCodeRelatedPart::getPart,
		// String.CASE_INSENSITIVE_ORDER));

		return GroupCodeRelatedPartArrayList;
	}

	/*
	 * Json 格式 { "groupDrawingId": "64040.25682.6110.50986",
	 * "groupDrawing":"CM9943130A1", "groupCode": "11010", "quantity": "1.0",
	 * "startDate":"2022/01/09", "endDate": "9999/12/31", "pnc": "66978C",
	 * "bigGroupCode": "11", "smallGroupCode": "010", "part": "CW761307",
	 * "partId":"64040.25682.23657.24068", "partKDType": "D", "partRemark": "",
	 * "replacePart": [] }
	 */
	public class GroupDrawingRelatedPart {
		private String groupDrawingId;
		private String groupDrawing;
		private String groupCode;
		private String quantity;
		private String startDate;
		private String endDate;
		private String pnc;
		private String bigGroupCode;
		private String smallGroupCode;
		private String part;
		private String partId;
		private String partNameCh;
		private String partNameEn;
		private String partKDType;
		private String partRemark;

		public String getBigGroupCode() {
			return bigGroupCode;
		}

		public String getSmallGroupCode() {
			return smallGroupCode;
		}

		public String getQuantity() {
			return quantity;
		}

		public void setQuantity(String quantity) {
			this.quantity = quantity;
		}

		public String getStartDate() {
			return startDate;
		}

		public void setStartDate(String startDate) throws Exception {
			if (!startDate.trim().isEmpty()) {
				this.startDate = exchangeDate(startDate);
			} else {
				this.startDate = "1900/01/01";
			}
		}

		public String getEndDate() {
			return endDate;
		}

		public void setEndDate(String endDate) {
			if (!endDate.trim().isEmpty()) {
				this.endDate = exchangeDate(endDate);
			} else {
				this.endDate = "9999/12/31";
			}
		}

		public String getGroupDrawingId() {
			return groupDrawingId;
		}

		public void setGroupDrawingId(String groupDrawingId) {
			this.groupDrawingId = groupDrawingId;
		}

		public String getGroupDrawing() {
			return groupDrawing;
		}

		public void setGroupDrawing(String groupDrawing) {
			this.groupDrawing = groupDrawing;
		}

		public String getGroupCode() {
			return groupCode;
		}

		public void setGroupCode(String groupCode) {
			this.groupCode = groupCode;
			if (groupCode.length() > 2) {
				this.bigGroupCode = groupCode.substring(0, 2);
				this.smallGroupCode = groupCode.substring(2);
			} else {
				this.bigGroupCode = groupCode;
				this.smallGroupCode = "";
			}
		}

		public String getPnc() {
			return pnc;
		}

		public void setPnc(String pnc) {
			this.pnc = pnc;
		}

		public String getPart() {
			return part;
		}

		public void setPart(String part) {
			this.part = part;
		}

		public String getPartId() {
			return partId;
		}

		public void setPartId(String partId) {
			this.partId = partId;
		}

		public String getPartNameCh() {
			return partNameCh;
		}

		public void setPartNameCh(String partNameCh) {
			this.partNameCh = partNameCh;
		}

		public String getPartNameEn() {
			return partNameEn;
		}

		public void setPartNameEn(String partNameEn) {
			this.partNameEn = partNameEn;
		}

		public String getPartKDType() {
			return partKDType;
		}

		public void setPartKDType(String partKDType) {
			if (partKDType.trim().isEmpty()) {
				this.partKDType = "";
				return;
			}
			switch (partKDType) {
			case "KD":
			case "KDY":
				this.partKDType = "K";
				break;
			default:
				this.partKDType = "D";
			}
		}

		public String getPartRemark() {
			return partRemark;
		}

		public void setPartRemark(String partRemark) {
			this.partRemark = partRemark;
		}
	}
	/* --------------------------------- */

	private Map<String, ArrayList<AlterPart>> getReplacePartInfo(Context context) throws Exception {
		StringList busSelect = new StringList();
		busSelect.add("to.name");
		busSelect.add("from.name");
		busSelect.add("to.id");
		busSelect.add("from.id");
		busSelect.add("attribute[SPLM_OptionalExchangeable]");
		StringList orderSelect = new StringList();

		RelationshipWithSelectList relatedPartDatas = DomainRelationship.query(context, "SPLM_RelatedOptionalPart",
				VAULT, "from.type == SPLM_Part  && attribute[SPLM_OptionalType]=='Alternate Part'", (short) 0,
				busSelect, orderSelect);

		Map<String, ArrayList<AlterPart>> mapping = new HashMap<String, ArrayList<AlterPart>>();
		for (RelationshipWithSelect relatedPartData : relatedPartDatas) {
			String partNo = relatedPartData.getSelectData("from.name");
			String partId = relatedPartData.getSelectData("from.id");
			String exchagenable = relatedPartData.getSelectData("attribute[SPLM_OptionalExchangeable]");
			String alterPartNo = relatedPartData.getSelectData("to.name");
			String alterPartId = relatedPartData.getSelectData("to.id");

			AlterPart tempAlter = new AlterPart();
			tempAlter.setPartNo(partNo);
			tempAlter.setPartId(partId);
			tempAlter.setExchangeable(exchagenable);
			tempAlter.setAlterPartNO(alterPartNo);
			tempAlter.setAlterPartId(alterPartId);
			filter(mapping, partNo, tempAlter);
		}
		return mapping;
	}

	public class AlterPart {
		private String partNo;
		private String partId;
		private String exchangeable;
		private String alterPartNO;
		private String alterPartId;

		public String getPartNo() {
			return partNo;
		}

		public void setPartNo(String partNo) {
			this.partNo = partNo;
		}

		public String getPartId() {
			return partId;
		}

		public void setPartId(String partId) {
			this.partId = partId;
		}

		public String getExchangeable() {
			return exchangeable;
		}

		public void setExchangeable(String exchangeable) {
			this.exchangeable = exchangeable;
		}

		public String getAlterPartNO() {
			return alterPartNO;
		}

		public void setAlterPartNO(String alterPartNO) {
			this.alterPartNO = alterPartNO;
		}

		public String getAlterPartId() {
			return alterPartId;
		}

		public void setAlterPartId(String alterPartId) {
			this.alterPartId = alterPartId;
		}
	}

	/* --------------------------------- */
	private String exchangeDate(String date) {
		String[] dates = date.substring(0, date.indexOf(" ")).split("/");
		return dates[2] + "/" + this.fillPrefixZero(dates[0], 2) + "/" + this.fillPrefixZero(dates[1], 2);
	}

	private String fillPrefixZero(String value, int fillLength) {
		int valueLength = value.length();
		if (valueLength < fillLength) {
			int left = fillLength - valueLength;
			value = String.format("%0" + left + "d%s", 0, value);
		}
		return value;
	}

	private String translateJsonIntoString(Object obj) {
		return new GsonBuilder().disableHtmlEscaping().create().toJson(obj);
	}

	private boolean isExistKey(Map<String, Integer> map, String... keys) {
		String key = String.join("_", keys);
		if (map.containsKey(key)) {
			return true;
		}
		map.put(key, 1);
		return false;
	}

	private <E> void filter(Map<String, ArrayList<E>> map, String filterStr, E input) {
		if (!map.containsKey(filterStr)) {
			ArrayList<E> array = new ArrayList<E>();
			map.put(filterStr, array);
		}
		map.get(filterStr).add(input);
	}

	private void filterGroupCode(Map<String, ArrayList<GroupCode>> indexGroupNoMap, String bigGroupCode,
			GroupCode groupCodeObj, ArrayList<GroupCode> allGroupCodeList) {
		if (!indexGroupNoMap.containsKey(bigGroupCode)) {
			ArrayList<GroupCode> groupCodeList = new ArrayList<GroupCode>();
			indexGroupNoMap.put(bigGroupCode, groupCodeList);
			groupCodeList.add(allGroupCodeList.stream().filter(obj -> bigGroupCode.equals(obj.getGroupCode()))
					.findFirst().orElse(new GroupCode()));
		}
		filter(indexGroupNoMap, bigGroupCode, groupCodeObj);
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

	/**
	 * 測試用 compareDate理解
	 */
	public void compareDateOnlyYMDTesting() {
		System.out.println(this.compareDateOnlyYMD("2022/01/05", "9999/12/31"));
		System.out.println(this.compareDateOnlyYMD("2022/01/08", "2022/01/09"));
		System.out.println(this.compareDateOnlyYMD("2022/01/11", "2022/01/09"));
		System.out.println(this.compareDateOnlyYMD("2022/01/09", "2022/01/09"));
		System.out.println(this.compareDateOnlyYMD("2023/01/09", "2022/01/09"));
		System.out.println(this.compareDateOnlyYMD("2022/11/09", "2022/01/09"));
		System.out.println(this.compareDateOnlyYMD("2022/11/09", "2022/12/09"));
	}

	// 開始結束的日期格式需要相同 -> startDate >= endDate (設計概念)
	private boolean compareDateOnlyYMD(String startDate, String endDate) {
		if(startDate.isEmpty()) {
			return true;
		} else if (endDate.isEmpty()) {
			return false;
		}

		char[] startDateChars = startDate.toCharArray();
		char[] endDateChars = endDate.toCharArray();
		for (int i = 0; i < startDateChars.length; ++i) {
			int temp = endDateChars[i] - startDateChars[i];
			if (temp == 0) {
				continue;
			} else if (temp < 0) {
				return false;
			} else if (temp > 0) {
				return true;
			}
		}
		return true; // 當結束日和開始日期相同
	}

	private final String TEMP_PIC = "C:/temp/Morga/pic/RE1442010001A.jpg";

	/**
	 * 測試用 圖片縮放比例
	 */
	public void testPic() {
		String oldPath = TEMP_PIC;
		String newPath = TEMP_PIC;
		try {
			copyPictureAndUploadImagePicture(oldPath, newPath);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private void copyPictureAndUploadImagePicture(String oldPath, String newPath) throws Exception {
		File newFile = new File(newPath);

		int maxHeight = 990;
		int maxWidth = 1500;
		try {
			BufferedImage originalImg = ImageIO.read(new FileInputStream(new File(oldPath)));
			BufferedImage newImg = scalingByHeight(originalImg, maxHeight);
			ImageIO.write(newImg, "jpg", newFile);
			BufferedImage secondImg = ImageIO.read(new FileInputStream(newFile));
			if (secondImg.getWidth() > maxWidth) {
				newImg = scalingByWidth(secondImg, maxWidth);
				ImageIO.write(newImg, "jpg", newFile);
			}
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}
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

	private BufferedImage scalingByWidth(BufferedImage img, int newWidth) {
		BigDecimal width = new BigDecimal(img.getWidth());
		BigDecimal height = new BigDecimal(img.getHeight());
		int newHeight = height.multiply(new BigDecimal(newWidth)).divide(width, 10, RoundingMode.HALF_UP)
				.setScale(5, RoundingMode.HALF_UP).intValue();
		BufferedImage newImg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		newImg.getGraphics().drawImage(img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
		return newImg;
	}

	private final static String DEFAULT_ROOT_DIRECTORY = "C:\\TEMP\\Morga\\";

	public void outputFile(String fileName, String dataStr) throws IOException {
		outputDataFile(fileName, dataStr, true);
	}

	public void outputOverWriteFile(String fileName, String dataStr) throws IOException {
		outputDataFile(fileName, dataStr, false);
	}

	public void outputDataFile(String fileName, String dataStr, boolean overwrite) throws IOException {
		File dataFile = new File(DEFAULT_ROOT_DIRECTORY + fileName);
		File dataRootFile = new File(DEFAULT_ROOT_DIRECTORY);
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
//			bw.write("******* " + convertNowDateFormat("yyyy:MM:dd HH:mm:ss") + " ******* " + "\n\n");
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("COULD NOT WRITE!!");
		}
	}

	public String convertNowDateFormat(String formatType) {
		return convertDateFormat(new Date().toGMTString(), formatType);
	}

	public String convertDateFormat(String targetDate, String dateFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		Date d = new Date(targetDate);
		return sdf.format(d);
	}

	/*
	 * -----------------------------------------------------------------------------
	 * -------------
	 */
	/**
	 * 測試用 Dassult系統
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public void RelatedPartTest(Context context, String[] args) throws Exception {
//		 測試 RelatedPart 線上資料
		System.out.println(translateJsonIntoString(this.getGroupDrawingRelatedPartInfo(context)));
	}

	public void RelatedGroupDrawingTest(Context context, String[] args) throws Exception {
//		 測試 RelatedGroupDrawing 線上資料
		System.out.println(translateJsonIntoString(this.getCatalogueRelatedInfo(context)));
	}

	public void GroupCodeTest(Context context, String[] args) throws Exception {
//		 測試 groupCode 線上資料
		System.out.println(translateJsonIntoString(this.getGroupCode(context)));
	}

	public void CMCCodeTest(Context context, String[] args) throws Exception {
//		 測試 cmcCode 線上資料
		System.out.println(translateJsonIntoString(this.getcmcCodeInfo(context)));
	}

	public void AlterPartTest(Context context, String[] args) throws Exception {
//		 測試 alter 線上資料
		System.out.println(translateJsonIntoString(this.getReplacePartInfo(context)));
	}

	public void AbbreDownloadTest(Context context, String[] args) throws Exception {
		String partCatalogueId = "32355.13920.48907.64138";
		getAbbrePath(context, partCatalogueId, this.getTempFolderPath());
	}

	public void replacePartOnlyOneTest(Context context, String[] args) throws Exception {
		// 修改partNo 的名稱即可
		List<String> partArray = Arrays.asList("MR224500");
		ArrayList<String> replaceList = new ArrayList<String>();
		this.getAlterPartRecursive(new ArrayList(partArray), replaceList, this.getReplacePartInfo(context));
		System.out.println(String.join("\n", replaceList));
	}

	/**
	 * 測試用，了解書籤內建 bookmark 用法
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public void pd4mlCustomerBookMarkTest(Context context, String[] args) throws Exception {
		StringBuffer html = new StringBuffer();
		StringBuffer css = new StringBuffer();
		String headerStr = "";
		css.append(
				"div.div_page_boxer {margin: 0px; padding : 0px; font-family: DFKai-sb; height:1220px;max-height:100%; width:100%; word-break: break-all;}");
//		css.append("h1 { pd4ml-bookmark-visibility: hidden }");
//		css.append("h2 { pd4ml-bookmark-visibility: hidden }");

		html.append("<html>");
		html.append("<body>");

		html.append("<div>");
		html.append("<p>dfjakldjf<a name=\"adf adfa\">&nbsp;</a></p>");
		html.append("<p>dfjakldjf</p>");
		html.append("<p>dfjakldjf</p>");
		html.append("<p>dfjakldjf</p>");
		html.append("</div>");
		html.append("<pd4ml-page-break/>");
		html.append("<div>");
		html.append("<p>dfjakldjf</p>");
		html.append("<p>dfjakldjf</p>");
		html.append("<p>dfjakldjf</p>");
		html.append("<p>dfjakldjf</p>");
		html.append("</div>");

		html.append("<div>");
		html.append("<p>dfjakldjf</p>");
		html.append("<p>dfjakldjf</p>");
		html.append("<p>dfjakldjf</p>");
		html.append("<p>dfjakldjf</p>");
		html.append("</div>");
		html.append("<pd4ml-page-break/>");
		html.append("<div>");
		html.append("<table>");
		html.append("<thead>");
		html.append("<tr><th>title</th></tr>");
		html.append("</thead>");
		html.append("<tbody>");
		for (int i = 0; i < 200; i++) {
			html.append("<tr><td>" + i + "</td></tr>");			
		}
		html.append("</tbody>");
		html.append("</table>");
		html.append("</div>");
		
		html.append("</body>");
		html.append("</html>");



		String htmlPath = "C:/temp/morga/htmlStructure.txt";
		String cssPath = "C:/temp/morga/cssStyle.txt";
		String htmlStr = String.join("", Files.readAllLines(Paths.get(htmlPath)));
		String cssStr = String.join("", Files.readAllLines(Paths.get(cssPath)));

		this.generatePDFPro(new File(OUTPUT_PATH2), htmlStr, cssStr, headerStr);
	}
}
