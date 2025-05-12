package org.prebid.server.bidder.model;

import lombok.Value;

import java.util.Collections;
import java.util.Set;

/**
 * Represents any kind of error produced by bidder.
 */
@Value(staticConstructor = "of")
public class BidderError {

    String message;

    Type type;

    Set<String> impIds;

    public static BidderError of(String message, Type type) {
        return of(message, type, Collections.emptySet());
    }

    public static BidderError create(String message, Type type) {
        return BidderError.of(message, type);
    }

    public static BidderError generic(String message) {
        return BidderError.of(message, Type.generic7);
    }

    public static BidderError invalidBid(String message) {
        return BidderError.of(message, Type.invalid_bid);
    }

    public static BidderError badInput(String message) {
        return BidderError.of(message, Type.bad_input);
    }

    public static BidderError emptyMediaTypes(String message) {
        return BidderError.of(message, Type.empty_media_types);
    }

    public static BidderError emptyImps(String message) {
        return BidderError.of(message, Type.empty_imps);
    }

    public static BidderError impMediaMismatch(String message) {
        return BidderError.of(message, Type.imp_media_mismatch);
    }

    public static BidderError no_imps_left(String message) {
        return BidderError.of(message, Type.no_imps_left);
    }

    public static BidderError no_imps_left_two(String message) {
        return BidderError.of(message, Type.no_imps_left_two);
    }

    public static BidderError bad_endpoint(String message) {
        return BidderError.of(message, Type.bad_endpoint);
    }

    public static BidderError bad_ip(String message) {
        return BidderError.of(message, Type.bad_ip);
    }

    public static BidderError bad_imp(String message) {
        return BidderError.of(message, Type.bad_imp);
    }

    public static BidderError bad_decode(String message) {
        return BidderError.of(message, Type.bad_decode);
    }

    public static BidderError bad_price_floors(String message) {
        return BidderError.of(message, Type.bad_price_floors);
    }

    public static BidderError bad_currency(String message) {
        return BidderError.of(message, Type.bad_currency);
    }

    public static BidderError bad_currencies_multiple(String message) {
        return BidderError.of(message, Type.bad_currencies_multiple);
    }



    public static BidderError rejectedIpf(String message, String impId) {
        return BidderError.of(message, Type.rejected_ipf, Collections.singleton(impId));
    }

    public static BidderError badServerResponse(String message) {
        return BidderError.of(message, Type.bad_server_response);
    }

    public static BidderError badServerResponseThree(String message) {
        return BidderError.of(message, Type.bad_server_response_three);
    }

    public static BidderError failedToRequestBids(String message) {
        return BidderError.of(message, Type.failed_to_request_bids);
    }

    public static BidderError timeout(String message) {
        return BidderError.of(message, Type.timeout);
    }

    public enum Type {
        /**
         * Should be used when returning errors which are caused by bad input.
         * It should _not_ be used if the error is a server-side issue (e.g. failed to send the external request).
         * Error of this type will not be written to the app log, since it's not an actionable item for the Prebid
         * Server hosts.
         */
        bad_input(2),

        /**
         * Should be used when returning errors which are caused by bad/unexpected behavior on the remote server.
         * <p>
         * For example:
         * <p>
         * - The external server responded with a 500
         * - The external server gave a malformed or unexpected response.
         * <p>
         * These should not be used to log _connection_ errors (e.g. "couldn't find host"), which may indicate config
         * issues for the PBS host company
         */
        bad_server_response(3),

        /**
         * Covers the case where a bidder failed to generate any http requests to get bids, but did not generate any
         * error messages. This should not happen in practice and will signal that a bidder is poorly coded.
         * If there was something wrong with a request such that a bidder could not generate a bid, then it should
         * generate an error explaining the deficiency. Otherwise, it will be extremely difficult to debug the reason
         * why a bidder is not bidding.
         */
        failed_to_request_bids(4),

        /**
         * Covers the case where a bid does not pass validation with error or warnings. One instance per invalid bid
         * created with aggregation for all warnings and errors.
         */
        invalid_bid(5),

        /**
         * Covers the case where a bid was rejected by price-floors feature functionality
         */
        rejected_ipf(6),

        timeout(1),
        bad_endpoint(10),
        bad_ip(11),
        bad_imp(12),
        bad_decode(13),
        bad_price_floors(14),
        bad_currency(15),
        bad_currencies_multiple(16),
        deprecated_bidder(17),
        got_native(18),
        targeting_issue(19),
        empty_media_types(20),
        empty_imps(21),
        imp_media_mismatch(22),
        no_imps_left(23),
        no_imps_left_two(24),
        bad_input_two(25),
        bad_server_response_two(26),
        bad_server_response_three(27),
        bot_traffic(28),
        request_excess(29),
        generic1(9991),
        generic2(9992),
        generic3(9993),
        generic4(9994),
        generic5(9995),
        generic6(9996),
        generic7(9997),
        generic8(9998),
        generic(999);

        private final Integer code;

        Type(final Integer errorCode) {
            this.code = errorCode;
        }

        public Integer getCode() {
            return code;
        }
    }
}
