/*
 * Constructor for handshake packet.
 * it also includes the marshaling function.
 18 bytes of handshake header, 10 bytes of zero bits and 4 bytes of peerID.
 String handshake_
 */
class HandShake {
    String header;
    int peerId;
    private String handShakeMsg;

    //Send a handshake message

    HandShake(int peerId) {
        header = "P2PFILESHARINGPROJ";
        this.peerId = peerId;
        handShakeMsg = header + "0000000000" + peerId;
    }

    //Receive a handshake message and process it to get peerid and header

    HandShake(byte[] received_handSHakeMsg) {
        //receive as a String
        String stringMessage = new String (received_handSHakeMsg);
        StringBuffer stringBuffer;
        //slice
        stringBuffer = new StringBuffer (stringMessage);
        header = stringBuffer.substring (0, 18);
        peerId = Integer.parseInt (stringBuffer.substring (28, 32));
    }

    //Convert a handShake message to byte to send

    //String handshake msg is changed to byte[] to send out and changed to string
    //to operate when receiving the handshake.
    byte[] Marshalling() {
        byte[] message;
        message = handShakeMsg.getBytes ();
        return message;
    }
}
