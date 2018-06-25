import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map.Entry;

public class PeriodicHeartBeatRunnable implements Runnable {

    private ServerStatus serverStatus;
    private int port;
    private int sequenceNumber;

    public PeriodicHeartBeatRunnable(ServerStatus serverStatus, int port) {
        this.serverStatus = serverStatus;
        this.port = port;
        this.sequenceNumber = 0;
    }

    @Override
    public void run() {
        while(true) {
            // broadcast HeartBeat message to all peers
            ArrayList<Thread> threadArrayList = new ArrayList<>();
            for (Entry<ServerInfo, Date> entry : serverStatus.getServerInfos().entrySet()) {
                ServerInfo si = entry.getKey();
                Thread thread = new Thread(new HeartBeatClientRunnable(si.getHost(), si.getPort(), String.format("hb|%s|%s", port, sequenceNumber)));
                threadArrayList.add(thread);
                thread.start();
            }

            for (Thread thread : threadArrayList) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                }
            }

            // increment the sequenceNumber
            sequenceNumber += 1;

            // sleep for two seconds
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
    }
}
