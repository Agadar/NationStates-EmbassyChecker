package com.github.agadar.embassychecker.domain;

import java.util.List;

/**
 * A region name with the specified tags it has.
 * 
 * @author Agadar <https://github.com/Agadar/>
 */
public final class RegionWithTags implements Comparable<RegionWithTags>
{
    /** Name of the region. */
    public final String Region;
    
    /** The specified tags this region has. */
    public final List<String> Tags;

    /**
     * Constructor.
     * 
     * @param region name of the region
     * @param tags the specified tags this region has. Should contain at least
     * one entry.
     */
    public RegionWithTags(String region, List<String> tags)
    {
        this.Region = region;
        this.Tags = tags;
    }

    @Override
    public int compareTo(RegionWithTags t)
    {
        return Region.compareTo(t.Region);
    }

    @Override
    public String toString()
    {
        String help = String.format("Region: %s; Tags: %s", Region, Tags.get(0));
        
        for (int i = 1; i < Tags.size(); i++)
        {
            help += ", " + Tags.get(i);
        }
        
        return help + ".";
    }

}
