package skyeagle.plugin.getmail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum UrlKeywords {
	//所有可以处理的数据库对应的类名，括号中是对应的正则表达式，用来判断是否用这个类来处里网址
	ScienceDirect("^https?://[^/]*science-?direct\\.com[^/]*/"), 
	RSC("^https?://(:?www\\.|google\\.)?pubs\\.rsc\\.org/"),
	AIP("^https?://scitation\\.aip\\.org/(?:search\\?|content/)"),
	Wiley("^https?://onlinelibrary\\.wiley\\.com[^\\/]*/"),
	APS("^https?://journals\\.aps\\.org/"),
	Springer("https?://link\\.springer\\.com/"),
	Arxiv("^https?://arxiv\\.org"),
	IOP("^https?://iopscience\\.iop\\.org/"),
	Nature("^https?://(?:[^/]+\\.)?(?:nature\\.com|palgrave-journals\\.com)"),
	IEEE("^https?://[^/]*ieeexplore\\.ieee\\.org[^/]*/"),
	ACS("^https?://pubs\\.acs\\.org[^/]*/"),
	Science("^https?://science\\.sciencemag\\.org[^/]*/");
	
	private String rex;

	private UrlKeywords(String rex) {
		this.rex = rex;
	}

	/*
	 * 根据正则表达式判断网址是否属于对应的枚举
	 */
	public Boolean isThisUrl(String url) {
		Pattern pattern = Pattern.compile(rex);
		Matcher matcher = pattern.matcher(url);
		return matcher.find();
	}
}
