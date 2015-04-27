package database;

/**
 * @author Conor Hayes
 * Realtime Database Interface
 */
public interface IRealtimeDatabase {

    /**
     * End Auction
     * @param id The auction ID
     * @param winner The winner
     * @param url The url
     */
    void endAuction(String id, String winner, String url);
}
