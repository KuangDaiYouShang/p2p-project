import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Date;

class EventLogger {

    private int me;//My ID
    public BufferedWriter logWriter;
    private String logMsg;
    EventLogger(int me) throws IOException {
        this.me = me;
        String logPath = "/cise/homes/cl2/Desktop/project/log_peer_" + me + ".log";
        logWriter = new BufferedWriter(new FileWriter(logPath));
    }

    synchronized private String NTP() {
        SimpleDateFormat time;
        time = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]: ");
        return time.format(new Date());
    }

    synchronized void telnet(int connectId) throws IOException {
        logMsg = NTP() + "Peer [" + me + "] makes a connection to Peer [" + connectId + "].\n";
        logWriter.write(logMsg);
        System.out.println(logMsg);
    }

    synchronized void telneted(int connectId) throws IOException {
        logMsg = NTP() + "Peer [" + me + "] is connected from Peer [" + connectId + "].\n";
        logWriter.write(logMsg);
        System.out.println(logMsg);
    }

    synchronized void changeOfPrefereedNeighbors(LinkedList list_PrefereedNeighbors) throws IOException {
        logMsg = NTP() + "Peer [" + me + "] has the preferred neighbors [";
        logWriter.write(logMsg);
        int psize = list_PrefereedNeighbors.size();
    		for (int i = 0; i < psize-1; i++) {
    			logWriter.write(list_PrefereedNeighbors.get(i) + ", ");
          logMsg = logMsg + list_PrefereedNeighbors.get(i) + ", ";
    			}
    		logWriter.write(list_PrefereedNeighbors.get(psize-1) + "].\n");
        logMsg = logMsg + list_PrefereedNeighbors.get(psize-1) + "].\n";
        System.out.println(logMsg);
    }

    synchronized void changeOfOptimisticallyUnchokedNeighbors(int OPTunchokedID) throws IOException {
        logMsg = NTP() + "Peer [" + me + "] has the optimistically neighbor [" + OPTunchokedID + "].\n";
        logWriter.write(logMsg);
        System.out.println(logMsg);
    }

    synchronized void unchoke_Msg(int unchokedById) throws IOException {
        logMsg = NTP() + "Peer [" + me + "] is unchoked by Peer [" + unchokedById + "].\n";
        logWriter.write(logMsg);
        System.out.println(logMsg);
    }

    synchronized void choke_Msg(int chokedById) throws IOException {
        logMsg = NTP() + "Peer [" + me + "] is choked by Peer [" + chokedById + "].\n";
        logWriter.write(logMsg);
        System.out.println(logMsg);
    }

    synchronized void haveReceived(int haveById, int index) throws IOException {
        logMsg = NTP() + "Peer [" + me + "] received the 'have' message from [" + haveById + "] for the piece [" + index + "].\n";
        logWriter.write(logMsg);
        System.out.println(logMsg);
    }

    synchronized void interestedReiceived(int receiveById) throws IOException {
        logMsg = NTP() + "Peer [" + me + "] received the 'interested' message from [" + receiveById + "].\n";
        logWriter.write(logMsg);
        System.out.println(logMsg);
    }

    synchronized void notInterestedReceived(int receiveById) throws IOException {
        logMsg = NTP() + "Peer [" + me + "] received the 'not interested' message from [" + receiveById + "].\n";
        logWriter.write(logMsg);
        System.out.println(logMsg);
    }

    synchronized void pieceDownloadMsg(int peerId, int index, int pieceNumber) throws IOException {
        logMsg = NTP() + "Peer [" + me + "] has downloaded the piece [" + index + "] from [" + peerId + "]. Now the number of pieces it has is [" + pieceNumber + "].\n";
        logWriter.write(logMsg);
        System.out.println(logMsg);
    }

    synchronized void fileDownloadedMsg() throws IOException {
        logMsg = NTP() + "Peer [" + me + "] has downloaded the complete file.\n";
        logWriter.write(logMsg);
        System.out.println(logMsg);

    }
}
