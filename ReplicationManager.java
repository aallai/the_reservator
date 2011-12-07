import java.io.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


public class ReplicationManager{ 
	public static void main(String[] args){
		Thread t = new Thread(new HeartbeatThread());
		t.start();
		try{
			long l = 0;

			while (l++ < 10000000) {
				Thread.sleep(1000); // must give the deamon thread a chance to run!
			} 
		}catch(InterruptedException ei){ // 250 mills might not be enough!
			System.err.println(ei);
		}
		
		System.out.println("Leaving main");
		System.exit(0);
	}
}

class HeartbeatThread implements Runnable{
	private int heartBeatCount = 0;
	private int nothingCount = 0;

	public HeartbeatThread(){
	}

	public void run(){ 
		boolean isActive = true;
		
		//create memoroy map for heartbeat file
		
	    // Create a read-write memory-mapped file
		File file = new File("heartbeat.txt");
	    	File outputFile = new File("progression.txt");
		FileChannel rwChannel;
	    ByteBuffer wrBuf;

		BufferedWriter bw;	    
      
	    int nothingCount = 0;
	    int beatCount = 0;
		try 
		{
			rwChannel = new RandomAccessFile(file, "rw").getChannel();
			System.out.println("Size: " + (int)rwChannel.size());
			wrBuf = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, 10);
			
			while(nothingCount < 20){
				byte readByte =wrBuf.get(0);
				wrBuf.clear();

				if (readByte == (byte)0x0) {
					wrBuf.put(0, (byte)0x1);
					beatCount++;
				} else {
					nothingCount++;
				}
					
				wrBuf.clear();
				
			    	try {
					bw = new BufferedWriter(new FileWriter("resultsReplicaManager.txt", true));
				        bw.write("mmap: " + readByte);
				        bw.newLine();

					if (nothingCount > 4){
						bw.write("Restart Daemon");
						bw.newLine();
					}

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

				/*
				i = (1 + rate); 
				futureval = principal * (float)Math.pow(i,year); 
				System.out.print(principal + " compounded after " + year + " years @ " + rate + "% = " + futureval);
				System.out.println();  */
			}

		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.exit(0);
	}

}
