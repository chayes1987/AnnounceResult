package database;

import utils.Constants;

/**
 * @author Conor Hayes
 * Database Facade
 */
public class DatabaseFacade {
    private static IRealtimeDatabase realtime_database;

    /**
     * Initializes the realtime database
     * @return The database to be used
     */
    public static IRealtimeDatabase getRealtimeDatabase(){
        if (Constants.REALTIME_DATABASE == DATABASE_TYPE.Firebase){
            realtime_database = new Firebase();
        }
        // Other Databases may be added
        return realtime_database;
    }
}
