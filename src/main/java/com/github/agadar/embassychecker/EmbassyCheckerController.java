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
import javax.swing.SwingUtilities;

/**
 * EmbassyCheckerController class for this program.
 * 
 * @author Agadar <https://github.com/Agadar/>
 */
public class EmbassyCheckerController
{
    /** Name of the region whose embassy regions to check. */
    private static String MainRegionName;
    
    /** Whether or not to check the RMB activity of each region. */
    private static boolean CheckRmbActivity;
    
    /** 
     * The maximum number of days since the last message on a region's 
     * message board before that region is considered inactive.
     */
    private static int MaxDaysSinceLastRmbMsg;
    
    /** Whether or not to check the age of each region. */
    private static boolean CheckRegionFounded;
    
    /**
     * The minimum number of days since a region may have been founded.
     */
    private static int MinDaysSinceFounded;
    
    /** Whether or not to check the region's tags. */
    private static boolean CheckRegionTags;
    
    /** The tags to check and warn for. */
    private static String[] TagsToCheck;
    
    /** The form to send updates to. */
    private static EmbassyCheckerForm Form;
    
    /** The user agent for this program. */
    private final static String UserAgent = "Agadar's Embassy Checker (https://github.com/Agadar/NationStates-EmbassyChecker)";
    
    /** Timestamp in seconds of now. */
    private static long Now;
    
    /** 
     * Initializes this controller. Call this once, before anything else.
     * 
     * @param form the form to send updates to
     */
    public static void init(EmbassyCheckerForm form)
    {
        // Set User Agent.
        NSAPI.setUserAgent(UserAgent);
        
        // Set form.
        Form = form;
    }
    
    /**
     * Starts building a report.
     * 
     * @param mainRegionName
     * @param checkRmbActivity
     * @param maxDaysSinceLastRmbMsg
     * @param checkRegionFounded
     * @param minDaysSinceFounded
     * @param checkRegionTags
     * @param tagsToCheck 
     */
    public static void buildReport(String mainRegionName, boolean checkRmbActivity, 
            int maxDaysSinceLastRmbMsg, boolean checkRegionFounded,
            int minDaysSinceFounded, boolean checkRegionTags,
            String[] tagsToCheck)
    {
        // Disable the start button, and clear the textarea.
        SwingUtilities.invokeLater(() ->
        {
            Form.BtnStart.setEnabled(false);
            Form.TxtAreaReport.setText("");
        });
        
        // Ensure regionName is not empty or null.
        if (mainRegionName == null || mainRegionName.isEmpty())
        {
            throw new IllegalArgumentException("No region name supplied!");
        }
        
        // Ensure maxDaysSinceLastRmbMsg is greater than 0 if checkRmbActivity.
        if (checkRmbActivity && maxDaysSinceLastRmbMsg <= 0)
        {
            throw new IllegalArgumentException("The maximum days of no RMB posts"
                    + " must be greater than 0!");
        }
        
        // Ensure minDaysSinceFounded is greater than 0 if checkRegionFounded.
        if (checkRegionFounded && minDaysSinceFounded <= 0)
        {
            throw new IllegalArgumentException("The minimum age of region in days"
                    + " must be greater than 0!");
        }
        
        // Ensure tagsToCheck is not null or empty if checkRegionTags
        if (checkRegionTags && (tagsToCheck == null || tagsToCheck.length == 0))
        {
            throw new IllegalArgumentException("At least one tag must be supplied!");
        }
        
        // Ensure at least one check is being done.
        if (!checkRmbActivity && !checkRegionFounded && !checkRegionTags)
        {
            throw new IllegalArgumentException("None of the checks is selected!");
        }
        
        // Set variables.
        MainRegionName = mainRegionName;
        CheckRmbActivity = checkRmbActivity;
        MaxDaysSinceLastRmbMsg = maxDaysSinceLastRmbMsg;
        CheckRegionFounded = checkRegionFounded;
        MinDaysSinceFounded = minDaysSinceFounded;
        CheckRegionTags = checkRegionTags;
        TagsToCheck = tagsToCheck;
        Now = System.currentTimeMillis() / 1000;
        
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
        final Region MainRegion = NSAPI.region(MainRegionName).shards(RegionShard.Embassies).execute();
        
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
        //for (final String regionName : embassyRegions)
        for (int i = 0; i < 20; i++)
        {String regionName = embassyRegions.get(i);
            Region region = NSAPI.region(regionName).shards(shardsToRetrieve).execute();
            
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
        if (CheckRmbActivity)
        {           
            generatedReport += checkRmbActivity(regions) + "%n";
        }
        
        // Do region founded check.
        if (CheckRegionFounded)
        {
            generatedReport += checkRegionFounded(regions) + "%n";
        }
        
        // Do tags check.
        if (CheckRegionTags)
        {
            generatedReport += checkRegionTags(regions) + "%n";
        }
        
        // Final variable needed for invokeLater because Java. Yeah.
        final String finalReport = String.format(generatedReport);
        
        // Once we're done, enable the start button again, and print the result.
        SwingUtilities.invokeLater(() ->
        {
            Form.BtnStart.setEnabled(true);
            Form.TxtAreaReport.setText(finalReport);
        });
    }
    
    /**
     * Checks the RMB activity of the given regions, and prints results.
     * 
     * @param region the regions of which the RMB activity to check
     */
    private static String checkRmbActivity(List<Region> regions)
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
    private static String checkRegionFounded(List<Region> regions)
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
    private static String checkRegionTags(List<Region> regions)
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
