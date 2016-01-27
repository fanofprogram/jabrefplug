package skyeagle.plugin.getpdf;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import skyeagle.plugin.gui.UpdateDialog;

public class Nature implements GetPdfFile {

	private String url;

	public Nature(String url) {
		this.url = url;
	}

	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {

		String base = "http://www.nature.com";
		// ��ȡ��ַ�����ݣ�html)��cookies
		Map<String, String> cookies = new TreeMap<>();
		String pagecontent = GetPDFUtil.initGetPDF(url, usingProxy, cookies);
		if (pagecontent == null) {
			dig.output("���粻ͨ�������������硣");
			return;
		}
		// ʹ��Jsoup���html���ݽ��н���
		Document doc = Jsoup.parse(pagecontent);
		// ����Jsoup�е�ѡ����Ѱ����Ҫ�Ľڵ�, ����Ҫ�ҵ���pdf�ļ�������
		// Scientific Reports�ǿ�Դ�ģ�����ֱ�����أ�������nature����־��һ��
		String pdflink = null;
		int flag=0;
		if (pagecontent.indexOf("Scientific Reports") != -1)
			pdflink = doc.select("a[data-track-dest=link:Download as PDF]").attr("href");
		else if (pagecontent.indexOf("Nature Communications") != -1)
			pdflink = doc.select("li[class=download-option articlepdf]>a").attr("href");
		else{
			pdflink = doc.select("a#download-pdf").attr("href");
			flag=-1;
		}
		if (pdflink.isEmpty()) {
			dig.output("ҳ�����Ҳ�������pdf�ļ������ӣ��볢��ʹ�ô�����������");
			// System.out.println("ҳ�����Ҳ�������pdf�ļ������ӣ��볢��ʹ�ô�����������");
			return;
		}
		pdflink = base + pdflink;
		// ��pdf������
		// Nature��վ������pdf�ļ���ʱ��Ҳ������ϴ���
		HttpURLConnection con = null;
		if (flag != -1)
			con = GetPDFUtil.createPDFLink(pdflink, cookies, false);
		else
			con = GetPDFUtil.createPDFLink(pdflink, cookies, usingProxy);
		int filesize = con.getContentLength();
		// �������վ��ȡpdf�ļ�
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
	}

	public static void main(String[] args) throws IOException {
		// String str =
		// "http://www.nature.com/nphys/journal/v11/n12/full/nphys3542.html";
		String str = "http://www.nature.com/ncomms/2015/150720/ncomms8584/full/ncomms8584.html";
		File file = new File("E:\\test.pdf");
		new Nature(str).getFile(null, file, false);
	}
}
