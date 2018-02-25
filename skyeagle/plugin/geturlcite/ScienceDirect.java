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

		// ��ȡ��ַ�����ݣ�html�������н���
		// ������Ҫʹ����Jsoup�⣬�����������⣬����html�ǳ����㡣
		// ���Ȼ�ȡdoc������ʵ����html���ݣ�
//		String actionUrl = null;
//		try {
//			Document doc;
//			doc = Jsoup.connect(url).timeout(30000).get();
//			// ����Jsoup�е�ѡ����Ѱ����Ҫ�Ľڵ�
//			// ����Ҫ�ҵ���������õı�
//			// div#export_popup>form��ʾ����˼��
//			// Ѱ������Ϊdiv��idΪexport_popup�Ľڵ������е�form�ӽڵ㡣
//			Elements forms = doc.select("div#export_popup>form");
//			// ���е�form�ĵ�һ��form����sciencedirect��ҳ��Դ���룬��ʵֻ��һ��form��
//			Element form = forms.first();
//			// ���form��action����ֵ������һ��url���������������ַ�ύ�ġ�
//			actionUrl = form.attr("action");
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			 e1.printStackTrace();
//			// ��ҳ�����ϻ����Ҳ������쳣�����ؿ�
//			return null;
//		}
		
		// ��ȡcookies
		Map<String, String> cookies = null;
		try {
			Response response = Jsoup.connect(url).timeout(20000).execute();
			cookies = response.cookies();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		

		// *************��������վģ���ύ������************************
		// postParams��Ҫ�ύ�ı�������
		// ��������ֱ�������ǵ������ύ����������ҳ��ѡ���ˡ�
		// �����ύ�Ĳ�����˼�ǣ����ǵ����ø�ʽΪBIBTEX����ժҪ��
		// ������Կ���ҳԴ�ļ�
		String articleNum=url.substring(url.lastIndexOf('/')+1);
		String baseUrl="https://www.sciencedirect.com/sdfe/arp/cite?pii=";
		String strParams="&format=text/x-bibtex&withabstract=true";

		// actionUrlΪ�����ַ����Ҫ��Ϊ������ַ��
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
			// ��ʾ���ǵ�����Ϊ���ı�������Ϊutf-8
			con.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

		// *************�������վ��ȡ���ص�����************************
		// ��ȡ��������
		StringBuilder buffer = new StringBuilder();
		try {
			// һ��Ҫ�з���ֵ�������޷��������͸�server�ˡ�
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
