import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class peerProcess {
    private int numOfPreferredNeighbors;
    private int unChokingInterval;
    private int opUnChokingInterval;
    private Segmentation seg;
    private int fileExist;
    //collection of peerDic to store the remote peers information
    private LinkedList <PeerDic> peerGroup = new LinkedList <> ();//This is like a interest list
    private PeerDic OptUnchokedNeighbor = null;//we pick preferred from interes
    private int _unChokingInterval;
    private int _opUnChokingInterval;
    private boolean trigOut = false;


    private void rateRanking(LinkedList<PeerDic> peerGroup) {
        Comparator <PeerDic> comparator = new Comparator <PeerDic> () {
            int rate1, rate2;
            public int compare (PeerDic p1, PeerDic p2) {
                rate1 = p1.getDownloadRate ();
                rate2 = p2.getDownloadRate ();
                if (rate1 > rate2)
                    return 1;
                else if (rate1 < rate2)
                    return -1;
                else
                    return 0;
            }
        };
        peerGroup.sort (comparator);
    }

    private synchronized void set_optUnchokedPeer() throws IOException {
      //Check whether there is a optUnchokedNeighbor.
  		//optUnchokedNeighbor is different from peerGroup
        if (OptUnchokedNeighbor != null) {
            OptUnchokedNeighbor.choke (true);
            peerGroup.add (OptUnchokedNeighbor);
            OptUnchokedNeighbor = null;
        }
        int i = 0;
        while ( i < peerGroup.size () ) {
            if (peerGroup.get (i).states[0] != -1) {
                OptUnchokedNeighbor = peerGroup.get (i);
                //make movement according to the states.
                OptUnchokedNeighbor.choke (false);
                seg.eventlogger.changeOfOptimisticallyUnchokedNeighbors (OptUnchokedNeighbor.remoteID);
                seg.eventlogger.unchoke_Msg (OptUnchokedNeighbor.remoteID);
                peerGroup.remove (peerGroup.get (i));
                return;
            }
            i++;
        }
    }

    private synchronized int isDone() {
      //check whether everything is finished.
  		//when one peer is finished, it calls completed() and is removed from the peerGroup.
      int i = 0;
      if (peerGroup.size() == 0) i++;
      if (OptUnchokedNeighbor == null) i++;
      return i;
    }

    //Each peer maintains bitfields for all neighbors and update them whenever it receives 'have' messages from
    //its neighbors
    synchronized void HaveBroadcast(int i) throws IOException {
      //tell all the peers now I have piece i
        int j = 0;
        while ( j < peerGroup.size () ) {
            peerGroup.get (j).have (i);
            j++;
        }
        if (OptUnchokedNeighbor != null)
            OptUnchokedNeighbor.have (i);
    }

    private int doubleTimeOut() {
      //There are two timeouts and corresponds to two actions.
        int time;
        time = _unChokingInterval >= _opUnChokingInterval ? _opUnChokingInterval : _unChokingInterval;
        _unChokingInterval = _unChokingInterval - time;
        _opUnChokingInterval = _opUnChokingInterval - time;
        return time * 1000;
    }

    synchronized void completed(PeerDic p) throws IOException {
      //reset timer / remove peer.
        if (!trigOut) {
            trigOut = true;
        }
        if (OptUnchokedNeighbor == p) {
            OptUnchokedNeighbor = null;
            _opUnChokingInterval = 0;
        } else {
            peerGroup.remove (p);
            _unChokingInterval = 0;
        }
    }

   private void readFile(String ID) throws Exception{
        String[] configList = new String[] {"Common.cfg", "PeerInfo.cfg"};
        System.out.println("Reading the common config information..............");
        int peerId = Integer.parseInt(ID);
        BufferedReader commReader = new BufferedReader(new FileReader (new File(configList[0])));
        String[] prop = commReader.readLine().split(" ");
        numOfPreferredNeighbors = Integer.parseInt(prop[1]);
        prop = commReader.readLine().split(" ");
        unChokingInterval = Integer.parseInt(prop[1]);
        prop = commReader.readLine().split(" ");
        opUnChokingInterval = Integer.parseInt(prop[1]);
        prop = commReader.readLine().split(" ");
        String fileName = prop[1];
        prop = commReader.readLine().split(" ");
       int fileSize = Integer.parseInt(prop[1]);
        prop = commReader.readLine().split(" ");
       int pieceSize = Integer.parseInt(prop[1]);
        _unChokingInterval = unChokingInterval;
        _opUnChokingInterval = opUnChokingInterval;
        extraFunctions.packPrint(numOfPreferredNeighbors, unChokingInterval, opUnChokingInterval, fileName, fileSize, pieceSize);
        System.out.println("Registrating for the peers................");
        Path tar =Paths.get("/cise/homes/cl2/Desktop/project/", configList[1]);
        Charset charset = Charset.forName("ISO-8859-1");
        List<String> lines = Files.readAllLines(tar, charset);
        String[] peerINFO;

       int id;
       for(int i = 0; i < lines.size() && lines.get(i) != null && !Objects.equals(lines.get(i), ""); i++)
        {
            peerINFO = lines.get(i).split(" ");
            id = Integer.parseInt(peerINFO[0]);
            if(id == peerId) {
                fileExist = Integer.parseInt(peerINFO[3]);
            }
        }
        seg = new Segmentation(peerId, fileName, fileExist, fileSize, pieceSize);
        //Set the values readed from peerInfo.cfg and create socket(XH)
        ServerSocket serverSocket = null;
        for (String line : lines) {
            peerINFO = line.split(" ");
            id = Integer.parseInt(peerINFO[0]);
            String ipAddress = peerINFO[1];
            int port = Integer.parseInt(peerINFO[2]);
            //Get the portNumber of this peerId
            PeerDic OnePeer;
            int d = id - peerId;
            if (d == 0) {
                serverSocket = new ServerSocket(port);
            } else if (d < 0) {
                Socket socket = new Socket(ipAddress, port);
                OnePeer = new PeerDic(peerId, id, socket, fileSize, pieceSize, seg, this);
                peerGroup.add(OnePeer);
                seg.eventlogger.telneted(id);
            } else if (d > 0) {
                Socket socket = null;
                if (serverSocket != null) {
                    socket = serverSocket.accept();
                }
                OnePeer = new PeerDic(peerId, id, socket, fileSize, pieceSize, seg, this);
                peerGroup.add(OnePeer);
                seg.eventlogger.telnet(id);
            }
        }
        System.out.println("Finish Reading the config information..............");
    }


    private void set_chokelist(LinkedList<Integer> pg) throws IOException {
        if (peerGroup.size () <= numOfPreferredNeighbors) {
            for (PeerDic _p : peerGroup) {
                if (_p.states[0] != -1) {
                    _p.choke(false);//unchoke it if it is not unchoked
                    seg.eventlogger.unchoke_Msg(_p.remoteID);//Peer is unchoked by remoteID
                    pg.add(_p.remoteID);
                }
            }
        } else {//if the numberOfPreferredNeighbors is smaller than the size, it should be the first bound.
            int i = 0;
            while ( i < peerGroup.size () ) {
                if (i < numOfPreferredNeighbors) {
                    if (peerGroup.get (i).states[0] != -1) {
                        peerGroup.get (i).choke (false);
                        seg.eventlogger.unchoke_Msg (peerGroup.get (i).remoteID);
                        pg.add (peerGroup.get (i).remoteID);
                    }
                } else {//once get enough for the list , choke the others.
                    if (peerGroup.get (i).states[0] != 1) {
                        peerGroup.get (i).choke (true);
                        seg.eventlogger.choke_Msg (peerGroup.get (i).remoteID);
                    }
                }
                i++;
            }
        }
    }

    private void startAllPeers() {
        for ( PeerDic p : peerGroup ) {
            new Thread (p).start ();
        }
    }

    private peerProcess(String ID) throws Exception {

        readFile(ID);
        startAllPeers();

        while ( true ) {
            double over = 0.1;
            Thread.sleep (doubleTimeOut());
            if (isDone() == 2) {
                Thread.sleep (1000);//check whether it is initialized,
 		seg.eventlogger.fileDownloadedMsg();//all done??
                seg.eventlogger.logWriter.close();
                break;
            }

            if (_unChokingInterval < over) {
                if (peerGroup.size () != 0) {
                    LinkedList <Integer> l = new LinkedList <> ();
                    set_chokelist(l);
                    if (l.size () != 0) {
                        seg.eventlogger.changeOfPrefereedNeighbors (l);
                    }
                }
                _unChokingInterval = unChokingInterval;
                rateRanking(peerGroup);
            }
            if (_opUnChokingInterval < over) {
                set_optUnchokedPeer();
                _opUnChokingInterval = opUnChokingInterval;
            }
        }
    }


    public static void main (String[] args) throws Exception {
        new peerProcess (args[ 0 ]);
    }
}
