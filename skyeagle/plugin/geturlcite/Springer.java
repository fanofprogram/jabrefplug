package skyeagle.plugin.geturlcite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Springer implements GetCite {

	private String url;

	public Springer(String url) {
		this.url = url;
	}

	@Override
	public String getCiteItem() {
		// ��ȡbibtex�ı���ַ
		String formUrl = null;
		try {
			// �������ַ�����������ļ�������ַ
			Document doc = Jsoup.connect(url).timeout(60000).get();
			Elements eles=doc.select("a[data-track-action=download article citation]");
			formUrl= eles.attr("href");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		String posturl= null;
		try {
			formUrl=URLEncoder.encode(formUrl, "utf-8");
			posturl = "https://link.springer.com/"+formUrl;
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// *************��������վģ���ύ������************************
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
		// *************�������վ��ȡ���ص�����************************
		// ��ȡ��������
		StringBuffer buffer = new StringBuffer();
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
		//�жϻ�õ�bibtex�ַ����Ƿ����Ҫ����������Ͻ����޸ġ�
//		String bibtex=buffer.toString();
//		if(!BibtexCheck.check(bibtex)){
//			BibtexCheck check=new BibtexCheck(bibtex);
//			check.change();
//			bibtex=check.sb.toString();
//		}
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
		String str = "https://link.springer.com/chapter/10.1007/978-3-319-93728-1_49";
		String sb = new Springer(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
