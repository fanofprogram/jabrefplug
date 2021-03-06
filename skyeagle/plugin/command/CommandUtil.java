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
	 * 这个函数的功能是将doi网址转为正常的文献网址 使用的是不让其自动跳转，不断的读取Location的地址 这样就可以获得最终的网址。
	 */
	public static String DOItoURL(String url) {
		// 直接输入DOI号的情况
		url = url.trim();
		if (url.indexOf("http") == -1)
			url = "http://doi.org/" + url;
		// doi有两种，iop有单独的形式
		String rex = "((https?://(dx\\.)?doi\\.org[^/]*/)|(https?://stacks\\.iop\\.org[^/]*/)|(https?://stacks\\.iop\\.org[^/]*/)|(https?://link\\.aps\\.org[^/]*/))";
		Pattern pattern = Pattern.compile(rex);
		Matcher matcher = pattern.matcher(url);
		// 如果没找到上面包含doi的网址，表明为正常网址，直接返回就行
		if (!matcher.find())
			return url;
		// 对doi网址进行处理，使之成为正常的网址
		int responseCode = 0;
		HttpURLConnection con = null;
		Boolean setcookieFlag=false;
		StringBuffer sbu = new StringBuffer();
		for (int i = 1; i < 10; i++) {
			try {
				URL u = new URL(url);
				con = (HttpURLConnection) u.openConnection();
				// 禁止网址自动跳转
				con.setInstanceFollowRedirects(false);
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				con.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; rv:37.0) Gecko/20100101 Firefox/37.0");
				//设置cookies
				if(setcookieFlag){
					con.setRequestProperty("Cookie", sbu.toString());
				}
				
				responseCode = con.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK)
					break;
				// 获取cookies
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
				// 获取跳转的新网址
				String newurl = con.getHeaderField("Location");
				if (newurl == null | newurl.indexOf("http") == -1)
					break;
				url = newurl;
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
		con.disconnect();
		return url;
	}

	public static void main(String[] args) {
		//String doi = "10.1039/C6TA02755E";
		String doi = "http://dx.doi.org/10.1002/aenm.201701430";
		System.out.println(DOItoURL(doi));
	}

}
