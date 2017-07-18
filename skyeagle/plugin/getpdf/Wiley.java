package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import org.jsoup.Jsoup;
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
		// 获取旧页面网址并取得pdf连接地址
		try {
			Document doc = Jsoup.connect(url).timeout(30000).get();
			Elements eles = doc.select("a#wol1backlink");
			if (eles.size() != 0) // 如果是新页面，则获得旧页面的网址
				url = doc.select("a#wol1backlink").attr("href");
			doc = Jsoup.connect(url).timeout(30000).get();
			link = doc.select("a#journalToolsPdfLink[title=Article in pdf format]").attr("href");
		} catch (IOException e1) {
			 dig.output("页面连接不上，请检查网络。");
			e1.printStackTrace();
			return;
		}
		link = basurl + link;
		link = link.replaceAll("epdf", "pdf");

		// 获取pdf页面
		String pdfpage = null;
		if (usingProxy) {
			pdfpage = GetPDFUtil.getPageContent(link, GetPDFUtil.getProxy());
		} else {
			pdfpage = GetPDFUtil.getPageContent(link);
		}
		Document doc = Jsoup.parse(pdfpage);
		String pdflink = doc.select("iframe#pdfDocument").attr("src");
		if (pdflink.isEmpty()) {
			 dig.output("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
//			System.out.println("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
			return;
		}

		//获取cookies
		Map<String, String> cookies = null;
		if (usingProxy)
			cookies = GetPDFUtil.getCookies(url, GetPDFUtil.getProxy());
		else
			cookies = GetPDFUtil.getCookies(url);
		
		// 打开pdf的连接
		// 由于使用代理获得了cookies。这时候，使用cookies相当于使用了代理
		//wiley比较特殊，下载的时候也必须挂着代理
		HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink, cookies,usingProxy);
		// 下面从网站获取pdf文件
		int filesize = con.getContentLength();
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();

	}

	public static void main(String[] args) {
		String str = "http://onlinelibrary.wiley.com/doi/10.1002/ange.201508492/full";
		File file = new File("E:\\test.pdf");
		new Wiley(str).getFile(new UpdateDialog(null, "down"), file, true);
	}
}
