package com.github.agadar.embassychecker.event;

import java.util.EventObject;

/**
 * Event that is fired when the retrieving of all embassy regions has started.
 *
 * @author Agadar
 */
public class RegionRetrievingStartedEvent extends EventObject
{
    /** The total number of regions to retrieve. */
    public final int RegionsToRetrieve;
    
    /** Estimate in seconds of how long it will take to retrieve all the regions. */
    public final long TimeUntilDone;
    
    /**
     * Constructs a new event.
     * 
     * @param source object that fired this event
     * @param regionsToRetrieve total number of regions to retrieve
     * @param timeUntilDone estimate in seconds of how long it will take to retrieve all the regions
     */
    public RegionRetrievingStartedEvent(Object source, int regionsToRetrieve, long timeUntilDone)
    {
        super(source);
        this.RegionsToRetrieve = regionsToRetrieve;
        this.TimeUntilDone = timeUntilDone;
    }
}
