import java.io.File;
//Support read and write at any position of the file
//Is very good to read and write with offset since we are operating the file in chunks.
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.stream.IntStream;


class Segmentation {

    private int chunkSize;
    private int fileSize;
    EventLogger eventlogger;
    private RandomAccessFile randomAccessFile;
    private BitSet bitField;
    int[] requestedField;
    //The following value is the size of bitField
    private int bitMapSize;
    int cnt = 0;//This count how many bits of file the peer has.

    Segmentation(int peerID, String fileName, int exist, int fileSize, int chunkSize) throws IOException {
        //fpath is  the path of the file
        String fpath = "/cise/homes/cl2/Desktop/project/peer_" + peerID + "/" + fileName;
        this.fileSize = fileSize;
        this.chunkSize = chunkSize;
        this.eventlogger = new EventLogger(peerID);
        bitMapSize = Math.round(fileSize/chunkSize);;
        File parentDir = new File("/cise/homes/cl2/Desktop/project/peer_"+ peerID +"/");
        //IF the parentDir doesn`t exist, we should make one..
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        randomAccessFile = new RandomAccessFile(fpath, "rw");
        bitField = new BitSet(bitMapSize);
        //Use the int array to match the specific position of the bitfield. This may be done
        //with two bitFields..
        requestedField = new int[bitMapSize];


        //The following switch checks whether the file exist or not. It exist, then  we
        //can initialize the bitField.
        switch (exist) {
            case 1:
                bitField.set(0, bitMapSize);
                cnt = bitMapSize;
                break;
            default:
                bitField.set(0, bitMapSize, false);
                cnt = 0;
                break;
        }

        for (int i=0; i < bitMapSize; i++) {
    			requestedField[i] = 0;
    		}
    }
    //Returns a new byte array containing all the bits in this bit set.
    byte[] bitFiled_to_Byte() {
        return bitField.toByteArray();
    }

    boolean checkBitField() {
//Returns whether all elements of this stream match the provided predicate
        return IntStream.range(0, bitMapSize).allMatch(i -> bitField.get(i));
    }

    void wfile(byte[] content, int indexOfChunk) throws IOException {
      //Using the type long
  		//If the complete file exist, cnt will keep increasing to bitMapSize
  		//otherwise, increase by one after writing one chunk.
        randomAccessFile.seek((long) indexOfChunk * chunkSize);
        randomAccessFile.write(content);
        cnt++;
    }

    //encapsulation of the data packet...
    byte[] rFile(int indexOfChunk) throws IOException {
        //every bit in the bitField means one chunk..
        //check whether the offset is the last part which is smaller than one chunk.
        int offset;
        offset = indexOfChunk == bitMapSize - 1 ? fileSize - indexOfChunk * chunkSize : chunkSize;
        //content of the piece.
        byte[] payload = new byte[offset + 4];
        byte[] chunkWanted = new byte[offset];
        //all of the index are stored as byte in the message.
        byte[] indexByte = extraFunctions.int2Byte(indexOfChunk);
        long checkpoint = (long) indexOfChunk*chunkSize;
        randomAccessFile.seek(checkpoint);
        randomAccessFile.read(chunkWanted);//Decides how much we read from the checkpoint.
        //src, srcPos, dest, destPos, length
    		//The first four bytes are for message length
    		//copy the byte_index and byte_data to the payload separately.
        System.arraycopy(indexByte, 0, payload, 0, 4);
        System.arraycopy(chunkWanted, 0, payload, 4, payload.length - 4);
        return payload;
    }


    boolean getBitField(int i) {
        return bitField.get(i);
    }

    void setBitField(int i, int selection) {
      if (selection == 1)bitField.set(i, true);
  		else bitField.set(i,false);
    }

}
