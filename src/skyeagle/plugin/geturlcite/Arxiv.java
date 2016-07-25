package skyeagle.plugin.geturlcite;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Arxiv implements GetCite {

	private String url;
	private final String NEWLINE = System.getProperty("line.separator");

	public Arxiv(String url) {
		// 如果是pdf的话，相当于直接下载，不能获取引用。
		this.url = url.replaceAll("pdf", "abs");
	}

	@Override
	public String getCiteItem() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		// arvix网页中包含有引用的元素，尝试直接抓取
		Document doc = null;
		try {
			doc = Jsoup.connect(url).timeout(30000).get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		// meta元素
		int authorIndex = 1;
		Elements eles = doc.select("meta");
		for (Element ele : eles) {
			String key = ele.attr("name");
			if (key.indexOf("citation") != -1) {
				if (key.equals("citation_author")) {
					key = key + authorIndex;
					authorIndex++;
				}
				String value = ele.attr("content");
				map.put(key, value);
			}
		}

		String abs = doc.select("blockquote.abstract").text();
		map.put("citation_abstract", abs);

		// generate bib item
		StringBuilder sb = new StringBuilder();
		sb.append("@article{" + NEWLINE);
		StringBuilder authors = new StringBuilder();
		Set<String> set = map.keySet();
		for (Iterator<String> it = set.iterator(); it.hasNext();) {
			String key = it.next();
			String value = map.get(key);
			key = key.substring("citation_".length(), key.length());
			if (key.indexOf("author") != -1) {
				authors.append(value + " and ");
				continue;
			}
			if (key.indexOf("url") != -1) {
				key = key.substring("pdf_".length(), key.length());
			}
			if (key.equals("date")) {
				key = "year";
				value = value.substring(0, 4);
			}
			if (key.indexOf("online") != -1)
				continue;
			if (key.equals("arxiv_id")) {
				key = "journal";
				value = "arXiv preprint arXiv:" + value;
			}
			sb.append(key + "={" + value + "}," + NEWLINE);
		}
		String tmpStr = authors.toString();
		sb.append("author={" + tmpStr.substring(0, tmpStr.length() - 5) + "}" + NEWLINE);
		sb.append("}");
		return sb.toString();
	}

	public static void main(String[] args) {
		String str = "http://arxiv.org/pdf/1607.03624";
		String sb = new Arxiv(str).getCiteItem();
		if (sb != null)
			System.out.println(sb);
	}
}
