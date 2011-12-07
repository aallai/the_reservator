import java.io.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


public class Daemonator{ 
	public static void main(String[] args){
	    // Create a read-write memory-mapped file
		File file = new File("heartbeat.txt");
	    FileChannel rwChannel;
	    ByteBuffer wrBuf;

	byte aByte;	    
	    int nothingCount = 0;
		try {
			try {
                                        BufferedWriter  bw = new BufferedWriter(new FileWriter("results.txt", true));
                                        bw.write("Starting Daemonator as sister process to Replication Manager");
                                        bw.newLine();
                                        bw.flush();
                                        bw.close();
			} catch (Exception e) {
			}
			rwChannel = new RandomAccessFile(file, "rw").getChannel();
			System.out.println("Size: " + (int)rwChannel.size());
			wrBuf = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, 10);
			
			while(true){
			        byte readByte =wrBuf.get(0);
                                wrBuf.clear();

                                if (readByte == (byte)0x1) {
                                        wrBuf.put(0, (byte)0x0);
					nothingCount++;
                                } else {
                                        nothingCount = 0;
                                }

                                wrBuf.clear();

                                try {
                                        BufferedWriter bw = new BufferedWriter(new FileWriter("resultsDaemonator.txt", true));
                                        bw.write("mmap: " + readByte + " nothingCount: " + nothingCount);
                                        bw.newLine();
                                        bw.flush();
                                        bw.close();
                                } catch(Exception e) {
                                        System.out.println(e);
                                }
                                try {
                                        Thread.sleep(10000);
                                } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                }
	
				wrBuf.clear();
				try {

					if (nothingCount == 20) {
						BufferedWriter  bw = new BufferedWriter(new FileWriter("resultsDaemonator.txt", true));
                                        	bw.write("Daemonator Timeout - No response from ReplicationManager response from Replication Manager.");
                                        	bw.newLine();
                                        	bw.flush();
                                        	bw.close();
						Runtime.getRuntime().exec(" ./goReplicationManager.script repMan ubuntu:kwilly_test_middle0 ubuntu:kwilly_test_middle1 ubuntu:kwilly_test_middle2");

						nothingCount = 0;
					}

					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
}

