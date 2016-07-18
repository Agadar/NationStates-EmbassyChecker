package com.github.agadar.embassychecker.domain;

import java.util.concurrent.TimeUnit;

/**
 * A region name with the time difference in days between when the region was
 * founded and now.
 * 
 * @author Agadar <https://github.com/Agadar/>
 */
public final class RegionFounded implements Comparable<RegionFounded>
{
    /** Name of the region. */
    public final String region;
    
    /** 
     * Time difference in days between when the region was founded and now.
     */
    public final int diffInDays;

    /**
     * Constructor.
     * 
     * @param region name of the region
     * @param diffInSeconds time difference in seconds between when the region 
     * was founded and now
     */
    public RegionFounded(String region, long diffInSeconds)
    {
        this(region, (int) TimeUnit.SECONDS.toDays(diffInSeconds));
    }
    
    /**
     * Constructor.
     * 
     * @param region name of the region
     * @param diffInDays time difference in days between when the region was
     * founded and now
     */
    public RegionFounded(String region, int diffInDays)
    {
        this.region = region;
        this.diffInDays = diffInDays;
    }

    @Override
    public int compareTo(RegionFounded t)
    {
        if (diffInDays > t.diffInDays)
        {
            return 1;
        }

        if (diffInDays < t.diffInDays)
        {
            return -1;
        }

        return region.compareTo(t.region);
    }

    @Override
    public String toString()
    {
        return String.format("Region: %s; Founded: %s days ago.", region, diffInDays);
    }

}