package skyeagle.plugin.geturlcite;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class IEEE implements GetCite {

	private String url;
	private final String NEWLINE = System.getProperty("line.separator");

	public IEEE(String url) {
		this.url = url;
	}

	@Override
	public String getCiteItem() {
		// 直接进行抓取元元素
		Document doc = null;
		try {
			doc = Jsoup.connect(url).timeout(30000).get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		TreeMap<String, String> map = new TreeMap<String, String>();
		Elements eles = doc.select("meta");
		int authorIndex = 0;
		for (Element ele : eles) {
			String key = ele.attr("name");
			if (!key.isEmpty() && key.indexOf("citation") != -1) {
				if (key.equals("citation_author")) {
					map.put(key + authorIndex, ele.attr("content"));
					authorIndex++;
				} else {
					map.put(key, ele.attr("content"));
				}
			}

		}

		// generate bib item
		StringBuilder sb = new StringBuilder();
		sb.append("@article{" + NEWLINE);
		StringBuilder authors = new StringBuilder();
		Set<String> set = map.keySet();
		for (Iterator<String> it = set.iterator(); it.hasNext();) {
			String key = it.next();
			String value = map.get(key);
			key = key.substring("citation_".length(), key.length());
			if (key.indexOf("author") != -1 && key.indexOf("institution") == -1) {
				authors.append(value + " and ");
				continue;
			} else if (key.equals("keywords")) {
				continue;
			}
			sb.append(key + "={" + value + "}," + NEWLINE);
		}
		String tmpStr = authors.toString();
		sb.append("author={" + tmpStr.substring(0, tmpStr.length() - 5) + "},"
				+ NEWLINE);

		// get journal and number
		{
			Elements es = doc.select("div.article-ftr>a");
			String journal = es.get(0).text();
			sb.append("journal={" + journal + "}," + NEWLINE);
			if(es.size()>1){
			String number = es.get(1).text();
			number = number.substring(6, number.length());
			sb.append("number={" + number + "}," + NEWLINE);
			}
		}

		// get year and pages
		{
			Elements es = doc.select("div.article-info>dl>dd");
			String pages=es.first().text();
			sb.append("pages={" + pages + "}," + NEWLINE);
			String year=doc.select("meta[name=citation_date]").attr("content");
			year=year.substring(year.length()-4, year.length());
			sb.append("year={" + year + "}," + NEWLINE);
		}
		sb.append("url={" + url + "}" + NEWLINE);
		sb.append("}");

		return sb.toString();
	}

	public static void main(String[] args) throws IOException {
		String str = "http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=7104418";
		String sb = new IEEE(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);

	}
}
