package broker;

import database.DatabaseFacade;
import database.IRealtimeDatabase;
import org.jeromq.ZMQ;
import utils.MessageParser;
import java.util.Properties;

/*
    The official documentation was consulted for the third party library 0mq used in this class
    0mq pub -> https://github.com/zeromq/jeromq/blob/master/src/test/java/guide/pathopub.java
    0mq sub -> https://github.com/zeromq/jeromq/blob/master/src/test/java/guide/pathosub.java
 */

/**
 * @author Conor Hayes
 * 0mq Broker
 */
public class ZeroMqBroker implements IBroker {
    private ZMQ.Context _context = ZMQ.context();
    private ZMQ.Socket _publisher = _context.socket(ZMQ.PUB);

    /**
     * Subscribes to the CheckHeartbeat command and publishes an acknowledgement
     * @param config The configuration file
     */
    @Override
    public void subscribeToHeartbeat(Properties config) {
        new Thread(
                () -> {
                    ZMQ.Socket heartbeatSub = _context.socket(ZMQ.SUB);
                    // Connect and subscribe to the topic - CheckHeartbeat
                    heartbeatSub.connect(config.getProperty("HEARTBEAT_ADR"));
                    String heartbeatTopic = config.getProperty("CHECK_HEARTBEAT_TOPIC");
                    heartbeatSub.subscribe(heartbeatTopic.getBytes());

                    while(true){
                        String checkHeartbeatEvt = new String(heartbeatSub.recv());
                        System.out.println("REC: " + checkHeartbeatEvt);
                        // Build and send the response
                        String heartbeatResponse = config.getProperty("CHECK_HEARTBEAT_TOPIC_RESPONSE") +
                                " <params>" + config.getProperty("SERVICE_NAME") + "</params>";
                        _publisher.send(heartbeatResponse.getBytes());
                        System.out.println("PUB: " + heartbeatResponse);
                    }
                }
        ).start();
    }

    /**
     * Subscribes to the Auction Over event
     */
    @Override
    public void subscribeToAuctionOverEvt(Properties config){
        ZMQ.Socket auctionOverSub = _context.socket(ZMQ.SUB);
        // Connect and subscribe to the topic - AuctionOver
        auctionOverSub.connect(config.getProperty("SUB_ADR"));
        String auctionOverTopic = config.getProperty("TOPIC");
        auctionOverSub.subscribe(auctionOverTopic.getBytes());
        System.out.println("SUB: " + auctionOverTopic);
        // Bind the publisher for acknowledgements
        _publisher.bind(config.getProperty("ACK_ADR"));

        while(true){
            String auctionOverEvt = new String(auctionOverSub.recv());
            System.out.println("REC: " + auctionOverEvt);
            publishAcknowledgement(auctionOverEvt);
            // Get the item details and write them to the database
            String id = MessageParser.parseMessage(auctionOverEvt, "<id>", "</id>");
            String winner = MessageParser.parseMessage(auctionOverEvt, "<params>", "</params>");
            IRealtimeDatabase database = DatabaseFacade.getRealtimeDatabase();
            String url = config.getProperty("FIREBASE_URL");
            database.endAuction(id, winner, url);
        }
    }

    /**
     * Publishes the acknowledgement of the NotifyBidders command
     * @param message The received message
     */
    @Override
    public void publishAcknowledgement(String message) {
        String acknowledgment = "ACK " + message;
        _publisher.send(acknowledgment.getBytes());
        System.out.println("PUB: " + acknowledgment);
    }
}
