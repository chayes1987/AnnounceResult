import com.firebase.client.Firebase;
import org.jeromq.ZMQ;
import org.jeromq.ZMQ.*;

/*
    @author Conor Hayes
 */

public class AnnounceResult {
    private Context context = ZMQ.context();
    private Socket ackPublisher = context.socket(ZMQ.PUB);

    public static void main(String[] args) {
        new AnnounceResult().subscribe();
    }

    private void subscribe(){
        Socket subscriber = context.socket(ZMQ.SUB);
        subscriber.connect(Constants.SUB_ADR);
        subscriber.subscribe(Constants.AUCTION_OVER_TOPIC.getBytes());
        System.out.println("SUB: " + Constants.AUCTION_OVER_TOPIC);
        ackPublisher.bind(Constants.ACK_ADR);

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
        Firebase fb = new Firebase(Constants.FIREBASE_URL + "/" + id);
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
        String auctionOverAck = "ACK: " + auctionOverEvt;
        ackPublisher.send(auctionOverAck.getBytes());
        System.out.println("ACK SENT...");
    }

    private String parseMessage(String message, String startTag, String endTag){
        int startIndex = message.indexOf(startTag) + startTag.length();
        String substring = message.substring(startIndex);
        return substring.substring(0, substring.lastIndexOf(endTag));
    }
}
