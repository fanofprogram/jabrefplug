package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import skyeagle.plugin.gui.UpdateDialog;

public class APS implements GetPdfFile {

//	private String url;

	public APS(String url) {
//		this.url = url;
	}

	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {
		
		dig.output("APS要进行爱因斯坦头像认证，现在还解决不了，请手工下载");
		return;
//		// 获取网址的内容（html)和cookies
//		Map<String, String> cookies = new TreeMap<>();
//		String pagecontent = GetPDFUtil.initGetPDF(url, usingProxy, cookies);
//		if (pagecontent == null) {
//			dig.output("网络不通，请检查代理和网络。");
//			return;
//		}
//		// 使用Jsoup库对html内容进行解析
//		Document doc = Jsoup.parse(pagecontent);
//		// 利用Jsoup中的选择器寻找需要的节点, 这里要找的是pdf文件的连接
//		String orglink = doc.select("div[class=article-nav-actions]>a").attr("href");
//		if (orglink.isEmpty()) {
//			// dig.output("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
//			System.out.println("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
//			return;
//		}
//
//		testEinstein(orglink, cookies);
//		
//		String pdflink = "http://journals.aps.org" + orglink;
//
//		// 打开pdf的连接
//		// 使用cookies
//		HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink, cookies, false);
//		int filesize = con.getContentLength();
//		// 下面从网站获取pdf文件
//		GetPDFUtil.getPDFFile(file, filesize, dig, con);
//		con.disconnect();

	}

	@SuppressWarnings("unused")
	private void testEinstein(String orglink, Map<String, String> cookies) {
		Document doc;
		// APS网站要选择爱因斯坦进行验证
		String link = "http://journals.aps.org" + orglink;
		try {
			doc = Jsoup.connect(link).cookies(cookies).ignoreHttpErrors(true).timeout(30000).get();
			String tmpStr = doc.select("input[name=captcha]").attr("value");
			Elements eles = doc.select("img[class=captcha]");
			ArrayList<String> value = new ArrayList<>();
			for (int i = 0; i < eles.size(); i++) {
				String temp = eles.get(i).attr("src");
				value.add(temp.substring(temp.length() - 9));
			}

			String posturl = "http://journals.aps.org/captcha";

			String choice = "choice=" + value.get(4);
			String method = "_method=PUT";
			String origin = "origin=" + URLEncoder.encode(orglink, "UTF-8");
			String captcha = "captcha=" + tmpStr;
			String postconten = choice + "&" + method + "&" + origin + "&" + captcha;

			URL u = new URL(posturl);
			HttpURLConnection con = (HttpURLConnection) u.openConnection();
			// 提交表单方式为POST，POST 只能为大写，严格限制，post会不识别
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			// 表示我们的连接为纯文本，编码为utf-8
			con.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
			OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
			// 向网站写表单数据
			osw.write(postconten);
			osw.flush();
			osw.close();

			// 获取cookie
			String key = null;
			for (int i = 1; (key = con.getHeaderFieldKey(i)) != null; i++) {
				if (key.equalsIgnoreCase("set-cookie")) {
					String cookie = null;
					cookie = con.getHeaderField(i);
					int i1 = cookie.indexOf("=");
					int i2 = cookie.indexOf(";");
					if (i1 != -1 && i2 != -1) {
						String _value = cookie.substring(i1 + 1, i2);
						String _key = cookie.substring(0, i1);
						cookies.put(_key, _value);
					}
				}
			}

			Response response = Jsoup.connect(link).ignoreHttpErrors(true).cookies(cookies).timeout(30000).execute();
			if (!response.hasHeaderWithValue("Content-Type", "application/pdf")) {
				System.out.println(response.header("Content-Type"));
				testEinstein(orglink, cookies);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		String str = "http://journals.aps.org/prb/abstract/10.1103/PhysRevB.92.094307";
		File file = new File("F:\\test.pdf");
		new APS(str).getFile(null, file, false);
	}
}
