package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import skyeagle.plugin.gui.UpdateDialog;

public class Springer implements GetPdfFile {
	private String url;

	public Springer(String url) {
		this.url = url;
	}

	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {
		String base = "http://link.springer.com";
		// 获取网址的内容（html)和cookies
		Map<String, String> cookies = new TreeMap<>();
		String pagecontent = GetPDFUtil.initGetPDF(url, usingProxy, cookies);
		if (pagecontent == null) {
			dig.output("网络不通，请检查代理和网络。");
			return;
		}
		// 使用Jsoup库对html内容进行解析
		Document doc = Jsoup.parse(pagecontent);
		// 利用Jsoup中的选择器寻找需要的节点, 这里要找的是pdf文件的连接
		String pdflink = doc.select("a#abstract-actions-download-article-pdf-link").attr("href");
		if (pdflink.isEmpty()) {
			dig.output("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
//			System.out.println("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
			return;
		}
		pdflink = base + pdflink;

		// 打开pdf的连接
		// 由于使用代理获得了cookies。这时候，使用cookies相当于使用了代理，所以不用再挂代理了
		HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink, cookies, false);
		int filesize = con.getContentLength();
		// 下面从网站获取pdf文件
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}

	public static void main(String[] args) throws IOException {
		String str = "http://link.springer.com/article/10.1007%2Fs11664-015-4303-6";
		File file = new File("E:\\test.pdf");
		new Springer(str).getFile(null, file, true);
	}
}
