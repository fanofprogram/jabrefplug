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

import org.jsoup.Connection.Response;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ACS implements GetCite {

	private String url;

	public ACS(String url) {
		//如果是网址是pdf文件，没有办法下载引用，因此需要修改
		this.url=url.replaceFirst("/pdf(plus)?", "");
	}

	@Override
	public String getCiteItem() {
		
		// 获取cookies
		Map<String, String> cookies = null;
		String citeURL=null;
		try {
			Connection conn= Jsoup.connect(url).header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0").timeout(30000);
			Response response = conn.execute();
			cookies = response.cookies();
			Document doc =conn.get();
			// 获取引用文件的网址
			citeURL = doc.select("a[title=Citation and abstract]").attr("href");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		// get ris url
		String risUrl="https://pubs.acs.org"+citeURL;
				
		HttpURLConnection con = null;
		try {
			URL u = new URL(risUrl);
			con = (HttpURLConnection) u.openConnection();
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
			// 设置cookies
			Set<String> set = cookies.keySet();
			for (Iterator<String> it = set.iterator(); it.hasNext();) {
				String tmp = it.next();
				String value = cookies.get(tmp);
				con.setRequestProperty("Cookie", tmp + "=" + value);
			}
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
		StringBuffer buffer=new StringBuffer();
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
		BibtexCheck check=new BibtexCheck();
		try {
			return check.ris2Bibtex(buffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public static void main(String[] args) {
		String str ="https://pubs.acs.org/doi/abs/10.1021/nl501953s";
		String sb = new ACS(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
