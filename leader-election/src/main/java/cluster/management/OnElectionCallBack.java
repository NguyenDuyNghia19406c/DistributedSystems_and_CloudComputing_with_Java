package cluster.management;

import org.apache.zookeeper.KeeperException;

public interface OnElectionCallBack {
    void onElectdToBeLeader() throws InterruptedException, KeeperException;
    void onWorker();
}
