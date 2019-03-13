
package skyeagle.plugin.geturlcite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.*;
import net.sf.jabref.export.LatexFieldFormatter;
import net.sf.jabref.imports.FieldContentParser;
import net.sf.jabref.imports.ImportFormatReader;
import net.sf.jabref.imports.ParserResult;

public class BibtexCheck {

	private PushbackReader _in;

	private BibtexDatabase _db;

	private HashMap<String, String> _meta;

	private HashMap<String, BibtexEntryType> entryTypes;

	private boolean _eof = false;

	private int line = 1;

	private FieldContentParser fieldContentParser = new FieldContentParser();

	private ParserResult _pr;

	private static final Integer LOOKAHEAD = 64;
	
	public StringBuilder sb=new StringBuilder();

	public BibtexCheck(String bib) {
		StringReader in = new StringReader(bib);
		_in = new PushbackReader(in, LOOKAHEAD);
	}

	public BibtexCheck() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Parses BibtexEntries from the given string and returns the collection of
	 * all entries found.
	 * 
	 * @param bibtexString
	 * 
	 * @return Returns null if an error occurred, returns an empty collection if
	 *         no entries where found.
	 */
	public static Collection<BibtexEntry> fromString(String bibtexString) {
		BibtexCheck parser = new BibtexCheck(bibtexString);
		try {
			return parser.parse().getDatabase().getEntries();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Parses BibtexEntries from the given string and returns one entry found
	 * (or null if none found)
	 * 
	 * It is undetermined which entry is returned, so use this in case you know
	 * there is only one entry in the string.
	 * 
	 * @param bibtexString
	 * 
	 * @return The bibtexentry or null if non was found or an error occurred.
	 */
	public static BibtexEntry singleFromString(String bibtexString) {
		Collection<BibtexEntry> c = fromString(bibtexString);
		if ((c == null) || (c.size() == 0)) {
			return null;
		}
		return c.iterator().next();
	}

	/**
	 * Check whether the source is in the correct format for this importer.
	 */
	public static boolean isRecognizedFormat(Reader inOrig) throws IOException {
		// Our strategy is to look for the "@<type> {" line.
		BufferedReader in = new BufferedReader(inOrig);

		Pattern pat1 = Pattern.compile("@[a-zA-Z]*\\s*\\{");

		String str;

		while ((str = in.readLine()) != null) {

			if (pat1.matcher(str).find())
				return true;
			else if (str.startsWith(GUIGlobals.SIGNATURE))
				return true;
		}

		return false;
	}

	private void skipWhitespace() throws IOException {
		int c;

		while (true) {
			c = read();
			if ((c == -1) || (c == 65535)) {
				_eof = true;
				return;
			}

			if (Character.isWhitespace((char) c)) {
				continue;
			} else
				// found non-whitespace char
				// Util.pr("SkipWhitespace, stops: "+c);
				unread(c);
			/*
			 * try { Thread.currentThread().sleep(500); } catch
			 * (InterruptedException ex) {}
			 */
			break;
		}
	}

	private String skipAndRecordWhitespace(int j) throws IOException {
		int c;
		StringBuffer sb = new StringBuffer();
		if (j != ' ')
			sb.append((char) j);
		while (true) {
			c = read();
			if ((c == -1) || (c == 65535)) {
				_eof = true;
				return sb.toString();
			}

			if (Character.isWhitespace((char) c)) {
				if (c != ' ')
					sb.append((char) c);
				continue;
			} else
				// found non-whitespace char
				// Util.pr("SkipWhitespace, stops: "+c);
				unread(c);
			/*
			 * try { Thread.currentThread().sleep(500); } catch
			 * (InterruptedException ex) {}
			 */
			break;
		}
		return sb.toString();
	}

	/**
	 * Will parse the BibTex-Data found when reading from reader.
	 * 
	 * The reader will be consumed.
	 * 
	 * Multiple calls to parse() return the same results
	 * 
	 * @return ParserResult
	 * @throws IOException
	 */
	public ParserResult parse() throws IOException {

		_db = new BibtexDatabase(); // Bibtex related contents.
		_meta = new HashMap<String, String>(); // Metadata in comments for
												// Bibkeeper.
		entryTypes = new HashMap<String, BibtexEntryType>(); // To store custem
																// entry types
																// parsed.
		_pr = new ParserResult(_db, null, entryTypes);

		skipWhitespace();

		try {
			while (!_eof) {
				boolean found = consumeUncritically('@');
				if (!found)
					break;
				sb.append('@');
				skipWhitespace();
				String entryType = parseTextToken();
				BibtexEntryType tp = BibtexEntryType.getType(entryType);
				boolean isEntry = (tp != null);
				// Util.pr(tp.getName());

				if (isEntry) // True if not comment, preamble or string.
				{
					/**
					 * Morten Alver 13 Aug 2006: Trying to make the parser more
					 * robust. If an exception is thrown when parsing an entry,
					 * drop the entry and try to resume parsing. Add a warning
					 * for the user.
					 * 
					 * An alternative solution is to try rescuing the entry for
					 * which parsing failed, by returning the entry with the
					 * exception and adding it before parsing is continued.
					 */
					try {
						sb.append(tp.getName());
						BibtexEntry be = parseEntry(tp);
						if (be.getCiteKey() == null || be.getCiteKey().equals("")) {
							_pr.addWarning(Globals.lang("empty BibTeX key") + ": " + be.getAuthorTitleYear(40) + " ("
									+ Globals.lang("grouping may not work for this entry") + ")");
						}
					} catch (IOException ex) {
						ex.printStackTrace();
						_pr.addWarning(Globals.lang("Error occured when parsing entry") + ": '" + ex.getMessage()
								+ "'. " + Globals.lang("Skipped entry."));

					}
				}

				skipWhitespace();
			}

			// Before returning the database, update entries with unknown type
			// based on parsed type definitions, if possible.
			checkEntryTypes(_pr);

			// Instantiate meta data:
			_pr.setMetaData(new MetaData(_meta, _db));

			return _pr;
		} catch (KeyCollisionException kce) {
			// kce.printStackTrace();
			throw new IOException("Duplicate ID in bibtex file: " + kce.toString());
		}
	}

	private int peek() throws IOException {
		int c = read();
		unread(c);

		return c;
	}

	private int read() throws IOException {
		int c = _in.read();
		if (c == '\n')
			line++;
		return c;
	}

	private void unread(int c) throws IOException {
		if (c == '\n')
			line--;
		_in.unread(c);
	}

	public BibtexString parseString() throws IOException {
		// Util.pr("Parsing string");
		skipWhitespace();
		consume('{', '(');
		// while (read() != '}');
		skipWhitespace();
		// Util.pr("Parsing string name");
		String name = parseTextToken();
		// Util.pr("Parsed string name");
		skipWhitespace();
		// Util.pr("Now the contents");
		consume('=');
		String content = parseFieldContent(name);
		// Util.pr("Now I'm going to consume a }");
		consume('}', ')');
		// Util.pr("Finished string parsing.");
		String id = Util.createNeutralId();
		return new BibtexString(id, name, content);
	}

	public String parsePreamble() throws IOException {
		return parseBracketedText().toString();
	}

	public BibtexEntry parseEntry(BibtexEntryType tp) throws IOException {
		String id = Util.createNeutralId();// createId(tp, _db);
		BibtexEntry result = new BibtexEntry(id, tp);
		skipWhitespace();
		consume('{', '(');
		sb.append('{');
		int c = peek();
		if ((c != '\n') && (c != '\r'))
			skipWhitespace();
		String key = null;
		boolean doAgain = true;
		while (doAgain) {
			doAgain = false;
			try {
				if (key != null)
					key = key + parseKey();// parseTextToken(),
				else
					key = parseKey();
			} catch (NoLabelException ex) {
				// This exception will be thrown if the entry lacks a key
				// altogether, like in "@article{ author = { ...".
				// It will also be thrown if a key contains =.
				c = (char) peek();
				if (Character.isWhitespace(c) || (c == '{') || (c == '\"')) {
					String fieldName = ex.getMessage().trim().toLowerCase();
					String cont = parseFieldContent(fieldName);
					result.setField(fieldName, cont);
				} else {
					if (key != null)
						key = key + ex.getMessage() + "=";
					else
						key = ex.getMessage() + "=";
					doAgain = true;
				}
			}
		}

		if ((key != null) && key.equals(""))
			key = null;
		
		result.setField(BibtexFields.KEY_FIELD, key);
		skipWhitespace();
		sb.append(key+",\n");
		while (true) {
			c = peek();
			if ((c == '}') || (c == ')')) {
				break;
			}

			if (c == ',')
				consume(',');

			skipWhitespace();

			c = peek();
			if ((c == '}') || (c == ')')) {
				break;
			}
			parseField(result);
		}

		consume('}', ')');
		sb.append('}');
		return result;
	}

	private void parseField(BibtexEntry entry) throws IOException {
		String key = parseTextToken().toLowerCase();
		// Util.pr("Field: _"+key+"_");
		skipWhitespace();
		if ((key != null) && key.equals(""))
			return;
		sb.append(key);
		consume('=');
		sb.append('=');
		String content = parseFieldContent(key);
		// Now, if the field in question is set up to be fitted automatically
		// with braces around
		// capitals, we should remove those now when reading the field:
		if (Globals.prefs.putBracesAroundCapitals(key)) {
			content = Util.removeBracesAroundCapitals(content);
		}
		if (content.length() > 0) {
			if (entry.getField(key) == null)
				entry.setField(key, content);
			else {
				// The following hack enables the parser to deal with multiple
				// author or
				// editor lines, stringing them together instead of getting just
				// one of them.
				// Multiple author or editor lines are not allowed by the bibtex
				// format, but
				// at least one online database exports bibtex like that, making
				// it inconvenient
				// for users if JabRef didn't accept it.
				if (key.equals("author") || key.equals("editor"))
					entry.setField(key, entry.getField(key) + " and " + content);
			}
		}
		sb.append('{'+content+"},\n");
	}

	private String parseFieldContent(String key) throws IOException {
		skipWhitespace();
		StringBuffer value = new StringBuffer();
		int c = '.';

		while (((c = peek()) != ',') && (c != '}') && (c != ')')) {

			if (_eof) {
				throw new RuntimeException("Error in line " + line + ": EOF in mid-string");
			}
			if (c == '"') {
				StringBuffer text = parseQuotedFieldExactly();
				value.append(fieldContentParser.format(text));
				/*
				 * 
				 * The following code doesn't handle {"} correctly: // value is
				 * a string consume('"');
				 * 
				 * while (!((peek() == '"') && (j != '\\'))) { j = read(); if
				 * (_eof || (j == -1) || (j == 65535)) { throw new
				 * RuntimeException("Error in line "+line+ ": EOF in
				 * mid-string"); }
				 * 
				 * value.append((char) j); }
				 * 
				 * consume('"');
				 */
			} else if (c == '{') {
				// Value is a string enclosed in brackets. There can be pairs
				// of brackets inside of a field, so we need to count the
				// brackets to know when the string is finished.
				StringBuffer text = parseBracketedTextExactly();
				value.append(fieldContentParser.format(text, key));

			} else if (Character.isDigit((char) c)) { // value is a number

				String numString = parseTextToken();
				// Morten Alver 2007-07-04: I don't see the point of parsing the
				// integer
				// and converting it back to a string, so I'm removing the
				// construct below
				// the following line:
				value.append(numString);
				/*
				 * try { // Fixme: What is this for?
				 * value.append(String.valueOf(Integer.parseInt(numString))); }
				 * catch (NumberFormatException e) { // If Integer could not be
				 * parsed then just add the text // Used to fix [ 1594123 ]
				 * Failure to import big numbers value.append(numString); }
				 */
			} else if (c == '#') {
				consume('#');
			} else {
				String textToken = parseTextToken();
				if (textToken.length() == 0)
					throw new IOException("Error in line " + line + " or above: "
							+ "Empty text token.\nThis could be caused " + "by a missing comma between two fields.");
				value.append("#").append(textToken).append("#");
				// Util.pr(parseTextToken());
				// throw new RuntimeException("Unknown field type");
			}
			skipWhitespace();
		}
		// Util.pr("Returning field content: "+value.toString());

		// Check if we are to strip extra pairs of braces before returning:
		if (Globals.prefs.getBoolean("autoDoubleBraces")) {
			// Do it:
			while ((value.length() > 1) && (value.charAt(0) == '{') && (value.charAt(value.length() - 1) == '}')) {
				value.deleteCharAt(value.length() - 1);
				value.deleteCharAt(0);
			}
			// Problem: if the field content is "{DNA} blahblah {EPA}", one pair
			// too much will be removed.
			// Check if this is the case, and re-add as many pairs as needed.
			while (hasNegativeBraceCount(value.toString())) {
				value.insert(0, '{');
				value.append('}');
			}

		}
		return value.toString();

	}

	/**
	 * Originalinhalt nach parseFieldContent(String) verschoben.
	 * 
	 * @return
	 * @throws IOException
	 */
	// private String parseFieldContent() throws IOException {
	// return parseFieldContent(null);
	// }

	/**
	 * Check if a string at any point has had more ending braces (}) than
	 * opening ones ({). Will e.g. return true for the string "DNA} blahblal
	 * {EPA"
	 * 
	 * @param s
	 *            The string to check.
	 * @return true if at any index the brace count is negative.
	 */
	private boolean hasNegativeBraceCount(String s) {
		// System.out.println(s);
		int i = 0, count = 0;
		while (i < s.length()) {
			if (s.charAt(i) == '{')
				count++;
			else if (s.charAt(i) == '}')
				count--;
			if (count < 0)
				return true;
			i++;
		}
		return false;
	}

	/**
	 * This method is used to parse string labels, field names, entry type and
	 * numbers outside brackets.
	 */
	private String parseTextToken() throws IOException {
		StringBuffer token = new StringBuffer(20);

		while (true) {
			int c = read();
			// Util.pr(".. "+c);
			if (c == -1) {
				_eof = true;

				return token.toString();
			}

			if (Character.isLetterOrDigit((char) c) || (c == ':') || (c == '-') || (c == '_') || (c == '*')
					|| (c == '+') || (c == '.') || (c == '/') || (c == '\'')) {
				token.append((char) c);
			} else {
				unread(c);
				// Util.pr("Pasted text token: "+token.toString());
				return token.toString();
			}
		}
	}

	/**
	 * Tries to restore the key
	 * 
	 * @return rest of key on success, otherwise empty string
	 * @throws IOException
	 *             on Reader-Error
	 */
	private String fixKey() throws IOException {
		StringBuilder key = new StringBuilder();
		int lookahead_used = 0;
		char currentChar;

		// Find a char which ends key (','&&'\n') or entryfield ('='):
		do {
			currentChar = (char) read();
			key.append(currentChar);
			lookahead_used++;
		} while ((currentChar != ',' && currentChar != '\n' && currentChar != '=') && (lookahead_used < LOOKAHEAD));

		// Consumed a char too much, back into reader and remove from key:
		unread(currentChar);
		key.deleteCharAt(key.length() - 1);

		// Restore if possible:
		switch (currentChar) {
		case '=':

			// Get entryfieldname, push it back and take rest as key
			key = key.reverse();

			boolean matchedAlpha = false;
			for (int i = 0; i < key.length(); i++) {
				currentChar = key.charAt(i);

				/// Skip spaces:
				if (!matchedAlpha && currentChar == ' ') {
					continue;
				}
				matchedAlpha = true;

				// Begin of entryfieldname (e.g. author) -> push back:
				unread(currentChar);
				if (currentChar == ' ' || currentChar == '\n') {

					/*
					 * found whitespaces, entryfieldname completed -> key in
					 * keybuffer, skip whitespaces
					 */
					StringBuilder newKey = new StringBuilder();
					for (int j = i; j < key.length(); j++) {
						currentChar = key.charAt(j);
						if (!Character.isWhitespace(currentChar)) {
							newKey.append(currentChar);
						}
					}

					// Finished, now reverse newKey and remove whitespaces:
					_pr.addWarning(Globals.lang("Line %0: Found corrupted BibTeX-key.", String.valueOf(line)));
					key = newKey.reverse();
				}
			}
			break;

		case ',':

			_pr.addWarning(
					Globals.lang("Line %0: Found corrupted BibTeX-key (contains whitespaces).", String.valueOf(line)));

		case '\n':

			_pr.addWarning(Globals.lang("Line %0: Found corrupted BibTeX-key (comma missing).", String.valueOf(line)));

			break;

		default:

			// No more lookahead, give up:
			unreadBuffer(key);
			return "";
		}

		return removeWhitespaces(key).toString();
	}

	/**
	 * removes whitespaces from <code>sb</code>
	 * 
	 * @param sb
	 * @return
	 */
	private StringBuilder removeWhitespaces(StringBuilder sb) {
		StringBuilder newSb = new StringBuilder();
		char current;
		for (int i = 0; i < sb.length(); ++i) {
			current = sb.charAt(i);
			if (!Character.isWhitespace(current))
				newSb.append(current);
		}
		return newSb;
	}

	/**
	 * pushes buffer back into input
	 * 
	 * @param sb
	 * @throws IOException
	 *             can be thrown if buffer is bigger than LOOKAHEAD
	 */
	private void unreadBuffer(StringBuilder sb) throws IOException {
		for (int i = sb.length() - 1; i >= 0; --i) {
			unread(sb.charAt(i));
		}
	}

	/**
	 * This method is used to parse the bibtex key for an entry.
	 */
	private String parseKey() throws IOException, NoLabelException {
		StringBuffer token = new StringBuffer(20);

		while (true) {
			int c = read();
			// Util.pr(".. '"+(char)c+"'\t"+c);
			if (c == -1) {
				_eof = true;

				return token.toString();
			}

			// Ikke: #{}\uFFFD~\uFFFD
			//
			// G\uFFFDr: $_*+.-\/?"^
			if (!Character.isWhitespace((char) c)
					&& (Character.isLetterOrDigit((char) c) || (c == ':') || ((c != '#') && (c != '{') && (c != '}')
							&& (c != '\uFFFD') && (c != '~') && (c != '\uFFFD') && (c != ',') && (c != '=')))) {
				token.append((char) c);
			} else {

				if (Character.isWhitespace((char) c)) {
					// We have encountered white space instead of the comma at
					// the end of
					// the key. Possibly the comma is missing, so we try to
					// return what we
					// have found, as the key and try to restore the rest in
					// fixKey().
					return token.toString() + fixKey();
				} else if (c == ',') {
					unread(c);
					return token.toString();
					// } else if (Character.isWhitespace((char)c)) {
					// throw new NoLabelException(token.toString());
				} else if (c == '=') {
					// If we find a '=' sign, it is either an error, or
					// the entry lacked a comma signifying the end of the key.

					return token.toString();
					// throw new NoLabelException(token.toString());

				} else
					throw new IOException("Error in line " + line + ":" + "Character '" + (char) c + "' is not "
							+ "allowed in bibtex keys.");

			}
		}

	}

	private class NoLabelException extends Exception {
		public NoLabelException(String hasRead) {
			super(hasRead);
		}
	}

	private StringBuffer parseBracketedText() throws IOException {
		// Util.pr("Parse bracketed text");
		StringBuffer value = new StringBuffer();

		consume('{');

		int brackets = 0;

		while (!((peek() == '}') && (brackets == 0))) {

			int j = read();
			if ((j == -1) || (j == 65535)) {
				throw new RuntimeException("Error in line " + line + ": EOF in mid-string");
			} else if (j == '{')
				brackets++;
			else if (j == '}')
				brackets--;

			// If we encounter whitespace of any kind, read it as a
			// simple space, and ignore any others that follow immediately.
			/*
			 * if (j == '\n') { if (peek() == '\n') value.append('\n'); } else
			 */
			if (Character.isWhitespace((char) j)) {
				String whs = skipAndRecordWhitespace(j);

				// System.out.println(":"+whs+":");

				if (!whs.equals("") && !whs.equals("\n\t")) { // &&
																// !whs.equals("\n"))

					whs = whs.replaceAll("\t", ""); // Remove tabulators.

					// while (whs.endsWith("\t"))
					// whs = whs.substring(0, whs.length()-1);

					value.append(whs);

				} else {
					value.append(' ');
				}

			} else
				value.append((char) j);

		}

		consume('}');

		return value;
	}

	private StringBuffer parseBracketedTextExactly() throws IOException {

		StringBuffer value = new StringBuffer();

		consume('{');

		int brackets = 0;

		while (!((peek() == '}') && (brackets == 0))) {

			int j = read();
			if ((j == -1) || (j == 65535)) {
				throw new RuntimeException("Error in line " + line + ": EOF in mid-string");
			} else if (j == '{')
				brackets++;
			else if (j == '}')
				brackets--;

			value.append((char) j);

		}

		consume('}');

		return value;
	}

	private StringBuffer parseQuotedFieldExactly() throws IOException {

		StringBuffer value = new StringBuffer();

		consume('"');

		int brackets = 0;

		while (!((peek() == '"') && (brackets == 0))) {

			int j = read();
			if ((j == -1) || (j == 65535)) {
				throw new RuntimeException("Error in line " + line + ": EOF in mid-string");
			} else if (j == '{')
				brackets++;
			else if (j == '}')
				brackets--;

			value.append((char) j);

		}

		consume('"');

		return value;
	}

	private void consume(char expected) throws IOException {
		int c = read();

		if (c != expected) {
			unread(c);
			throw new RuntimeException(
					"Error in line " + line + ": Expected " + expected + " but received " + (char) c);
		}

	}

	private boolean consumeUncritically(char expected) throws IOException {
		int c;
		while (((c = read()) != expected) && (c != -1) && (c != 65535)) {
			// do nothing
		}

		if ((c == -1) || (c == 65535))
			_eof = true;

		// Return true if we actually found the character we were looking for:
		return c == expected;
	}

	private void consume(char expected1, char expected2) throws IOException {
		// Consumes one of the two, doesn't care which appears.

		int c = read();

		if ((c != expected1) && (c != expected2)) {
			throw new RuntimeException(
					"Error in line " + line + ": Expected " + expected1 + " or " + expected2 + " but received " + c);

		}

	}

	public void checkEntryTypes(ParserResult _pr) {

		for (BibtexEntry be : _db.getEntries()) {
			if (be.getType() instanceof UnknownEntryType) {
				// Look up the unknown type name in our map of parsed types:

				Object o = entryTypes.get(be.getType().getName().toLowerCase());
				if (o != null) {
					BibtexEntryType type = (BibtexEntryType) o;
					be.setType(type);
				} else {
					// System.out.println("Unknown entry type:
					// "+be.getType().getName());
					_pr.addWarning(Globals.lang("unknown entry type") + ": " + be.getType().getName() + ". "
							+ Globals.lang("Type set to 'other'") + ".");
					be.setType(BibtexEntryType.OTHER);
				}
			}
		}
	}

	

	public static boolean check(String bibtex) {
		// TODO Auto-generated method stub
		BibtexEntry be = singleFromString(bibtex);
		if (be == null)
			return false;
		else
			return true;
	}

	public void change() {
		// TODO Auto-generated method stub
		try {
			parse();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	  /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    public String ris2Bibtex(StringBuffer sb) throws IOException {

    	String[] entries = sb.toString().replaceAll("\u2013", "-").replaceAll("\u2014", "--").replaceAll("\u2015", "--").split("ER  -.*\\n");
    	
    	StringBuilder bibtex=new StringBuilder();
        for (int i = 0; i < entries.length; i++){

            if (entries[i].trim().length() == 0)
                continue;

            String type = "", author = "", editor = "", startPage = "", endPage = "",
                    comment = "";
            HashMap<String, String> hm = new HashMap<String, String>();


            String[] fields = entries[i].split("\n");

            for (int j = 0; j < fields.length; j++){
                StringBuffer current = new StringBuffer(fields[j]);
                boolean done = false;
                while (!done && (j < fields.length-1)) {
                    if ((fields[j+1].length() >= 6) && !fields[j+1].substring(2, 6).equals("  - ")) {
                        if ((current.length() > 0)
                                && !Character.isWhitespace(current.charAt(current.length()-1))
                                && !Character.isWhitespace(fields[j+1].charAt(0)))
                            current.append(' ');
                        current.append(fields[j+1]);
                        j++;
                    } else
                        done = true;
                }
                String entry = current.toString();
                if (entry.length() < 6) continue;
                else{
                    String lab = entry.substring(0, 2);
                    String val = entry.substring(6).trim();
                    if (lab.equals("TY")){
                        if (val.equals("BOOK")) type = "book";
                        else if (val.equals("JOUR") || val.equals("MGZN")) type = "article";
                        else if (val.equals("THES")) type = "phdthesis";
                        else if (val.equals("UNPB")) type = "unpublished";
                        else if (val.equals("RPRT")) type = "techreport";
                        else if (val.equals("CONF")) type = "inproceedings";
                        else if (val.equals("CHAP")) type = "incollection";//"inbook";

                        else type = "other";
                    }else if (lab.equals("T1") || lab.equals("TI")) {
                        String oldVal = hm.get("title");
                        if (oldVal == null)
                            hm.put("title", val);
                        else {
                            if (oldVal.endsWith(":") || oldVal.endsWith(".") || oldVal.endsWith("?"))
                                hm.put("title", oldVal+" "+val);
                            else
                                hm.put("title", oldVal+": "+val);
                        }
                    }
                        // =
                        // val;
                    else if (lab.equals("T2") || lab.equals("T3") || lab.equals("BT")) {
                        hm.put("booktitle", val);
                    }
                    else if (lab.equals("AU") || lab.equals("A1")) {
                        if (author.equals("")) // don't add " and " for the first author
                            author = val;
                        else author += " and " + val;
                    }
                    else if (lab.equals("A2")){
                        if (editor.equals("")) // don't add " and " for the first editor
                            editor = val;
                        else editor += " and " + val;
                    }
                    else if (lab.equals("JA") || lab.equals("JF") || lab.equals("JO")) {
                        if (type.equals("inproceedings"))
                            hm.put("booktitle", val);
                        else
                            hm.put("journal", val);
                    }

                    else if (lab.equals("SP")) startPage = val;
                    else if (lab.equals("PB")) {
                        if (type.equals("phdthesis"))
                            hm.put("school", val);
                        else
                            hm.put("publisher", val);
                    }
                    else if (lab.equals("AD") || lab.equals("CY"))
                        hm.put("address", val);
                    else if (lab.equals("EP")) endPage = val;
                    else if (lab.equals("SN"))
                        hm.put("issn", val);
                    else if (lab.equals("VL")) hm.put("volume", val);
                    else if (lab.equals("IS")) hm.put("number", val);
                    else if (lab.equals("N2") || lab.equals("AB")) {
                        String oldAb = hm.get("abstract");
                        if (oldAb == null)
                            hm.put("abstract", val);
                        else
                            hm.put("abstract", oldAb+"\n"+val);
                    }

                    else if (lab.equals("UR")) hm.put("url", val);
                    else if ((lab.equals("Y1") || lab.equals("PY")) && val.length() >= 4) {
                        String[] parts = val.split("/");
                        hm.put("year", parts[0]);
                        if ((parts.length > 1) && (parts[1].length() > 0)) {
                            try {
                                int month = Integer.parseInt(parts[1]);
                                if ((month > 0) && (month <= 12)) {
                                    //System.out.println(Globals.MONTHS[month-1]);
                                    hm.put("month", "#"+Globals.MONTHS[month-1]+"#");
                                }
                            } catch (NumberFormatException ex) {
                                // The month part is unparseable, so we ignore it.
                            }
                        }
                    }

                    else if (lab.equals("KW")){
                        if (!hm.containsKey("keywords")) hm.put("keywords", val);
                        else{
                            String kw = hm.get("keywords");
                            hm.put("keywords", kw + ", " + val);
                        }
                    }
                    else if (lab.equals("U1") || lab.equals("U2") || lab.equals("N1")) {
                        if (comment.length() > 0)
                            comment = comment+"\n";
                        comment = comment+val;
                    }
                    // Added ID import 2005.12.01, Morten Alver:
                    else if (lab.equals("ID"))
                        hm.put("refid", val);
                        // Added doi import (sciencedirect.com) 2011.01.10, Alexander Hug <alexander@alexanderhug.info>
                    else if (lab.equals("M3")){
                        String doi = val;
                        if (doi.startsWith("doi:")){
                            doi = doi.replaceAll("(?i)doi:", "").trim();
                            hm.put("doi", doi);
                        }
                    }
                }
                // fix authors
                if (author.length() > 0) {
                    author = AuthorList.fixAuthor_lastNameFirst(author);
                    hm.put("author", author);
                }
                if (editor.length() > 0) {
                    editor = AuthorList.fixAuthor_lastNameFirst(editor);
                    hm.put("editor", editor);
                }
                if (comment.length() > 0) {
                    hm.put("comment", comment);
                }

                hm.put("pages", startPage + "--" + endPage);
            }

            // Remove empty fields:
            ArrayList<Object> toRemove = new ArrayList<Object>();
            for (Iterator<String> it = hm.keySet().iterator(); it.hasNext();) {
                Object key = it.next();
                String content = hm.get(key);
                if ((content == null) || (content.trim().length() == 0))
                    toRemove.add(key);
            }
            for (Iterator<Object> iterator = toRemove.iterator(); iterator.hasNext();) {
                hm.remove(iterator.next());

            }

            // create one here
            BibtexEntry b = new BibtexEntry(BibtexFields.DEFAULT_BIBTEXENTRY_ID, Globals
                    .getEntryType(type)); // id assumes an existing database so don't
            b.setField(hm);
            StringWriter sw=new StringWriter();
            b.write(sw, new LatexFieldFormatter(), false);
            bibtex.append(sw.getBuffer().toString());
        }

        return bibtex.toString();

    }
}