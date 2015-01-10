package rh.java.http.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class HttpServer extends Observable implements Observer, Runnable {
	private ServerSocket server;
	
	private Map<String, Class<? extends UpgradeHandler>> upgradeHandlers = new HashMap<String, Class<? extends UpgradeHandler>>();
	
	private Map<ClientHandler, Thread> threads = new HashMap<ClientHandler, Thread>();
	
	public HttpServer(int port) throws IOException {
		server = new ServerSocket(port);
	}
	
	public int getPort() {
		return server.getLocalPort();
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
					System.out.println("Upgrade in request: " + request.getHeader("Upgrade").getContent());
					upgradeHandlerClass = upgradeHandlers.get(request.getHeader("Upgrade").getContent());
					
					/*
					if(upgradeHandlerClass != null) {
						try {
							upgradeHandler = upgradeHandlerClass.newInstance();
						} catch (InstantiationException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					}
					*/
					/*
					for(UpgradeHandler handler : upgradeHandlers) {
						if(handler.canUpgrade(type, request)) {
							upgradeHandler = handler;
							break;
						}
					}
					*/
				}
				
				// If we have an upgrade handler: send handler otherwise
				if(upgradeHandlerClass != null) {
					Thread t = threads.remove(clientHandler);
					t.interrupt();
					System.out.println("Upgrade handler found.");
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
				
			} /*else if(arg1 instanceof UpgradeHandler) {
				handler.deleteObserver(this);
				setChanged();
				notifyObservers(arg1);
			} */ else {
				System.err.println("Unknown update argument from ClientHandler: " + arg);
			}
			/*
			  else if(arg1 instanceof byte[]) {
				byte[] data = (byte[]) arg1;
				System.out.println("received: " + new String(data));
			}
			 */
		}
	}
	
	public void addUpgradeHandler(String type, Class<? extends UpgradeHandler> clazz) {
		upgradeHandlers.put(type, clazz);
	}
	
	public void removeUpgradeHandler(Class<? extends UpgradeHandler> clazz) {
		upgradeHandlers.remove(clazz);
	}
}
