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

		// ACS��վû��Ȩ�޵Ļ�Ҳ�ṩpdf���ӣ�������ﲻ�ÿ��Ǵ�������
		//ֱ����Jsoup����ȡhtml��ҳ
		String pdfurl = "http://pubs.acs.org";
		Document doc = null;
		try {
			Connection conn= Jsoup.connect(url).header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0").timeout(30000);
			doc =conn.get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// ����Jsoup�е�ѡ����Ѱ��pdf�ļ�������
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
		
		//��ȡcookies
		Map<String, String> cookies=null;
		if (usingProxy)
			cookies=GetPDFUtil.getCookies(newUrl, GetPDFUtil.getProxy());
		else
			cookies=GetPDFUtil.getCookies(newUrl);
		
		// ��pdf������
		// ����ʹ�ô�������cookies����ʱ��ʹ��cookies�൱��ʹ���˴���
		HttpURLConnection con = GetPDFUtil.createPDFLink(newUrl, cookies,false);
		int filesize = con.getContentLength();
		// �������վ��ȡpdf�ļ�
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}

	public static void main(String[] args) {
		String str = "https://doi.org/10.1021/acs.chemmater.9b04131";
		File file = new File("F:\\test.pdf");
		new ACS(str).getFile(new UpdateDialog(null, "down"), file, false);
	}
}
