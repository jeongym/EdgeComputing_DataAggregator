package kr.re.keti;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class SlaveWorker implements Runnable // extends Thread // implements Runnable
{

	SlaveWorker(ArrayList<String> ip_list, String fname)
	{
		this.stop = false;
		this.slaveList = new ArrayList<String>();
		this.fileList = new ArrayList<String>();
		this.slaveList = (ArrayList<String>) ip_list.clone();
		this.origin_foldername = fname;
		this.foldername = fname;
	}
	SlaveWorker(String ip, String fname)
	{
		this.stop = false;
		this.slaveList = new ArrayList<String>();
		this.fileList = new ArrayList<String>();
		this.master_ip = ip;
		this.origin_foldername = fname;
		this.foldername = fname;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		Scanner sc = new Scanner(System.in);
		DataProcess dataprocess = new  DataProcess(foldername);
		int check=-1, ip_number=0, i;
		
//		System.out.println(manage.file_list[0]); // ex. [1.txt, 2.txt]
//		System.out.println(manage.file_list[1]);
		while(!stop)
		{
			if(Thread.interrupted())
				break;
			
			System.out.print("filename(declare an end = end )\t(ex) 2.txt ?\t");
			String filename = sc.nextLine();
			if(filename.equals("end"))
				break;
			System.out.println("function : 1. Information     2. Where     3. Length     4. Read");
//			System.out.println("function : 1. fileExist     2. fileCreate     3. fileRemove     4. fileWrite     5. fileRead     6. fileWhere     7. fileLength");
//			System.out.println("function : 8. fileOpen\t 9. fileClose");
			System.out.print("function number\t(ex) 1 ?\t");
			int func = sc.nextInt();
			sc.nextLine();
			while(func<1 || func>9)
			{
				System.out.print("function is wrong.\nfunction number\t(ex) 1 ?\t");
				func = sc.nextInt();
				sc.nextLine();
			}
			
			ip_number = IPList(master_ip);
			System.out.println("slaves ip list : " + slaveList);
			
			fileList.clear();
//			System.out.println("master ip list : " + master_ip);
			for(i=0; i<slaveList.size(); i++)
			{
				System.out.println("request to " + slaveList.get(i));
				check = dataprocess.fileExist(filename, slaveList.get(i));
				if(check == 1)
					fileList.add(slaveList.get(i));
			}
			System.out.println("request to " + master_ip);
			check = dataprocess.fileExist(filename, master_ip);
			if(check == 1)
				fileList.add(master_ip);
			
			//after fileExsit, fileList using -> work

			if(func == 1) // fileExsit always execute.
			{
				if(fileList.size() == 0)
					System.out.println("Anyone doesn't had DATA.");
				else if(dataprocess.fileInfo(filename) != null)
		          System.out.println("* [" + fileList + "] : had DATA" + dataprocess.fileInfo(filename));
				else
					System.out.println("* [" + fileList + "] : had DATA but Metadata DB doesn't had");
			}
			else if(func == 2)
			{
				check = dataprocess.fileWhere(foldername+filename); 
//				System.out.println(check);
				if(check == -1)
					check = dataprocess.fileWhere(filename, slaveList) + 1;
				
				if(check == -1)
					System.out.println("* Anyone doesn't had DATA.");
//				else if(check == -2)
//					System.out.println("\tfile cannot search.");
				else
					System.out.printf("* DATA is in %dth place. (0 is local, 1~ are connected nodes)\n", check);
				
			}
			else if(func == 3)
			{
				check = (int)dataprocess.fileLength(foldername+filename);
				if(check == -1)
				{
					check = (int)dataprocess.fileLength(filename, slaveList); 
					if(check == -1)
						System.out.println("* Anyone doesn't had DATA and couldn't confirm the length of DATA.");
//					else if(check == -2)
//						System.out.println("\tfile length measure is false.");
					else
						System.out.println("* The length of DATA is [" + check + "] Bytes");
				}
				else
					System.out.println("* The length of DATA is [" + check + "] Bytes");
					
			}
			else if(func == 4)
			{
				// FileExist() above
				if (fileList.size() == 0)
					System.out.println("Anyone doesn't had and couldn't read DATA.");
					
				// link or data = mysql check 
				// link : how to ??
				check  = dataprocess.fileRead(filename, fileList);
				if(check == 1)
					System.out.println("[" + fileList + "] : had and could read DATA.");
				else
					System.out.println("[" + fileList + "] : had and couldn't read DATA.");
			}
		}
		
	}

	public int IPList(String ipAddress)
	{
		int i=0;
		try {
			Socket socket = new Socket(ipAddress, 8888);
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();
			
			String message = "list";
			os.write( message.getBytes() ); // file list
			os.flush();
			
			if(slaveList.size() > 0)
				slaveList.clear();
			
//			slaveList.add(master_ip);
			String resultFromServer = "";
			byte[] data = new byte[100];
			while (true)
			{
				int len = is.read(data);
				if(len < 0)
					break;
				resultFromServer = new String(data,0,len);
				String[] read_text = resultFromServer.split("\n");
				for(i=0; i<read_text.length; i++)
				{
//					System.out.println("\t" + read_text[i]);
					slaveList.add(read_text[i]);
				}
			}
			socket.close();				
			while(!socket.isClosed()) {			}// socket close wait - success
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return i;	
	}
	public void threadStop(boolean stop)
	{
		this.stop = stop;
	}
	public void slaveAdd(ArrayList<String> slist)
	{
		slaveList = (ArrayList<String>) slist.clone();
	}

	private boolean stop = false;
	public static ArrayList<String> slaveList=null;
	public static ArrayList<String> fileList=null;
	public static String master_ip=null;
	public String my_hashcode=null;
	public static String origin_foldername = "/home/eunae/keti/";
	public static String foldername = "/home/eunae/keti/";
}
