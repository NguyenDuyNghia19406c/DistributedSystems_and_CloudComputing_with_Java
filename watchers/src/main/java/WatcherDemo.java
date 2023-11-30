import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;


public class WatcherDemo implements Watcher {

    private static final String ZOOKEEPER_ADDESS = "localhost:2181";
    private  static final int SESSION_TIMEOUT = 3000;
    private static final String TARGET_ZNODE = "/target_znode";
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        WatcherDemo watcherDemo = new WatcherDemo();

        watcherDemo.connectToZookeeper();
        watcherDemo.watchTargetZnode();
        watcherDemo.run();
        watcherDemo.close();
        System.out.println("Disconnect");
    }

    public void connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDESS, SESSION_TIMEOUT, this);
    }

    private void run() throws InterruptedException {
        synchronized (zooKeeper){
            zooKeeper.wait();
        }
    }

    private void close() throws InterruptedException{
        zooKeeper.close();
    }

    public void watchTargetZnode() throws InterruptedException, KeeperException {
        Stat stat = zooKeeper.exists(TARGET_ZNODE, this);
        if(stat==null){
            return;
        }

        byte [] data = zooKeeper.getData(TARGET_ZNODE, this, stat);

        List<String> children = zooKeeper.getChildren(TARGET_ZNODE, this);

        System.out.println("Data " + new String(data) + " + child: " + children);

    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()){
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected){
                    System.out.println("Successfully connected to Zookeeper");
                } else {
                    synchronized (zooKeeper){
                        System.out.println("Disconnect from Zookeeper Event");
                        zooKeeper.notifyAll();
                    }
                }
                break;
            case NodeDeleted:
                System.out.println(TARGET_ZNODE + " was deleted");
                break;
            case NodeCreated:
                System.out.println((TARGET_ZNODE + " was created"));
                break;
            case NodeDataChanged:
                System.out.println((TARGET_ZNODE + " was changed"));
                break;
            case NodeChildrenChanged:
                System.out.println((TARGET_ZNODE + " was changed"));
                break;
        }
        try{
            watchTargetZnode();
        }catch(KeeperException e){
        }catch(InterruptedException e){
        }
    }
}
