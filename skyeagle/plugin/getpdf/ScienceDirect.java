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

//		// 获取转发网址
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
		// 创建Map对象
		Map<String, String> map = new HashMap<>();
		// 循环加入map集合
		for (int i = 0; i < str1.length; i++) {
			// 根据"="截取字符串数组
			if(str1[i].contains("=")){
			String[] str2 = str1[i].split("=");
			// str2[0]为KEY,str2[1]为值
			map.put(str2[0], str2[1]);
			}
		}
		// 获取网址的内容（html)和cookies
		Map<String, String> cookies = new TreeMap<>();
		String pagecontent = GetPDFUtil.initGetPDF(newUrl, usingProxy, cookies);
		if (pagecontent == null) {
			dig.output("网络不通，请检查代理和网络。");
			return;
		}
		System.out.println(cookies.toString());
		// 使用Jsoup库对html内容进行解析
		Document doc1 = Jsoup.parse(pagecontent);
		// 利用Jsoup中的选择器寻找需要的节点, 提交的表单地址（ip地址选择表单）
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
		// *************下面从网站获取返回的数据************************
		// 读取返回内容
		// // 打开pdf的连接
		// // 由于使用代理获得了cookies。这时候，使用cookies相当于使用了代理，所以不用再挂代理了
		// HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink,
		// cookies,false);
		// con.setRequestProperty("Referer", newUrl); // 就因为没加这句，一直下载不了，找了一整天的原因
		// int filesize = con.getContentLength();
		// // 下面从网站获取pdf文件
		// GetPDFUtil.getPDFFile(file, filesize, dig, con);
		// con.disconnect();
	}

	public static void main(String[] args) throws IOException {
		String str = "http://www.sciencedirect.com/science/article/pii/S2211285519311073";
		File file = new File("F:\\test.pdf");
		new ScienceDirect(str).getFile(null, file, false);
	}
}
