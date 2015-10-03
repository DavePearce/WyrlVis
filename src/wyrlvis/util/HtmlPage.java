package wyrlvis.util;

import java.io.*;

/**
 * A simple abstraction of a web-page.
 * 
 * @author djp
 *
 */
public class HtmlPage {
	
	public void writeHeader(PrintStream out) {
		
	}
	
	public void writeBody(PrintStream out) {
		
	}
	
	public void write(OutputStream os) {
		PrintStream out;
		try {
			out = new PrintStream(os, false, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			out = new PrintStream(os);
		}
		out.println("<html>");
		out.println("<header>");
		writeHeader(out);
		out.println("</header>");
		out.println("<body>");
		writeBody(out);
		out.println("</body>");
		out.println("</html>");
	}
}
