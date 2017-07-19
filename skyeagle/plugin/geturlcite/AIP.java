package skyeagle.plugin.geturlcite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class AIP implements GetCite {

	private String url;

	public AIP(String url) {
		//�������ַ��pdf�ļ���û�а취�������ã������Ҫ�޸�
		this.url=url.replaceFirst("/pdf", "/full");
	}

	@Override
	public String getCiteItem() {

		// ��ȡbibtex�ı���ַ
		String formUrl = null;
		Elements eles;
		try {
			// �������ַ�����������ļ�������ַ
			Document doc = Jsoup.connect(url).timeout(60000).get();
			// ��ȡ�����ļ����ļ���
			//formUrl= doc.select(":contains(Download Citation)").attr("href");
			eles=doc.select("a:contains(Download Citation)");
			formUrl=eles.get(1).attr("href");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		String doi=formUrl.substring(formUrl.lastIndexOf('=')+1);
		String posturl="http://aip.scitation.org/action/downloadCitation";


		// *************��������վģ���ύ������************************
		// ��ȡcookies
		Map<String, String> cookies = null;
		try {
			Response response = Jsoup.connect(url).timeout(20000).execute();
			cookies = response.cookies();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// AIP��վʹ��post�ύ��
		HttpURLConnection con = null;
		try {
			String postParams = "doi="+doi+"&downloadFileName=aip_jap122&include=cit&format=bibtex&direct=&submit=Download+article+citation+data: undefined";
			URL u = new URL(posturl);
			con = (HttpURLConnection) u.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Referer",eles.get(0).attr("href"));
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
			// ����cookies
			Set<String> set = cookies.keySet();
			for (Iterator<String> it = set.iterator(); it.hasNext();) {
				String tmp = it.next();
				String value = cookies.get(tmp);
				con.setRequestProperty("Cookie", tmp + "=" + value);
			}
			
			OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
			// ����վд������
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
		// *************�������վ��ȡ���ص�����************************
		// ��ȡ��������
		StringBuilder buffer = new StringBuilder();
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
		return buffer.toString();
	}

	public static void main(String[] args) {
		String str = "http://aip.scitation.org/doi/pdf/10.1063/1.4993913";
		String sb = new AIP(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
