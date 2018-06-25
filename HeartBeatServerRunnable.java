import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class HeartBeatServerRunnable implements Runnable{

    private Socket toClient;
    private ConcurrentHashMap<ServerInfo, Date> serverStatus;

    public HeartBeatServerRunnable (Socket toClient, ConcurrentHashMap<ServerInfo, Date> serverStatus) {
        this.toClient = toClient;
        this.serverStatus = serverStatus;
    }

    @Override
    public void run() {
        try {
            String localIP = (((InetSocketAddress) toClient.getLocalSocketAddress()).getAddress()).toString().replace("/", "");
            String remoteIP = (((InetSocketAddress) toClient.getRemoteSocketAddress()).getAddress()).toString().replace("/", "");

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(toClient.getInputStream()));

            String line = bufferedReader.readLine();
            String[] tokens = line.split("\\|");
            switch (tokens[0]) {
                case "hb":
                    serverStatus.put(new ServerInfo(remoteIP, toClient.getPort()), new Date());
                    if (tokens[1].equals("0")) {
                        ArrayList<Thread> threadArrayList = new ArrayList<>();
                        for (ServerInfo si : serverStatus.keySet()) {
                            String ip = si.getHost();
                            if (ip.equals(remoteIP) || ip.equals(localIP)) {
                                continue;
                            }
                            Thread thread = new Thread(new HeartBeatClientRunnable(ip, new Integer(toClient.getPort()), String.format("si|%s|%s", remoteIP, toClient.getPort())));
                            threadArrayList.add(thread);
                            thread.start();
                        }
                        for (Thread thread : threadArrayList) {
                            thread.join();
                        }
                    }
                    break;
                case "si":
                    boolean found = false;
                    for(ServerInfo si : serverStatus.keySet()) {
                        if(si.getHost().equals(tokens[1]))
                            found = true;
                    }
                    if (!found) {
                        serverStatus.put(new ServerInfo(tokens[1], new Integer(tokens[2])), new Date());

                        // relay
                        ArrayList<Thread> threadArrayList = new ArrayList<>();
                        for (ServerInfo si : serverStatus.keySet()) {
                            String ip = si.getHost();
                            if (ip.equals(tokens[1]) || ip.equals(remoteIP) || ip.equals(localIP)) {
                                continue;
                            }
                            Thread thread = new Thread(new HeartBeatClientRunnable(ip, si.getPort(), line));
                            threadArrayList.add(thread);
                            thread.start();
                        }
                        for (Thread thread : threadArrayList) {
                            thread.join();
                        }
                    }
                    break;
            }
            bufferedReader.close();
            toClient.close();
        } catch (Exception e) {
        }
    }
}
