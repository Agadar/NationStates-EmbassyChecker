package com.github.agadar.embassychecker;

import com.github.agadar.embassychecker.domain.RegionFounded;
import com.github.agadar.embassychecker.domain.RegionLastMsg;
import com.github.agadar.embassychecker.domain.RegionWithTags;
import com.github.agadar.embassychecker.event.RegionEventsListener;
import com.github.agadar.embassychecker.event.RegionRetrievedEvent;
import com.github.agadar.embassychecker.event.RegionRetrievingStartedEvent;

import com.github.agadar.nationstates.NationStates;
import com.github.agadar.nationstates.domain.region.Region;
import com.github.agadar.nationstates.enumerator.EmbassyStatus;
import com.github.agadar.nationstates.enumerator.RegionTag;
import com.github.agadar.nationstates.shard.RegionShard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Query for doing an embassies check and returning a report as a String.
 * Modeled slightly after the queries in the NS API Java Wrapper.
 *
 * @author Agadar <https://github.com/Agadar/>
 */
public class EmbassyCheckQuery {

    /**
     * Name of the region whose embassy regions to check.
     */
    private final String regionName;

    /**
     * The maximum number of days since the last message on a region's message
     * board before that region is considered inactive.
     */
    private int maxDaysSinceLastRmbMsg;

    /**
     * The minimum number of days since a region may have been founded.
     */
    private int minDaysSinceFounded;

    /**
     * The tags to check and warn for.
     */
    private RegionTag[] tagsToCheck;

    /**
     * The shards to retrieve for each embassy region.
     */
    private final List<RegionShard> shardsToRetrieveLst = new ArrayList<>();

    /**
     * Current time in seconds.
     */
    private final long now = System.currentTimeMillis() / 1000;

    /**
     * The event listeners for this query.
     */
    private final List<RegionEventsListener> listeners = new ArrayList<>();

    /**
     * Instantiates a new EmbassyCheckQuery, using the given region name.
     *
     * @param regionName name of the region whose embassies to check
     * @throws IllegalArgumentException if regionName is null or empty
     */
    public EmbassyCheckQuery(String regionName) throws IllegalArgumentException {
        if (regionName == null || regionName.isEmpty()) {
            throw new IllegalArgumentException("No region name supplied!");
        }

        this.regionName = regionName;
        shardsToRetrieveLst.add(RegionShard.NAME);
    }

    /**
     * Adds new region event listeners to this query.
     *
     * @param newListeners the listeners to add
     * @return this
     */
    public EmbassyCheckQuery addListeners(RegionEventsListener... newListeners) {
        synchronized (listeners) {
            for (RegionEventsListener listener : newListeners) {
                if (!listeners.contains(listener)) {
                    listeners.add(listener);
                }
            }
        }
        return this;
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
    public EmbassyCheckQuery rmbActivity(int days) throws IllegalArgumentException {
        if (days <= 0) {
            throw new IllegalArgumentException("The maximum days of no RMB posts"
                    + " must be greater than 0!");
        }

        maxDaysSinceLastRmbMsg = days;
        shardsToRetrieveLst.add(RegionShard.REGIONAL_MESSAGES);
        return this;
    }

    /**
     * Makes this query check the age of each embassy region of the chosen
     * region. Any region which has not existed for longer than the given number
     * of days will be included in the report.
     *
     * @param days minimum number of days a region must have existed
     * @return this
     * @throws IllegalArgumentException if days <= 0
     */
    public EmbassyCheckQuery minimumAge(int days) throws IllegalArgumentException {
        if (days <= 0) {
            throw new IllegalArgumentException("The minimum age of region in days"
                    + " must be greater than 0!");
        }

        minDaysSinceFounded = days;
        shardsToRetrieveLst.add(RegionShard.FOUNDED);
        return this;
    }

    /**
     * Makes this query check the tags of each embassy region of the chosen
     * region. Any region that has one or more of these tags will be included in
     * the report.
     *
     * @param tags the tags to look for
     * @return this
     * @throws IllegalArgumentException if tags is null or empty
     */
    public EmbassyCheckQuery regionTags(RegionTag[] tags) throws IllegalArgumentException {
        if (tags == null || tags.length == 0) {
            throw new IllegalArgumentException("At least one tag must be supplied!");
        }

        tagsToCheck = tags;
        shardsToRetrieveLst.add(RegionShard.TAGS);
        return this;
    }

    /**
     * Executes this query, returning a report as a String.
     *
     * @return the report
     * @throws IllegalArgumentException if none of the checks was selected
     */
    public String execute() throws IllegalArgumentException {
        // Throw exception if none of the checks was selected.
        if (maxDaysSinceLastRmbMsg == 0 && minDaysSinceFounded == 0
                && tagsToCheck == null) {
            throw new IllegalArgumentException("None of the checks is selected!");
        }

        // Retrieve embassies of the specified region.
        final Region MainRegion = NationStates.region(regionName).shards(RegionShard.EMBASSIES).execute();

        // Null-check on the region.
        if (MainRegion == null) {
            throw new IllegalArgumentException("Region does not exist!");
        }

        // Extract all region names from embassies that are established or pending,
        // because we don't care about other embassies.
        final List<String> embassyRegions = new ArrayList<>();
        MainRegion.embassies.forEach(embassy -> {
            if (embassy.status == EmbassyStatus.ESTABLISHED || embassy.status == EmbassyStatus.PENDING) {
                embassyRegions.add(embassy.regionName);
            }
        });

        // The retrieved regions.
        final List<Region> regions = new ArrayList<>();

        // Fire RegionRetrievingStartedEvent
        synchronized (listeners) {
            final RegionRetrievingStartedEvent event
                    = new RegionRetrievingStartedEvent(this, embassyRegions.size());

            listeners.stream().forEach((listener) -> {
                listener.handleRetrievingStarted(event);
            });
        }

        // Iterate over retrieved region names, retrieving the regions.
        for (int i = 0; i < embassyRegions.size(); i++) {
            final String embassyRegionName = embassyRegions.get(i);
            final Region region = NationStates.region(embassyRegionName)
                    .shards(shardsToRetrieveLst.toArray(new RegionShard[shardsToRetrieveLst.size()]))
                    .execute();
            boolean Retrieved;

            // Null check to make sure the region didn't CTE in the meantime.
            if (Retrieved = region != null) {
                // Add the region to the list.
                regions.add(region);
            }

            // Fire RegionRetrievedEvent
            synchronized (listeners) {
                final RegionRetrievedEvent event
                        = new RegionRetrievedEvent(this, embassyRegionName, i, Retrieved);

                listeners.stream().forEach((listener) -> {
                    listener.handleRegionRetrieved(event);
                });
            }
        }

        // The generated report.
        String generatedReport = "";

        // Do RMB activity check.
        if (maxDaysSinceLastRmbMsg > 0) {
            generatedReport += checkRmbActivity(regions) + "%n";
        }

        // Do region founded check.
        if (minDaysSinceFounded > 0) {
            generatedReport += checkRegionFounded(regions) + "%n";
        }

        // Do tags check.
        if (tagsToCheck != null) {
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
    private String checkRmbActivity(List<Region> regions) {
        // List that will contain regions that haven't had RMB posts in x days.
        final List<RegionLastMsg> regionLastMsgs = new ArrayList<>();

        // Iterate over the regions, doing the check.
        for (Region region : regions) {
            // Null/empty check on retrieved messages.
            if (region.regionalMessages == null || region.regionalMessages.isEmpty()) {
                regionLastMsgs.add(new RegionLastMsg(region.name));
                continue;
            }

            // Check whether the time between now and when the last posted RMB
            // message is more than the maxMsSinceLastRmbMsg. If so, add to regionLastMsgs.
            final long msgTimeStamp = region.regionalMessages.get(region.regionalMessages
                    .size() - 1).timestamp;
            final long diff = now - msgTimeStamp;

            if (diff >= TimeUnit.DAYS.toSeconds(maxDaysSinceLastRmbMsg)) {
                regionLastMsgs.add(new RegionLastMsg(region.name, diff));
            }
        }

        // Now sort the list and return it.
        Collections.sort(regionLastMsgs);
        String generatedReport = "-------Regions without new RMB messages "
                + "during the last " + maxDaysSinceLastRmbMsg + " days-------%n";
        generatedReport += "Total regions found: " + regionLastMsgs.size() + ".%n";
        generatedReport = regionLastMsgs.stream().map((rlm) -> rlm + "%n").reduce(generatedReport, String::concat);
        return generatedReport;
    }

    /**
     * Checks the founding dates of the given regions, and prints results.
     *
     * @param regions the regions of which the founding dates to check
     */
    private String checkRegionFounded(List<Region> regions) {
        // List that will contain regions that haven't had RMB posts in x days.
        final List<RegionFounded> regionFoundeds = new ArrayList<>();

        // Iterate over the regions, doing the check.
        regions.stream().filter((region) -> !(region.founded == 0)).forEach((region) -> {
            // Check whether the time between now and when the region was founded
            // is less than the minMsSinceFounded. If so, add to regionFoundeds.
            final long diff = now - region.founded;
            if (diff < TimeUnit.DAYS.toSeconds(minDaysSinceFounded)) {
                regionFoundeds.add(new RegionFounded(region.name, diff));
            }
        });

        // Now sort the list and return it.
        Collections.sort(regionFoundeds);
        String generatedReport = "-------Regions that were founded less than "
                + minDaysSinceFounded + " days ago-------%n";
        generatedReport += "Total regions found: " + regionFoundeds.size() + ".%n";
        generatedReport = regionFoundeds.stream().map((rf) -> rf + "%n").reduce(generatedReport, String::concat);
        return generatedReport;
    }

    /**
     * Checks the tags of the given regions, and prints results.
     *
     * @param regions the regions of which the tags to check
     */
    private String checkRegionTags(List<Region> regions) {
        // List of regions that have one or more of the tags
        final List<RegionWithTags> regionsWithTags = new ArrayList<>();

        // Array to list.
        final List<RegionTag> TagsToCheckLst = Arrays.asList(tagsToCheck);

        // Iterate over the regions, doing the check.
        regions.stream().filter((region) -> !(region.tags == null || region.tags.isEmpty())).forEach((region) -> {
            // Found tags in this region's tags.
            final List<RegionTag> foundTags = new ArrayList<>();
            // For each tag to check, check if the region has it. If so, add it to foundTags.
            TagsToCheckLst.stream().filter((tagToCheck) -> (region.tags.contains(tagToCheck))).forEach((tagToCheck) -> {
                foundTags.add(tagToCheck);
            });

            // If any of the specified tags were found, create an entry in regionsWithTags.
            if (foundTags.size() > 0) {
                regionsWithTags.add(new RegionWithTags(region.name, foundTags));
            }
        });

        // Now sort the list and return it.
        Collections.sort(regionsWithTags);
        String generatedReport = "-------Regions with one or more of the "
                + "specified tags-------%n";
        generatedReport += "Total regions found: " + regionsWithTags.size() + ".%n";
        generatedReport = regionsWithTags.stream().map((rwt) -> rwt + "%n").reduce(generatedReport, String::concat);
        return generatedReport;
    }
}
