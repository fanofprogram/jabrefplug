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

import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class AIP implements GetCite {

	private String url;

	public AIP(String url) {
		// 如果是网址是pdf文件，没有办法下载引用，因此需要修改
		this.url = url.replaceFirst("/pdf", "/full");
	}

	@Override
	public String getCiteItem() {
		// 没有下面的语句，访问https，抛出异常javax.net.ssl.SSLHandshakeException: Received
		// fatal alert: handshake_failure
		System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");

		// 获取bibtex的表单地址
		String formUrl = null;
		Elements eles;
		try {
			// 下面的网址是下载引用文件表单的网址
			Connection conn = Jsoup.connect(url).timeout(60000);
			conn.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			conn.header("Accept-Encoding", "gzip, deflate, sdch");
			conn.header("Accept-Language", "zh-CN,zh;q=0.8");
			conn.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");
			Document doc=conn.get();
			// 获取引用文件的文件名
			// formUrl= doc.select(":contains(Download Citation)").attr("href");
			eles = doc.select("a:contains(Download Citation)");
			formUrl = eles.get(0).attr("href");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		String citeurl = "http://aip.scitation.org" + formUrl;

		String doi, downloadFileName;
		try {
			Connection conn = Jsoup.connect(citeurl).timeout(60000);
			conn.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			conn.header("Accept-Encoding", "gzip, deflate, sdch");
			conn.header("Accept-Language", "zh-CN,zh;q=0.8");
			conn.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");
			Document doc=conn.get();
			doi = doc.select("input[name=doi]").attr("value");
			downloadFileName=doc.select("input[name=downloadFileName]").attr("value");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		String posturl = "https://aip.scitation.org/action/downloadCitation";

		// *************下面向网站模拟提交表单数据************************
		// 获取cookies
		Map<String, String> cookies = null;
		try {
			Connection conn = Jsoup.connect(url).timeout(60000);
			conn.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			conn.header("Accept-Encoding", "gzip, deflate, sdch");
			conn.header("Accept-Language", "zh-CN,zh;q=0.8");
			conn.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");
			Response response = conn.execute();
			cookies = response.cookies();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// AIP网站使用post提交的
		HttpURLConnection con = null;
		try {
			String postParams = "doi=" +  URLEncoder.encode(doi, "utf-8")
					+ "&downloadFileName="+downloadFileName+"&include=cit&format=bibtex&direct=&submit=Download+article+citation+data";
			URL u = new URL(posturl);
			con = (HttpURLConnection) u.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Referer", eles.get(0).attr("href"));
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
			// 设置cookies
			Set<String> set = cookies.keySet();
			for (Iterator<String> it = set.iterator(); it.hasNext();) {
				String tmp = it.next();
				String value = cookies.get(tmp);
				con.setRequestProperty("Cookie", tmp + "=" + value);
			}

			OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
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
			BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
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

	public static void main(String[] args) {
		String str = "https://aip.scitation.org/doi/full/10.1063/1.5086061";
		String sb = new AIP(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
