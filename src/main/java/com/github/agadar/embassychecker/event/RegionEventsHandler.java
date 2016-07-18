package com.github.agadar.embassychecker.event;

/**
 * Interface for handlers of region events.
 * 
 * @author marti
 */
public interface RegionEventsHandler
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
