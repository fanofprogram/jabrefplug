package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import skyeagle.plugin.gui.UpdateDialog;

public class ScienceDirect implements GetPdfFile {

	private String url;

	public ScienceDirect(String url) {
		this.url = url;
	}

	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {
		// 获取网址的内容（html)和cookies
		Map<String,String> cookies=new TreeMap<>();
		String pagecontent=GetPDFUtil.initGetPDF(url, usingProxy, cookies);
		if(pagecontent==null){
			dig.output("网络不通，请检查代理和网络。");
			return;
		}
		// 使用Jsoup库对html内容进行解析
		Document doc = Jsoup.parse(pagecontent);
		// 利用Jsoup中的选择器寻找需要的节点, 这里要找的是pdf文件的连接
		String pdflink = doc.select("a[title=Download PDF]#pdfLink").attr("href");
		if (pdflink.isEmpty()) {
			 dig.output("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
//			System.out.println("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
			return;
		}

		// 打开pdf的连接
		// 由于使用代理获得了cookies。这时候，使用cookies相当于使用了代理，所以不用再挂代理了
		HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink, cookies,false);
		con.setRequestProperty("Referer", url); // 就因为没加这句，一直下载不了，找了一整天的原因
		int filesize = con.getContentLength();
		// 下面从网站获取pdf文件
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}

	public static void main(String[] args) throws IOException {
		String str = "http://www.sciencedirect.com/science/article/pii/S0925838815318776";
		File file = new File("F:\\test.pdf");
		new ScienceDirect(str).getFile(null, file, false);
	}
}
