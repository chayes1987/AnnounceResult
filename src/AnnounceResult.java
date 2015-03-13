import com.firebase.client.Firebase;
import org.jeromq.ZMQ;
import org.jeromq.ZMQ.*;
import java.io.FileInputStream;
import java.util.Properties;

/*
    @author Conor Hayes
    The official documentation was consulted for the third party library 0mq used in this class
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
            ar.subscribeToHeartbeat();
            ar.subscribe();
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

    private void subscribe(){
        Socket subscriber = _context.socket(ZMQ.SUB);
        subscriber.connect(_config.getProperty("SUB_ADR"));
        String topic = _config.getProperty("TOPIC");
        subscriber.subscribe(topic.getBytes());
        System.out.println("SUB: " + topic);
        _publisher.bind(_config.getProperty("ACK_ADR"));

        while(true){
            String auctionOverEvt = new String(subscriber.recv());
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

    private void subscribeToHeartbeat(){
        new Thread(
                () -> {
                    Socket subscriber = _context.socket(ZMQ.SUB);
                    subscriber.connect(_config.getProperty("HEARTBEAT_ADR"));
                    String topic = _config.getProperty("CHECK_HEARTBEAT_TOPIC");
                    subscriber.subscribe(topic.getBytes());

                    while(true){
                        String checkHeartbeatEvt = new String(subscriber.recv());
                        System.out.println("REC: " + checkHeartbeatEvt);
                        String message = _config.getProperty("CHECK_HEARTBEAT_TOPIC_RESPONSE") +
                                " <params>" + _config.getProperty("SERVICE_NAME") + "</params>";
                        _publisher.send(message.getBytes());
                        System.out.println("PUB: " + message);
                    }
                }
        ).start();
    }
}
