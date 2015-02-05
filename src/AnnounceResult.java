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
    }
}
