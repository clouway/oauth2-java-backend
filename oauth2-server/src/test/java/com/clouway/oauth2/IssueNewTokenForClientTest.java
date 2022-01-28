package com.clouway.oauth2;

import com.clouway.friendlyserve.Request;
import com.clouway.friendlyserve.Response;
import com.clouway.friendlyserve.testing.ParamRequest;
import com.clouway.friendlyserve.testing.RsPrint;
import com.clouway.oauth2.authorization.Authorization;
import com.clouway.oauth2.client.Client;
import com.clouway.oauth2.common.DateTime;
import com.clouway.oauth2.token.GrantType;
import com.clouway.oauth2.token.IdTokenFactory;
import com.clouway.oauth2.token.Identity;
import com.clouway.oauth2.token.TokenRequest;
import com.clouway.oauth2.token.TokenResponse;
import com.clouway.oauth2.token.Tokens;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;

import static com.clouway.oauth2.token.BearerTokenBuilder.aNewToken;
import static com.clouway.oauth2.token.IdentityBuilder.aNewIdentity;
import static com.clouway.oauth2.authorization.AuthorizationBuilder.newAuthorization;
import static com.clouway.oauth2.client.ClientBuilder.aNewClient;
import static com.clouway.oauth2.token.TokenRequest.newTokenRequest;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Miroslav Genov (miroslav.genov@clouway.com)
 */
public class IssueNewTokenForClientTest {

  @Rule
  public JUnitRuleMockery context = new JUnitRuleMockery();

  private Tokens tokens = context.mock(Tokens.class);

  private IdTokenFactory idTokenFactory = context.mock(IdTokenFactory.class);

  private Request request = context.mock(Request.class);

  private IssueNewTokenActivity controller = new IssueNewTokenActivity(tokens, idTokenFactory);

  @Test
  @SuppressWarnings("unchecked")
  public void happyPath() throws IOException {
    final DateTime anyTime = new DateTime();
    final Identity identity = aNewIdentity().withId("::user_id::").build();
    final Authorization anyAuhtorization = newAuthorization().build();

    context.checking(new Expectations() {{

      oneOf(request).header("Host");
      will(returnValue("::host::"));

      oneOf(idTokenFactory).create(with(any(String.class)), with(any(String.class)), with(any(Identity.class)),
              with(any(Long.class)), with(any(DateTime.class)));
      will(returnValue(Optional.of("::base64.encoded.idToken::")));

      oneOf(tokens).issueToken(with(any(TokenRequest.class)));
      will(returnValue(new TokenResponse(true, aNewToken().withValue("::token::").build(), "::refresh token::")));
    }});

    Response response = controller.execute(aNewClient().withId("::client id::").build(), identity, anyAuhtorization.scopes, request, anyTime, ImmutableMap.of("::index::", "::1::"));
    String body = new RsPrint(response).printBody();

    assertThat(body, containsString("id_token"));
    assertThat(body, containsString("::token::"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void idTokenWasNotAvailable() throws IOException {
    final DateTime anyTime = new DateTime();
    final Identity identity = aNewIdentity().withId("::user_id::").build();
    final Authorization anyAuhtorization = newAuthorization().build();

    context.checking(new Expectations() {{

      oneOf(request).header("Host");
      will(returnValue("::host::"));

      oneOf(idTokenFactory).create(with(any(String.class)), with(any(String.class)), with(any(Identity.class)),
              with(any(Long.class)), with(any(DateTime.class)));
      will(returnValue(Optional.absent()));

      oneOf(tokens).issueToken(with(any(TokenRequest.class)));
      will(returnValue(new TokenResponse(true, aNewToken().withValue("::token::").build(), "::refresh token::")));
    }});

    Response response = controller.execute(aNewClient().withId("::client id::").build(), identity, anyAuhtorization.scopes, request, anyTime, ImmutableMap.of("::index::", "::1::"));
    String body = new RsPrint(response).printBody();

    assertThat(body, not(containsString("id_token")));
    assertThat(body, containsString("::token::"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void tokenCannotBeIssued() throws Exception {
    final Client client = aNewClient().build();
    final DateTime anyTime = new DateTime();
    final Identity identity = aNewIdentity().withId("::user_id::").build();

    context.checking(new Expectations() {{
      oneOf(tokens).issueToken(with(any(TokenRequest.class)));
      will(returnValue(new TokenResponse(false, null, "")));
    }});

    Response response = controller.execute(client, identity, Collections.<String>emptySet(), new ParamRequest(Collections.<String, String>emptyMap()), anyTime, ImmutableMap.of("::index::", "::1::"));
    String body = new RsPrint(response).printBody();

    assertThat(response.status().code, is(HttpURLConnection.HTTP_BAD_REQUEST));
    assertThat(body, containsString("invalid_request"));
  }

  @Test
  public void parametersArePassed() throws Exception {
    final Client client = aNewClient().build();
    final DateTime anyTime = new DateTime();
    final Identity identity = aNewIdentity().withId("::user_id::").build();
    final Authorization authorization = newAuthorization().build();

    context.checking(new Expectations() {{
      oneOf(tokens).issueToken(
              newTokenRequest()
                      .grantType(GrantType.AUTHORIZATION_CODE)
                      .client(client)
                      .identity(identity)
                      .scopes(authorization.scopes)
                      .when(anyTime)
                      .params(ImmutableMap.of("::index::", "::1::"))
                      .build());
      will(returnValue(new TokenResponse(false, null, "")));
    }});

    controller.execute(client, identity, authorization.scopes, new ParamRequest(Collections.<String, String>emptyMap()), anyTime, ImmutableMap.of("::index::", "::1::"));
  }

}