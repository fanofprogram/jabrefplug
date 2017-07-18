package skyeagle.plugin.geturlcite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Nature implements GetCite {

	private String url;
	private final String NEWLINE = System.getProperty("line.separator");
	public Nature(String url) {
		this.url = url;
	}

	@Override
	public String getCiteItem() {
		String ris="http://www.nature.com";
		try {
			Document doc=Jsoup.connect(url).ignoreHttpErrors(true).timeout(60000).get();
			//找到ris文件的连接，href$表示href的值的末尾为.ris
			String links=doc.select("a[href$=.ris]").attr("href");
			ris=ris+links;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		// *************下面向网站模拟提交表单数据************************
				// Nature网站不是使用post提交的，用的get
				HttpURLConnection con = null;
				try {
					URL u = new URL(ris);
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
				StringBuilder sb=new StringBuilder();
				StringBuilder authors=new StringBuilder();
				try {
					// 一定要有返回值，否则无法把请求发送给server端。
					BufferedReader br = new BufferedReader(new InputStreamReader(
							con.getInputStream(), "UTF-8"));

					// *************下面将获得的ris的内容变为bibtex格式***********
					sb.append("@article{"+NEWLINE);
					String temp;
					while ((temp = br.readLine()) != null) {
						if(temp.indexOf("AU")!=-1){
							String author=temp.substring(6);
							authors.append(author+" and ");
							continue;
						}else if(temp.indexOf("TI")!=-1){
							sb.append("title={"+temp.substring(6)+"},"+NEWLINE);
						}else if(temp.indexOf("JA")!=-1){
							sb.append("Journal={"+temp.substring(6)+"},"+NEWLINE);
						}else if(temp.indexOf("PY")!=-1){
							sb.append("Year={"+temp.substring(6, 10)+"},"+NEWLINE);
						}else if(temp.indexOf("UR  -")!=-1){
							sb.append("Url={"+temp.substring(6)+"},"+NEWLINE);
						}else if(temp.indexOf("SP")!=-1){
							sb.append("Pages={"+temp.substring(6)+"},"+NEWLINE);
						}else if(temp.indexOf("VL")!=-1){
							sb.append("Volume={"+temp.substring(6)+"},"+NEWLINE);
						}
						
						//sb.append(temp);
						//sb.append("\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
				
				String tmpStr=authors.toString();
				sb.append("author={"+tmpStr.substring(0, tmpStr.length()-5)+"}"+NEWLINE);
				sb.append("}");
				return sb.toString();
	}

	public static void main(String[] args) throws IOException {
		String str = "http://www.nature.com/articles/srep18386";
		String sb = new Nature(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
