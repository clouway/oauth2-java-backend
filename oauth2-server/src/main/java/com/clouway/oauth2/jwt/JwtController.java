package com.clouway.oauth2.jwt;

import com.clouway.friendlyserve.Request;
import com.clouway.friendlyserve.Response;
import com.clouway.oauth2.BearerTokenResponse;
import com.clouway.oauth2.DateTime;
import com.clouway.oauth2.Identity;
import com.clouway.oauth2.InstantaneousRequest;
import com.clouway.oauth2.OAuthError;
import com.clouway.oauth2.client.Client;
import com.clouway.oauth2.client.JwtKeyStore;
import com.clouway.oauth2.jws.Pem;
import com.clouway.oauth2.jws.Signature;
import com.clouway.oauth2.jws.SignatureFactory;
import com.clouway.oauth2.token.BearerToken;
import com.clouway.oauth2.token.GrantType;
import com.clouway.oauth2.token.IdTokenFactory;
import com.clouway.oauth2.token.TokenResponse;
import com.clouway.oauth2.token.Tokens;
import com.clouway.oauth2.user.FindIdentityRequest;
import com.clouway.oauth2.user.IdentityFinder;
import com.clouway.oauth2.util.Params;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.clouway.oauth2.token.TokenRequest.newTokenRequest;

/**
 * @author Miroslav Genov (miroslav.genov@clouway.com)
 */
public class JwtController implements InstantaneousRequest {
  private final Gson gson = new Gson();

  private final SignatureFactory signatureFactory;
  private final Tokens tokens;
  private final JwtKeyStore keyStore;
  private final IdentityFinder identityFinder;
  private final IdTokenFactory idTokenFactory;

  public JwtController(SignatureFactory signatureFactory, Tokens tokens, JwtKeyStore keyStore, IdentityFinder identityFinder, IdTokenFactory idTokenFactory) {
    this.signatureFactory = signatureFactory;
    this.tokens = tokens;
    this.keyStore = keyStore;
    this.identityFinder = identityFinder;
    this.idTokenFactory = idTokenFactory;
  }

  @Override
  public Response handleAsOf(Request request, DateTime instant) {
    String assertion = request.param("assertion");
    String scope = request.param("scope") == null ? "" : request.param("scope");

    List<String> parts = Lists.newArrayList(Splitter.on(".").split(assertion));

    // Error should be returned if any of the header parts is missing
    if (parts.size() != 3) {
      return OAuthError.invalidRequest("bad request was provided");
    }

    String headerContent = parts.get(0);
    String headerValue = new String(BaseEncoding.base64Url().decode(headerContent));
    String content = new String(BaseEncoding.base64Url().decode(parts.get(1)));

    byte[] signatureValue = BaseEncoding.base64Url().decode(parts.get(2));

    Jwt.Header header = gson.fromJson(headerValue, Jwt.Header.class);
    Jwt.ClaimSet claimSet = gson.fromJson(content, Jwt.ClaimSet.class);

    Optional<Pem.Block> possibleResponse = keyStore.findKey(header, claimSet);

    if (!possibleResponse.isPresent()) {
      return OAuthError.invalidGrant("unknown claims");
    }

    Pem.Block serviceAccountKey = possibleResponse.get();

    Optional<Signature> optSignature = signatureFactory.createSignature(signatureValue, header);

    // Unknown signture was provided, so we are returning request as invalid.
    if (!optSignature.isPresent()) {
      return OAuthError.invalidRequest("Unknown signature was provided.");
    }

    byte[] headerAndContentAsBytes = String.format("%s.%s", parts.get(0), parts.get(1)).getBytes();

    if (!optSignature.get().verifyWithPrivateKey(headerAndContentAsBytes, serviceAccountKey)) {
      return OAuthError.invalidGrant("Invalid signature was provided.");
    }

    Map<String, String> params = new Params().parse(request, "assertion", "scope");

    Optional<Identity> possibleIdentity = identityFinder.findIdentity(new FindIdentityRequest(claimSet.iss, GrantType.JWT, instant, params, ""));

    if (!possibleIdentity.isPresent()) {
      return OAuthError.invalidGrant("unknown identity");
    }

    Identity identity = possibleIdentity.get();

    Set<String> scopes = Sets.newTreeSet(Splitter.on(" ").omitEmptyStrings().split(scope));
    Client client = new Client(claimSet.iss, "", "", Collections.<String>emptySet(), false);

    TokenResponse response = tokens.issueToken(
            newTokenRequest()
                    .grantType(GrantType.JWT)
                    .client(client)
                    .identity(identity)
                    .scopes(scopes)
                    .when(instant)
                    .params(params)
                    .build());

    if (!response.isSuccessful()) {
      return OAuthError.invalidRequest("tokens issuing is temporary unavailable");
    }

    BearerToken accessToken = response.accessToken;
    Optional<String> possibleIdToken = idTokenFactory.create(request.header("Host"), client.id, identity, accessToken.ttlSeconds(instant), instant);
    if (possibleIdToken.isPresent()) {
      return new BearerTokenResponse(accessToken.value, accessToken.ttlSeconds(instant), accessToken.scopes, response.refreshToken, possibleIdToken.get());
    }
    return new BearerTokenResponse(accessToken.value, accessToken.ttlSeconds(instant), accessToken.scopes, response.refreshToken);
  }
}