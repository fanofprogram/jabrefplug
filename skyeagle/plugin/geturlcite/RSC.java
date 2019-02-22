package skyeagle.plugin.geturlcite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class RSC implements GetCite {

	private String url;

	public RSC(String url) {
		//rsc的网页有三种，articlelanding的网页上有引用
		if(url.indexOf("articlepdf")!=-1)
			url=url.replaceAll("articlepdf", "articlelanding");
		else if(url.indexOf("articlehtml")!=-1)
			url=url.replaceAll("articlehtml", "articlelanding");
		this.url =url;
	}

	@Override
	public String getCiteItem() {
		// 提交表单的网址
		//String baseurl = "http://pubs.rsc.org/en/content/getformatedresult/";

		// 获取doi
		String doi = null;
		String posturl=null;
		try {
			Document doc = Jsoup.connect(url).timeout(30000).get();
			// 获取引用文件的文件名
			doi = doc.select("input#DOI").attr("value");
			//posturl = baseurl + doi.toLowerCase()
			//		+ "?downloadtype=article";
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
		// postParams是要提交的表单的数据
		// 我们这里直接用我们的数据提交，不用在网页上选择了。
		//原来用post，现在用get了 20181008

		HttpURLConnection con = null;
		try {
			//String postParams = "ResultAbstractFormat=BibTex&go=Go";
			String getUrl="https://pubs.rsc.org/en/content/formatedresult?";
			getUrl=getUrl+"markedids="+doi+"&downloadtype=article&managertype=bibtex";
			URL u = new URL(getUrl);
			con = (HttpURLConnection) u.openConnection();
			// 提交表单方式为POST，POST 只能为大写，严格限制，post会不识别
			//con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			con.setRequestProperty("User-Agent",
					"Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
			/*OutputStreamWriter osw = new OutputStreamWriter(
					con.getOutputStream(), "UTF-8");
			// 向网站写表单数据
			osw.write(postParams);
			osw.flush();
			osw.close();*/
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
		//判断最后一个字符是不是}，不是的话加上}，否则jabref会出错
		String bibtex=buffer.toString().trim();
		char lastChar=bibtex.charAt(bibtex.length()-1);
		if(lastChar!='}'){
			if(lastChar!='\"')
				buffer.append('\"');
			buffer.append('}');
		}
		
		return buffer.toString();
	}

	public static void main(String[] args) throws IOException {
		String str = "https://pubs.rsc.org/en/Content/ArticleLanding/2019/EE/c8ee03112f";
		 String sb = new RSC(str).getCiteItem();
		 if (sb != null)
		 System.out.println(sb);

	}
}
