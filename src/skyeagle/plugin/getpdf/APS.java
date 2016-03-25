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
		// 获取网址的内容（html)和cookies
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
		// 利用Jsoup中的选择器寻找需要的节点, 这里要找的是pdf文件的连接
		String orglink = doc.select("div[class=article-nav-actions]>a").attr("href");
		if (!testEinstein(dig, orglink))
			return;
		
		dig.toFront();
		dig.setVisible(true);

		String pdflink = "http://journals.aps.org" + orglink;

		// 打开pdf的连接
		// 使用cookies
		HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink, cookies, false);
		int filesize = con.getContentLength();
		// 下面从网站获取pdf文件
		GetPDFUtil.getPDFFile(file, filesize, dig, con);
		con.disconnect();
		dig.setModal(true);
		dig.stop=false;
	}

	private boolean testEinstein(UpdateDialog dig, String orglink) {
		Document doc;
		String tmpStr = null;
		ArrayList<String> value = new ArrayList<>();
		// APS网站要选择爱因斯坦进行验证
		String link = "http://journals.aps.org" + orglink;
		try {
			doc = Jsoup.connect(link).cookies(cookies).ignoreHttpErrors(true).timeout(30000).get();
			tmpStr = doc.select("input[name=captcha]").attr("value");
			if (tmpStr.isEmpty()) {
				dig.output("不能下载pdf文件，请尝试使用代理或更换代理。");
				// System.out.println("不能下载pdf文件，请尝试使用代理或更换代理。");
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

		// 产生图像窗口的父窗口。
		JDialog dialog = new JDialog();
		// 设置对话框的起始位置
		Toolkit kit = Toolkit.getDefaultToolkit(); // 定义工具包
		Dimension screenSize = kit.getScreenSize(); // 获取屏幕的尺寸
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

			HttpURLConnection con = (HttpURLConnection) u.openConnection(); // 提交表单方式为POST，POST
																			// 只能为大写，严格限制，post会不识别
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setUseCaches(false);
			// 表示我们的连接为纯文本，编码为utf-8
			con.setRequestProperty("Referer", link);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			// 设置cookies
			Set<String> set = cookies.keySet();
			for (Iterator<String> it = set.iterator(); it.hasNext();) {
				String _name = it.next();
				String _value = cookies.get(_name);
				con.setRequestProperty("Cookie", _name + "=" + _value);
			}

			// con.setInstanceFollowRedirects(false);
			OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
			// 向网站写表单数据
			osw.write(postcontent);
			osw.flush();
			osw.close();
			// 必须有这个语句才能下载pdf文件，具体原因还不清楚
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
