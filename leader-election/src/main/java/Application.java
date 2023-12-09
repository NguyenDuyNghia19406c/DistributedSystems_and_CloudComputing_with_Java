import cluster.management.LeaderElection;
import cluster.management.OnElectionAction;
import cluster.management.OnElectionCallBack;
import cluster.management.ServiceRegistry;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.WatchedEvent;

import java.io.IOException;
import java.util.EventListener;

public class Application implements Watcher{
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final int DEFAULT_PORT = 8080;
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws InterruptedException, KeeperException, IOException {
        int currentServerPort = args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Application application = new Application();
        ZooKeeper zooKeeper = application.connectToZookeeper();

        ServiceRegistry serviceRegistry = new ServiceRegistry(zooKeeper);
        OnElectionAction onElectionAction = new OnElectionAction(serviceRegistry, currentServerPort);

        LeaderElection leaderElection = new LeaderElection(zooKeeper, onElectionAction);
        leaderElection.volunteerForLeadership();
        leaderElection.reelectLeader();

        application.run();
    }


    public ZooKeeper connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
        return  zooKeeper;
    }
    private void run() throws InterruptedException {
        synchronized (zooKeeper){
            zooKeeper.wait();
        }
    }

    private void close() throws InterruptedException{
        zooKeeper.close();
    }

    @Override
    public  void process(WatchedEvent event){
        switch (event.getType()){
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected){
                    System.out.println("Successfully connected to Zookeeper");
                }else{
                    synchronized (zooKeeper){
                        System.out.println("Disconnect from Zookeeper Event");
                        zooKeeper.notifyAll();
                    }
                }
        }
    }
}
