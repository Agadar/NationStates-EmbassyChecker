package com.github.agadar.embassychecker;

import com.github.agadar.nsapi.NSAPI;
import com.github.agadar.nsapi.domain.region.Region;
import com.github.agadar.nsapi.domain.shared.Happening;
import com.github.agadar.nsapi.enums.shard.RegionShard;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final static String MainRegionName = "the western isles";
    
    /** Whether or not to check the RMB activity of each region. */
    private final static boolean CheckRmbActivity = true;
    
    /** 
     * The maximum number of days since the last message on a region's 
     * message board before that region is considered inactive.
     */
    private final static int MaxDaysSinceLastRmbMsg = 30;
    
    /** Whether or not to check the age of each region. */
    private final static boolean CheckRegionFounded = true;
    
    /**
     * The minimum number of days since a region may have been founded.
     */
    private final static int MinDaysSinceFounded = 90;
    
    /** Whether or not to check the region's tags. */
    private final static boolean CheckRegionTags = true;
    
    /** The tags to check and warn for. */
    private final static String[] TagsToCheck = new String[] { "Raider", "Mercenary" };
    
    /** The user agent for this program. */
    private final static String UserAgent = "Agadar's Embassy Checker (https://github.com/Agadar/NationStates-EmbassyChecker)";
    
    // Timestamp in seconds of now.
    private final static long Now = System.currentTimeMillis() / 1000;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        // Ensure at least one check is being done.
        if (!CheckRmbActivity && !CheckRegionFounded && !CheckRegionTags)
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
        
        if (CheckRegionTags)
        {
            shardsToRetrieveLst.add(RegionShard.Tags);
        }
        
        final RegionShard[] shardsToRetrieve = shardsToRetrieveLst.toArray(
                new RegionShard[shardsToRetrieveLst.size()]);
        
        // Retrieve embassies of the specified region.
        Region MainRegion = NSAPI.region(MainRegionName).shards(RegionShard.Embassies).execute();
        
        // Null-check on the region.
        if (MainRegion == null)
        {
            throw new IllegalArgumentException("Region does not exist!");
        }
        
        // Extract all region names from embassies that aren't closing, rejected,
        // or denied, because we don't care about those.
        final List<String> embassyRegions = new ArrayList<>();
        MainRegion.Embassies.forEach(embassy -> 
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
        for (final String region : embassyRegions)
        {
            // Null check to make sure the region didn't CTE in the meantime.
            if ((MainRegion = NSAPI.region(region).shards(shardsToRetrieve).execute()) != null)
            {
                // Add the region to the list.
                regions.add(MainRegion);
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
            checkRegionFounded(regions);
        }
        
        // Do tags check.
        if (CheckRegionTags)
        {
            checkRegionTags(regions);
        }
    }
    
    /**
     * Checks the RMB activity of the given regions, and prints results.
     * 
     * @param region the regions of which the RMB activity to check
     */
    private static void checkRmbActivity(List<Region> regions)
    {
        // List that will contain regions that haven't had RMB posts in x days.
        final List<RegionLastMsg> regionLastMsgs = new ArrayList<>();
        
        // Iterate over the regions, doing the check.
        for (Region region : regions)
        {       
            // Null/empty check on retrieved messages.
            if (region.RegionalMessages == null || region.RegionalMessages.isEmpty())
            {
                regionLastMsgs.add(new RegionLastMsg(region.Name));
                continue;
            }

            // Check whether the time between now and when the last posted RMB
            // message is more than the maxMsSinceLastRmbMsg. If so, add to regionLastMsgs.
            final long msgTimeStamp = region.RegionalMessages.get(region.RegionalMessages
                    .size() - 1).Timestamp;
            final long diff = Now - msgTimeStamp;

            if (diff >= TimeUnit.DAYS.toSeconds(MaxDaysSinceLastRmbMsg))
            {
                regionLastMsgs.add(new RegionLastMsg(region.Name, diff));
            }
        }
        
        // Now sort the list and print it out.
        Collections.sort(regionLastMsgs);
        System.out.println();
        System.out.println("-------Regions in which no RMB messages were "
                           + "posted during the last " + MaxDaysSinceLastRmbMsg
                           + " days-------");
        System.out.println("Total regions found: " + regionLastMsgs.size());
        regionLastMsgs.forEach(regionLastMsg -> System.out.println(regionLastMsg));
    }
    
    /**
     * Checks the founding dates of the given regions, and prints results.
     * 
     * @param regions the regions of which the founding dates to check
     */
    private static void checkRegionFounded(List<Region> regions)
    {
        // List that will contain regions that haven't had RMB posts in x days.
        final List<RegionFounded> regionFoundeds = new ArrayList<>();

        // Iterate over the regions, doing the check.
        for (Region region : regions)
        {       
            // Null/empty check on retrieved messages.
            if (region.History == null || region.History.isEmpty())
            {
                continue;
            }
            
            // Check if the very last item is the 'founded' message. If not, continue.
            final Happening lastHapp = region.History.get(region.History.size() - 1);
            
            if (!lastHapp.Description.contains("Region founded by "))
            {
                continue;
            }

            // Check whether the time between now and when the region was founded
            // is less than the minMsSinceFounded. If so, add to regionFoundeds.
            final long foundedTimeStamp = lastHapp.Timestamp;
            final long diff = Now - foundedTimeStamp;

            if (diff < TimeUnit.DAYS.toSeconds(MinDaysSinceFounded))
            {
                regionFoundeds.add(new RegionFounded(region.Name, diff));
            }
        }
        
        // Now sort the list and print it out.
        Collections.sort(regionFoundeds);
        System.out.println();
        System.out.println("-------Regions that were founded less than "
                           + MinDaysSinceFounded + " days ago-------");
        System.out.println("Total regions found: " + regionFoundeds.size());
        regionFoundeds.forEach(regionLastMsg -> System.out.println(regionLastMsg));
    }
    
    /**
     * Checks the tags of the given regions, and prints results.
     * 
     * @param regions the regions of which the tags to check
     */
    private static void checkRegionTags(List<Region> regions)
    {
        // List of regions that have one or more of the tags
        List<String> regionsWithTags = new ArrayList<>();
        
        // Array to list.
        List<String> TagsToCheckLst = Arrays.asList(TagsToCheck);
        
        // Iterate over the regions, doing the check.
        for (Region region : regions)
        {
            // Null/empty check on retrieved messages.
            if (region.Tags == null || region.Tags.isEmpty())
            {
                continue;
            }
            
            // Check whether the region has any of the tags, and if so, add the
            // region name to regionsWithTags.
            if (!Collections.disjoint(region.Tags, TagsToCheckLst))
            {
                regionsWithTags.add(region.Name);
            }
        }
        
        // Now sort the list and print it out.
        Collections.sort(regionsWithTags);
        System.out.println();
        System.out.println("-------Regions with one or more of the specified tags-------");
        System.out.println("Total regions found: " + regionsWithTags.size());
        regionsWithTags.forEach(regionName -> System.out.println(regionName));
    }
}
