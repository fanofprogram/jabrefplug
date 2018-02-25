package skyeagle.plugin.geturlcite;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ScienceDirect implements GetCite{
	private String url;

	public ScienceDirect(String url) {
		this.url = url;
	}

	public String getCiteItem() {

		// 获取网址的内容（html）并进行解析
		// 这里主要使用了Jsoup库，第三方函数库，解析html非常方便。
		// 首先获取doc对象（其实就是html内容）
//		String actionUrl = null;
//		try {
//			Document doc;
//			doc = Jsoup.connect(url).timeout(30000).get();
//			// 利用Jsoup中的选择器寻找需要的节点
//			// 这里要找的是输出引用的表单
//			// div#export_popup>form表示的意思是
//			// 寻找名字为div，id为export_popup的节点中所有的form子节点。
//			Elements forms = doc.select("div#export_popup>form");
//			// 所有的form的第一个form（看sciencedirect网页的源代码，其实只有一个form）
//			Element form = forms.first();
//			// 获得form的action属性值，这是一个url，表单就是向这个网址提交的。
//			actionUrl = form.attr("action");
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			 e1.printStackTrace();
//			// 网页连不上或者找不到，异常，返回空
//			return null;
//		}
		
		// 获取cookies
		Map<String, String> cookies = null;
		try {
			Response response = Jsoup.connect(url).timeout(20000).execute();
			cookies = response.cookies();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		

		// *************下面向网站模拟提交表单数据************************
		// postParams是要提交的表单的数据
		// 我们这里直接用我们的数据提交，不用在网页上选择了。
		// 这里提交的参数意思是，我们的引用格式为BIBTEX，带摘要。
		// 具体可以看网页源文件
		String articleNum=url.substring(url.lastIndexOf('/')+1);
		String baseUrl="https://www.sciencedirect.com/sdfe/arp/cite?pii=";
		String strParams="&format=text/x-bibtex&withabstract=true";

		// actionUrl为相对网址，需要变为绝对网址。
		String formUrl = baseUrl+articleNum+strParams;
		HttpURLConnection con = null;
		try {
			URL u = new URL(formUrl);
			con = (HttpURLConnection) u.openConnection();
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Referer", url);
			con.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			con.setRequestProperty("User-Agent",
					"Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
			// 表示我们的连接为纯文本，编码为utf-8
			con.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
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
	
	public static void main(String[] args) throws IOException {
		String str = "https://www.sciencedirect.com/science/article/pii/S0360544218301166";
		String sb = new ScienceDirect(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
