import java.net.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.io.*;


//Uses the UTF-8 encoding for characters
public class PrimaryStation {	
	
	
    public static void main(String[] args) throws IOException {
        
        //
        // sockets and other variables declaration
        //
        // maximum number of clients connected: 10
        //
               
        ServerSocket serverSocket = null;
        Socket[] client_sockets;
        client_sockets = new Socket[10];
        PrintWriter[] s_out;
        s_out = new PrintWriter[10];
        BufferedReader[] s_in;
        s_in = new BufferedReader[10];
        
        int[] ns; // send sequence number
        ns = new int[10];
        
        int[] nr; // receive sequence number
        nr = new int[10];
        
        String inputLine = null;
        String outputLine = null;
        
        //
        //get port number from the command line
        //
        int nPort = 4444; // default port number
        //nPort = Integer.parseInt(args[0]);        
        
        String flag = "01111110";
        String fcs = "00000000";
        String[] address;
        address = new String[10];
        int[] clientID;
        clientID = new int[10];
        
        String control = null;
        String information = "";
        
        boolean bListening = true;
        
        String[] sMessages; // frame buffer
        sMessages = new String[20];
        int nMsg = 0;        
                
        boolean bAlive 	= false;		
        String responseControl = null; // control field of the input
        //
        // initialize some var's for array handling
        //
        int s_count = 0;
        int i = 0;       
        
        //
        // create server socket
        //
        try {
            serverSocket = new ServerSocket(nPort);
            
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + args[0]);
            System.exit(-1);
        }
        
        //
        // this variable defines how many clients are connected
        //
        int nClient = 0;
                
        //
        // set timeout on the socket so the program does not
        // hang up
        //
        serverSocket.setSoTimeout(1000);
             
        //
        // main server loop
        //
        while (bListening){
        	
        	try {        		
        		//
        		// trying to listen to the socket to accept
        		// clients
        		// if there is nobody to connect, exception will be
        		// thrown - set by setSoTimeout()
        		//
        		client_sockets[s_count]=serverSocket.accept();
        		
        		//
        		// connection got accepted
        		//
        		
        		if (client_sockets[s_count]!=null){
        		
        			System.out.println("Connection from " + client_sockets[s_count].getInetAddress() + " accepted.");
        			        			
        			System.out.println("accepted client");
        			s_out[s_count] = new PrintWriter(client_sockets[s_count].getOutputStream(),true);
        			s_in[s_count] = new BufferedReader(new InputStreamReader(client_sockets[s_count].getInputStream()));
					
        			clientID[s_count] = s_count+1;		
        			
					address[s_count] = "00000000"+Integer.toBinaryString(clientID[s_count]);
					int len = address[s_count].length();					
					address[s_count] = address[s_count].substring(len-8);				
					        			
        			System.out.println("client address: " + address[s_count]);
        			
        			// send client address to the new client
        			s_out[s_count].println(address[s_count]);
        			
        			//
                	// initialization
                	// 
        			
        			// ===========================================================
        			// insert codes here to send SNRM message
        			//
        			control = "11001001";
        			
        			s_out[s_count].println(flag + address[s_count] + control + fcs + flag);
        			System.out.println("Sent SNRM to station " + clientID[s_count]);            		
            		// ===============================================================
        			
            		// recv UA message
//        			System.out.println("before");
            		inputLine = s_in[s_count].readLine();
//            		System.out.println("after: " + inputLine);
            		responseControl = inputLine.substring(16, 24);
            		
            		if(responseControl.equals("11000110") || responseControl.equals("11001110")) {
            			System.out.println("Received UA from station " + clientID[s_count]);
            		}
            		else {
            			System.out.println("UA error -- station " + clientID[s_count]);
            		}       
        			
            		// initialize ns and nr
            		ns[s_count] = -1;
            		nr[s_count] = 0;
            		
            		//            		 
        			// increment count of clients
        			//
        			s_count++;
        			nClient = s_count;
        			bAlive = true;
        		}
        	}
        	catch (InterruptedIOException e) {}
		
        	for (i=0;i<s_count;i++) {

        		// ==============================================================
        		// insert codes here to send â€œRR,*,Pâ€� msg
    			
        		control = "10001" + Integer.toBinaryString(nr[i] | 0x8).substring(1);
        		s_out[i].println(flag + address[i] + control + fcs + flag);
        		System.out.println("Sent < RR,*,P > to station " + clientID[i] + "control:" + control);
        		// ==============================================================
        		
        		
        		// recv response from the client
        		inputLine = s_in[i].readLine();
        		boolean finalMessage = false;
        		
        		if(inputLine != null) {		
        		
        			// get control field of the response frame
        			responseControl = inputLine.substring(16, 24);
        		
        			if(responseControl.substring(0,4).equals("1000")) {
        				// recv â€œRR,*,Fâ€�, no data to send from B
        				System.out.println("Receive RR, *, F from station " + clientID[i]);
        			}
        			else if(responseControl.substring(0, 1).equals("0")) {
                		// ==============================================================
        				// insert codes here to handle the frame â€œI, *, *â€� received
        				
        				//how many frames have been received
        				int counter = 1;
        				
        				do
        				{
        					System.out.println("responseControl:" + responseControl);
    						finalMessage = responseControl.substring(4,5).equals("1"); 
        					
        					//if the frame is to the primary station; consume it
        					//The address of the primary station is "00000000"
        					if(inputLine.substring(8,16).equals("00000000"))
        					{
        						String data = inputLine.substring(24, inputLine.indexOf(fcs + flag));
        						System.out.println("Received " + data + " from client " + i+1);
        						System.out.println("Decoded Message:\t" + getStringFromBinary(data));
        						nr[i] = Integer.parseInt(responseControl.substring(1,4), 2) + 1;
        					}
        					//if the frame is to the secondary station; buffer the frame to send
        					else
        					{
        						String tempAddress = inputLine.substring(8,16);
        						System.out.println("Message to be sent to address " + tempAddress);
        						
    							sMessages[nMsg] = inputLine;    							
    							nMsg++;
    							System.out.println("Number of bufferred messages :" + nMsg);
        					}
        				
        	        		// continue to get messages from the client if not final message
        					if(!finalMessage)
        					{
        						System.out.println("Counter value:"+counter);
        						
        						//We know that the secondary won't be able to send anymore without an ack
        						if(counter == 3)
        						{
        							break;
        						}        						
        						inputLine = s_in[i].readLine();
        						System.out.println("inputLine:\t" + inputLine);
        						responseControl = inputLine.substring(16, 24);
        						counter++;
        					}
        					else
        						break;
        	        		
                		// ==============================================================
        				}while(inputLine != null);
        			}
        		}
        	}
        	
    		// ==============================================================
        	// insert codes here to send frames in the buffer       	
        	        		
        	// send I frame
        	for(int j = 0; j < nMsg; j++)
        	{        		
        		int clientId = Integer.parseInt(sMessages[j].substring(8, 16),2)-1;
        		System.out.println("Sending buffered message to client" + clientId);
        		s_out[clientId].println(sMessages[j]);
        	}
        	
        	nMsg = 0;        	
    		// ==============================================================
			
		//
		// stop server automatically when
		// all clients disconnect
		//
		// no active clients
		//
			if (!bAlive && s_count > 0){
				System.out.println("All clients are disconnected - stopping");
				bListening = false;
			}
			
		}// end of while loop
		
		//
		// close all sockets
		//
		
		for (i=0;i<s_count;i++){
			client_sockets[i].close();
		}
        
        serverSocket.close();
        
    }// end main 
    
    private static String getStringFromBinary(String binaryStr)
    {
    	//make a copy
    	String bStr = binaryStr + "";
    	String message = "";
    	
    	while(bStr != null)
    	{	    		    		
    		if(bStr.length() <= 7)
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
    
}// end of class PrimaryStation
