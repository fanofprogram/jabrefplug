package skyeagle.plugin.geturlcite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class IOP implements GetCite {

	private String url;

	public IOP(String url) {
		this.url = url;
	}

	@Override
	public String getCiteItem() {
		// 提交表单的网址
		String baseurl = "http://iopscience.iop.org";

		// 获取articleID
		Elements ele = null;
		String posturl = null;
		try {
			Document doc = Jsoup.connect(url).ignoreHttpErrors(true).timeout(30000).get();
			// 获取引用文件的文件名
			//articleID = doc.select("input[name=articleID]").attr("value");
//			ele = doc.select("span#articleId");
//			String articleID=ele.get(0).text();
//			posturl = baseurl
//					+ "export?articleId="
//					+ URLEncoder.encode(articleID, "utf-8")
//					+ "&exportFormat=iopexport_bib&exportType=abs&navsubmit=Export%2Babstract";
			posturl=baseurl+doc.select("a[title=Export BibTex]").attr("href");
		} catch (UnsupportedEncodingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return null;
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return null;
		}
		// *************下面向网站模拟提交表单数据************************
		// 我们这里直接用我们的数据提交，不用在网页上选择了。
		//IOP使用get方法提交

		url=posturl;
		int responseCode = 0;
		HttpURLConnection con = null;
		StringBuffer sbu = new StringBuffer();
		for (int i = 1; i < 10; i++) {
			try {
				URL u = new URL(url);
				con = (HttpURLConnection) u.openConnection();
				// 禁止网址自动跳转
				con.setInstanceFollowRedirects(false);
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				con.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
								
				responseCode = con.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK)
					break;
				// 获取跳转的新网址
				String newurl = con.getHeaderField("Location");
				if (newurl == null | newurl.indexOf("http") == -1)
					break;
				url = newurl;
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}

//		HttpURLConnection con = null;
//		try {
//			URL u = new URL(url);
//			con = (HttpURLConnection) u.openConnection();
//			con.setDoOutput(true);
//			con.setDoInput(true);
//			con.setUseCaches(false);
//			con.setRequestProperty("Content-Type",
//					"application/x-www-form-urlencoded");
//			con.setRequestProperty("User-Agent",
//					"Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
//			System.out.println(con.getResponseCode());
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		} finally {
//			if (con != null) {
//				con.disconnect();
//			}
//		}

		// *************下面从网站获取返回的数据************************
		// 读取返回内容
		StringBuilder buffer = new StringBuilder();
		try {
			// 一定要有返回值，否则无法把请求发送给server端。
			BufferedReader br = new BufferedReader(new InputStreamReader(
					con.getInputStream(), "UTF-8"));
			String temp;
			while ((temp = br.readLine()) != null) {
				buffer.append(temp);
				buffer.append("\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		//判断获得的bibtex字符串是否符合要求，如果不符合进行修改。
		String bibtex=buffer.toString();
		if(!BibtexCheck.check(bibtex)){
			BibtexCheck check=new BibtexCheck(bibtex);
			check.change();
			bibtex=check.sb.toString();
		}
		return bibtex;
	}

	public static void main(String[] args) throws IOException {
		String str = "https://iopscience.iop.org/article/10.1088/2515-7655/ab0c3a/";
		String sb = new IOP(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
