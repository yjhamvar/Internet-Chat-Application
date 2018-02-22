README FILE FOR JAVA CHAT APPLICATION

1. Server.java needs to be compiled using the command: javac Server.java
2. Client.java needs to be compiled using the command: javac Client.java
3. To start the server, use the following command in a new terminal: java Server
4. To start the client, use the following command in a new terminal: java Client
5. Repeat step 4 for every new client that you want to start. Every new client must be created in a new terminal

###Commands to send text messages###

1. Broadcast Message: Once connected to the server, just type any text in the client terminal to send the messages to everyone who is connected to the server

2. Unicast Message: @<name of recipient> <message>
	example: @Client1 Hello
	This will send the message only to Client1
	
3. Blockcast Message: !@<name of client to be blocked> <message>
	example: !@Client2 Hi
	This will send the message to everyone EXCEPT Client2
	

###Commands to send Files (Commands are case sensitive)###

Note: Path of file must be the complete path of the file and backward slashes "\" must be replaced with forward slashes "/" in the path

1. Broadcast File: #File broadcast <complete path of file>
	example: #File broadcast d:/temp.pptx
	This will send the file to every client connected to the server
	
2. Unicast File: #File unicast @<name of recipient> <complete path of file>
	example: #File unicast @Client1 d:/test.txt
	This will send the file only to Client1
	
3. Blockcast File: #File blockcast @<name of client to be blocked> <complete path of file>
	example: #File blockcast @Client2 d:/test.txt
	This will send the file to everyone EXCEPT Client2