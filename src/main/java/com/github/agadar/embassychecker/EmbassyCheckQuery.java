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
 * Query for doing an embassies check and returning a report as a String.
 * Modelled slightly after the queries in the NS API Java Wrapper.
 * 
 * @author Agadar <https://github.com/Agadar/>
 */
public class EmbassyCheckQuery 
{
    /** Name of the region whose embassy regions to check. */
    private final String RegionName;
    
    /** 
     * The maximum number of days since the last message on a region's 
     * message board before that region is considered inactive.
     */
    private int MaxDaysSinceLastRmbMsg;
    
    /**
     * The minimum number of days since a region may have been founded.
     */
    private int MinDaysSinceFounded;
    
    /** The tags to check and warn for. */
    private String[] TagsToCheck;
    
    /** The shards to retrieve for each embassy region. */
    private final List<RegionShard> ShardsToRetrieveLst = new ArrayList<>();       
    
    /** Current time in seconds. */
    private final long Now = System.currentTimeMillis() / 1000;
    
    /**
     * Instantiates a new EmbassyCheckQuery, using the given region name.
     * 
     * @param regionName name of the region whose embassies to check
     * @throws IllegalArgumentException if regionName is null or empty
     */
    public EmbassyCheckQuery(String regionName) throws IllegalArgumentException
    {
        if (regionName == null || regionName.isEmpty())
        {
            throw new IllegalArgumentException("No region name supplied!");
        }
        
        this.RegionName = regionName;
        ShardsToRetrieveLst.add(RegionShard.Name);
    }
    
    /**
     * Makes this query check the RMB activity of each embassy region of the
     * chosen region. Any region which has not had a new RMB message posted
     * between now and the given number of days ago will be included in the
     * report.
     * 
     * @param days maximum number of days since a region's last RMB activity
     * @return this
     * @throws IllegalArgumentException if days <= 0
     */
    public EmbassyCheckQuery rmbActivity(int days) throws IllegalArgumentException
    {
        if (days <= 0)
        {
            throw new IllegalArgumentException("The maximum days of no RMB posts"
                    + " must be greater than 0!");
        }
        
        MaxDaysSinceLastRmbMsg = days;
        ShardsToRetrieveLst.add(RegionShard.RegionalMessages);        
        return this;
    }
    
    /**
     * Makes this query check the age of each embassy region of the chosen 
     * region. Any region which has not existed for longer than the given
     * number of days will be included in the report.
     * 
     * @param days minimum number of days a region must have existed
     * @return this
     * @throws IllegalArgumentException if days <= 0
     */
    public EmbassyCheckQuery minimumAge(int days) throws IllegalArgumentException
    {
        if (days <= 0)
        {
            throw new IllegalArgumentException("The minimum age of region in days"
                    + " must be greater than 0!");
        }
        
        MinDaysSinceFounded = days;
        ShardsToRetrieveLst.add(RegionShard.History);
        return this;
    }
    
    /**
     * Makes this query check the tags of each embassy region of the chosen
     * region. Any region that has one or more of these tags will be included
     * in the report.
     * 
     * @param tags the tags to look for
     * @return this
     * @throws IllegalArgumentException if tags is null or empty
     */
    public EmbassyCheckQuery regionTags(String[] tags) throws IllegalArgumentException
    {
        if (tags == null || tags.length == 0)
        {
            throw new IllegalArgumentException("At least one tag must be supplied!");
        }
        
        TagsToCheck = tags;
        ShardsToRetrieveLst.add(RegionShard.Tags);
        return this;
    }
    
    /**
     * Executes this query, returning a report as a String.
     * 
     * @return the report
     * @throws IllegalArgumentException if none of the checks was selected
     */
    public String execute() throws IllegalArgumentException
    {
        // Throw exception if none of the checks was selected
        if (MaxDaysSinceLastRmbMsg == 0 && MinDaysSinceFounded == 0 &&
            TagsToCheck == null)
        {
            throw new IllegalArgumentException("None of the checks is selected!");
        }
        
        // Retrieve embassies of the specified region.
        final Region MainRegion = NSAPI.region(RegionName).shards(RegionShard.Embassies).execute();
        
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

        // The retrieved regions.
        final List<Region> regions = new ArrayList<>();
        
        // Iterate over retrieved region names, retrieving the regions.
        //for (final String embassyRegionName : embassyRegions)
        for (int i = 0; i < 15; i++)
        {String embassyRegionName = embassyRegions.get(i);
            Region region = NSAPI.region(embassyRegionName)
                    .shards(ShardsToRetrieveLst.toArray(new RegionShard[ShardsToRetrieveLst.size()]))
                    .execute();
            
            // Null check to make sure the region didn't CTE in the meantime.
            if (region != null)
            {
                // Add the region to the list.
                regions.add(region);
            }
        }
        
        // The generated report.
        String generatedReport = "";
        
        // Do RMB activity check.
        if (MaxDaysSinceLastRmbMsg > 0)
        {           
            generatedReport += checkRmbActivity(regions) + "%n";
        }
        
        // Do region founded check.
        if (MinDaysSinceFounded > 0)
        {
            generatedReport += checkRegionFounded(regions) + "%n";
        }
        
        // Do tags check.
        if (TagsToCheck != null)
        {
            generatedReport += checkRegionTags(regions) + "%n";
        }
        
        // Final variable needed for invokeLater because Java. Yeah.
        return String.format(generatedReport);
    }
    
    /**
     * Checks the RMB activity of the given regions, and prints results.
     * 
     * @param region the regions of which the RMB activity to check
     */
    private String checkRmbActivity(List<Region> regions)
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
        
        // Now sort the list and return it.
        Collections.sort(regionLastMsgs);
        String generatedReport = "-------Regions without new RMB messages "
            + "during the last " + MaxDaysSinceLastRmbMsg + " days-------%n";
        generatedReport += "Total regions found: " + regionLastMsgs.size() + ".%n";
        for (RegionLastMsg rlm : regionLastMsgs)
        {
            generatedReport += rlm + "%n";
        }
        return generatedReport;
    }
    
    /**
     * Checks the founding dates of the given regions, and prints results.
     * 
     * @param regions the regions of which the founding dates to check
     */
    private String checkRegionFounded(List<Region> regions)
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
        
        // Now sort the list and return it.
        Collections.sort(regionFoundeds);
        String generatedReport = "-------Regions that were founded less than "
                           + MinDaysSinceFounded + " days ago-------%n";
        generatedReport += "Total regions found: " + regionFoundeds.size() + ".%n";
        for (RegionFounded rf : regionFoundeds)
        {
            generatedReport += rf + "%n";
        }
        return generatedReport;
    }
    
    /**
     * Checks the tags of the given regions, and prints results.
     * 
     * @param regions the regions of which the tags to check
     */
    private String checkRegionTags(List<Region> regions)
    {
        // List of regions that have one or more of the tags
        final List<RegionWithTags> regionsWithTags = new ArrayList<>();
        
        // Array to list.
        final List<String> TagsToCheckLst = Arrays.asList(TagsToCheck);
        
        // Iterate over the regions, doing the check.
        for (Region region : regions)
        {
            // Null/empty check on retrieved messages.
            if (region.Tags == null || region.Tags.isEmpty())
            {
                continue;
            }
            
            // Found tags in this region's tags.
            final List<String> foundTags = new ArrayList<>();
            
            // For each tag to check, check if the region has it. If so, add it to foundTags.
            for (String tagToCheck : TagsToCheckLst)
            {
                if (region.Tags.contains(tagToCheck))
                {
                    foundTags.add(tagToCheck);
                }
            }
            
            // If any of the specified tags were found, create an entry in regionsWithTags.
            if (foundTags.size() > 0)
            {
                regionsWithTags.add(new RegionWithTags(region.Name, foundTags));
            }
        }

        // Now sort the list and return it.
        Collections.sort(regionsWithTags);
        String generatedReport = "-------Regions with one or more of the "
                + "specified tags-------%n";
        generatedReport += "Total regions found: " + regionsWithTags.size() + ".%n";
        for (RegionWithTags rwt : regionsWithTags)
        {
            generatedReport += rwt + "%n";
        }
        return generatedReport;
    }
}
