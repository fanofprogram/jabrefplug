package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import skyeagle.plugin.gui.UpdateDialog;

public class Science implements GetPdfFile {

	private String url;

	public Science(String url) {
		this.url = url;
	}

	@Override
	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {
		// TODO Auto-generated method stub

		// 获取网址的内容（html)和cookies
		Map<String, String> cookies = new TreeMap<>();
		String pagecontent = GetPDFUtil.initGetPDF(url, usingProxy, cookies);
		if (pagecontent == null) {
			dig.output("网络不通，请检查代理和网络。");
			return;
		}
		// 使用Jsoup库对html内容进行解析
		String pdflink = null;
		Document doc = Jsoup.parse(pagecontent);
		// 利用Jsoup中的选择器寻找需要的节点, 这里要找的是pdf文件的连接
		Elements tmpnod = doc.select("a[target=_blank]");
		for (Element ele : tmpnod) {
			String tmp = ele.attr("href");
			if (tmp.indexOf("pdf") != -1) {
				pdflink = tmp;
				break;
			}
		}
		pdflink = "http://science.sciencemag.org" + pdflink;

		// 打开pdf的连接
		// science在下载文献的时候也需要挂上代理
		HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink, cookies, usingProxy);
		int filesize = con.getContentLength();
		// 下面从网站获取pdf文件
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}

	public static void main(String[] args) throws IOException {
		String str = "http://science.sciencemag.org/content/early/2015/11/24/science.aad3749.abstract";
		File file = new File("F:\\test.pdf");
		new Science(str).getFile(null, file, true);
	}
}
