package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import skyeagle.plugin.gui.UpdateDialog;

public class Arxiv implements GetPdfFile {
	private String url;
	public Arxiv(String url) {
		this.url = url.replaceAll("abs", "pdf");
	}
	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {
		// 获取网址的内容（html)和cookies
		Map<String,String> cookies=GetPDFUtil.getCookies(url);
		// 打开pdf的连接
		//arxiv可以直接下载pdf文件
		HttpURLConnection con = GetPDFUtil.createPDFLink(url, cookies,false);
		int filesize = con.getContentLength();
		// 下面从网站获取pdf文件
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}
	public static void main(String[] args) throws IOException {
		String str = "http://arxiv.org/pdf/1504.06082";
		File file = new File("F:\\test.pdf");
		new Arxiv(str).getFile(null, file, false);
	}
}
