package com.github.agadar.embassychecker.event;

/**
 * Interface for listeners to region events.
 * 
 * @author marti
 */
public interface RegionEventsListener
{
    /**
     * Handler for when the regions retrieval process has begun.
     * 
     * @param event 
     */
    void handleRetrievingStarted(RegionRetrievingStartedEvent event);
    
    /**
     * Handler for when a region was retrieved.
     * 
     * @param event 
     */
    void handleRegionRetrieved(RegionRetrievedEvent event);
}
