package wyrlvis;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;

import static java.net.HttpURLConnection.*;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Responsible for initialising and starting the HTTP server which manages the
 * tool
 * 
 * @author djp
 *
 */
public class WyrlVisMain {
	
	public static final int HTTP_PORT = 8080;
	
	/**
	 * Responsible for serving files from the file system.
	 * 
	 * @author David J. Pearce
	 *
	 */
	private static class FileServer implements HttpHandler {		
		/**
		 * The mime type for files handled by this server.
		 */
		private final String mimeType;
		
		/**
		 * The root directory for the file store this server serves from.
		 */
		private final File rootDir;
		
		private static int CHUNK_SIZE = 1024;
		
		public FileServer(File rootDir, String mimeType) {
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
	
	// =======================================================================
	// Main Entry Point
	// =======================================================================
	public static void main(String[] argc) {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
			server.createContext("/js", new FileServer(new File("."),"text/javascript"));
			server.setExecutor(null); // creates a default executor
			server.start();
			System.out.println("SERVER HAS BEGUN");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
}
