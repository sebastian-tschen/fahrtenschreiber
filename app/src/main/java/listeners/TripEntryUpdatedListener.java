package listeners;

import android.app.Activity;

import bastel.de.fahrtenschreiber.pojo.TripEntry;

@FunctionalInterface
public interface TripEntryUpdatedListener{
    void tripEntryUpdated(TripEntry latestTripEntry);

}
