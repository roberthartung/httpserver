package rh.java.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpServer extends Observable implements Observer, Runnable {
	private static Logger logger = Logger.getLogger(HttpServer.class.getName());
	
	static {
		logger.setLevel(Level.ALL);
	}
	
	
	private ServerSocket server;
	
	private Map<String, Class<? extends UpgradeHandler>> upgradeHandlers = new HashMap<String, Class<? extends UpgradeHandler>>();
	
	// private Map<ClientHandler, Thread> threads = new HashMap<ClientHandler, Thread>();
	
	private Map<ClientHandler, Future<?>> clients = new HashMap<ClientHandler, Future<?>>();
	
	private ExecutorService executorService;
	
	/**
	 * Create a new HttpServer at [port]
	 * 
	 * @param port
	 * @throws IOException
	 */
	
	public HttpServer(Integer port) throws IOException {
		if(port != null) {
			server = new ServerSocket();
			server.bind(new InetSocketAddress("0.0.0.0", port));
		} else {
			server = new ServerSocket();
			// server.bind(null);
			server.bind(new InetSocketAddress("0.0.0.0", 0));
		}
		executorService = Executors.newFixedThreadPool(15);
	}
	
	/**
	 * Returns the port the server listens on
	 * 
	 * @return
	 */
	
	public int getPort() {
		InetSocketAddress addr = (InetSocketAddress) server.getLocalSocketAddress();
		return addr.getPort();
	}
	
	/**
	 * Thread: Accept clients
	 * 
	 * TODO(rh): Use Thread pool instead of running a thread for each individual client
	 */
	
	public void run() {
		Socket client;
		ClientHandler handler;
		
		try {
			while((client = server.accept()) != null) {
				handler = new ClientHandler(client);
				handler.addObserver(this);
				clients.put(handler, executorService.submit(handler));
				// Thread t = new Thread(handler);
				// threads.put(handler,  t);
				// t.start();
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
					upgradeHandlerClass = upgradeHandlers.get(request.getFirstHeader("Upgrade"));
				}
				
				// TODO(rh): Configurable?
				String header = request.getFirstHeader("Connection");
				if(header != null) {
					switch(header.toLowerCase()) {
						case "keep-alive" :
							request.setResponseHeader("Connection", "keep-alive");
							break;
					}
				}
				
				// If we have an upgrade handler: send handler otherwise
				if(upgradeHandlerClass != null) {
					Future<?> f = clients.remove(clientHandler);
					logger.finest(clientHandler + " interrupted.");
					if(!f.cancel(true)) {
						logger.severe("Unable to cancel ClientHandler.");
					}
					// executorService.
					// Thread t = threads.remove(clientHandler);
					// t.interrupt();
					UpgradeHandler handler = clientHandler.upgrade(upgradeHandlerClass, request);
					if(handler != null) {
						// new Thread(handler).start();
						executorService.submit(handler);
						setChanged();
						notifyObservers(handler);
					} else {
						logger.severe("Failed to use upgrade handler.");
					}
				} else {
					setChanged();
					notifyObservers(request);
				}
			} else {
				logger.severe("Unknown update argument from ClientHandler: " + arg);
			}
		} else {
			logger.severe("Update from unknown observed child");
		}
	}
	
	public void addUpgradeHandler(String type, Class<? extends UpgradeHandler> clazz) {
		upgradeHandlers.put(type, clazz);
	}
	
	public void removeUpgradeHandler(Class<? extends UpgradeHandler> clazz) {
		upgradeHandlers.remove(clazz);
	}
}
