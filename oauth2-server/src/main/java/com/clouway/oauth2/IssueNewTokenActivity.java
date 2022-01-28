package com.clouway.oauth2;

import com.clouway.friendlyserve.Request;
import com.clouway.friendlyserve.Response;
import com.clouway.oauth2.client.Client;
import com.clouway.oauth2.common.DateTime;
import com.clouway.oauth2.token.BearerToken;
import com.clouway.oauth2.token.GrantType;
import com.clouway.oauth2.token.IdTokenFactory;
import com.clouway.oauth2.token.TokenResponse;
import com.clouway.oauth2.token.Tokens;
import com.clouway.oauth2.token.Identity;
import com.google.common.base.Optional;

import java.util.Map;
import java.util.Set;

import static com.clouway.oauth2.token.TokenRequest.newTokenRequest;


/**
 * IssueNewTokenActivity is representing the activity which is performed for issuing of new token.
 *
 * @author Miroslav Genov (miroslav.genov@clouway.com)
 */
class IssueNewTokenActivity implements AuthorizedIdentityActivity {
  private final Tokens tokens;
  private final IdTokenFactory idTokenFactory;

  IssueNewTokenActivity(Tokens tokens, IdTokenFactory idTokenFactory) {
    this.tokens = tokens;
    this.idTokenFactory = idTokenFactory;
  }

  @Override
  public Response execute(Client client, Identity identity, Set<String> scopes, Request request, DateTime instant, Map<String, String> params) {
    TokenResponse response = tokens.issueToken(
            newTokenRequest()
                    .grantType(GrantType.AUTHORIZATION_CODE)
                    .client(client)
                    .identity(identity)
                    .scopes(scopes)
                    .when(instant)
                    .params(params)
                    .build());

    if (!response.isSuccessful()) {
      return OAuthError.invalidRequest("Token cannot be issued.");
    }

    BearerToken accessToken = response.accessToken;
    Optional<String> possibleIdToken = idTokenFactory.create(
            request.header("Host"),
            client.id,
            identity,
            accessToken.ttlSeconds(instant),
            instant
    );

    if (possibleIdToken.isPresent()) {
      return new BearerTokenResponse(accessToken.value, accessToken.ttlSeconds(instant), accessToken.scopes, response.refreshToken, possibleIdToken.get());
    }

    return new BearerTokenResponse(accessToken.value, accessToken.ttlSeconds(instant), accessToken.scopes, response.refreshToken);
  }
}
