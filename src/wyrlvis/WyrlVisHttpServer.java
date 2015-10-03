package wyrlvis;

import java.io.*;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import wyrlvis.util.HttpFileHandler;

/**
 * Responsible for initialising and starting the HTTP server which manages the
 * tool
 * 
 * @author djp
 *
 */
public class WyrlVisHttpServer {
	
	public static final int HTTP_PORT = 8080;
		
	// =======================================================================
	// Main Entry Point
	// =======================================================================
	public static void main(String[] argc) {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
			server.createContext("/js", new HttpFileHandler(new File("."),"text/javascript"));
			server.setExecutor(null); // creates a default executor
			server.start();
			System.out.println("SERVER HAS BEGUN");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
}
