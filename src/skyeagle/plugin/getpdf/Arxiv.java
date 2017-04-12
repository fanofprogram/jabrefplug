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
		// ��ȡ��ַ�����ݣ�html)��cookies
		Map<String,String> cookies=GetPDFUtil.getCookies(url);
		//arxiv��վ��pdf������ַ���������ض�������Ҫ��ȡ�ض������ַ
		int responseCode = 0;
		HttpURLConnection con = null;
		for (int i = 1; i < 10; i++) {
			try {
				URL u = new URL(url);
				con = (HttpURLConnection) u.openConnection();
				//��ֹ��ַ�Զ���ת
				con.setInstanceFollowRedirects(false);
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				con.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
				responseCode = con.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK)
					break;
				//��ȡ��ת������ַ
				String newurl = con.getHeaderField("Location");
				if(newurl==null|newurl.indexOf("http")==-1)
					break;
				url = newurl;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		con.disconnect();
		// ��pdf������
		con = GetPDFUtil.createPDFLink(url, cookies,false);
		int filesize = con.getContentLength();
		// �������վ��ȡpdf�ļ�
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}
	public static void main(String[] args) throws IOException {
		String str = "http://arxiv.org/pdf/1608.05215";
		File file = new File("E:\\media\\work\\test.pdf");
		new Arxiv(str).getFile(null, file, false);
	}
}
