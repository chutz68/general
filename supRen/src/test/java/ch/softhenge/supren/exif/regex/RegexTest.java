package ch.softhenge.supren.exif.regex;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.CoreMatchers;
import org.hamcrest.number.OrderingComparison;
import org.junit.Test;

public class RegexTest {

	private final String fileExtRegex = "(\\.(?i)(jpg|png|gif|bmp|cr2)$)";
	
	private final String filenames[] =  {
			  "IMG_0652.jpg"
			, "img_1652.JPG"
			, "IMG_4444-23.jpg"
			, "20130301_0321_E6D.CR2"
			, "20130303_E-6D_0290.jpg"
			, "P9040009.jpg"
			, "GalaxySIIHandy.jpg"
			, "20130301_E6D_0985.CR2"
		};
	
	@Test
	public void testFileNameRegexPat1() {
		String regex = "(^img_)([0-9]{4})" + fileExtRegex;
		boolean regexcchk[] = { 
			  true
			, true
			, false
			, false
			, false
			, false
			, false
			, false
		};
		Pattern pat = Pattern.compile(regex);
		for (int i = 0; i < filenames.length; i++) {
			Matcher mat = pat.matcher(filenames[i].toLowerCase());
			assertThat(filenames[i] + " idx: " + i, mat.matches(), CoreMatchers.is(regexcchk[i]));
			if (mat.matches()) {
				assertThat("Group Count", mat.groupCount(), CoreMatchers.is(OrderingComparison.greaterThanOrEqualTo(3)));
				if (i == 0)		assertThat(mat.group(2), CoreMatchers.is("0652"));
				if (i == 1) 	assertThat(mat.group(2), CoreMatchers.is("1652"));
			}
		}
	}

	@Test
	public void testFileNameRegexPat2() {
		String regex = "(^[0-9]{8}_)([0-9]{6})" + fileExtRegex;
		boolean regexcchk[] = { 
				  false
				, false
				, false
				, false
				, false
				, false
				, false
				, false
		};
		Pattern pat = Pattern.compile(regex);
		for (int i = 0; i < filenames.length; i++) {
			Matcher mat = pat.matcher(filenames[i].toLowerCase());
			assertThat(filenames[i] + " idx: " + i, mat.matches(), CoreMatchers.is(regexcchk[i]));
		}
	}

	@Test
	public void testFileNameRegexPat3() {
		String regex = "(^[0-9]{8})(_)([a-zA-Z0-9\\-]{4})(_)([0-9]{4})" + fileExtRegex;
		boolean regexcchk[] = { 
				  false
				, false
				, false
				, false
				, true
				, false
				, false
				, false
		};
		Pattern pat = Pattern.compile(regex);
		for (int i = 0; i < filenames.length; i++) {
			Matcher mat = pat.matcher(filenames[i].toLowerCase());
			assertThat(filenames[i] + " idx: " + i, mat.matches(), CoreMatchers.is(regexcchk[i]));
			if (mat.matches()) {
				assertThat("Group Count", mat.groupCount(), CoreMatchers.is(OrderingComparison.greaterThanOrEqualTo(3)));
				assertThat(mat.group(5), CoreMatchers.is("0290"));
			}
		}
	}

	@Test
	public void testFileNameRegexPat4() {
		String regex = "(^[0-9]{8}_)([0-9]{4})(_\\w*)" + fileExtRegex;
		boolean regexcchk[] = { 
				  false
				, false
				, false
				, true
				, false
				, false
				, false
				, false
		};
		Pattern pat = Pattern.compile(regex);
		for (int i = 0; i < filenames.length; i++) {
			Matcher mat = pat.matcher(filenames[i].toLowerCase());
			assertThat(filenames[i] + " idx: " + i, mat.matches(), CoreMatchers.is(regexcchk[i]));
			if (mat.matches()) {
				assertThat("Group Count", mat.groupCount(), CoreMatchers.is(OrderingComparison.greaterThanOrEqualTo(3)));
				assertThat(mat.group(2), CoreMatchers.is("0321"));
			}
		}
	}

	@Test
	public void testFileNameRegexPat5() {
		String regex = "(^[0-9]{8}_)(\\w{3}_)([0-9]{4})" + fileExtRegex;
		boolean regexcchk[] = { 
				  false
				, false
				, false
				, false
				, false
				, false
				, false
				, true
		};
		Pattern pat = Pattern.compile(regex);
		for (int i = 0; i < filenames.length; i++) {
			Matcher mat = pat.matcher(filenames[i].toLowerCase());
			assertThat(filenames[i] + " idx: " + i, mat.matches(), CoreMatchers.is(regexcchk[i]));
			if (mat.matches()) {
				assertThat("Group Count", mat.groupCount(), CoreMatchers.is(OrderingComparison.greaterThanOrEqualTo(3)));
				assertThat(mat.group(3), CoreMatchers.is("0985"));
			}
		}
	}

	@Test
	public void testFileNameRegexPat6() {
		String regex = "(^p[0-9]{3})([0-9]{4})" + fileExtRegex;
		boolean regexcchk[] = { 
				  false
				, false
				, false
				, false
				, false
				, true
				, false
				, false
		};
		Pattern pat = Pattern.compile(regex);
		for (int i = 0; i < filenames.length; i++) {
			Matcher mat = pat.matcher(filenames[i].toLowerCase());
			assertThat(filenames[i] + " idx: " + i, mat.matches(), CoreMatchers.is(regexcchk[i]));
			if (mat.matches()) {
				assertThat("Group Count", mat.groupCount(), CoreMatchers.is(OrderingComparison.greaterThanOrEqualTo(3)));
				assertThat(mat.group(2), CoreMatchers.is("0009"));
			}
		}
	}	
	
	
	@Test
	public void testCameraName4ch() {
		String regex = "[a-zA-Z0-9\\-]{4}";
		Pattern pat = Pattern.compile(regex);
		Matcher mat;
		mat = pat.matcher("IP-5");
		assertTrue(regex, mat.matches());
		mat = pat.matcher("IX95");
		assertTrue(regex, mat.matches());		
	}

}
