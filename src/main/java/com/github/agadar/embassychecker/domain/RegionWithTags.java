package com.github.agadar.embassychecker.domain;

import com.github.agadar.nationstates.enumerator.RegionTag;
import java.util.List;

/**
 * A region name with the specified tags it has.
 *
 * @author Agadar <https://github.com/Agadar/>
 */
public final class RegionWithTags implements Comparable<RegionWithTags> {

    /**
     * Name of the region.
     */
    public final String region;

    /**
     * The specified tags this region has.
     */
    public final List<RegionTag> tags;

    /**
     * Constructor.
     *
     * @param region name of the region
     * @param tags the specified tags this region has. Should contain at least
     * one entry.
     */
    public RegionWithTags(String region, List<RegionTag> tags) {
        this.region = region;
        this.tags = tags;
    }

    @Override
    public int compareTo(RegionWithTags t) {
        return region.compareTo(t.region);
    }

    @Override
    public String toString() {
        String help = String.format("Region: %s; Tags: %s", region, tags.get(0));

        for (int i = 1; i < tags.size(); i++) {
            help += ", " + tags.get(i).toString();
        }

        return help + ".";
    }

}
