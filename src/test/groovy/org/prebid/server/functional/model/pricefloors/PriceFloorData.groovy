package org.prebid.server.functional.model.pricefloors

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.prebid.server.functional.model.Currency
import org.prebid.server.functional.model.ResponseModel
import org.prebid.server.functional.model.bidder.BidderName
import org.prebid.server.functional.util.PBSUtils

import static org.prebid.server.functional.model.Currency.USD

@EqualsAndHashCode
@ToString(includeNames = true, ignoreNulls = true)
class PriceFloorData implements ResponseModel {

    String floorProvider
    Currency currency
    Integer skipRate
    Integer useFetchDataRate
    String floorsSchemaVersion
    Integer modelTimestamp
    List<ModelGroup> modelGroups
    List<BidderName> noFloorSignalBidders

    static PriceFloorData getPriceFloorData() {
        new PriceFloorData(floorProvider: PBSUtils.randomString,
                currency: USD,
                floorsSchemaVersion: 2,
                modelGroups: [ModelGroup.modelGroup])
    }
}
