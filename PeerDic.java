import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;

public class PeerDic implements Runnable{
    int remoteID;
    private int localID;
    private BitSet remoteBitField;
    private int bitMapSize;
    private Segmentation seg;
    private peerProcess peerProcess;
    InputStream in;
    int[] states;
    private ArrayList<Integer> haveList = new ArrayList <>();
    private FileManager file;
    private messageManager msgManager;

    PeerDic(int idNumber, int antherId, Socket socket, int fileSize, int chunkSize, Segmentation seg, peerProcess peerProcess) throws IOException
    {
        this.seg = seg;
        this.peerProcess = peerProcess;
        localID = idNumber;
        remoteID = antherId;
        bitMapSize = Math.round(fileSize/chunkSize);
        remoteBitField = new BitSet(bitMapSize);
        in = socket.getInputStream ();
        OutputStream out = socket.getOutputStream();
        file = new FileManager(in, out);//choke, unchoke, interested, not interested, have, bitfield, request, piece..
        /*
          I choke you  ------- states[0]
          I am choked ------- states[1]
          I interest you ------- states[2]
          I am interested by you ------- states[3]
          intermediate choke --------states[4]
          have-------states[5]
          intermediate request-------states[6]
        */
        states = new int[7];
        for (int i = 0; i < 6; i++) {
          if(i != 4 && i != 6) states[i] = 0;
          else states[i] = -1;
        }
        msgManager = new messageManager(states, seg, antherId, file, bitMapSize, remoteBitField, peerProcess);
    }

    @Override
    public void run() {
        try {
            file.do_handShake(localID, seg, remoteID);
            sendBitField();
            do {
                state4choke();
                //If the following conditions are satisfied, jump out of the loop.
            			// I have everything, remote peer has everything, and i sent all the have.
                if (msgManager.seg.checkBitField() && checkBitFd(msgManager.remoteBitField) && msgManager.states[5] != 1) {
                    Thread.sleep(1000);
		            peerProcess.completed(this);
                    break;
                }
                //if the inputStream is not ready yet, go back to the beginning of the loop.
                if (in.available() == 0) {
                    Thread.sleep(50);
                    continue;
                }
                //create the container to put received message and change it to messagePack structure.
                byte[] receiveMessage = file.receiveFile();
                messagePack msg = extraFunctions.byte2Msg(receiveMessage);
                boolean getchokeAndDiff = (states[6] != -1 && (msg.messageType == 0 || states[1] == 1));
                //If i am unchoked and i have request or i have request but im choked.
                //clear the request
                //states[6] is the inter mediate value of request
                if (getchokeAndDiff) {
                    states[2] = -1;
                    seg.requestedField[states[6]] = 0;
                    states[6] = -1;
                }
                msgManager.nextMove(msg);
            } while (true);
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized void choke(boolean choke_or_not)
    {
        if (choke_or_not) {
            states[0] = 1;
            states[4] = 1;//choke
        }
        else {
            states[0] = -1;
            states[4] = 2;//unchoke
        }
    }

    synchronized int getDownloadRate()
    {   //pay attention to this as it is integer.
        int dRate = msgManager.piecesReceivedFrom;
        //store the piecesReceivedFrom use and then clear it for the next round.
        msgManager.piecesReceivedFrom = 0;
        return dRate;
    }



    synchronized void have(int i) throws IOException
    {
        haveList.add(i);
        states[5] = 1;
    }

    private synchronized void state4choke() throws IOException
    {
        switch (states[4]) {
            case 0:
                break;
            case 1: {
                file.sendFile (0);
                states[4] = 0;
                break;
            }
            case 2: {
                file.sendFile (1);
                states[4] = 0;
                break;
            }
        }
        if (states[5] == 1) {//states decides the action, so if have = 1, send have.
            if (haveList.size() != 0) {
                do {
                    messagePack m = new messagePack(4);
                    m.datapart(haveList.get(0));
                    file.sendFile(m);
                    haveList.remove(0);
                } while (haveList.size() != 0);
            }
            states[5] = -1;

        }
    }

    private synchronized boolean checkBitFd(BitSet bitField)//All bitField true, return true. otherwise false.
    {
        for(int i=0;i<bitMapSize;i++)
            if(!bitField.get(i))
                return false;
        return true;
    }

    private synchronized void sendBitField() throws IOException {
        byte[] sendBitField = seg.bitFiled_to_Byte ();
        messagePack bitFieldMsg = new messagePack (5, sendBitField);
        file.sendFile (bitFieldMsg);
        //call the method in FileManager and send out.
    }
}
