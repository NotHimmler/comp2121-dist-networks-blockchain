import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map.Entry;
import java.util.ArrayList;

public class PeriodicPrinterRunnable implements Runnable{

    private ServerStatus serverStatus;

    public PeriodicPrinterRunnable(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    @Override
    public void run() {
        while(true) {
            ConcurrentHashMap<ServerInfo, Date> serverInfos = serverStatus.getServerInfos();
            for (Entry<ServerInfo, Date> entry : serverInfos.entrySet()) {
                // if greater than 2T, remove
                ServerInfo si = entry.getKey();
                if (new Date().getTime() - entry.getValue().getTime() > 4000) {
                    serverStatus.removeAddressDetails(si.getHost(), si.getPort());
                } else {
                    System.out.printf(si.getHost() + "-" + entry.getValue() + " ");
                }
            }
            System.out.println();

            // sleep for two seconds
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
    }
}
