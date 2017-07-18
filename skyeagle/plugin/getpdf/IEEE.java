package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import skyeagle.plugin.gui.UpdateDialog;

public class IEEE implements GetPdfFile {

	private String url;

	public IEEE(String url) {
		this.url = url;
	}

	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {

		// 获取网址的cookies
		Map<String, String> cookies = null;
		if (usingProxy)
			cookies = GetPDFUtil.getCookies(url, GetPDFUtil.getProxy());
		else
			cookies = GetPDFUtil.getCookies(url);
		
		// 使用Jsoup库对html内容进行解析
		Document doc = null;
		String pdflink=null;
		String link=null;
		try {
			doc = Jsoup.connect(url).cookies(cookies).timeout(30000).get();
			link= doc.select("a#full-text-pdf").attr("href");
			if (link.isEmpty()) {
				 dig.output("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
//				System.out.println("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
				return;
			}
			link="http://ieeexplore.ieee.org"+link;
			//需要再次解析html内容，获得真正的pdf下载地址
			doc=Jsoup.connect(link).cookies(cookies).timeout(30000).get();
			Elements tmpnod = doc.select("frame[frameborder=0]");
			for (Element ele : tmpnod) {
				String tmp = ele.attr("src");
				if (tmp.indexOf("http") != -1) {
					pdflink = tmp;
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			dig.output("网络不通，请检查代理和网络。");
			return;
		}
		
		// 打开pdf的连接
		// 由于使用代理获得了cookies。这时候，使用cookies相当于使用了代理，所以不用再挂代理了
		HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink, cookies,false);
		con.setRequestProperty("Referer", link);
		int filesize = con.getContentLength();
		// 下面从网站获取pdf文件
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}

	public static void main(String[] args) throws IOException {
		String str = "http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=7104418";
		File file = new File("F:\\test.pdf");
		new IEEE(str).getFile(null, file, false);
	}

}
