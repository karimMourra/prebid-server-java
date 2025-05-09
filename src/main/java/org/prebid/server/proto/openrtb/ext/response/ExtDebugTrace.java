package org.prebid.server.proto.openrtb.ext.response;

import lombok.Value;

import java.util.List;

/**
 * Defines the contract for bidresponse.ext.debug.trace
 */
@Value(staticConstructor = "of")
public class ExtDebugTrace {

    List<ExtTraceActivityInfrastructure> activityInfrastructure;
}
