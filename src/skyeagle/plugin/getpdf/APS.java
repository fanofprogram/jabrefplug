package skyeagle.plugin.getpdf;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JDialog;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import skyeagle.plugin.gui.APSImageDialog;
import skyeagle.plugin.gui.UpdateDialog;

public class APS implements GetPdfFile {

	private String url;
	static Map<String, String> cookies = new TreeMap<>();
	public static Boolean isSeclectedEinstein = false;

	public APS(String url) {
		this.url = url;
	}

	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {

		dig.setModal(false);
		// ��ȡ��ַ�����ݣ�html)��cookies
		Response res;
		Document doc = null;
		try {
			res = Jsoup.connect(url).timeout(30000).execute();
			cookies = res.cookies();
			doc = Jsoup.connect(url).cookies(cookies).timeout(30000).get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// ����Jsoup�е�ѡ����Ѱ����Ҫ�Ľڵ�, ����Ҫ�ҵ���pdf�ļ�������
		String orglink = doc.select("div[class=article-nav-actions]>a").attr("href");
		if (!testEinstein(dig, orglink))
			return;
		
		dig.toFront();
		dig.setVisible(true);

		String pdflink = "http://journals.aps.org" + orglink;

		// ��pdf������
		// ʹ��cookies
		HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink, cookies, false);
		int filesize = con.getContentLength();
		// �������վ��ȡpdf�ļ�
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
		dig.setModal(true);
		dig.stop=false;
	}

	private boolean testEinstein(UpdateDialog dig, String orglink) {
		Document doc;
		String tmpStr = null;
		ArrayList<String> value = new ArrayList<>();
		// APS��վҪѡ����˹̹������֤
		String link = "http://journals.aps.org" + orglink;
		try {
			doc = Jsoup.connect(link).cookies(cookies).ignoreHttpErrors(true).timeout(30000).get();
			tmpStr = doc.select("input[name=captcha]").attr("value");
			if (tmpStr.isEmpty()) {
				dig.output("��������pdf�ļ����볢��ʹ�ô�����������");
				// System.out.println("��������pdf�ļ����볢��ʹ�ô�����������");
				return false;
			}
			Elements eles = doc.select("img[class=captcha]");
			for (int i = 0; i < eles.size(); i++) {
				String temp = "http://journals.aps.org" + eles.get(i).attr("src");
				value.add(temp);
			}
		}catch(UnsupportedMimeTypeException e){
			return true;
		}catch (Exception e) {
			e.printStackTrace();
		}

		// ����ͼ�񴰿ڵĸ����ڡ�
		JDialog dialog = new JDialog();
		// ���öԻ������ʼλ��
		Toolkit kit = Toolkit.getDefaultToolkit(); // ���幤�߰�
		Dimension screenSize = kit.getScreenSize(); // ��ȡ��Ļ�ĳߴ�
		Point pt = new Point();
		pt.x = screenSize.width / 4;
		pt.y = screenSize.height / 4;
		dialog.setLocation(pt);
		APSImageDialog apsDialog = new APSImageDialog(dialog, value);

		String filenameEinstein = apsDialog.nameEinsteinImg;
		try {
			String choice = "choice=" + filenameEinstein;
			String method = "_method=PUT";
			String origin = "origin=" + URLEncoder.encode(orglink, "UTF-8");
			String captcha = "captcha=" + tmpStr;

			String postcontent = choice + "&" + method + "&" + origin + "&" + captcha;
			String postUrl = "http://journals.aps.org/captcha";
			URL u = new URL(postUrl);

			HttpURLConnection con = (HttpURLConnection) u.openConnection(); // �ύ����ʽΪPOST��POST
																			// ֻ��Ϊ��д���ϸ����ƣ�post�᲻ʶ��
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setUseCaches(false);
			// ��ʾ���ǵ�����Ϊ���ı�������Ϊutf-8
			con.setRequestProperty("Referer", link);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			// ����cookies
			Set<String> set = cookies.keySet();
			for (Iterator<String> it = set.iterator(); it.hasNext();) {
				String _name = it.next();
				String _value = cookies.get(_name);
				con.setRequestProperty("Cookie", _name + "=" + _value);
			}

			// con.setInstanceFollowRedirects(false);
			OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
			// ����վд������
			osw.write(postcontent);
			osw.flush();
			osw.close();
			// �������������������pdf�ļ�������ԭ�򻹲����
			con.getResponseCode();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	public static void main(String[] args) throws IOException {
		String str = "http://journals.aps.org/prl/abstract/10.1103/PhysRevLett.116.100401";
		File file = new File("F:\\test.pdf");
		new APS(str).getFile(null, file, false);
	}
}
