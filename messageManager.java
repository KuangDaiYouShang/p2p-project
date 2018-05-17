import java.io.IOException;
import java.util.BitSet;
import java.util.Random;

public class messageManager {
    int piecesReceivedFrom = 0;
    int[] states;
    Segmentation seg; //get local bitField, read & write
    private int remoteID;
    private FileManager file;// do handshake , send & receive messagePack
    private int bitMapSize;
    private peerProcess peerProcess;
    public BitSet remoteBitField;

    messageManager(int[] states, Segmentation seg, int remoteID, FileManager file, int bitMapSize, BitSet remoteBitField, peerProcess peerProcess) {
        this.states = states;
        this.seg = seg;
        this.remoteID = remoteID;
        this.file = file;
        this.bitMapSize = bitMapSize;
        this.remoteBitField = remoteBitField;
        this.peerProcess = peerProcess;
    }


    void nextMove(messagePack msg) throws IOException {
      switch(msg.messageType) {
       case 0:
           seg.eventlogger.choke_Msg(remoteID);
           //choked by remoteID.
           states[1] = 1;
           break;
       case 1:
           handle_unchoke();
           break;
       case 2:
           states[3] = 1;
           seg.eventlogger.interestedReiceived(remoteID);
           break;
       case 3:
           states[3] = -1;
           seg.eventlogger.notInterestedReceived(remoteID);
           break;
       case 4:
           handle_have(msg);
           break;
       case 5:
           handle_bitfield(msg);
           break;
       case 6:
           handle_request(msg);
           break;
       case 7:
           handle_piece(msg);
           break;
       }
    }

    private void handle_unchoke() throws IOException
    {
        int temp;
        do {
            temp = getPiece(remoteBitField);//Compare the remoteBitField with local bitField and specify the bits I want!
            //There are two situations. 1. you don`t have what I want. 2. what I want is not
            //requested yet.
            if (temp == -1 || seg.requestedField[temp] == 0) break;
        } while (true);

        //Next move for thenextMovehich are not included in the loop
    		//send uninterested for situation1. Send request for situation 2.
        if (temp == -1)
            file.sendFile(3);
        else
        {
            messagePack _messagePack = new messagePack(6);
            //adding request payload to the message im going to send
			      //request is a 4byte-index and the int is changed to byte in the plusPayload function.
            _messagePack.datapart(temp);//specify the _wantedParts in the states
            states[6] =temp;//state[6] has three possible values, -1, 0 and the index of bit i want. -1 means nothing i want.
            file.sendFile(_messagePack);
            states[2] = 1;
            states[1] = -1; //unchoke, i`m not choked by you anymore.
            seg.requestedField[temp]=1;
        }
        seg.eventlogger.unchoke_Msg(remoteID);
    }


    private void handle_have(messagePack m) throws IOException
    {   //what comes in is the 4byte have payload and we have to change it to Int
		    //because this is how it works in the BitSet.
        int pieceNum=extraFunctions.byte2Int(m.messagePayload);
        seg.eventlogger.haveReceived(remoteID, pieceNum);
        remoteBitField.flip(pieceNum);
        	//Since the remote peer told me what it has, lets check what I have, seg is a
          //local concept. If I don`t have it, send interest. Otherwise, send not-interested.
        if (seg.getBitField (pieceNum)) {
            file.sendFile(3);
        } else {
            file.sendFile(2);
        }

    }

    private void handle_request(messagePack m)throws IOException
    {
        int index_of_chunk = extraFunctions.byte2Int(m.messagePayload);
        //if the relationship is not choked and the remote peer doesn`t have that part.
        if(states[0] !=1 && !remoteBitField.get (index_of_chunk))
        {   //give the remote peer what it wants
            messagePack ms = new messagePack(7);
            ms.datapart(seg.rFile(index_of_chunk));
            file.sendFile(ms);
        }
    }

    private void handle_piece(messagePack _messagePack) throws IOException
    {
        //piece message includes the 4byte index of piece in the payload
        // we have to change it to int first
        byte[] indexOfPiece;
        int i;
        int k = 1;
        int temp;
        byte[] contentByte;
        indexOfPiece = new byte[4];
        contentByte = new byte[_messagePack.messagePayload.length - 4];
        i = 0;
        while (i < 4) {
            indexOfPiece[i] = _messagePack.messagePayload[i];
            i++;
        }
        temp = extraFunctions.byte2Int(indexOfPiece);

        if(!seg.getBitField (temp))
        {
            int j;
            j = 0;
            i = 4;
            while (i < _messagePack.messagePayload.length) {
                contentByte[j++] = _messagePack.messagePayload[i];
                i++;
            }

            if (k == 1) {
                seg.wfile(contentByte, temp);
            }
            seg.setBitField(temp, 1);

            seg.eventlogger.pieceDownloadMsg(remoteID, temp, seg.cnt);
            peerProcess.HaveBroadcast(temp); //tell all the peers now i have piece i
            states[6] = -1;
            states[2]= -1;
            seg.requestedField[temp]=2;
        }

        if(states[2]!=1)
        {
            int piece;
            //getParts finds the difference between the local and remote peer.
            //before, u don`t have what i want, but now you have.
            // i was not interested in you before.
            while(true)
            {
                piece = getPiece(remoteBitField);
                if (piece == -1 || seg.requestedField[piece] == 0) break;
            }
            states[6] = piece;
            //The situation that no difference.
            if (piece == -1) {
                file.sendFile(_messagePack);
                states[2] = -1;
            } else {
                messagePack ms = new messagePack(6);
                ms.datapart(piece);
                file.sendFile(ms);
                states[2] = 1;
                seg.requestedField[piece]=1;
            }
        }
        //everytime you get a piece from the remote peer, the piecesReceivedFrom is added by one.
        plusDownloadRate();
    }

    private int getPiece(BitSet bitField)
    {   //randomly pick one bit from the bitField
        int base;
        base = new Random().nextInt(bitMapSize);
        int i;
        i = 0;
        while (i < bitMapSize) {//the remote peer have, but I don`t have
            int temp = base + i;
            if (!bitField.get((temp) % bitMapSize) || seg.getBitField ((temp) % bitMapSize)) {
                i++;
            } else {
                return (temp) % bitMapSize;
            }
        }
        return -1;
    }

    private void handle_bitfield(messagePack _messagePack) throws IOException
    {   		//extract the bitfield from message
		        //this part decides interest or not
        remoteBitField = rdBitfd(_messagePack.messagePayload);
        int index = getPiece(remoteBitField);
        if (index < 0) {
            file.sendFile(3);
        } else {
            file.sendFile(2);
        }
    }

    private BitSet rdBitfd(byte[] contentByte)
    {   //Returns a new bit set of the clone of argument byte array.
        BitSet temp;
        temp = BitSet.valueOf(contentByte);
        return temp;
    }

    private synchronized void plusDownloadRate()
    {
        piecesReceivedFrom = piecesReceivedFrom++;
    }

}
