package skyeagle.plugin.getpdf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import TaoZuiCeZhe.MadSkill.Handler;
import TaoZuiCeZhe.MadSkill.ImageCompareImpl;
import TaoZuiCeZhe.MadSkill.ImageCompareInterface;
import TaoZuiCeZhe.MadSkill.ImageReader;
import TaoZuiCeZhe.MadSkill.SelfException;
import skyeagle.plugin.gui.UpdateDialog;

public class APS implements GetPdfFile {

	private String url;
	public File sourceDir = new File(System.getProperty("user.home") + "/.jabref/plugins/sourceImgs");
	public File targetDir = new File(System.getProperty("user.home") + "/.jabref/plugins/targetImgs");
	private final String FILE_SEPAR = System.getProperty("file.separator");

	public APS(String url) {
		this.url = url;
	}

	public void getFile(UpdateDialog dig, File file, Boolean usingProxy) {

		// ��ȡ��ַ�����ݣ�html)��cookies
		Map<String, String> cookies = new TreeMap<>();
		String pagecontent = GetPDFUtil.initGetPDF(url, usingProxy, cookies);
		if (pagecontent == null) {
			// dig.output("���粻ͨ�������������硣");
			System.out.println("���粻ͨ�������������硣");
			return;
		}
		// ʹ��Jsoup���html���ݽ��н���
		Document doc = Jsoup.parse(pagecontent);
		// ����Jsoup�е�ѡ����Ѱ����Ҫ�Ľڵ�, ����Ҫ�ҵ���pdf�ļ�������
		String orglink = doc.select("div[class=article-nav-actions]>a").attr("href");
		testEinstein(orglink, cookies);

		String pdflink = "http://journals.aps.org" + orglink;

//		 ��pdf������
//		 ʹ��cookies
		 HttpURLConnection con = GetPDFUtil.createPDFLink(pdflink, cookies,false);
		 System.out.println(con.getRequestProperty("Content-Type"));
		 int filesize = con.getContentLength();
		 // �������վ��ȡpdf�ļ�
		 GetPDFUtil.getPDFFile(file, filesize, dig, con);
		 con.disconnect();
	}

	private void testEinstein(String orglink, Map<String, String> cookies) {
		Document doc;
		String tmpStr=null;
		ArrayList<String> value = new ArrayList<>();
		// APS��վҪѡ����˹̹������֤
		String link = "http://journals.aps.org" + orglink;
		try {
			doc = Jsoup.connect(link).cookies(cookies).ignoreHttpErrors(true).timeout(30000).get();
			tmpStr = doc.select("input[name=captcha]").attr("value");
			if (tmpStr.isEmpty()) {
				// dig.output("��������pdf�ļ����볢��ʹ�ô�����������");
				System.out.println("��������pdf�ļ����볢��ʹ�ô�����������");
				return;
			}
			Elements eles = doc.select("img[class=captcha]");
			for (int i = 0; i < eles.size(); i++) {
				String temp = eles.get(i).attr("src");
				value.add(temp);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ArrayList<String> sourceImgs = downloadAllImgs(value, cookies);
		ArrayList<String> targetImgs = getTargetImageFileNameList();
		String filenameEinstein = getEinsteinFileName(sourceImgs, targetImgs);
		
//		for (String img : sourceImgs) {
//			File file = new File(img);
//			file.delete();
//		}
		int beginIndex = filenameEinstein.lastIndexOf(FILE_SEPAR);
		int lastIndex=filenameEinstein.lastIndexOf(".");
		
		String subEinstein=filenameEinstein.substring(beginIndex+1, lastIndex);
		System.out.println(subEinstein);
		try {
		String choice = "choice=" + subEinstein; 
		String method ="_method=PUT";
		String origin = "origin=" + URLEncoder.encode(orglink,"UTF-8");
		String captcha = "captcha=" + tmpStr;
		
		String postcontent = choice + "&" + method + "&" + origin + "&" + captcha;
		String postUrl="http://journals.aps.org/captcha";  
		URL u = new URL(postUrl);

		HttpURLConnection con = (HttpURLConnection)
		u.openConnection(); // �ύ����ʽΪPOST��POST ֻ��Ϊ��д���ϸ����ƣ�post�᲻ʶ��
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setUseCaches(false); 
		//��ʾ���ǵ�����Ϊ���ı�������Ϊutf-8
		con.setRequestProperty("Referer",link);
		con.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		con.setInstanceFollowRedirects(false);
		OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
		// ����վд������
		osw.write(postcontent);
		osw.flush();
		osw.close();
		
		// ��ȡcookie
		String key = null;
		for (int i = 1; (key =con.getHeaderFieldKey(i)) != null; i++) {
			if(key.equalsIgnoreCase("set-cookie")) {
				String cookie = null;
				cookie = con.getHeaderField(i);
				int i1 = cookie.indexOf("=");
				int i2 = cookie.indexOf(";");
				if (i1 != -1 && i2 != -1) { 
					String _value =cookie.substring(i1 + 1, i2);
					String _key = cookie.substring(0, i1);
					cookies.put(_key, _value);
					}
				}
			}
		  
		System.out.println(con.getHeaderFields().toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}

	}

	private ArrayList<String> getTargetImageFileNameList() {
		ArrayList<String> targetImageFileNameList = new ArrayList<String>();
		if(targetDir.isDirectory()){
			String[] nameList = targetDir.list();
			if(nameList != null){
				for(String name:nameList){
					targetImageFileNameList.add(targetDir.getAbsolutePath()+FILE_SEPAR+name);
				}
			}
		}
		return targetImageFileNameList;
	}

	private String getEinsteinFileName(ArrayList<String> sourceImageFileNameList,
			ArrayList<String> targetImageFileNameList) {
		int diffs = 0;
		String EinsteinFileName = null;

		ImageCompareInterface imageCompare = new ImageCompareImpl();
		InvocationHandler handler = new Handler(imageCompare);

		// ������̬�������
		ImageCompareInterface proxy = (ImageCompareInterface) Proxy.newProxyInstance(
				ImageCompareImpl.class.getClassLoader(), ImageCompareImpl.class.getInterfaces(), handler);
		if (proxy != null) {
			try {
				if (sourceImageFileNameList.isEmpty()) {
					throw new SelfException("No images in source image folder!");
				} else if (targetImageFileNameList.isEmpty()) {
					throw new SelfException("No images in target image folder!");
				} else {
					for (String targetImageName : targetImageFileNameList) {
						InputStream targetIs = ImageReader.getImageInputStream(targetImageName, 2);
						String targetImageHashStr = proxy.getImageHASHString(targetIs, targetImageName,
								2);
						for (String ImageName : sourceImageFileNameList) {
							InputStream sourceIs = ImageReader.getImageInputStream(ImageName, 1);
							String sourceImageHashStr = proxy.getImageHASHString(sourceIs, ImageName, 1);
							diffs = proxy.HanMingDistance(sourceImageHashStr, targetImageHashStr);
							if (diffs <= 5) {
								EinsteinFileName = ImageName;
								break;
							}
						}
						if (diffs <= 5) {
							break;
						}
					}
				}

			} catch (SelfException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return EinsteinFileName;
	}

	private ArrayList<String> downloadAllImgs(ArrayList<String> value, Map<String, String> cookies) {
		ArrayList<String> imgFiles = new ArrayList<>();
		String baseurl = "http://journals.aps.org";
		for (String strTmp : value) {
			String imgUrl = baseurl + strTmp;
			imgFiles.add(downloadSingleImg(imgUrl, cookies));
		}
		return imgFiles;
	}

	private String downloadSingleImg(String imgUrl, Map<String, String> cookies) {

		int index = imgUrl.lastIndexOf("/");
		String imgFileName = imgUrl.substring(index) + ".png";
		File imgfile = new File(sourceDir, imgFileName);
		HttpURLConnection con = null;
		try {
			URL u = new URL(imgUrl);
			con = (HttpURLConnection) u.openConnection();
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// ����cookies
		Set<String> set = cookies.keySet();
		for (Iterator<String> it = set.iterator(); it.hasNext();) {
			String tmp = it.next();
			String value = cookies.get(tmp);
			con.setRequestProperty("Cookie", tmp + "=" + value);
		}
		InputStream in = null;
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(imgfile));
			in = new BufferedInputStream(con.getInputStream());
			int bytesRead = 0;
			byte[] buffer = new byte[512]; // ��������С
			while ((bytesRead = in.read(buffer)) != -1) { // ��ȡ����
				out.write(buffer, 0, bytesRead); // д�����ݵ��ļ�
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return imgfile.getAbsolutePath();
	}

	public static void main(String[] args) throws IOException {
		String str = "http://journals.aps.org/prb/abstract/10.1103/PhysRevB.93.094102";
		File file = new File("F:\\test.pdf");
		new APS(str).getFile(null, file, false);
	}
}
