package listeners;

import bastel.de.fahrtenschreiber.pojo.TripEntry;

@FunctionalInterface
public interface EventuallyGetLatestTripCallback{
    void lastTripRecieved(TripEntry tripEntry);
}
