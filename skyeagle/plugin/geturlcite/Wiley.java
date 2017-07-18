package skyeagle.plugin.geturlcite;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Wiley implements GetCite {

	private String url;

	public Wiley(String url) {
		this.url = url;
	}

	@Override
	public String getCiteItem() {
		// 提交表单的网址
		String posturl = "http://onlinelibrary.wiley.com/documentcitationdownloadformsubmit";

		// 获取doi
		String doi = null;
		try {
			Document doc = Jsoup.connect(url).timeout(30000).get();
			Elements eles=doc.select("a#wol1backlink");
			if(eles.size()!=0)
			{
				url=doc.select("a#wol1backlink").attr("href");
			}
			doi = doc.select("meta[name=citation_doi]").attr("content");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		// *************下面向网站模拟提交表单数据************************

		 // postParams是要提交的表单的数据
		 // 我们这里直接用我们的数据提交，不用在网页上选择了。
		
		 HttpURLConnection con = null;
		 try {
		 String postParams = "doi="+URLEncoder.encode(doi, "utf-8")+
				 "&fileFormat=BIBTEX&hasAbstract=CITATION_AND_ABSTRACT";
		
		 URL u = new URL(posturl);
		 con = (HttpURLConnection) u.openConnection();
		 // 提交表单方式为POST，POST 只能为大写，严格限制，post会不识别
		 con.setRequestMethod("POST");
		 con.setDoOutput(true);
		 con.setDoInput(true);
		 con.setUseCaches(false);
		 con.setRequestProperty("Content-Type",
		 "application/x-www-form-urlencoded");
		 con.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
		
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
		String str = "http://onlinelibrary.wiley.com/doi/10.1002/aenm.201500360/abstract";
		String sb = new Wiley(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
