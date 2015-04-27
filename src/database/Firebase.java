package database;

/*
    The official documentation was consulted for the third party library Firebase used in this class
    Firebase -> https://www.firebase.com/docs/java-api/javadoc/com/firebase/client/Firebase.html
 */

/**
 * @author Conor Hayes
 * Firebase
 */
public class Firebase implements IRealtimeDatabase {

    /**
     * Ends the auction
     * @param id The ID of the auction
     * @param winner The winner of the auction
     * @Param url The Firebase URL
     */
    @Override
    public void endAuction(String id, String winner, String url){
        // Create the reference to Firebase
        com.firebase.client.Firebase fb = new com.firebase.client.Firebase(url + id);
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
}
