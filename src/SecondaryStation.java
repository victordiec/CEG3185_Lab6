import java.net.*;
import java.io.*;


//Uses the ASCII encoding, 
//Maximum length of the information field is 64 bytes
//Since each ASCII character is represented by 7 bits, 9 characters can be placed
//into the information field

public class SecondaryStation {
	
	static final String FLAG 	= "01111110";
	static final String FCS 	= "00000000";
	
	public static void main(String[] args) {
//		 declaration section:
//		 os: output stream
//		 is: input stream
		Socket clientSocket = null;
		PrintStream os = null;
		DataInputStream is = null;
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		
		String id = null;
		String information = null;
		String control = null;		
		String address = null;  
		String response = null; // control field of the socket input
		
		int ns = -1; // send sequence number
		int nr = 0; //receive sequence number
		
		//
		String answer = null; // input using keyboard
		
		//String to be sent
		String sendStr = null;
       		
//		 Initialization section:
//		 Try to open a socket on port 4444
//		 Try to open input and output streams
		try {
			clientSocket = new Socket("127.0.0.1", 4444);
			os = new PrintStream(clientSocket.getOutputStream());
			is = new DataInputStream(clientSocket.getInputStream());
		} 
		catch (UnknownHostException e) {
			System.err.println("Don't know about host: hostname");
		} 
		catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to: hostname");
		}
				
		if (clientSocket != null && os != null && is != null) {			
			
			try {				
				
				//responseLine is the message received
				String responseLine;				
				responseLine = is.readLine();
				
				//receive client address from the primary station
				//
				id = responseLine;
				System.out.println("client address: " + id);
				
				
				responseLine = is.readLine();
				response = responseLine.substring(16, 24);

				// recv SNRM msg
				// fifth bit should always be a polling bit
				if(response.equals("11000001") || response.equals("11001001")) {
					//===========================================================
					// insert codes here to send the UA msg					
					
					sendStr = "";
					
					//address stays the same in response
					address = id;;
					
					//control for UA is 1100F110
					//Final bit should always be 1 since it is a response to a poll
					
//					if(response.substring(4,5).equals("1"))
//					{
						control = "11001110";
//					}
//					else
//					{
//						control = "11000110";
//					}
					
					//Fill with FCS with 0s
					
					//Last Step
					sendStr = FLAG + address + control + FCS + FLAG;
					os.println(sendStr);
					
					//===========================================================
					System.out.println("sent UA msg");
				}
				
				// main loop; recv and send data msgs
				while (true) {
					responseLine = is.readLine();
					response = responseLine.substring(16, 24);
					
					System.out.println("recv msg -- control " + response);				
					
					// recv ??RR,*,P?? msg
					if(response.substring(0,5).equals("10001")) {
						
						// enter data msg using keyboard 
						System.out.println("Is there any message to send? (y/n)");
						answer = in.readLine();						
						
						if(answer.toLowerCase().equals("y") || answer.toLowerCase().equals("yes")) {
							System.out.println("Please enter the destination address using 8-bits binary string (e.g. 00000001):");
							address = in.readLine();
							
							System.out.println("Please enter the message to send?");
							answer = in.readLine();
							
							//===========================================================
							// insert codes here to send an I msg;
							
							String infoString = answer;
							String infoBytes = "";
							int finalBit = 0;
							do
							{
								if(infoString.length() <= 9)
								{
									finalBit = 1;
									infoBytes = convertToBinary(infoString) + "";
									
								}
								else
								{
									infoBytes = convertToBinary(infoString.substring(0, 10)) + "";
									infoString = infoString.substring(0);
								}
								
								ns = ns+1 % 8;
								control = "0" + Integer.toBinaryString(ns | 0x8).substring(1) + "" + finalBit + "" + Integer.toBinaryString(nr | 0x8).substring(1);
								sendStr = FLAG + address + control + infoBytes + FCS + FLAG;
								System.out.println("Address:\t" + address + "\nControl:\t" + control + "\nInfoBytes:\t" + infoBytes);
								System.out.println("Sending " + sendStr);
								os.println(sendStr);
							
							}
							while(finalBit != 1);
							//===========================================================
						
						}				
						else {
							//===========================================================
							// insert codes here to send ??RR,*,F??
							control = "10001" + Integer.toBinaryString(nr | 0x8);
							address = id;
							sendStr = FLAG + address + control + FCS + FLAG;
							System.out.println("Address:\t" + address + "\nControl:\t" + control);
							os.print(sendStr);
							//===========================================================
						}
					}
					
					// recv an I frame
					if(response.substring(0,1).equals("0")) {
						String data = responseLine.substring(24, responseLine.length()-8);
						System.out.println("");
						System.out.println("Received data: " + data);						
						
						nr = Integer.parseInt(response.substring(1,4), 2) + 1;
						System.out.println("nr: " + nr);
					}
				}
			} 
			catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
			} 
			catch (IOException e) {
				System.err.println("IOException: " + e);
			}	
		}
	}
	
	private static String convertToBinary(String original)
	{
		
		byte[] bytes = original.getBytes();
	    StringBuilder binary = new StringBuilder();
		for (byte b : bytes)
		{
			int val = b;
			for (int i = 0; i < 8; i++)
		 	{
		    	binary.append((val & 128) == 0 ? 0 : 1);
		        val <<= 1;
		    }
//		    binary.append(' ');
		}
		return binary.toString();
	}
	

}// end of class SecondaryStation
