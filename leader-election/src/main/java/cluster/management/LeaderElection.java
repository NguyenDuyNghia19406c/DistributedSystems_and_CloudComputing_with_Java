package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LeaderElection  implements Watcher {
//    private static final String ZOOKEEPER_ADDESS = "localhost:2181";
//    private  static final int SESSION_TIMEOUT = 3000;
    private static final String ELECTION_NAMESPACE = "/election";
    private String currentZnodeName;
    private ZooKeeper zooKeeper;
    private final OnElectionCallBack onElectionCallBack;

//    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
//        LeaderElection leaderElection = new LeaderElection();
//
//        leaderElection.connectToZookeeper();
//        leaderElection.volunteerForLeadership();
//        leaderElection.reelectLeader();
//        leaderElection.run();
//        leaderElection.close();
//        System.out.println("Disconnect");
//    }
    public LeaderElection(ZooKeeper zooKeeper, OnElectionCallBack onElectionCallBack){
        this.zooKeeper = zooKeeper;
        this.onElectionCallBack = onElectionCallBack;
    }
//
//    public void connectToZookeeper() throws IOException {
//        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDESS, SESSION_TIMEOUT, this);
//    }
//
//    private void run() throws InterruptedException {
//        synchronized (zooKeeper){
//            zooKeeper.wait();
//        }
//    }
//
//    private void close() throws InterruptedException{
//        zooKeeper.close();
//    }



    public void volunteerForLeadership() throws InterruptedException, KeeperException {
        String znodePrefix = ELECTION_NAMESPACE + "/c_";
        String znodeFullPath = zooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        System.out.println("znode name" + znodeFullPath);
        this.currentZnodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    public void reelectLeader() throws InterruptedException, KeeperException {
        Stat predecessorStat = null;
        String predecessorZnodeName = "";

        while (predecessorStat == null){
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);

            Collections.sort(children);
            String smallestChild = children.get(0);

            if (smallestChild.equals(currentZnodeName)){
                System.out.println("Iam the leader");
                onElectionCallBack.onElectdToBeLeader();
                return;
            }else{
                System.out.println("Iam not the leader");
                int predecessorIndex = Collections.binarySearch(children, currentZnodeName) - 1;
                predecessorZnodeName = children.get(predecessorIndex);
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + '/' + predecessorZnodeName, this);
            }
        }
        onElectionCallBack.onWorker();

        System.out.println("Watching Znode: " + predecessorZnodeName);
        System.out.println();
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
                }break;
            case NodeDeleted:
                try{
                    reelectLeader();
                }catch (KeeperException e){
                }catch (InterruptedException e){
                }
        }
    }
}
