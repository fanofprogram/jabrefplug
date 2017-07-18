package skyeagle.plugin.geturlcite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class AIP implements GetCite {

	private String url;

	public AIP(String url) {
		this.url = url;
	}

	@Override
	public String getCiteItem() {
		// 提交表单的网址基础地址
		String baseurl = "http://scitation.aip.org/";

		// 获取bibtex的表单地址
		String formUrl = null;
		try {
			// 下面的网址是下载引用文件表单的网址
			Document doc = Jsoup.connect(url).timeout(30000).get();
			// 获取引用文件的文件名
			formUrl= doc.select(":contains(bibtex)").attr("href");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		String posturl=baseurl+formUrl;

		// *************下面向网站模拟提交表单数据************************
		// AIP网站不是使用post提交的，用的get
		HttpURLConnection con = null;
		try {
			URL u = new URL(posturl);
			con = (HttpURLConnection) u.openConnection();
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
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
			String eid=null;
			while ((temp = br.readLine()) != null) {
				// 将eid的值给pages
				if (temp.indexOf("eid")!=-1) {
					eid = temp.substring(temp.length()-7, temp.length()-1);
				}
				if(temp.indexOf("pages")!=-1)
				{
					temp="   pages = \""+eid+"\",";
				}
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
		String str = "http://scitation.aip.org/content/aip/journal/jap/117/15/10.1063/1.4918311";
		String sb = new AIP(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
