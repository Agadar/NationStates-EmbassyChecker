package com.github.agadar.embassychecker.domain;

import java.util.Collection;
import java.util.stream.Collectors;

import com.github.agadar.nationstates.enumerator.RegionTag;

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
    public final Collection<RegionTag> tags;

    /**
     * Constructor.
     *
     * @param region name of the region
     * @param tags   the specified tags this region has. Should contain at least one
     *               entry.
     */
    public RegionWithTags(String region, Collection<RegionTag> tags) {
        this.region = region;
        this.tags = tags;
    }

    @Override
    public int compareTo(RegionWithTags t) {
        return region.compareTo(t.region);
    }

    @Override
    public String toString() {
        String tagsText = tags.stream().map(tag -> tag.toString()).collect(Collectors.joining(", "));
        return String.format("Region: %s; Tags: %s.", region, tagsText);
    }
}
