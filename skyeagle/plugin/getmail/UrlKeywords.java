package skyeagle.plugin.getmail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum UrlKeywords {
	//���п��Դ�������ݿ��Ӧ���������������Ƕ�Ӧ��������ʽ�������ж��Ƿ����������������ַ
	ScienceDirect("^https?://[^/]*science-?direct\\.com[^/]*/"), 
	RSC("^https?://(:?www\\.|google\\.)?pubs\\.rsc\\.org/"),
	AIP("^https?://.*scitation.*"),
	Wiley("^https?://onlinelibrary\\.wiley\\.com[^\\/]*/"),
	APS("^https?://journals\\.aps\\.org/"),
	Springer("https?://link\\.springer\\.com/"),
	Arxiv("^https?://arxiv\\.org"),
	IOP("^https?://iopscience\\.iop\\.org/"),
	Nature("^https?://(?:[^/]+\\.)?(?:nature\\.com|palgrave-journals\\.com)"),
	//IEEE("^https?://[^/]*ieeexplore\\.ieee\\.org[^/]*/"),
	//Science("^https?://(science|www)\\.sciencemag\\.org[^/]*/"),
	//Pnas("^https?://www\\.pnas\\.org[^/]*/"),
	ACS("^https?://pubs\\.acs\\.org[^/]*/");
	
	private String rex;

	private UrlKeywords(String rex) {
		this.rex = rex;
	}

	/*
	 * ����������ʽ�ж���ַ�Ƿ����ڶ�Ӧ��ö��
	 */
	public Boolean isThisUrl(String url) {
		Pattern pattern = Pattern.compile(rex);
		Matcher matcher = pattern.matcher(url);
		return matcher.find();
	}
}
