package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import skyeagle.plugin.gui.UpdateDialog;

public class Wiley implements GetPdfFile {

	private String url;

	public Wiley(String url) {
		this.url = url;
	}

	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {

		String basurl = "http://onlinelibrary.wiley.com";
		String link = null;
		// 取得pdf连接地址
		try {
			Connection conn = Jsoup.connect(url)
					.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0")
					.timeout(30000);
			Document doc = conn.get();

			// Elements eles = doc.select("a#wol1backlink");
			// if (eles.size() != 0) // 如果是新页面，则获得旧页面的网址
			// url = doc.select("a#wol1backlink").attr("href");
			// doc = Jsoup.connect(url).timeout(30000).get();

			link = doc.select("a[title=Article PDF]").attr("href");
		} catch (IOException e1) {
			dig.output("页面连接不上，请检查网络。");
			e1.printStackTrace();
			return;
		}
		link = basurl + link;
		// link = link.replaceAll("epdf", "pdf");

		// 获取pdf页面
		// String pdfpage = null;
		// if (usingProxy) {
		// pdfpage = GetPDFUtil.getPageContent(link, GetPDFUtil.getProxy());
		// } else {
		// pdfpage = GetPDFUtil.getPageContent(link);
		// }
		String newUrl = null;
		String cook=null;
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(link).openConnection();
			con.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
			con.addRequestProperty("User-Agent", "Mozilla");
			con.addRequestProperty("Referer", "google.com");
			boolean redirect = false;
			do {
				// normally, 3xx is redirect
				int status = ((HttpURLConnection) con).getResponseCode();
				if (status != HttpURLConnection.HTTP_OK) {
					if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
							|| status == HttpURLConnection.HTTP_SEE_OTHER) {
						redirect = true;
						// get redirect url from "location" header field
						newUrl = ((HttpURLConnection) con).getHeaderField("Location");
						// get the cookie if need, for login
						cook = ((HttpURLConnection) con).getHeaderField("Set-Cookie");

						// open the new connnection again
						con = (HttpURLConnection) new URL(newUrl).openConnection();
						con.setRequestProperty("Cookie", cook);
						con.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
						con.addRequestProperty("User-Agent", "Mozilla");
						con.addRequestProperty("Referer", "google.com");
					}
				}else{
					redirect=false;
				}
			} while (redirect);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String pdflink=newUrl.replace("pdf", "pdfdirect");
		// if (pdflink.isEmpty()) {
		// dig.output("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
		//// System.out.println("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
		// return;
		// }

		// 获取cookies
		Map<String, String> cookies = null;
		if (usingProxy)
			cookies = GetPDFUtil.getCookies(url, GetPDFUtil.getProxy());
		else
			cookies = GetPDFUtil.getCookies(url);

		// 打开pdf的连接
		// 由于使用代理获得了cookies。这时候，使用cookies相当于使用了代理
		// wiley比较特殊，下载的时候也必须挂着代理
		// HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink,
		// cookies,usingProxy);
		HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink, cookies, usingProxy);
		// 下面从网站获取pdf文件
		int filesize = con.getContentLength();
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}

	public static void main(String[] args) {
		String str = "https://onlinelibrary.wiley.com/doi/abs/10.1002/adma.201902980";
		File file = new File("F:\\test.pdf");
		new Wiley(str).getFile(new UpdateDialog(null, "down"), file, false);
	}
}
