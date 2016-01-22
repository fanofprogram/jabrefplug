package skyeagle.plugin.geturlcite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ACS implements GetCite {

	private String url;

	public ACS(String url) {
		this.url = url;
	}

	@Override
	public String getCiteItem() {
		// 提交表单的网址
		String posturl = "http://pubs.acs.org/action/downloadCitation";

		// 获取doi和downloadFileName
		String doi = null;
		String downloadFileName = null;
		try {
			// 所有文章网址的规律是baseurl加上doi，所以可以从网址中提取doi
			String baseurl = "http://pubs.acs.org/doi/abs/";
			doi = url.substring(baseurl.length(), url.length());
			// 下面的网址是下载引用文件表单的网址
			String downloadCiteUrl = "http://pubs.acs.org/action/showCitFormats?doi="
					+ URLEncoder.encode(doi, "utf-8");
			Document doc = Jsoup.connect(downloadCiteUrl).timeout(30000).get();
			// 获取引用文件的文件名
			downloadFileName = doc.select("input[name=downloadFileName]").attr(
					"value");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		// *************下面向网站模拟提交表单数据************************
		// acs网站使用了cookie，必须对其处理

		// 获取cookies
		Map<String, String> cookies = null;
		try {
			Response response = Jsoup.connect(posturl).timeout(20000).execute();
			cookies = response.cookies();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// postParams是要提交的表单的数据
		// 我们这里直接用我们的数据提交，不用在网页上选择了。

		HttpURLConnection con = null;
		try {
			String postParams = "doi=" + URLEncoder.encode(doi, "utf-8")
					+ "&downloadFileName="
					+ URLEncoder.encode(downloadFileName, "utf-8")
					+ "&include=abs&format=bibtex&direct=true"
					+ "&submit=Download"
					+ URLEncoder.encode("Citation(s)", "utf-8");

			URL u = new URL(posturl);
			con = (HttpURLConnection) u.openConnection();
			// 提交表单方式为POST，POST 只能为大写，严格限制，post会不识别
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
			// 设置cookies
			Set<String> set = cookies.keySet();
			for (Iterator<String> it = set.iterator(); it.hasNext();) {
				String tmp = it.next();
				String value = cookies.get(tmp);
				con.setRequestProperty("Cookie", tmp + "=" + value);
			}

			OutputStreamWriter osw = new OutputStreamWriter(
					con.getOutputStream(), "UTF-8");
			// 向网站写表单数据
			osw.write(postParams);
			osw.flush();
			osw.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

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
		return buffer.toString();
	}

	public static void main(String[] args) {
		String str = "http://pubs.acs.org/doi/abs/10.1021/acsami.5b01460";
		String sb = new ACS(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
