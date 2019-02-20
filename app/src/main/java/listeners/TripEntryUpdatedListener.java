package listeners;

import bastel.de.fahrtenschreiber.pojo.TripEntry;

public interface TripEntryUpdatedListener {
    void tripEntryUpdated(TripEntry latestTripEntry);

}
