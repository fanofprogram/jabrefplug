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
		// ȡ��pdf���ӵ�ַ
		try {
			Connection conn = Jsoup.connect(url)
					.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0")
					.timeout(30000);
			Document doc = conn.get();

			// Elements eles = doc.select("a#wol1backlink");
			// if (eles.size() != 0) // �������ҳ�棬���þ�ҳ�����ַ
			// url = doc.select("a#wol1backlink").attr("href");
			// doc = Jsoup.connect(url).timeout(30000).get();

			link = doc.select("a[title=Article PDF]").attr("href");
		} catch (IOException e1) {
			dig.output("ҳ�����Ӳ��ϣ��������硣");
			e1.printStackTrace();
			return;
		}
		link = basurl + link;
		// link = link.replaceAll("epdf", "pdf");

		// ��ȡpdfҳ��
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
		// dig.output("ҳ�����Ҳ�������pdf�ļ������ӣ��볢��ʹ�ô�����������");
		//// System.out.println("ҳ�����Ҳ�������pdf�ļ������ӣ��볢��ʹ�ô�����������");
		// return;
		// }

		// ��ȡcookies
		Map<String, String> cookies = null;
		if (usingProxy)
			cookies = GetPDFUtil.getCookies(url, GetPDFUtil.getProxy());
		else
			cookies = GetPDFUtil.getCookies(url);

		// ��pdf������
		// ����ʹ�ô�������cookies����ʱ��ʹ��cookies�൱��ʹ���˴���
		// wiley�Ƚ����⣬���ص�ʱ��Ҳ������Ŵ���
		// HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink,
		// cookies,usingProxy);
		HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink, cookies, usingProxy);
		// �������վ��ȡpdf�ļ�
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
