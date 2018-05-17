class extraFunctions
{
    static void  packPrint(int v1, int v2, int v3, String v4, int v5, int v6) {
      System.out.println("NumberOfPreferredNeighbours " + v1);
      System.out.println("UnckokingInterval " + v2);
      System.out.println("OptimisticUnchokingInterval " + v3);
      System.out.println("FileName " + v4);
      System.out.println("FileSize " + v5);
      System.out.println("PieceSize " + v6);
    }

    static int byte2Int(byte[] byteArray)
    {
        int val=0, i;
        i=0;
        while (byteArray.length > i) {
            int shift= (3 - i) << 3;
            val = val + ((byteArray[i] & 0x000000FF) << shift);
            i++;
        }
        return val;
    }
    //changing the indexOfchunk to the byte.
    static byte[] int2Byte(int number)
    {
        byte[] result;
        result = new byte[4];
        result[0] = (byte) (number / 16777216);
        result[1] = (byte) (number / 65536);
        result[2] = (byte) (number / 256);
        result[3] = (byte) number;
        return result;
    }

    //decapsulate the data and message type from whole message
    static messagePack byte2Msg(byte[] temp)
    {
        int messageType,i;
        byte[] payload=new byte[temp.length-5];
        messageType=(int)temp[4];
        i=5;
        while (i < temp.length) {
            payload[i-5]=temp[i];
            i++;
        }
        return(new messagePack(messageType, payload));
    }

}
