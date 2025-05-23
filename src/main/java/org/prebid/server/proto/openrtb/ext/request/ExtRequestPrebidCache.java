package org.prebid.server.proto.openrtb.ext.request;

import lombok.Value;

/**
 * Defines the contract for bidrequest.ext.prebid.cache
 */
@Value(staticConstructor = "of")
public class ExtRequestPrebidCache {

    public static final ExtRequestPrebidCache EMPTY = new ExtRequestPrebidCache(null, null, null);

    /**
     * Defines the contract for bidrequest.ext.prebid.cache.bids
     */
    ExtRequestPrebidCacheBids bids;

    /**
     * Defines the contract for bidrequest.ext.prebid.cache.vastxml
     */
    ExtRequestPrebidCacheVastxml vastxml;

    Boolean winningonly;
}
