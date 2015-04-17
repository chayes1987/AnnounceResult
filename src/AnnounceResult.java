import com.firebase.client.Firebase;
import org.jeromq.ZMQ;
import org.jeromq.ZMQ.*;
import java.io.FileInputStream;
import java.util.Properties;

/*
    @author Conor Hayes
    The official documentation was consulted for the third party libraries 0mq & Firebase used in this class
    0mq pub -> https://github.com/zeromq/jeromq/blob/master/src/test/java/guide/pathopub.java
    0mq sub -> https://github.com/zeromq/jeromq/blob/master/src/test/java/guide/pathosub.java
    Firebase -> https://www.firebase.com/docs/java-api/javadoc/com/firebase/client/Firebase.html
    Config -> http://www.mkyong.com/java/java-properties-file-examples/
 */

public class AnnounceResult {
    private Context _context = ZMQ.context();
    private Socket _publisher = _context.socket(ZMQ.PUB);
    private static Properties _config;

    public static void main(String[] args){
        AnnounceResult ar = new AnnounceResult();
        _config = ar.readConfig();

        if(_config != null)
            ar.subToCheckHeartbeatEvt();
            ar.subToAuctionOverEvt();
    }

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

    private void subToAuctionOverEvt(){
        Socket auctionOverSub = _context.socket(ZMQ.SUB);
        auctionOverSub.connect(_config.getProperty("SUB_ADR"));
        String auctionOverTopic = _config.getProperty("TOPIC");
        auctionOverSub.subscribe(auctionOverTopic.getBytes());
        System.out.println("SUB: " + auctionOverTopic);
        _publisher.bind(_config.getProperty("ACK_ADR"));

        while(true){
            String auctionOverEvt = new String(auctionOverSub.recv());
            System.out.println("REC: " + auctionOverEvt);
            publishAcknowledgement(auctionOverEvt);
            String id = parseMessage(auctionOverEvt, "<id>", "</id>");
            String winner = parseMessage(auctionOverEvt, "<params>", "</params>");
            endAuction(id, winner);
        }
    }

    private void endAuction(String id, String winner){
        Firebase fb = new Firebase(_config.getProperty("FIREBASE_URL") + id);
        try{
            fb.child("status").setValue("Complete");
            fb.child("winner").setValue(winner);
            System.out.println("Auction #" + id + " Complete");
            Thread.sleep(2000);
        }catch(Exception e) {
            System.out.println(e.toString());
        }
    }

    private void publishAcknowledgement(String auctionOverEvt){
        String acknowledgment = "ACK " + auctionOverEvt;
        _publisher.send(acknowledgment.getBytes());
        System.out.println("PUB: " + acknowledgment);
    }

    private String parseMessage(String message, String startTag, String endTag){
        int startIndex = message.indexOf(startTag) + startTag.length();
        String substring = message.substring(startIndex);
        return substring.substring(0, substring.lastIndexOf(endTag));
    }

    private void subToCheckHeartbeatEvt(){
        new Thread(
                () -> {
                    Socket heartbeatSub = _context.socket(ZMQ.SUB);
                    heartbeatSub.connect(_config.getProperty("HEARTBEAT_ADR"));
                    String heartbeatTopic = _config.getProperty("CHECK_HEARTBEAT_TOPIC");
                    heartbeatSub.subscribe(heartbeatTopic.getBytes());

                    while(true){
                        String checkHeartbeatEvt = new String(heartbeatSub.recv());
                        System.out.println("REC: " + checkHeartbeatEvt);
                        String heartbeatResponse = _config.getProperty("CHECK_HEARTBEAT_TOPIC_RESPONSE") +
                                " <params>" + _config.getProperty("SERVICE_NAME") + "</params>";
                        _publisher.send(heartbeatResponse.getBytes());
                        System.out.println("PUB: " + heartbeatResponse);
                    }
                }
        ).start();
    }
}
