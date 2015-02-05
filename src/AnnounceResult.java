import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import org.jeromq.ZMQ;
import org.jeromq.ZMQ.*;

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
        }
    }

    private void publishAcknowledgement(String auctionOverEvt){
        String auctionOverAck = "ACK: " + auctionOverEvt;
        ackPublisher.send(auctionOverAck.getBytes());
        System.out.println("ACK SENT...");
    }
}
