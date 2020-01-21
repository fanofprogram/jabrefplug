package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;

import skyeagle.plugin.gui.UpdateDialog;

public class ACS implements GetPdfFile {

	private String url;

	public ACS(String url) {
		this.url = url;
	}

	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {

		// ACS网站没有权限的话也提供pdf连接，因此这里不用考虑代理问题
		//直接用Jsoup来获取html网页
		String pdfurl = "http://pubs.acs.org";
		Document doc = null;
		try {
			Connection conn= Jsoup.connect(url).header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0").timeout(30000);
			doc =conn.get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 利用Jsoup中的选择器寻找pdf文件的连接
		String pdflink = doc.select("a[title^=PDF]").attr("href");
		pdfurl = pdfurl + pdflink;
		
		String newUrl = null;
		String cook=null;
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(pdfurl).openConnection();
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
		
		//获取cookies
		Map<String, String> cookies=null;
		if (usingProxy)
			cookies=GetPDFUtil.getCookies(newUrl, GetPDFUtil.getProxy());
		else
			cookies=GetPDFUtil.getCookies(newUrl);
		
		// 打开pdf的连接
		// 由于使用代理获得了cookies。这时候，使用cookies相当于使用了代理
		HttpURLConnection con = GetPDFUtil.createPDFLink(newUrl, cookies,false);
		int filesize = con.getContentLength();
		// 下面从网站获取pdf文件
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}

	public static void main(String[] args) {
		String str = "https://doi.org/10.1021/acs.chemmater.9b04131";
		File file = new File("F:\\test.pdf");
		new ACS(str).getFile(new UpdateDialog(null, "down"), file, false);
	}
}
