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
    private final static boolean checkRmbActivity = true;
    
    /** 
     * The maximum number of days since the last message on a region's 
     * message board before that region is considered inactive.
     */
    private final static int maxDaysSinceLastRmbMsg = 30;
    
    /** Whether or not to check the age of each region. */
    private final static boolean checkRegionFounded = true;
    
    /**
     * The minimum number of days since a region may have been founded.
     */
    private final static int minDaysSinceFounded = 90;
    
    /** The user agent for this program. */
    private final static String USER_AGENT = "Agadar's script for checking RMB activity of embassy regions";
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        // Ensure at least one check is being done.
        if (!checkRmbActivity && !checkRegionFounded)
        {
            throw new IllegalArgumentException("No checks enabled!");
        }
        
        // Set User Agent.
        NSAPI.setUserAgent(USER_AGENT);
        
        // Check what shards to retrieve, according to what checks we want to do.
        final List<RegionShard> shards = new ArrayList<>();
        
        if (checkRmbActivity)
        {
            shards.add(RegionShard.Embassies);
        }
        
        if (checkRegionFounded)
        {
            shards.add(RegionShard.History);
        }
        
        // Retrieve all relevant data of the specified region.
        Region r = NSAPI.region(Region).shards(shards.toArray(
                new RegionShard[shards.size()])).execute();
        
        // Null-check on the region.
        if (r == null)
        {
            throw new IllegalArgumentException("Region does not exist!");
        }
        
        // Extract all region names from valid embassies.
        final List<String> embassyRegions = new ArrayList<>();
        r.Embassies.forEach(embassy -> 
        {
            if (embassy.Status == null)
            {
                embassyRegions.add(embassy.RegionName);
            }
        });
        System.out.println("Checking " + embassyRegions.size() + " embassy regions...");
        
        // List that will contain regions that haven't had RMB posts in a month.
        final List<RegionLastMsg> regionLastMsgs = new ArrayList<>();
        
        // maxDaysSinceLastRmbMsg converted to seconds.
        final long maxMsSinceLastRmbMsg = TimeUnit.DAYS.toSeconds(maxDaysSinceLastRmbMsg);
        
        // Timestamp in seconds of now.
        final long now = System.currentTimeMillis() / 1000;
        
        // Iterate over retrieved region names.
        for (final String region : embassyRegions)
        {
            // Null check to make sure the region didn't CTE in the meantime.
            if ((r = NSAPI.region(region).shards(RegionShard.RegionalMessages).execute()) == null)
            {
                continue;
            }
            
            if (checkRmbActivity)
            {           
                // Null/empty check on retrieved messages.
                if (r.RegionalMessages == null || r.RegionalMessages.isEmpty())
                {
                    regionLastMsgs.add(new RegionLastMsg(region));
                    continue;
                }

                // Check whether the time between now and when the last posted RMB
                // message is more than the maxMsSinceLastRmbMsg. If so, add to regionLastMsgs.
                final long msgTimeStamp = r.RegionalMessages.get(r.RegionalMessages
                        .size() - 1).Timestamp;
                final long diff = now - msgTimeStamp;

                if (diff >= maxMsSinceLastRmbMsg)
                {
                    regionLastMsgs.add(new RegionLastMsg(region, diff));
                }
            }
        }
        
        // Now sort the list and print it out.
        System.out.println();
        System.out.println("------ RESULTS ------");
        Collections.sort(regionLastMsgs);
        regionLastMsgs.forEach(regionLastMsg -> System.out.println(regionLastMsg));        
    }
    
    /**
     * Checks the RMB activity of the given region.
     * 
     * @param region the region of which the RMB activity to check
     */
    private static void checkRmbActivity(Region r)
    {
        
    }
}
