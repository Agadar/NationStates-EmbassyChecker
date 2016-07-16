package com.github.agadar.testproject;

import com.github.agadar.nsapi.NSAPI;
import com.github.agadar.nsapi.domain.region.Region;
import com.github.agadar.nsapi.enums.shard.RegionShard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Agadar <https://github.com/Agadar/>
 */
public class Main
{
    private final static String REGION = "the western isles";
    private final static String USER_AGENT = "Agadar's script for checking RMB activity of embassy regions";
    private final static int DAYS = 30;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        // Set User Agent.
        NSAPI.setUserAgent(USER_AGENT);
        
        // Retrieve all embassy region names of The Western Isles.
        Region r = NSAPI.region(REGION).shards(RegionShard.Embassies).execute();
        final List<String> embassyRegions = new ArrayList<>(r.Embassies.size());
        r.Embassies.forEach(embassy -> embassyRegions.add(embassy.RegionName));
        System.out.println("Checking " + embassyRegions.size() + " embassy regions...");
        
        // List that will contain regions that haven't had RMB posts in a month.
        final List<RegionLastMsg> regionLastMsgs = new ArrayList<>();
        
        // The threshold in days to seconds.
        final long threshold = TimeUnit.DAYS.toSeconds(DAYS);
        
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
            
            // Null/empty check on retrieved messages.
            if (r.RegionalMessages == null || r.RegionalMessages.isEmpty())
            {
                regionLastMsgs.add(new RegionLastMsg(region));
                continue;
            }
            
            // Check whether the time between now and when the last posted RMB
            // message is more than the threshold. If so, add to regionLastMsgs.
            final long msgTimeStamp = r.RegionalMessages.get(r.RegionalMessages
                    .size() - 1).Timestamp;
            final long diff = now - msgTimeStamp;
            
            if (diff >= threshold)
            {
                regionLastMsgs.add(new RegionLastMsg(region, diff));
            }
        }
        
        // Now sort the list and print it out.
        System.out.println();
        System.out.println("------ RESULTS ------");
        Collections.sort(regionLastMsgs);
        regionLastMsgs.forEach(regionLastMsg -> System.out.println(regionLastMsg));        
    }
}
