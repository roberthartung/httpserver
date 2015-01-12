package rh.java.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpServer extends Observable implements Observer, Runnable {
	private ServerSocket server;
	
	private Map<String, Class<? extends UpgradeHandler>> upgradeHandlers = new HashMap<String, Class<? extends UpgradeHandler>>();
	
	private Map<ClientHandler, Thread> threads = new HashMap<ClientHandler, Thread>();
	
	public HttpServer(Integer port) throws IOException {
		if(port != null) {
			server = new ServerSocket(port);
		} else {
			server = new ServerSocket();
			server.bind(null);
		}
	}
	
	public int getPort() {
		InetSocketAddress addr = (InetSocketAddress) server.getLocalSocketAddress();
		return addr.getPort();
	}
	
	public void run() {
		Socket client;
		ClientHandler handler;
		
		try {
			while((client = server.accept()) != null) {
				handler = new ClientHandler(client);
				handler.addObserver(this);
				Thread t = new Thread(handler);
				threads.put(handler,  t);
				t.start();
			}
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Observe method. Called from ClientHandlers if they have a new request
	 */
	
	@Override
	public void update(Observable observee, Object arg) {
		if(observee instanceof ClientHandler) {
			ClientHandler clientHandler = (ClientHandler) observee;
			if(arg instanceof HttpRequest) {
				HttpRequest request = (HttpRequest) arg;
				
				// Check if we can upgrade this request
				// UpgradeHandler upgradeHandler = null;
				Class<? extends UpgradeHandler> upgradeHandlerClass = null;
				if(request.hasHeader("Upgrade")) {
					upgradeHandlerClass = upgradeHandlers.get(request.getHeader("Upgrade").getContent());
				}
				
				// If we have an upgrade handler: send handler otherwise
				if(upgradeHandlerClass != null) {
					Thread t = threads.remove(clientHandler);
					t.interrupt();
					UpgradeHandler handler = clientHandler.upgrade(upgradeHandlerClass, request);
					if(handler != null) {
						new Thread(handler).start();
						setChanged();
						notifyObservers(handler);
					} else {
						System.err.println("Failed to use upgrade handler.");
					}
				} else {
					setChanged();
					notifyObservers(request);
				}
				
			} else {
				System.err.println("Unknown update argument from ClientHandler: " + arg);
			}
		}
	}
	
	public void addUpgradeHandler(String type, Class<? extends UpgradeHandler> clazz) {
		upgradeHandlers.put(type, clazz);
	}
	
	public void removeUpgradeHandler(Class<? extends UpgradeHandler> clazz) {
		upgradeHandlers.remove(clazz);
	}
}
