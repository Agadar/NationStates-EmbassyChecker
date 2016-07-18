package com.github.agadar.embassychecker.event;

import java.util.EventObject;

/**
 * Event that is fired when a region has been retrieved.
 *
 * @author Agadar
 */
public class RegionRetrievedEvent extends EventObject
{
    /** Name of the region that was retrieved. */
    public final String RegionName;
    
    /** 
     * Position of the retrieved region in the query. Starts at 0 and increments
     * by 1 for each region retrieved.
     */ 
    public final int PositionInQuery;
    
    /** Estimate in seconds of how long it will take to retrieve the remaining regions. */
    public final long TimeUntilDone;
    
    /** Boolean indicating whether the region was retrieved successfully. */
    public final boolean Retrieved;
    
    /**
     * Constructs a new event.
     * 
     * @param source object that fired this event
     * @param regionName name of the region that was retrieved
     * @param positionInQuery position of the retrieved region in the query
     * @param timeUntilDone estimate in seconds of how long it will take to retrieve the remaining regions
     * @param retrieved whether the region was retrieved successfully
     */
    public RegionRetrievedEvent(Object source, String regionName, int positionInQuery,
            long timeUntilDone, boolean retrieved)
    {
        super(source);
        this.RegionName = regionName;
        this.PositionInQuery = positionInQuery;
        this.TimeUntilDone = timeUntilDone;
        this.Retrieved = retrieved;
    }

}
