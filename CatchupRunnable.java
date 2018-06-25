import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map.Entry;

public class CatchupRunnable implements Runnable {
	private Blockchain blockchain;
	private String host;
	private int port;

	public CatchupRunnable(Blockchain blockchain, String host, int port) {
		this.blockchain = blockchain;
		this.host = host;
		this.port = port;
	}

	@Override
	public void run() {
		Block ourHead = blockchain.getHead();
		byte[] prevHash = ourHead == null ? new byte[32] : ourHead.getPreviousHash();
		String ourHeadPrevHash = Base64.getEncoder().encodeToString(prevHash);
		//Get head block and check if previous block hash is the same as ours
		//If not the same, request the previous block until it is
		try {
			// create socket with a timeout of 2 seconds
			Socket toServer = new Socket();
			toServer.connect(new InetSocketAddress(host, port), 2000);
			PrintWriter printWriter = new PrintWriter(toServer.getOutputStream(), true);
			// send the message forward
			printWriter.println("cu");
			printWriter.flush();

			ObjectInputStream ois = new ObjectInputStream(toServer.getInputStream());
			//receive reply
			Block recBlock = (Block) ois.readObject();
			Block head = recBlock;
			// close printWriter and socket
			printWriter.close();
			toServer.close();
			int length = 1;
			String baseBlockPrevHash = Base64.getEncoder().encodeToString(new byte[32]);
			
			while(!Base64.getEncoder().encodeToString(recBlock.getPreviousHash()).equals(baseBlockPrevHash) && recBlock != null) {
				toServer = new Socket();
				length++;
				toServer.connect(new InetSocketAddress(host, port), 2000);
				printWriter = new PrintWriter(toServer.getOutputStream(), true);
				// send the message forward
				printWriter.printf("cu|%s\n", Base64.getEncoder().encodeToString(recBlock.getPreviousHash()));
				printWriter.flush();

				ois = new ObjectInputStream(toServer.getInputStream());
				//receive reply
				Block tempBlock = (Block) ois.readObject();
				recBlock.setPreviousBlock(tempBlock);
				recBlock = tempBlock;
				// close printWriter and socket
				printWriter.close();
				toServer.close();
			}
			//recBlock.setPreviousBlock(ourHead == null ? null : ourHead.getPreviousBlock());
			blockchain.setHead(head);
			blockchain.setLength(length);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}