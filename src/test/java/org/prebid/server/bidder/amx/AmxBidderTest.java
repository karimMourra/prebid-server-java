package org.prebid.server.bidder.amx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.iab.openrtb.request.App;
import com.iab.openrtb.request.Banner;
import com.iab.openrtb.request.BidRequest;
import com.iab.openrtb.request.Imp;
import com.iab.openrtb.request.Publisher;
import com.iab.openrtb.request.Site;
import com.iab.openrtb.request.Video;
import com.iab.openrtb.response.Bid;
import com.iab.openrtb.response.BidResponse;
import com.iab.openrtb.response.SeatBid;
import org.junit.jupiter.api.Test;
import org.prebid.server.VertxTest;
import org.prebid.server.bidder.model.BidderBid;
import org.prebid.server.bidder.model.BidderCall;
import org.prebid.server.bidder.model.BidderError;
import org.prebid.server.bidder.model.HttpRequest;
import org.prebid.server.bidder.model.HttpResponse;
import org.prebid.server.bidder.model.Result;
import org.prebid.server.proto.openrtb.ext.ExtPrebid;
import org.prebid.server.proto.openrtb.ext.request.amx.ExtImpAmx;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.prebid.server.proto.openrtb.ext.response.BidType.banner;
import static org.prebid.server.proto.openrtb.ext.response.BidType.video;
import static org.prebid.server.proto.openrtb.ext.response.BidType.xNative;

public class AmxBidderTest extends VertxTest {

    private static final String ENDPOINT_URL = "https://test.com/prebid/bid";

    private final AmxBidder target = new AmxBidder(ENDPOINT_URL, jacksonMapper);

    @Test
    public void creationShouldFailOnInvalidEndpointUrl() {
        assertThatIllegalArgumentException().isThrownBy(() -> new AmxBidder("invalid_url", jacksonMapper));
    }

    @Test
    public void makeHttpRequestsShouldReturnErrorIfImpExtCouldNotBeParsed() {
        // given
        final BidRequest bidRequest = givenBidRequest(
                impBuilder -> impBuilder.ext(mapper.valueToTree(ExtPrebid.of(null, mapper.createArrayNode()))));
        // when
        final Result<List<HttpRequest<BidRequest>>> result = target.makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors()).allMatch(error -> error.getType() == BidderError.Type.bad_input
                && error.getMessage().startsWith("Cannot deserialize value"));
    }

    @Test
    public void makeHttpRequestsShouldCreateCorrectURL() {
        // given
        final BidRequest bidRequest = givenBidRequest(impBuilder -> impBuilder.banner(Banner.builder().build()));

        // when
        final Result<List<HttpRequest<BidRequest>>> result = target.makeHttpRequests(bidRequest);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).hasSize(1)
                .extracting(HttpRequest::getUri)
                .containsExactly("https://test.com/prebid/bid?v=pbs1.2");
    }

    @Test
    public void makeHttpRequestsShouldUpdateRequestAndImps() {
        // given
        final BidRequest bidRequest = givenBidRequest(
                bidRequestBuilder -> bidRequestBuilder.app(App.builder().build()).site(Site.builder().build()),
                impBuilder -> impBuilder.banner(Banner.builder().build()));

        // when
        final Result<List<HttpRequest<BidRequest>>> result = target.makeHttpRequests(bidRequest);

        // then
        final BidRequest expectedBidRequest = BidRequest.builder()
                .app(App.builder().publisher(Publisher.builder().id("testTagId").build()).build())
                .site(Site.builder().publisher(Publisher.builder().id("testTagId").build()).build())
                .imp(singletonList(Imp.builder()
                        .id("123")
                        .banner(Banner.builder().build())
                        .tagid("testAdUnitId")
                        .ext(mapper.valueToTree(ExtPrebid.of(null,
                                ExtImpAmx.of("testTagId", "testAdUnitId"))))
                        .build())).build();
        assertThat(result.getValue())
                .extracting(HttpRequest::getPayload)
                .containsExactly(expectedBidRequest);
    }

    @Test
    public void makeHttpRequestsShouldNotUpdateImpTagIdIfAdUnitIdNotPresent() {
        // given
        final BidRequest bidRequest = givenBidRequest(
                bidRequestBuilder -> bidRequestBuilder.app(App.builder().build()).site(Site.builder().build()),
                impBuilder -> impBuilder
                        .tagid("someTagId")
                        .ext(mapper.valueToTree(ExtPrebid.of(null,
                                ExtImpAmx.of("testTagId", null))))
                        .banner(Banner.builder().build()));

        // when
        final Result<List<HttpRequest<BidRequest>>> result = target.makeHttpRequests(bidRequest);

        // then
        assertThat(result.getValue())
                .extracting(HttpRequest::getPayload)
                .flatExtracting(BidRequest::getImp)
                .extracting(Imp::getTagid)
                .containsExactly("someTagId");
    }

    @Test
    public void makeHttpRequestsShouldUpdateImpTagIdIfAdUnitIdIsPresent() {
        // given
        final BidRequest bidRequest = givenBidRequest(
                bidRequestBuilder -> bidRequestBuilder.app(App.builder().build()).site(Site.builder().build()),
                impBuilder -> impBuilder
                        .ext(mapper.valueToTree(ExtPrebid.of(null,
                                ExtImpAmx.of("testTagId", "testAdUnitId"))))
                        .banner(Banner.builder().build()));

        // when
        final Result<List<HttpRequest<BidRequest>>> result = target.makeHttpRequests(bidRequest);

        // then
        assertThat(result.getValue())
                .extracting(HttpRequest::getPayload)
                .flatExtracting(BidRequest::getImp)
                .extracting(Imp::getTagid)
                .containsExactly("testAdUnitId");
    }

    @Test
    public void makeBidsShouldReturnErrorIfResponseBodyCouldNotBeParsed() {
        // given
        final BidderCall<BidRequest> httpCall = givenHttpCall(null, "invalid");

        // when
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors()).allMatch(error -> error.getType() == BidderError.Type.bad_server_response
                && error.getMessage().startsWith("Failed to decode: Unrecognized token"));
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeBidsShouldReturnEmptyListIfBidResponseIsNull() throws JsonProcessingException {
        // given
        final BidderCall<BidRequest> httpCall = givenHttpCall(null,
                mapper.writeValueAsString(null));

        // when
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeBidsShouldReturnEmptyListIfBidResponseSeatBidIsNull() throws JsonProcessingException {
        // given
        final BidderCall<BidRequest> httpCall = givenHttpCall(null,
                mapper.writeValueAsString(BidResponse.builder().build()));

        // when
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeBidsShouldReturnBannerBidIfBannerIsBidExtNotPresent() throws JsonProcessingException {
        // given
        final BidderCall<BidRequest> httpCall = givenHttpCall(
                BidRequest.builder().build(), mapper.writeValueAsString(givenBidResponse(identity())));

        // when
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .containsExactly(BidderBid.of(Bid.builder().build(), banner, "USD"));
    }

    @Test
    public void makeBidsShouldReturnBannerBidWithSeatSetWhenBidderCodeIsPresentInBidExt()
            throws JsonProcessingException {

        // given
        final ObjectNode bidExt = mapper.createObjectNode();
        bidExt.put("bc", "seat");
        final BidderCall<BidRequest> httpCall = givenHttpCall(BidRequest.builder().build(),
                mapper.writeValueAsString(
                        givenBidResponse(bidBuilder -> bidBuilder.ext(bidExt))));

        // when
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .containsExactly(BidderBid.of(Bid.builder().ext(bidExt).build(), banner, "seat", "USD"));
    }

    @Test
    public void makeBidsShouldReturnVideoBidIfStartDelayIsPresentInBidExt() throws JsonProcessingException {
        // given
        final ObjectNode bidExt = mapper.createObjectNode();
        bidExt.put("startdelay", 2);
        final BidderCall<BidRequest> httpCall = givenHttpCall(BidRequest.builder().build(),
                mapper.writeValueAsString(
                        givenBidResponse(bidBuilder -> bidBuilder.ext(bidExt))));

        // when
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .containsExactly(BidderBid.of(Bid.builder()
                        .ext(bidExt)
                        .build(), video, "USD"));
    }

    @Test
    public void makeBidsShouldReturnxNativeBidIfCreativeTypeIsPresentInBidExt() throws JsonProcessingException {
        // given
        final ObjectNode bidExt = mapper.createObjectNode();
        bidExt.put("ct", 10);
        final BidderCall<BidRequest> httpCall = givenHttpCall(BidRequest.builder().build(),
                mapper.writeValueAsString(
                        givenBidResponse(bidBuilder -> bidBuilder.ext(bidExt))));

        // when
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .containsExactly(BidderBid.of(Bid.builder()
                        .ext(bidExt)
                        .build(), xNative, "USD"));
    }

    @Test
    public void makeBidsShouldReturnBannerBidIfCreativeTypeAndStartDelayNotPresentInBidExt()
            throws JsonProcessingException {
        // given
        final BidderCall<BidRequest> httpCall = givenHttpCall(BidRequest.builder().build(),
                mapper.writeValueAsString(
                        givenBidResponse(identity())));

        // when
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .containsExactly(BidderBid.of(Bid.builder().build(), banner, "USD"));
    }

    @Test
    public void makeBidsShouldSkipBidAndAddErrorIfFailedToParseBidExt() throws JsonProcessingException {
        // given
        final ObjectNode bidExt = mapper.createObjectNode();
        bidExt.put("startdelay", "2");
        final BidderCall<BidRequest> httpCall = givenHttpCall(BidRequest.builder().build(),
                mapper.writeValueAsString(
                        givenBidResponse(bidBuilder -> bidBuilder
                                .id("bidId")
                                .adm("</Impression>")
                                .ext(mapper.createObjectNode().set("startdelay", mapper.createObjectNode())))));

        // when
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).hasSize(1)
                .satisfies(error -> {
                    assertThat(error).extracting(BidderError::getType).containsExactly(BidderError.Type.bad_input);
                    assertThat(error).extracting(BidderError::getMessage)
                            .element(0).asString().startsWith("Cannot deserialize value");
                });
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeBidsShouldSetADomainsFromBidAdomainsField() throws JsonProcessingException {
        // given
        final BidderCall<BidRequest> httpCall = givenHttpCall(
                BidRequest.builder()
                        .imp(singletonList(Imp.builder().video(Video.builder().build()).id("123").build()))
                        .build(),
                mapper.writeValueAsString(
                        givenBidResponse(bidBuilder -> bidBuilder.adomain(singletonList("someAdomain")).impid("123"))));

        // when
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        // then
        final ArrayNode metaAdvrtisers = mapper.createArrayNode();
        metaAdvrtisers.add("someAdomain");
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .extracting(BidderBid::getBid)
                .extracting(Bid::getExt)
                .extracting(bidExt -> bidExt.get("prebid"))
                .extracting(bidExtPrebid -> bidExtPrebid.get("meta"))
                .extracting(bidExtPrebidMeta -> bidExtPrebidMeta.get("advertiserDomains"))
                .containsExactly(metaAdvrtisers);
    }

    @Test
    public void makeBidsShouldSetDemandSourceFromBidExtDsField() throws JsonProcessingException {
        // given
        final ObjectNode givenBidExt = mapper.createObjectNode();
        givenBidExt.set("ds", new TextNode("someDs"));
        final BidderCall<BidRequest> httpCall = givenHttpCall(
                BidRequest.builder()
                        .imp(singletonList(Imp.builder().video(Video.builder().build()).id("123").build()))
                        .build(),
                mapper.writeValueAsString(
                        givenBidResponse(bidBuilder -> bidBuilder.ext(givenBidExt).impid("123"))));

        // when
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        // then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .extracting(BidderBid::getBid)
                .extracting(Bid::getExt)
                .extracting(bidExt -> bidExt.get("prebid"))
                .extracting(bidExtPrebid -> bidExtPrebid.get("meta"))
                .extracting(bidExtPrebidMeta -> bidExtPrebidMeta.get("demandSource"))
                .containsExactly(new TextNode("someDs"));
    }

    @Test
    public void makeBidsShouldSetDemandSourceAndADomainsAndTolerateExistingBidExt() throws JsonProcessingException {
        // given
        final ObjectNode givenBidExt = mapper.createObjectNode();
        final ObjectNode givenBidExtPrebid = mapper.createObjectNode();
        givenBidExtPrebid.set("property1", new TextNode("someValue"));
        givenBidExt.set("prebid", givenBidExtPrebid);
        givenBidExt.set("ds", new TextNode("someDs"));
        final BidderCall<BidRequest> httpCall = givenHttpCall(
                BidRequest.builder()
                        .imp(singletonList(Imp.builder().video(Video.builder().build()).id("123").build()))
                        .build(),
                mapper.writeValueAsString(
                        givenBidResponse(bidBuilder -> bidBuilder
                                .ext(givenBidExt)
                                .adomain(singletonList("someAdomain"))
                                .impid("123"))));

        // when
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        // then
        final ObjectNode expectedBidExt = mapper.createObjectNode();
        final ObjectNode expectedBidExtPrebid = mapper.createObjectNode();
        expectedBidExtPrebid.set("property1", new TextNode("someValue"));
        final ObjectNode expectedBidExtPrebidMeta = mapper.createObjectNode();
        expectedBidExtPrebidMeta.set("demandSource", new TextNode("someDs"));

        final ArrayNode metaAdvrtisers = mapper.createArrayNode();
        metaAdvrtisers.add("someAdomain");
        expectedBidExtPrebidMeta.set("advertiserDomains", metaAdvrtisers);
        expectedBidExtPrebid.set("property1", new TextNode("someValue"));
        expectedBidExtPrebid.set("meta", expectedBidExtPrebidMeta);
        expectedBidExt.set("prebid", expectedBidExtPrebid);
        expectedBidExt.set("ds", new TextNode("someDs"));

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .extracting(BidderBid::getBid)
                .extracting(Bid::getExt)
                .containsExactly(expectedBidExt);
    }

    private static BidRequest givenBidRequest(
            Function<BidRequest.BidRequestBuilder, BidRequest.BidRequestBuilder> bidRequestCustomizer,
            Function<Imp.ImpBuilder, Imp.ImpBuilder> impCustomizer) {

        return bidRequestCustomizer.apply(BidRequest.builder()
                        .imp(singletonList(givenImp(impCustomizer))))
                .build();
    }

    private static BidRequest givenBidRequest(Function<Imp.ImpBuilder, Imp.ImpBuilder> impCustomizer) {
        return givenBidRequest(identity(), impCustomizer);
    }

    private static Imp givenImp(Function<Imp.ImpBuilder, Imp.ImpBuilder> impCustomizer) {
        return impCustomizer.apply(Imp.builder()
                        .id("123")
                        .banner(Banner.builder().id("banner_id").build()).ext(mapper.valueToTree(ExtPrebid.of(null,
                                ExtImpAmx.of("testTagId", "testAdUnitId")))))
                .build();
    }

    private static BidResponse givenBidResponse(Function<Bid.BidBuilder, Bid.BidBuilder> bidCustomizer) {
        return BidResponse.builder()
                .cur("USD")
                .seatbid(singletonList(SeatBid.builder().bid(singletonList(bidCustomizer.apply(Bid.builder()).build()))
                        .build()))
                .build();
    }

    private static BidderCall<BidRequest> givenHttpCall(BidRequest bidRequest, String body) {
        return BidderCall.succeededHttp(
                HttpRequest.<BidRequest>builder().payload(bidRequest).build(),
                HttpResponse.of(200, null, body),
                null);
    }
}
