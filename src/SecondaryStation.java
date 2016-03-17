import java.net.*;
import java.io.*;


//Uses the ASCII encoding, 
//Maximum length of the information field is 64 bytes
//Since each ASCII character is represented by 7 bits, 9 characters can be placed
//into the information field

//Window Size of 4

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
		String responseControl = null; // control field of the socket input
		
		int ns = 0; 	// send sequence number
						// will now also represent the next frame to be sent
		
		int nr = 0;  	//receive sequence number
						//last sequence number sent by another station to this station
		
		int ack = 0;
		
		//
		String answer = null; // input using keyboard
		
		//String to be sent
		String sendStr = null;
       	
		//Circular Array for the use of storing messages before they are sent
		String buffer[] = new String[14];
		
		//Next free frame in buffer to add information for sliding window
		int nextFreeFrame = 0;
		
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
				responseControl = responseLine.substring(16, 24);

				// recv SNRM msg
				// fifth bit should always be a polling bit
				if(responseControl.equals("11000001") || responseControl.equals("11001001")) {
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
					responseControl = responseLine.substring(16, 24);
					
					System.out.println("recv msg -- control " + responseControl);				
					
					// recv ??RR,*,P?? msg
					if(responseControl.substring(0,5).equals("10001")) {
						
						ack += Integer.parseInt(responseControl.substring(5),2);
						
						// enter data msg using keyboard 
						System.out.println("Is there any message to send? (y/n)");
						answer = in.readLine();						
						
						//Ask the user if there is more information to send, if the buffer is not full
						//ns == -1 lets it work at the beginning
						if((answer.toLowerCase().equals("y") || answer.toLowerCase().equals("yes")) && (nextFreeFrame + 1 != ack)) 
						{														
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
								//Each information field can hold a maximum of 73 characters
								if(infoString.length() <= 73)
								{
									finalBit 	= 1;
									infoBytes 	= convertToBinary(infoString) + "";
									
								}
								else
								{
									infoBytes = convertToBinary(infoString.substring(0, 74)) + "";
									infoString = infoString.substring(74);
								}
								
//								ns = ns + 1 % buffer.length;
								control = "0" + Integer.toBinaryString(nextFreeFrame %8 | 0x8).substring(1) + "" + finalBit + "" + Integer.toBinaryString(nr%8 | 0x8).substring(1);
								sendStr = FLAG + address + control + infoBytes + FCS + FLAG;
								System.out.println("Address:\t" + address + "\nControl:\t" + control + "\nInfoBytes:\t" + infoBytes);
								System.out.println("Adding to buffer:\t" + sendStr);

								//If the next free frame after the one that is about to be filled is already full
								if(nextFreeFrame+1 % buffer.length == nr)
								{
									throw new Exception("Overloaded buffer");
								}
								else
								{
									buffer[nextFreeFrame] = sendStr;
									nextFreeFrame = nextFreeFrame + 1 % buffer.length;
								}
							}
							while(finalBit != 1);
							//===========================================================
						
						}
						
						System.out.println("ns:" + ns + "ack:" + ack + ",nextFreeFrame:" + nextFreeFrame);
						if(ns < ack + 3 && nextFreeFrame != ns)
						{
							//While the end of the sliding window has yet to be reached or there is no more data to send
							do
							{								
								System.out.println("Sending: frame" + ns + "\t" + buffer[ns]);
								os.println(buffer[ns]);
								ns = ns + 1 % buffer.length;
							}
							while(ns < ack + 3 && ns != nextFreeFrame);
							System.out.println("Finished sending frames");
						}
						else {
							//===========================================================
							// insert codes here to send ??RR,*,F??
							control = "10001" + Integer.toBinaryString(nr % 8| 0x8).substring(1);
							address = id;
							sendStr = FLAG + address + control + FCS + FLAG;
							System.out.println("Sending RR F\nAddress:\t" + address + "\nControl:\t" + control);
							os.println(sendStr);
							//===========================================================
						}
					}
					
					// recv an I frame
					if(responseControl.substring(0,1).equals("0")) {
						String data = responseLine.substring(24, responseLine.indexOf(FCS + FLAG));
						System.out.println("");
						System.out.println("Received data: " + data);
						System.out.println("Decoded:" + getStringFromBinary(data));
						
						nr = Integer.parseInt(responseControl.substring(1,4), 2) + 1;
						System.out.println("nr: " + nr);
						
						//They're waiting for this frame to send
						System.out.println("ACK: " + ack + "," + responseControl.substring(5) + " " + Integer.parseInt(responseControl.substring(5), 2));
						ack += Integer.parseInt(responseControl.substring(5), 2);
					}
				}
			} 
			catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
				e.printStackTrace();
			} 
			catch (IOException e) {
				System.err.println("IOException: " + e);
				e.printStackTrace();
			}
			catch(Exception e)
			{
				System.err.println("Exception: " + e);
				e.printStackTrace();
			}
		}
	}
	
	private static String convertToBinary(String original)
	{
		System.out.println("String to be converted to binary:\t" + original);
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
		return binary.toString().substring(1);
	}
	
    private static String getStringFromBinary(String binaryStr)
    {
    	//make a copy
    	String bStr = binaryStr + "";
    	String message = "";
    	
    	while(bStr != null)
    	{	    		    		
    		if(bStr.length() == 7)
    		{
    			System.out.println("decoded:" + Integer.parseInt(bStr, 2));
    			message += (char)Integer.parseInt(bStr, 2) + "";
    			break;
    		}
    		else
    		{
    			System.out.println("decoded:" + Integer.parseInt(bStr.substring(0,7), 2) + "\t" + bStr.substring(0,7));
    			message += (char)Integer.parseInt(bStr.substring(0,7), 2) + "";
    			bStr = bStr.substring(8);
    		}
    	}
    	
    	return message;
    }

}// end of class SecondaryStation
