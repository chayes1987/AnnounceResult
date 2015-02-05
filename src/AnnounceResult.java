import org.jeromq.ZMQ;
import org.jeromq.ZMQ.*;

public class AnnounceResult {
    private Context context = ZMQ.context();
    private Socket ackPublisher = context.socket(ZMQ.PUB);

    public static void main(String[] args) {

    }
}
