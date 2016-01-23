package skyeagle.plugin.command;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandUtil {

	/*
	 * ��������Ĺ����ǽ�doi��ַתΪ������������ַ
	 * ʹ�õ��ǲ������Զ���ת�����ϵĶ�ȡLocation�ĵ�ַ
	 * �����Ϳ��Ի�����յ���ַ��
	 */
	public static String DOItoURL(String url) {
		//doi�����֣�iop�е�������ʽ
		String rex = "((https?://dx\\.doi\\.org[^/]*/)|(https?://stacks\\.iop\\.org[^/]*/))";
		Pattern pattern = Pattern.compile(rex);
		Matcher matcher = pattern.matcher(url);
		// ���û�ҵ��������doi����ַ������Ϊ������ַ��ֱ�ӷ��ؾ���
		if (!matcher.find())
			return url;
		// ��doi��ַ���д���ʹ֮��Ϊ��������ַ
		int responseCode = 0;
		HttpURLConnection con = null;
		for (int i = 1; i < 10; i++) {
			try {
				URL u = new URL(url);
				con = (HttpURLConnection) u.openConnection();
				//��ֹ��ַ�Զ���ת
				con.setInstanceFollowRedirects(false);
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				con.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
				responseCode = con.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK)
					break;
				//��ȡ��ת������ַ
				String newurl = con.getHeaderField("Location");
				if(newurl==null|newurl.indexOf("http")==-1)
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
		String doi = "http://dx.doi.org/10.1021/acs.jpcc.5b03939";
		DOItoURL(doi);
	}

}
