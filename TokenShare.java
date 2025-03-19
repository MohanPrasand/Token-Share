import java.util.*;
import java.io.*;
import java.net.*;


class FileMap{
    static HashMap<String, String> files = new HashMap<>();
    static void add(String filename){
        try{
            File f = new File(filename);
            if(!f.exists()){
                System.out.println("File not found: "+filename);
                return;
            }
            String fname = "";
            for(char i: filename.toCharArray()){
                if(i=='/' || i=='\\')
                    fname = "";
                else
                    fname += i;
            }
            files.put(fname, filename);
            System.out.println("\nFile Added: "+filename);
            String tkn = Token.encode(InetAddress.getLocalHost().getHostAddress(),fname);
            System.out.println("Token: "+tkn+"\n");
        }
        catch(Exception e){
            System.out.println("File Not Found: "+filename);
        }
        
    }

    static String get(String filename){
        return files.getOrDefault(filename, null);
    }
}

class Sender extends Thread{
    ServerSocket server;
    Sender(){
        try{
            server = new ServerSocket(2502);
        }
        catch(Exception e){
            System.out.println("Server Already running");
        }
    }

    public void run(){
        while(true){
            try{
                Socket client = server.accept();
                InputStream clientr = client.getInputStream();
                OutputStream clientw = client.getOutputStream();
                byte fname[] = new byte[1024];
                clientr.read(fname);
                String filename = new String(fname).trim();
                if(FileMap.get(filename) == null){
                    clientw.write(0);
                    clientw.flush();
                    client.close();
                    continue;
                }
                clientw.write(1);
                clientw.flush();
                FileInputStream file = new FileInputStream(FileMap.get(filename));
                byte[] buffer = new byte[1024];
                int readb;
                while((readb = file.read(buffer)) != -1){
                    clientw.write(buffer, 0, readb);
                }
                file.close();
                client.close();
            }
            catch(Exception e){
                System.out.println("Server Stopped");
                break;
            }
        }
    }
}

class Receiver extends Thread{
    String ipaddr;
    String filename;
    Receiver(String ipaddr, String filename){
        this.ipaddr = ipaddr;
        this.filename = filename;
    }

    public void run(){
        try{
            Socket server = new Socket(this.ipaddr, 2502);
            InputStream serverr = server.getInputStream();
            OutputStream serverw = server.getOutputStream();
            byte[] status = new byte[1];
            
            serverw.write((filename+"\n").getBytes());
            serverw.flush();
            serverr.read(status);
            
            if(status[0] == 0){
                System.out.println("\nCould not receive: "+filename+"\n");
                server.close();
                return;
            }
            FileOutputStream filew = new FileOutputStream(new File(this.filename));
            byte[] buffer = new byte[1024];
            int data;
            System.out.println("Receiving...");
            while((data=serverr.read(buffer)) != -1){
                filew.write(buffer, 0, data);
            }

            System.out.println("\nFile Received: "+ filename+"\n");
            server.close();
            filew.close();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Failed Receiving: " + this.filename);
        }
    }
}

class Token{
    static String encode(String ip, String filename){
        String token = ip+'*'+filename;
        byte[] tokenByte = token.getBytes();
        StringBuilder hexToken = new StringBuilder();
        for(byte b: tokenByte){
            hexToken.append(String.format("%02x", b));
        }
        return hexToken.toString();
    }

    static String[] decode(String hexToken){
        String[] ret = {"",""};
        String token = "";
        for(int i=0; i<hexToken.length(); i+=2){
            String t = hexToken.substring(i, i+2);
            token += Character.toString((char)Integer.parseInt(t, 16));
        }
        int ind = 0;
        for(char i: token.toCharArray()){
            if(i=='*')
                ind++;
            else{
                ret[ind] = ret[ind] + i;
            }
        }
        return ret;
    }
}

class TokenShare{
    public static void main(String[] args){
        try{
            Scanner scanner = new Scanner(System.in);
            Sender sender = new Sender();
            System.out.println("\t|-----------------|");
            System.out.println("    ----|   Token Share   |----");
            System.out.println("\t|-----------------|");
            while(true){
                System.out.print("1. Send file\n2. Receive file\n3. Exit\nEnter your choice: ");
                int ch = scanner.nextInt();
                scanner.nextLine();
                if(ch==1){
                    System.out.print("Enter filepath: ");
                    String filepath = scanner.nextLine();
                    FileMap.add(filepath);
                    if(!sender.isAlive())
                        sender.start();
                }
                else if(ch==2){
                    System.out.print("Enter token: ");
                    String token = scanner.nextLine();

                    String[] rec = Token.decode(token);
                    
                    Receiver receiver = new Receiver(rec[0], rec[1]);
                    System.out.println("Receiving: "+rec[1]);
                    receiver.start();
                    receiver.join();
                }
                else
                    break;
            }
            System.out.println("Bye");
            scanner.close();
        }
        catch(Exception e){
            System.err.println(e);
        }
    }
}