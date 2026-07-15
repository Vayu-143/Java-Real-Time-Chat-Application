Compile:
javac -d out src/common/ChatLogger.java src/server/ChatServer.java src/server/ClientHandler.java src/client/ChatClient.java src/gui/ChatClientGUI.java

Run Server:
java -cp out server.ChatServer

Run Console Client:
java -cp out client.ChatClient

Run GUI Client:
java -cp out gui.ChatClientGUI