package listeners;

import bastel.de.fahrtenschreiber.pojo.TripEntry;

@FunctionalInterface
public interface TripEntryUpdatedListener{
    void tripEntryUpdated(TripEntry latestTripEntry);

}
