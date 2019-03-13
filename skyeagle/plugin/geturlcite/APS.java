package skyeagle.plugin.geturlcite;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class APS implements GetCite {

	private String url;

	public APS(String url) {
		this.url = url;
	}

	@Override
	public String getCiteItem() {
		// 提交表单的网址基础地址
		String baseurl=url.replaceAll("abstract", "export");
		String posturl=baseurl+"?type=bibtex&download=true";

		// *************下面向网站模拟提交表单数据************************
		// APS用的get
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
		//判断获得的bibtex字符串是否符合要求，如果不符合进行修改。
		String bibtex=buffer.toString();
		if(!BibtexCheck.check(bibtex)){
			BibtexCheck check=new BibtexCheck(bibtex);
			check.change();
			bibtex=check.sb.toString();
		}
		return bibtex;
	}

	public static void main(String[] args) {
		String str = "http://journals.aps.org/prb/abstract/10.1103/PhysRevB.92.094307";
		String sb = new APS(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
