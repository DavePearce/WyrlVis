package wyrlvis.util;

import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Responsible for serving static files from the file system.
 * 
 * @author David J. Pearce
 *
 */
public class HttpFileHandler implements HttpHandler {
	/**
	 * The mime type for files handled by this server.
	 */
	private final String mimeType;
	
	/**
	 * The root directory for the file store this server serves from.
	 */
	private final File rootDir;
	
	private static int CHUNK_SIZE = 1024;
	
	public HttpFileHandler(File rootDir, String mimeType) {
		this.rootDir = rootDir;
		this.mimeType = mimeType;
	}
	
	@Override
	public void handle(HttpExchange hx) throws IOException {
		if (hx.getRequestMethod().equals("GET")) {
			try {
				URI request = hx.getRequestURI();
				File file = new File(rootDir,request.getPath());
				Headers headers = hx.getResponseHeaders();
				headers.add("Content-Type", mimeType);
				hx.sendResponseHeaders(HTTP_OK, file.length());
				OutputStream os = hx.getResponseBody();
				writeFile(os,file);					
			} catch(Exception e) {
				hx.sendResponseHeaders(HTTP_BAD_REQUEST,0);
			}
		} else {
			System.out.println("BAD METHOD");
			hx.sendResponseHeaders(HTTP_BAD_METHOD, 0);
		}
		
		hx.close();
	}
	
	/**
	 * Write a given file in chunks to the output stream.
	 * @param out
	 * @param file
	 * @throws IOException
	 */
	private void writeFile(OutputStream out, File file) throws IOException {
		FileInputStream fin = new FileInputStream(file);
		byte[] bytes = new byte[CHUNK_SIZE];
		int nread;
		while ((nread = fin.read(bytes, 0, CHUNK_SIZE)) == CHUNK_SIZE) {
			out.write(bytes, 0, CHUNK_SIZE);
		}
		out.write(bytes, 0, nread);
		fin.close();
	}
}
