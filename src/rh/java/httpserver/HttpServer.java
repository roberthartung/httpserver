package rh.java.httpserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

public class HttpServer extends Observable implements Observer, Runnable {
	private ServerSocket server;

	public HttpServer(int port) throws IOException {
		server = new ServerSocket(port);
	}
	
	public void run() {
		Socket client;
		ClientHandler handler;
		try {
			while((client = server.accept()) != null) {
				handler = new ClientHandler(client);
				handler.addObserver(this);
				new Thread(handler).start();
			}
			server.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void update(Observable arg0, Object arg1) {
		if(arg0 instanceof ClientHandler) {
			final ClientHandler handler = (ClientHandler) arg0;
			if(arg1 instanceof HttpRequest) {
				HttpRequest request = (HttpRequest) arg1;
				
				if(handler.isUpgraded()) {
					// TODO(rh): Demo application
					Timer t = new Timer();
					t.scheduleAtFixedRate(new TimerTask() {
						
						@Override
						public void run() {
							ByteBuffer buffer = ByteBuffer.allocate(3);
							buffer.put((byte) (0x80 | 0x02)); 	// FIN + Binary frame
							buffer.put((byte) (0x00 | 1)); 		// 1 Byte Length
							buffer.put((byte) 1); 				// 1 Data Byte
							try {
								handler.write(buffer);
							} catch (IOException e) {
								cancel();
								e.printStackTrace();
							}
						}
					}, 0, 500);
					
					System.out.println("Handler is upgraded. Don't send default HTTP response.");
				} else {
					setChanged();
					notifyObservers(request);
				} 
			} else if(arg1 instanceof byte[]) {
				byte[] data = (byte[]) arg1;
				System.out.println("received: " + new String(data));
			}
		}
	}
}
