package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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
		//arxiv网站在pdf链接网址这里做了重定向，所以要获取重定向的网址
		int responseCode = 0;
		HttpURLConnection con = null;
		for (int i = 1; i < 10; i++) {
			try {
				URL u = new URL(url);
				con = (HttpURLConnection) u.openConnection();
				//禁止网址自动跳转
				con.setInstanceFollowRedirects(false);
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				con.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
				responseCode = con.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK)
					break;
				//获取跳转的新网址
				String newurl = con.getHeaderField("Location");
				if(newurl==null|newurl.indexOf("http")==-1)
					break;
				url = newurl;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		con.disconnect();
		// 打开pdf的连接
		con = GetPDFUtil.createPDFLink(url, cookies,false);
		int filesize = con.getContentLength();
		// 下面从网站获取pdf文件
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}
	public static void main(String[] args) throws IOException {
		String str = "http://arxiv.org/pdf/1608.05215";
		File file = new File("E:\\media\\work\\test.pdf");
		new Arxiv(str).getFile(null, file, false);
	}
}
