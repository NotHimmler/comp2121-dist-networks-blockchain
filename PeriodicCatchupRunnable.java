import java.util.ArrayList;
import java.util.Base64;

public class PeriodicCatchupRunnable implements Runnable {

	private Blockchain blockchain;
	private ServerStatus serverStatus;

	public PeriodicCatchupRunnable(ServerStatus serverStatus, Blockchain blockchain) {
		this.serverStatus = serverStatus;
		this.blockchain = blockchain;
	}

	@Override
	public void run() {
		while(true) {
			//Send out latest blocks to up to 5 random peers
			ArrayList<ServerInfo> serverInfos = serverStatus.getServerArray();
			int numServers = serverInfos.size();
			ArrayList<Thread> threads = new ArrayList<Thread>();
			Block head = blockchain.getHead();
			byte[] hash = (head == null ? new byte[32] : head.calculateHash());
			String base64Str = Base64.getEncoder().encodeToString(hash);
			ArrayList<Integer> usedIndexes = new ArrayList<Integer>();
			for(int i = 0; i < 5 && i < numServers; i++){
				int index;
				if(numServers <= 5 ) {
					index = i;
				} else {
					index = Integer.valueOf((int) Math.round(Math.random()*numServers));
					while(usedIndexes.contains(index)) index = Integer.valueOf((int) Math.round(Math.random()*numServers));
					usedIndexes.add(index);
				}
				ServerInfo si = serverInfos.get(i);
				String message = String.format("lb|%d|%d|%s", serverStatus.getLocalPort(), blockchain.getLength(), base64Str);
				HeartBeatClientRunnable hbcr = new HeartBeatClientRunnable(si.getHost(), si.getPort(), message);
				Thread hbcrt = new Thread(hbcr);
				threads.add(hbcrt);
				hbcrt.start();
			}
			for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                }
            }
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
		}		
	}
}