package skyeagle.plugin.command;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandUtil {

	/*
	 * ��������Ĺ����ǽ�doi��ַתΪ������������ַ ʹ�õ��ǲ������Զ���ת�����ϵĶ�ȡLocation�ĵ�ַ �����Ϳ��Ի�����յ���ַ��
	 */
	public static String DOItoURL(String url) {
		// ֱ������DOI�ŵ����
		url = url.trim();
		if (url.indexOf("http") == -1)
			url = "http://doi.org/" + url;
		// doi�����֣�iop�е�������ʽ
		String rex = "((https?://(dx\\.)?doi\\.org[^/]*/)|(https?://stacks\\.iop\\.org[^/]*/)|(https?://stacks\\.iop\\.org[^/]*/)|(https?://link\\.aps\\.org[^/]*/))";
		Pattern pattern = Pattern.compile(rex);
		Matcher matcher = pattern.matcher(url);
		// ���û�ҵ��������doi����ַ������Ϊ������ַ��ֱ�ӷ��ؾ���
		if (!matcher.find())
			return url;
		// ��doi��ַ���д���ʹ֮��Ϊ��������ַ
		int responseCode = 0;
		HttpURLConnection con = null;
		Boolean setcookieFlag=false;
		StringBuffer sbu = new StringBuffer();
		for (int i = 1; i < 10; i++) {
			try {
				URL u = new URL(url);
				con = (HttpURLConnection) u.openConnection();
				// ��ֹ��ַ�Զ���ת
				con.setInstanceFollowRedirects(false);
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				con.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
				//����cookies
				if(setcookieFlag){
					con.setRequestProperty("Cookie", sbu.toString());
				}
				
				responseCode = con.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK)
					break;
				// ��ȡcookies
				String cookieskey = "Set-Cookie";
				Map<String, List<String>> maps = con.getHeaderFields();
				List<String> coolist = maps.get(cookieskey);
				if (coolist != null) {
					Iterator<String> it = coolist.iterator();
					while (it.hasNext()) {
						sbu.append(it.next());
					}
					setcookieFlag=true;
				}
				// ��ȡ��ת������ַ
				String newurl = con.getHeaderField("Location");
				System.out.println(newurl);
				if (newurl == null | newurl.indexOf("http") == -1)
					break;
				url = newurl;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		con.disconnect();
		return url;
	}

	public static void main(String[] args) {
		//String doi = "10.1039/C6TA02755E";
		String doi = "https://doi.org/10.1021/acs.chemmater.7b05299";
		System.out.println(DOItoURL(doi));
	}

}
