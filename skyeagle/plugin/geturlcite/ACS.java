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
		//�������ַ��pdf�ļ���û�а취�������ã������Ҫ�޸�
		this.url=url.replaceFirst("/pdf(plus)?", "");
	}

	@Override
	public String getCiteItem() {
		//��ȡdoi��
		String doi=null;
		try {
			Document doc = Jsoup.connect(url).header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0").timeout(30000).get();
			// ��ȡ�����ļ����ļ���
			String temp = doc.select("a[title=Download Citation]").attr("href");
			int beginIndex=temp.indexOf("doi=");
			doi=temp.substring(beginIndex);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		// *************��������վģ���ύ������************************
		// acs��վʹ����cookie��������䴦��

		// ��ȡcookies
		Map<String, String> cookies = null;
		try {
			Response response = Jsoup.connect(url).timeout(20000).execute();
			cookies = response.cookies();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// postParams��Ҫ�ύ�ı�������
		// ��������ֱ�������ǵ������ύ����������ҳ��ѡ���ˡ�
		String posturl="https://pubs.acs.org/action/downloadCitation";
		HttpURLConnection con = null;
		try {
			String postParams ="direct=true&"+ doi + "&downloadFileName=achs_cmatexAxA&format=bibtex&include=abs"
					+ "&submit=Download+" + URLEncoder.encode("Citation(s)", "utf-8");
			URL u = new URL(posturl);
			con = (HttpURLConnection) u.openConnection();
			// �ύ����ʽΪPOST��POST ֻ��Ϊ��д���ϸ����ƣ�post�᲻ʶ��
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
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
				//ACS��վ�ṩ��bib�ļ������⣬authour���ٸ����ţ����³���
				//��˼�⵽author�к�����һ������
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
