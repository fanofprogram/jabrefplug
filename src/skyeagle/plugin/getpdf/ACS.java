package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import org.jsoup.Jsoup;
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
			doc = Jsoup.connect(url).ignoreContentType(true).timeout(30000).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 利用Jsoup中的选择器寻找pdf文件的连接
		String pdflink = doc.select("a[title^=High-Res]").attr("href");
		pdfurl = pdfurl + pdflink;
		
		//获取cookies
		Map<String, String> cookies=null;
		if (usingProxy)
			cookies=GetPDFUtil.getCookies(pdfurl, GetPDFUtil.getProxy());
		else
			cookies=GetPDFUtil.getCookies(pdfurl);
		
		// 打开pdf的连接
		// 由于使用代理获得了cookies。这时候，使用cookies相当于使用了代理
		HttpURLConnection con = GetPDFUtil.createPDFLink(pdfurl, cookies,false);
		int filesize = con.getContentLength();
		// 下面从网站获取pdf文件
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}

	public static void main(String[] args) {
		String str = "http://pubs.acs.org/doi/abs/10.1021/acsami.5b01460";
		File file = new File("F:\\test.pdf");
		new ACS(str).getFile(new UpdateDialog(null, "down"), file, false);
	}
}
