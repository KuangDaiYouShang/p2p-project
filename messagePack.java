/*
 This class is a constructor for packet. It implements the encapsulation.
 In all, it has 2 types of container and corresponding datapart function.
 */
public class messagePack {
  //messageLength 4
	//messageType 1 byte
	//messagePayload ???
    int messageType;
    byte[] messagePayload;
    int messageLength;

    //without payload..
    messagePack(int messageType)
    {
        this.messageType = messageType;
        this.messagePayload = null;
        messageLength = 5;//4+1 without payload
    }

    //with payload and without length.
    messagePack(int messageType, byte[] messagePayload)
    {   //value in the function is assigned with the arguement.
        this.messagePayload = messagePayload;
        this.messageType = messageType;
        messageLength = messagePayload.length + 1;
    }
    //'choke'. 'unchoke', 'interested' and 'not interested' messages have no payload.
    void datapart(int part)    {
        //adding the payload as the format of int, so need to change it to byte first.
        byte[] message;
        message = extraFunctions.int2Byte(part);
        messagePayload = message;
        messageLength = 1 + messagePayload.length;
    }

    void datapart(byte[] messagePayload)    {

        this.messagePayload = messagePayload;
        messageLength = messagePayload.length + 1;
    }


    byte[] encMsg()
    {
        //this is (payload+messageType) + 4
        byte[] message = new byte[messageLength + 4];
        //change the int message length to 4 byte.
        // 1byte = 8 bit
        message[4] = (byte)messageType;
        for(int k = 0; k < 4; k++) message[k] = (byte) (messageLength >> 24 - k*8);

        int j = 0;
        if (messagePayload == null) {
            return message;
        } else {
            int i = 5;
            while (i < messageLength + 4) {
                message[i] = messagePayload[j++];
                i++;
            }
            return message;
        }
    }

}
