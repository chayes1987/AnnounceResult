import com.firebase.client.Firebase;
import org.jeromq.ZMQ;
import org.jeromq.ZMQ.*;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * @author Conor Hayes
 */

/*
    The official documentation was consulted for the third party libraries 0mq & Firebase used in this class
    0mq pub -> https://github.com/zeromq/jeromq/blob/master/src/test/java/guide/pathopub.java
    0mq sub -> https://github.com/zeromq/jeromq/blob/master/src/test/java/guide/pathosub.java
    Firebase -> https://www.firebase.com/docs/java-api/javadoc/com/firebase/client/Firebase.html
    Config -> http://www.mkyong.com/java/java-properties-file-examples/
    Coding Standards -> http://www.oracle.com/technetwork/java/codeconvtoc-136057.html
 */

/**
 * This class is responsible for announcing the result of an auction
 */
public class AnnounceResult {
    private Context _context = ZMQ.context();
    private Socket _publisher = _context.socket(ZMQ.PUB);
    private static Properties _config;

    /**
     * Main Method
     * @param args Command line args
     */
    public static void main(String[] args){
        AnnounceResult ar = new AnnounceResult();
        _config = ar.readConfig();

        // Check the configuration
        if(_config != null)
            ar.subToCheckHeartbeatEvt();
            ar.subToAuctionOverEvt();
    }

    /**
     * Read the configuration file
     * @return The contents of the file in a properties object, null if exception
     */
    private Properties readConfig() {
        Properties config = new Properties();
        try {
            config.load(new FileInputStream("properties/config.properties"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return config;
    }

    /**
     * Subscribes to the Auction Over event
     */
    private void subToAuctionOverEvt(){
        Socket auctionOverSub = _context.socket(ZMQ.SUB);
        // Connect and subscribe to the topic - AuctionOver
        auctionOverSub.connect(_config.getProperty("SUB_ADR"));
        String auctionOverTopic = _config.getProperty("TOPIC");
        auctionOverSub.subscribe(auctionOverTopic.getBytes());
        System.out.println("SUB: " + auctionOverTopic);
        // Bind the publisher for acknowledgements
        _publisher.bind(_config.getProperty("ACK_ADR"));

        while(true){
            String auctionOverEvt = new String(auctionOverSub.recv());
            System.out.println("REC: " + auctionOverEvt);
            publishAcknowledgement(auctionOverEvt);
            // Get the item details and write them to Firebase
            String id = parseMessage(auctionOverEvt, "<id>", "</id>");
            String winner = parseMessage(auctionOverEvt, "<params>", "</params>");
            endAuction(id, winner);
        }
    }

    /**
     * Ends the auction
     * @param id The ID of the auction
     * @param winner The winner of the auction
     */
    private void endAuction(String id, String winner){
        // Create the reference to Firebase
        Firebase fb = new Firebase(_config.getProperty("FIREBASE_URL") + id);
        // Update the status and winner fields in Firebase
        try{
            fb.child("status").setValue("Complete");
            fb.child("winner").setValue(winner);
            System.out.println("Auction #" + id + " Complete");
            // Allow time for the write to execute (Issue in Java)
            Thread.sleep(2000);
        }catch(Exception e) {
            System.out.println(e.toString());
        }
    }

    /**
     * Publishes the acknowledgement of the NotifyBidders command
     * @param auctionOverEvt The received message
     */
    private void publishAcknowledgement(String auctionOverEvt){
        String acknowledgment = "ACK " + auctionOverEvt;
        _publisher.send(acknowledgment.getBytes());
        System.out.println("PUB: " + acknowledgment);
    }

    /**
     * Parses message received
     * @param message The received message
     * @param startTag The starting delimiter
     * @param endTag The ending delimiter
     * @return The required string
     */
    private String parseMessage(String message, String startTag, String endTag){
        int startIndex = message.indexOf(startTag) + startTag.length();
        String substring = message.substring(startIndex);
        return substring.substring(0, substring.lastIndexOf(endTag));
    }

    /**
     * Subscribes to the CheckHeartbeat command and publishes an acknowledgement
     */
    private void subToCheckHeartbeatEvt(){
        new Thread(
                () -> {
                    Socket heartbeatSub = _context.socket(ZMQ.SUB);
                    // Connect and subscribe to the topic - CheckHeartbeat
                    heartbeatSub.connect(_config.getProperty("HEARTBEAT_ADR"));
                    String heartbeatTopic = _config.getProperty("CHECK_HEARTBEAT_TOPIC");
                    heartbeatSub.subscribe(heartbeatTopic.getBytes());

                    while(true){
                        String checkHeartbeatEvt = new String(heartbeatSub.recv());
                        System.out.println("REC: " + checkHeartbeatEvt);
                        // Build and send the response
                        String heartbeatResponse = _config.getProperty("CHECK_HEARTBEAT_TOPIC_RESPONSE") +
                                " <params>" + _config.getProperty("SERVICE_NAME") + "</params>";
                        _publisher.send(heartbeatResponse.getBytes());
                        System.out.println("PUB: " + heartbeatResponse);
                    }
                }
        ).start();
    }
}
