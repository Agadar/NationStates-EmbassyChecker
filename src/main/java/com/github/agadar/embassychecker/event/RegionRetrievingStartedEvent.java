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
    
    /**
     * Constructs a new event.
     * 
     * @param source object that fired this event
     * @param regionsToRetrieve total number of regions to retrieve
     */
    public RegionRetrievingStartedEvent(Object source, int regionsToRetrieve)
    {
        super(source);
        this.RegionsToRetrieve = regionsToRetrieve;
    }
}
