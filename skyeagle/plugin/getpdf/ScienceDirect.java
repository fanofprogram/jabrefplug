package skyeagle.plugin.getpdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


import skyeagle.plugin.gui.UpdateDialog;

public class ScienceDirect implements GetPdfFile {

	private String url;

	public ScienceDirect(String url) {
		this.url = url;
	}

	/* (non-Javadoc)
	 * @see skyeagle.plugin.getpdf.GetPdfFile#getFile(skyeagle.plugin.gui.UpdateDialog, java.io.File, java.lang.Boolean)
	 */
	/* (non-Javadoc)
	 * @see skyeagle.plugin.getpdf.GetPdfFile#getFile(skyeagle.plugin.gui.UpdateDialog, java.io.File, java.lang.Boolean)
	 */
	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {

//		// ��ȡת����ַ
		String newUrl = null;
		String cook = null;
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			con.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
			con.addRequestProperty("User-Agent", "Mozilla");
			con.addRequestProperty("Referer", "google.com");
			boolean redirect = false;
			do {
				// normally, 3xx is redirect
				int status = ((HttpURLConnection) con).getResponseCode();
				cook = ((HttpURLConnection) con).getHeaderField("Set-Cookie");
				if (status != HttpURLConnection.HTTP_OK) {
					if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
							|| status == HttpURLConnection.HTTP_SEE_OTHER) {
						redirect = true;
						// get redirect url from "location" header field
						newUrl = ((HttpURLConnection) con).getHeaderField("Location");
						// get the cookie if need, for login
						// open the new connnection again
						con = (HttpURLConnection) new URL(newUrl).openConnection();
						con.setRequestProperty("Cookie", cook);
						con.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
						con.addRequestProperty("User-Agent", "Mozilla");
						con.addRequestProperty("Referer", "google.com");
					}
				} else {
					redirect = false;
				}
			} while (redirect);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String[] str1 = cook.split(";");
		// ����Map����
		Map<String, String> map = new HashMap<>();
		// ѭ������map����
		for (int i = 0; i < str1.length; i++) {
			// ����"="��ȡ�ַ�������
			if(str1[i].contains("=")){
			String[] str2 = str1[i].split("=");
			// str2[0]ΪKEY,str2[1]Ϊֵ
			map.put(str2[0], str2[1]);
			}
		}
		// ��ȡ��ַ�����ݣ�html)��cookies
		Map<String, String> cookies = new TreeMap<>();
		String pagecontent = GetPDFUtil.initGetPDF(newUrl, usingProxy, cookies);
		if (pagecontent == null) {
			dig.output("���粻ͨ�������������硣");
			return;
		}
		System.out.println(cookies.toString());
		// ʹ��Jsoup���html���ݽ��н���
		Document doc1 = Jsoup.parse(pagecontent);
		// ����Jsoup�е�ѡ����Ѱ����Ҫ�Ľڵ�, �ύ�ı���ַ��ip��ַѡ�����
		String posturl = doc1.select("form.chooseorg-form").attr("action");
		System.out.println(posturl);

		String orgUrl = "https://www.sciencedirect.com/user/chooseorg/api/submit";
		try {
			// JSONObject payload = new JSONObject();
			// // {"dept":"47407","rememberOrg":"Y"}
			// payload.put("dept", 47407);
			// payload.put("rememberOrg", "Y");
			// payload.put("typeTab", 1);
			Connection connection = Jsoup.connect(posturl).userAgent(
					"Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36") 
					.header("Content-Type", "application/json;charset=UTF-8")
					.header("Accept", "application/json, text/plain, */*").header("Accept-Encoding", "gzip, deflate, br")
					.header("Accept-Language", "zh-CN,zh;q=0.9").header("Connection", "keep-alive")
					.header("referer", posturl)
					.header("x-sdfe-corr-id","atp-dee1120b-9e6c-45bf-bca0-3ba37008aba0")
					.cookies(map)
					.requestBody("{\"dept\":\"47407\",\"rememberOrg\":\"Y\"}")
					.followRedirects(true)
					.ignoreContentType(true)
					.ignoreHttpErrors(true)
					.timeout(1000 * 10).method(Connection.Method.POST);
			Response response = connection.execute();
			cookies = response.cookies();
//			Document doc = connection.post();
			System.out.println(response.statusCode());
//			System.out.println(doc.toString());
			System.out.println(cookies.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}
		// *************�������վ��ȡ���ص�����************************
		// ��ȡ��������
		// // ��pdf������
		// // ����ʹ�ô�������cookies����ʱ��ʹ��cookies�൱��ʹ���˴������Բ����ٹҴ�����
		// HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink,
		// cookies,false);
		// con.setRequestProperty("Referer", newUrl); // ����Ϊû����䣬һֱ���ز��ˣ�����һ�����ԭ��
		// int filesize = con.getContentLength();
		// // �������վ��ȡpdf�ļ�
		// GetPDFUtil.getPDFFile(file, filesize, dig, con);
		// con.disconnect();
	}

	public static void main(String[] args) throws IOException {
		String str = "http://www.sciencedirect.com/science/article/pii/S2211285519311073";
		File file = new File("F:\\test.pdf");
		new ScienceDirect(str).getFile(null, file, false);
	}
}
