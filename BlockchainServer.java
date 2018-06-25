import java.io.*;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Base64;

public class BlockchainServer {

    public static void main(String[] args) {

        if (args.length != 3) {
            return;
        }

        int localPort = 0;
        int remotePort = 0;
        String remoteHost = null;

        try {
            localPort = Integer.parseInt(args[0]);
            remoteHost = args[1];
            remotePort = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return;
        }

        Blockchain blockchain = new Blockchain();

        ServerStatus serverStatus = new ServerStatus(remoteHost, remotePort, localPort);

        Block ourHead = blockchain.getHead();
		byte[] prevHash = ourHead == null ? new byte[32] : ourHead.getPreviousHash();
		String ourHeadPrevHash = Base64.getEncoder().encodeToString(prevHash);
		//Get head block and check if previous block hash is the same as ours
		//If not the same, request the previous block until it is
		try {
			// create socket with a timeout of 2 seconds
			Socket toServer = new Socket();
			toServer.connect(new InetSocketAddress(remoteHost, remotePort), 2000);
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
				toServer.connect(new InetSocketAddress(remoteHost, remotePort), 2000);
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

        PeriodicHeartBeatRunnable phr = new PeriodicHeartBeatRunnable(serverStatus, localPort);
        Thread phrt = new Thread(phr);
        phrt.start();
        
        PeriodicPrinterRunnable ppr = new PeriodicPrinterRunnable(serverStatus);
        Thread pprt = new Thread(ppr);
        pprt.start();

        PeriodicCatchupRunnable pcur = new PeriodicCatchupRunnable(serverStatus, blockchain);
        Thread pcurt = new Thread(pcur);
        pcurt.start();

        PeriodicCommitRunnable pcr = new PeriodicCommitRunnable(blockchain);
        Thread pct = new Thread(pcr);
        pct.start();

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(localPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new BlockchainServerRunnable(clientSocket, blockchain, serverStatus)).start();
            }
        } catch (IllegalArgumentException e) {
        } catch (IOException e) {
        } finally {
            try {
                pcr.setRunning(false);
                pct.join();
                if (serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
            } catch (InterruptedException e) {
            }
        }
    }
}
