package skyeagle.plugin.geturlcite;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import skyeagle.plugin.geturlcite.BibtexCheck;

public class Wiley implements GetCite {

	private String url;

	public Wiley(String url) {
		this.url = url;
	}

	@Override
	public String getCiteItem() {
		// 没有下面的语句，访问https，抛出异常javax.net.ssl.SSLHandshakeException: Received
		// fatal alert: handshake_failure
		System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");

		String baseUrl = "https://onlinelibrary.wiley.com";
		String citeUrl = null;
		String publishYear = "0";
		try {
			Document doc = Jsoup.connect(url).timeout(30000).get();
			publishYear = doc.select("meta[name=citation_publication_date]").attr("content");
			if (publishYear.length() != 0)
				publishYear = publishYear.trim().substring(0, 4);
			else
				publishYear = "0";
			//选择器：span:contains(Export citation)，span元素，其内容包含Export citation
			//a:has(),a这个元素，其包含上面的元素
			Elements eles = doc.select("a:has(span:contains(Export citation))");
			citeUrl=eles.get(0).attr("href");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		citeUrl = baseUrl + citeUrl;
		String doi, downloadFileName;
		try {
			Document doc = Jsoup.connect(citeUrl).timeout(30000).get();
			Element form = doc.select("form.citation-form").first();
			doi = form.select("input[name=doi]").attr("value");
			downloadFileName = form.select("input[name=downloadFileName]").attr("value");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		// 获取cookies
		Map<String, String> cookies = null;
		try {
			Response response = Jsoup.connect(citeUrl).timeout(20000).execute();
			cookies = response.cookies();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// *************下面向网站模拟提交表单数据************************

		// postParams是要提交的表单的数据
		// 我们这里直接用我们的数据提交，不用在网页上选择了。
		String downloadUrl = baseUrl + "/action/downloadCitation";

		HttpURLConnection con = null;
		try {

			String postParams = "doi=" + URLEncoder.encode(doi, "utf-8") + "&downloadFileName=" + downloadFileName
					+ "&include=abs&format=bibtex&direct=direct&submit=Download";
			URL u = new URL(downloadUrl);
			con = (HttpURLConnection) u.openConnection();
			// 提交表单方式为POST，POST 只能为大写，严格限制，post会不识别
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
			// con.setRequestProperty("Referer", URLEncoder.encode(citeUrl,
			// "utf-8"));
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
				if (temp.indexOf("pages") != -1) {
					buffer.append("year = {" + publishYear + "},");
					buffer.append("\n");
				}
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
		String str = "https://onlinelibrary.wiley.com/doi/abs/10.1002/aenm.201801409";
		String sb = new Wiley(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
