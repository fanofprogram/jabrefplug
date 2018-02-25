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
		//获取doi号
		String doi=null;
		try {
			Document doc = Jsoup.connect(url).header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0").timeout(30000).get();
			// 获取引用文件的文件名
			String temp = doc.select("a[title=Download Citation]").attr("href");
			int beginIndex=temp.indexOf("doi=");
			doi=temp.substring(beginIndex);
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
			Response response = Jsoup.connect(url).timeout(20000).execute();
			cookies = response.cookies();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// postParams是要提交的表单的数据
		// 我们这里直接用我们的数据提交，不用在网页上选择了。
		String posturl="https://pubs.acs.org/action/downloadCitation";
		HttpURLConnection con = null;
		try {
			String postParams ="direct=true&"+ doi + "&downloadFileName=achs_cmatexAxA&format=bibtex&include=abs"
					+ "&submit=Download+" + URLEncoder.encode("Citation(s)", "utf-8");
			URL u = new URL(posturl);
			con = (HttpURLConnection) u.openConnection();
			// 提交表单方式为POST，POST 只能为大写，严格限制，post会不识别
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
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
				//ACS网站提供的bib文件有问题，authour后少个逗号，导致出错
				//因此检测到author行后，最后加一个逗号
				if(temp.startsWith("author")&&!(temp.endsWith(",")))
					temp=temp+',';
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
		String str ="http://pubs.acs.org/doi/abs/10.1021/acs.chemmater.7b04975";
//		try {
//			str = URLEncoder.encode("F:\\gradle-2.10-all.zip","utf-8");
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		String sb = new ACS(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
