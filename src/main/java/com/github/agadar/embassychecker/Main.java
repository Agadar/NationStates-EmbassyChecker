package com.github.agadar.embassychecker;

import com.github.agadar.nsapi.NSAPI;
import com.github.agadar.nsapi.domain.region.Region;
import com.github.agadar.nsapi.enums.shard.RegionShard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Main class for this program.
 * 
 * @author Agadar <https://github.com/Agadar/>
 */
public class Main
{
    /** Name of the region whose embassy regions to check. */
    private final static String Region = "the western isles";
    
    /** Whether or not to check the RMB activity of each region. */
    private final static boolean CheckRmbActivity = true;
    
    /** 
     * The maximum number of days since the last message on a region's 
     * message board before that region is considered inactive.
     */
    private final static int MaxDaysSinceLastRmbMsg = 30;
    
    /** Whether or not to check the age of each region. */
    private final static boolean CheckRegionFounded = false;
    
    /**
     * The minimum number of days since a region may have been founded.
     */
    private final static int MinDaysSinceFounded = 90;
    
    /** The user agent for this program. */
    private final static String UserAgent = "Agadar's script for checking embassy regions";
    
    // Timestamp in seconds of now.
    private final static long Now = System.currentTimeMillis() / 1000;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        // Ensure at least one check is being done.
        if (!CheckRmbActivity && !CheckRegionFounded)
        {
            throw new IllegalArgumentException("No checks enabled!");
        }
        
        // Set User Agent.
        NSAPI.setUserAgent(UserAgent);
        
        // Check what shards to retrieve from each embassy region, according to 
        // what checks we want to do.
        final List<RegionShard> shardsToRetrieveLst = new ArrayList<>();
        shardsToRetrieveLst.add(RegionShard.Name);
        
        if (CheckRmbActivity)
        {
            shardsToRetrieveLst.add(RegionShard.RegionalMessages);
        }
        
        if (CheckRegionFounded)
        {
            shardsToRetrieveLst.add(RegionShard.History);
        }
        
        final RegionShard[] shardsToRetrieve = shardsToRetrieveLst.toArray(
                new RegionShard[shardsToRetrieveLst.size()]);
        
        // Retrieve embassies of the specified region.
        Region r = NSAPI.region(Region).shards(RegionShard.Embassies).execute();
        
        // Null-check on the region.
        if (r == null)
        {
            throw new IllegalArgumentException("Region does not exist!");
        }
        
        // Extract all region names from embassies that aren't closing, rejected,
        // or denied, because we don't care about those.
        final List<String> embassyRegions = new ArrayList<>();
        r.Embassies.forEach(embassy -> 
        {
            if (embassy.Status == null || !(embassy.Status.equals("closing") || 
                embassy.Status.equals("rejected") || embassy.Status.equals("denied")))
            {
                embassyRegions.add(embassy.RegionName);
            }
        });
        System.out.println("Checking " + embassyRegions.size() + " regions...");
        
        // The retrieved regions.
        final List<Region> regions = new ArrayList<>();
        
        // Iterate over retrieved region names, retrieving the regions.
        //for (final String region : embassyRegions)
        for (int i = 0; i < 40; i++)
        {
            String region = embassyRegions.get(i);
            // Null check to make sure the region didn't CTE in the meantime.
            if ((r = NSAPI.region(region).shards(shardsToRetrieve).execute()) != null)
            {
                // Add the region to the list.
                regions.add(r);
            }
        }
        
        // Do RMB activity check.
        if (CheckRmbActivity)
        {           
            checkRmbActivity(regions);
        }
        
        // Do region founded check.
        if (CheckRegionFounded)
        {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Checks the RMB activity of the given regions
     * 
     * @param region the regions of which the RMB activity to check
     */
    private static void checkRmbActivity(List<Region> regions)
    {
         // List that will contain regions that haven't had RMB posts in x days.
        final List<RegionLastMsg> regionLastMsgs = new ArrayList<>();
        
        // MaxDaysSinceLastRmbMsg converted to seconds.
        final long maxMsSinceLastRmbMsg = TimeUnit.DAYS.toSeconds(MaxDaysSinceLastRmbMsg);
        
        // Iterate over the regions, doing the check.
        for (Region region : regions)
        {       
            // Null/empty check on retrieved messages.
            if (region.RegionalMessages == null || region.RegionalMessages.isEmpty())
            {
                regionLastMsgs.add(new RegionLastMsg(Region));
                continue;
            }

            // Check whether the time between now and when the last posted RMB
            // message is more than the maxMsSinceLastRmbMsg. If so, add to regionLastMsgs.
            final long msgTimeStamp = region.RegionalMessages.get(region.RegionalMessages
                    .size() - 1).Timestamp;
            final long diff = Now - msgTimeStamp;

            if (diff >= maxMsSinceLastRmbMsg)
            {
                regionLastMsgs.add(new RegionLastMsg(region.Name, diff));
            }
        }
        
        // Now sort the list and print it out.
        System.out.println();
        System.out.println("-------Regions in which no RMB message is posted for " 
                           + MaxDaysSinceLastRmbMsg + " or more days-------");
        Collections.sort(regionLastMsgs);
        regionLastMsgs.forEach(regionLastMsg -> System.out.println(regionLastMsg));
    }
}
