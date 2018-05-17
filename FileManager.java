import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


class FileManager {
    private InputStream in;
    private OutputStream out;
    FileManager(InputStream in, OutputStream out) throws IOException {
        this.in = in;
        this.out = out;
    }

    //whatever you want to send, make it byte array and put it onto stream.
    synchronized void sendFile(messagePack _messagePack) throws IOException {
        byte[] msg = _messagePack.encMsg();
        //the file piece is encapsulated in messagePack class.
        out.write(msg);
        out.flush();
    }

    synchronized private void sendFile(HandShake handShakeMsg) throws IOException{
        byte[] msg = handShakeMsg.Marshalling();
        out.write(msg);
        out.flush();
    }

    synchronized void sendFile(int type) throws IOException{
        messagePack _messagePack = new messagePack(type);
        byte[] msg = _messagePack.encMsg();
        out.write(msg);
        out.flush();
    }

    synchronized byte[] receiveFile() throws IOException{
      //The 4-byte message length specifies the message length in bytes. It does not include
  		//the length of the message length field itself.

  		//specifies the size of the payload.
  		//read the length part and payload part seperately, and then copy them to the same byte array.
        byte[] byteLength = new byte[4];
        byte[] tempByte;
        int receiveInt;
        int totalInt;
        totalInt = 0;
        while(true)
        {
            if ((totalInt < 4)) {//byte[], offset, length
                receiveInt = in.read (byteLength, totalInt, 4 - totalInt);
                totalInt = totalInt + receiveInt;
            } else {
                break;
            }
        }

        //change Msglength to int so that I can tell the size of FileByte and create byte
    		//array with specific size.
        int fileLength;
        fileLength = extraFunctions.byte2Int(byteLength);
        byte[] FileByte = new byte[fileLength];
        //clear the counter totalInt before reading the file.
    		//we don`t have to clear receiveInt because it is overwritten
        totalInt = 0;

        if (fileLength > totalInt) {
            do {
                receiveInt = in.read(FileByte, totalInt, fileLength - totalInt);
                totalInt = totalInt + receiveInt;
            } while (totalInt < fileLength);
        }
        byte[] outputByte;
        outputByte = new byte[byteLength.length+FileByte.length];
        System.arraycopy(byteLength, 0, outputByte, 0, 4);
        System.arraycopy(FileByte, 0, outputByte, 4, 4 + FileByte.length - 4);
        return outputByte;
    }

    synchronized void do_handShake(int id, Segmentation seg, int remoteID) throws IOException {
	//send out the handshake message to the remote peer;
        HandShake handshake = new HandShake (id);
        sendFile(handshake);
        seg.eventlogger.telnet (remoteID);
        //read the byte hand shake message from the inputstream
        byte[] rev = new byte[32];
        in.read(rev, 0, 32);
        //call the handshake constructor receiver version in handshake class
        HandShake revMsg = new HandShake (rev);
        if (revMsg.header.equals ("P2PFILESHARINGPROJ"))
            if (revMsg.peerId == remoteID) {
                seg.eventlogger.telneted (remoteID);//This peer is connected from peer remoteID.
            }
    }
}
