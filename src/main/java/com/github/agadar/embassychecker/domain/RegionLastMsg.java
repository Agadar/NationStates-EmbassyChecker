package com.github.agadar.embassychecker.domain;

import java.util.concurrent.TimeUnit;

/**
 * A region name with the time difference in days between when the last
 * message was posted on the region's message board and now.
 * 
 * @author Agadar <https://github.com/Agadar/>
 */
public final class RegionLastMsg implements Comparable<RegionLastMsg>
{
    /** Name of the region. */
    public final String region;
    
    /** 
     * Time difference in days between when the last message was posted
     * on the region's message board and now.
     */
    public final int diffInDays;

    /**
     * Constructor.
     * 
     * @param region name of the region
     * @param diffInSeconds time difference in seconds between when 
     * the last message was posted on the region's message board and now
     */
    public RegionLastMsg(String region, long diffInSeconds)
    {
        this(region, (int) TimeUnit.SECONDS.toDays(diffInSeconds));
    }

    /**
     * Constructor. Sets the difference to Integer.MAX_VALUE. Use this if not a
     * single message was ever posted on the region's message board.
     * 
     * @param region 
     */
    public RegionLastMsg(String region)
    {
        this(region, Integer.MAX_VALUE);
    }
    
    /**
     * Constructor.
     * 
     * @param region name of the region
     * @param diffInDays time difference in days between when the last message
     * was posted on the region's message board and now
     */
    public RegionLastMsg(String region, int diffInDays)
    {
        this.region = region;
        this.diffInDays = diffInDays;
    }

    @Override
    public int compareTo(RegionLastMsg t)
    {
        if (diffInDays > t.diffInDays)
        {
            return -1;
        }

        if (diffInDays < t.diffInDays)
        {
            return 1;
        }

        return region.compareTo(t.region);
    }

    @Override
    public String toString()
    {
        String help = String.format("Region: %s; Last RMB msg: ", region);
        
        if (diffInDays != Integer.MAX_VALUE)
        {
            help += String.format("%s days ago.", diffInDays);
        }
        else
        {
            help += "Never.";
        }
        
        return help;
    }

}
