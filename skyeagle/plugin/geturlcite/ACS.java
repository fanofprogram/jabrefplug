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
		//�������ַ��pdf�ļ���û�а취�������ã������Ҫ�޸�
		this.url=url.replaceFirst("/pdf(plus)?", "");
	}

	@Override
	public String getCiteItem() {
		
		// ��ȡcookies
		Map<String, String> cookies = null;
		String citeURL=null;
		try {
			Connection conn= Jsoup.connect(url).header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0").timeout(30000);
			Response response = conn.execute();
			cookies = response.cookies();
			Document doc =conn.get();
			// ��ȡ�����ļ�����ַ
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
			// ����cookies
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

		// *************�������վ��ȡ���ص�����************************
		// ��ȡ��������
		StringBuffer buffer=new StringBuffer();
		try {
			// һ��Ҫ�з���ֵ�������޷��������͸�server�ˡ�
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
