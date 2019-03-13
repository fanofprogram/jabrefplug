package skyeagle.plugin.geturlcite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Science implements GetCite {

	private String url;
	public Science(String url) {
		this.url = url;
	}

	public String getCiteItem() {
		String refUrl="http://science.sciencemag.org";
		try {
			Document doc = Jsoup.connect(url).timeout(30000).get();
			String link=doc.select("li[class=bibtext first]>a").attr("href");
			refUrl=refUrl+link;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HttpURLConnection con = null;
		try {
			URL u = new URL(refUrl);
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
		String str = "http://science.sciencemag.org/content/357/6358/eaak9997";
		String sb = new Science(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
