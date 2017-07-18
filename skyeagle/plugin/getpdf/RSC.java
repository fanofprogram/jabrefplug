package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.TreeMap;

import skyeagle.plugin.gui.UpdateDialog;

public class RSC implements GetPdfFile {

	private String url;

	public RSC(String url) {
		url = url.toLowerCase();
		if (url.indexOf("articlepdf") != -1)
			url = url.replaceAll("articlepdf", "articlehtml");
		else if (url.indexOf("articlelanding") != -1)
			url = url.replaceAll("articlelanding", "articlehtml");
		this.url = url;
	}

	@Override
	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {
		// 获取网址的内容（html)和cookies
		Map<String, String> cookies = new TreeMap<>();
		String pagecontent = GetPDFUtil.initGetPDF(url, usingProxy, cookies);
		if (pagecontent == null) {
			dig.output("代理设置错误。");
			return;
		}
		
		String pdflink = url.replaceAll("articlehtml", "articlepdf");
		// 打开pdf的连接
		// 由于使用代理获得了cookies。这时候，使用cookies相当于使用了代理，所以不用再挂代理了
		HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink, cookies, false);
		int filesize = con.getContentLength();
		//判断是否有权限下载pdf
		//application/pdf
		String returntype=con.getContentType();
		if(returntype.indexOf("pdf")==-1) {
			 dig.output("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
//				System.out.println("页面上找不到下载pdf文件的连接，请尝试使用代理或更换代理。");
				return;
			}

		// 下面从网站获取pdf文件
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}

	public static void main(String[] args) throws IOException {
		String str = "http://pubs.rsc.org/en/Content/ArticleLanding/2015/TA/C5TA07526B";
		File file = new File("E:\\test.pdf");
		new RSC(str).getFile(new UpdateDialog(null, "down"), file, false);
	}
}
