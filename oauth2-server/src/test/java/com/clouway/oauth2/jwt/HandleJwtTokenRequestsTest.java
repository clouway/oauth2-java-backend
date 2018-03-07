package com.clouway.oauth2.jwt;

import com.clouway.friendlyserve.Request;
import com.clouway.friendlyserve.Response;
import com.clouway.friendlyserve.testing.RsPrint;
import com.clouway.oauth2.DateTime;
import com.clouway.oauth2.Identity;
import com.clouway.oauth2.client.Client;
import com.clouway.oauth2.client.JwtKeyStore;
import com.clouway.oauth2.jws.Pem;
import com.clouway.oauth2.jws.Signature;
import com.clouway.oauth2.jws.SignatureFactory;
import com.clouway.oauth2.token.GrantType;
import com.clouway.oauth2.token.IdTokenFactory;
import com.clouway.oauth2.token.TokenRequest;
import com.clouway.oauth2.token.TokenResponse;
import com.clouway.oauth2.token.Tokens;
import com.clouway.oauth2.user.FindIdentityRequest;
import com.clouway.oauth2.user.IdentityFinder;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static com.clouway.friendlyserve.testing.FakeRequest.aNewRequest;
import static com.clouway.oauth2.BearerTokenBuilder.aNewToken;
import static com.clouway.oauth2.IdentityBuilder.aNewIdentity;
import static com.clouway.oauth2.token.TokenRequest.newTokenRequest;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * @author Miroslav Genov (miroslav.genov@clouway.com)
 */
public class HandleJwtTokenRequestsTest {

  @Rule
  public JUnitRuleMockery context = new JUnitRuleMockery();

  private final String body = "eyJpc3MiOiJ4eHhAZGV2ZWxvcGVyLmNvbSIsInNjb3BlIjoidGVzdDEgdGVzdDIiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjkwMDIvb2F1dGgyL3Rva2VuIiwiZXhwIjoxNDYxMjM4OTQ4LCJpYXQiOjE0NjEyMzUzNDgsInN1YiI6InVzZXJAZXhhbXBsZS5jb20iLCJwcm4iOiJ1c2VyQGV4YW1wbGUuY29tIn0";
  private final String signature = "WBAzzss3J8Ea6-xxOCVS2OZ2HoqpiLdfCLhIJEevaPck377qTpiM__lHta_S8dSCuTl5FjREqixIiwGrJVJEIkfExUwS5YWekdJRniSKdqLjmXussePaCSgco3reJDqNcRCGiv9DSLH0GfZFdv11Ik5nyaHjNnS4ykEi76guaY8-T3uVFjOH4e2o8Wm0vBbq9hzo9UHdgnsI2BLrzDVoydGWM7uZW8MQNKTuGWY_Ywyj1hilr9rw4yy2FvBe7G-56qaq8--IlVNZ6ocJX2dYhZPqDtZUYwLRqwFyM_F53Kt81I8Qht6HBgH-fgrfbd7Ms67BeLGsupFvuM9sF-hGOQ";

  @Mock
  JwtKeyStore repository;

  @Mock
  Tokens tokens;

  @Mock
  SignatureFactory signatureFactory;

  @Mock
  IdentityFinder identityFinder;

  @Mock
  IdTokenFactory idTokenFactory;

  private JwtController controller;

  @Before
  public void setUp() {
    controller = new JwtController(signatureFactory, tokens, repository, identityFinder, idTokenFactory);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void happyPath() throws IOException {
    final Signature anySignatureThatWillVerifies = context.mock(Signature.class);
    final DateTime anyInstantTime = new DateTime();

    context.checking(new Expectations() {{
      oneOf(repository).findKey(with(any(Jwt.Header.class)), with(any(Jwt.ClaimSet.class)));
      will(returnValue(Optional.of(new Pem.Block("", ImmutableMap.<String, String>of(), new byte[]{}))));

      oneOf(signatureFactory).createSignature(with(any(byte[].class)), with(any(Jwt.Header.class)));
      will(returnValue(Optional.of(anySignatureThatWillVerifies)));

      oneOf(anySignatureThatWillVerifies).verifyWithPrivateKey(with(any(byte[].class)), with(any(Pem.Block.class)));
      will(returnValue(true));

      oneOf(identityFinder).findIdentity(with(any(FindIdentityRequest.class)));
      will(returnValue(Optional.of(aNewIdentity().build())));

      oneOf(idTokenFactory).create(with(any(String.class)),with(any(String.class)),with(any(Identity.class)),with(any(Long.class)),with(any(DateTime.class)));
      will(returnValue(Optional.of("::id_token::")));

      oneOf(tokens).issueToken(with(any(TokenRequest.class)));
      will(returnValue(new TokenResponse(true, aNewToken().withValue("::access_token::").expiresAt(anyInstantTime.plusSeconds(1000)).build(), "::refresh_token::")));
    }});

    Response response = controller.handleAsOf(newJwtRequest(String.format("%s.%s.%s", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9", body, signature), "::host::"), anyInstantTime);
    String responseContent = new RsPrint(response).printBody();
    assertThat(responseContent, containsString("::access_token::"));
    assertThat(responseContent, containsString("::refresh_token::"));
    assertThat(responseContent, containsString("1000"));
    assertThat(responseContent,containsString("::id_token::"));
  }

  @Test
  public void idTokenWasNotGenerated() throws Exception {
    final Signature anySignatureThatWillVerifies = context.mock(Signature.class);
    final DateTime anyInstantTime = new DateTime();

    context.checking(new Expectations() {{
      oneOf(repository).findKey(with(any(Jwt.Header.class)), with(any(Jwt.ClaimSet.class)));
      will(returnValue(Optional.of(new Pem.Block("", ImmutableMap.<String, String>of(), new byte[]{}))));

      oneOf(signatureFactory).createSignature(with(any(byte[].class)), with(any(Jwt.Header.class)));
      will(returnValue(Optional.of(anySignatureThatWillVerifies)));

      oneOf(anySignatureThatWillVerifies).verifyWithPrivateKey(with(any(byte[].class)), with(any(Pem.Block.class)));
      will(returnValue(true));

      oneOf(identityFinder).findIdentity(with(any(FindIdentityRequest.class)));
      will(returnValue(Optional.of(aNewIdentity().build())));

      oneOf(idTokenFactory).create(with(any(String.class)),with(any(String.class)),with(any(Identity.class)),with(any(Long.class)),with(any(DateTime.class)));
      will(returnValue(Optional.absent()));

      oneOf(tokens).issueToken(with(any(TokenRequest.class)));
      will(returnValue(new TokenResponse(true, aNewToken().withValue("::access_token::").expiresAt(anyInstantTime.plusSeconds(1000)).build(), "::refresh_token::")));
    }});

    Response response = controller.handleAsOf(newJwtRequest(String.format("%s.%s.%s", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9", body, signature), "::host::"), anyInstantTime);
    String responseContent = new RsPrint(response).printBody();
    assertThat(responseContent, containsString("::access_token::"));
    assertThat(responseContent, containsString("::refresh_token::"));
    assertThat(responseContent, containsString("1000"));
    assertFalse(responseContent.contains("id_token"));
  }

  @Test
  public void identityNotFound() throws Exception {
    final Signature anySignatureThatWillVerifies = context.mock(Signature.class);
    final DateTime anyInstantTime = new DateTime();

    context.checking(new Expectations() {{
      oneOf(repository).findKey(with(any(Jwt.Header.class)), with(any(Jwt.ClaimSet.class)));
      will(returnValue(Optional.of(new Pem.Block("", ImmutableMap.<String, String>of(), new byte[]{}))));

      oneOf(signatureFactory).createSignature(with(any(byte[].class)), with(any(Jwt.Header.class)));
      will(returnValue(Optional.of(anySignatureThatWillVerifies)));

      oneOf(anySignatureThatWillVerifies).verifyWithPrivateKey(with(any(byte[].class)), with(any(Pem.Block.class)));
      will(returnValue(true));

      oneOf(identityFinder).findIdentity(with(any(FindIdentityRequest.class)));
      will(returnValue(Optional.absent()));

    }});

    Response response = controller.handleAsOf(newJwtRequest(String.format("%s.%s.%s", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9", body, signature), "::host::"), anyInstantTime);
    String responseContent = new RsPrint(response).printBody();
    assertThat(responseContent, containsString("unknown identity"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void scopesArePassed() throws IOException {
    final Signature anySignatureThatWillVerifies = context.mock(Signature.class);
    final DateTime anyInstantTime = new DateTime();
    final Client jwtClient = new Client("xxx@developer.com", "", "", Collections.<String>emptySet(), false);
    final Identity identity = aNewIdentity().withId("xxx@developer.com").build();

    context.checking(new Expectations() {{
      oneOf(repository).findKey(with(any(Jwt.Header.class)), with(any(Jwt.ClaimSet.class)));
      will(returnValue(Optional.of(new Pem.Block("", ImmutableMap.<String, String>of(), new byte[]{}))));

      oneOf(signatureFactory).createSignature(with(any(byte[].class)), with(any(Jwt.Header.class)));
      will(returnValue(Optional.of(anySignatureThatWillVerifies)));

      oneOf(anySignatureThatWillVerifies).verifyWithPrivateKey(with(any(byte[].class)), with(any(Pem.Block.class)));
      will(returnValue(true));

      oneOf(identityFinder).findIdentity(with(any(FindIdentityRequest.class)));
      will(returnValue(Optional.of(identity)));

      oneOf(tokens).issueToken(
              newTokenRequest()
                      .grantType(GrantType.JWT)
                      .client(jwtClient)
                      .identity(identity)
                      .scopes(Sets.newHashSet("CanDoX", "CanDoY"))
                      .when(anyInstantTime)
                      .params(Maps.<String, String>newHashMap())
                      .build());
      will(returnValue(new TokenResponse(true, aNewToken().build(), "")));

      oneOf(idTokenFactory).create(with(any(String.class)),with(any(String.class)),with(any(Identity.class)),with(any(Long.class)),with(any(DateTime.class)));
      will(returnValue(Optional.of("::id_token::")));
    }});

    controller.handleAsOf(newJwtRequest(String.format("%s.%s.%s", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9", body, signature), "CanDoX CanDoY", "::host::"), anyInstantTime);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void customRequestParamsArePassed() throws IOException {
    final Signature anySignatureThatWillVerifies = context.mock(Signature.class);
    final DateTime anyInstantTime = new DateTime();
    final Client jwtClient = new Client("xxx@developer.com", "", "", Collections.<String>emptySet(), false);
    final Identity identity = aNewIdentity().withId("xxx@developer.com").build();

    context.checking(new Expectations() {{
      oneOf(repository).findKey(with(any(Jwt.Header.class)), with(any(Jwt.ClaimSet.class)));
      will(returnValue(Optional.of(new Pem.Block("", ImmutableMap.<String, String>of(), new byte[]{}))));

      oneOf(signatureFactory).createSignature(with(any(byte[].class)), with(any(Jwt.Header.class)));
      will(returnValue(Optional.of(anySignatureThatWillVerifies)));

      oneOf(anySignatureThatWillVerifies).verifyWithPrivateKey(with(any(byte[].class)), with(any(Pem.Block.class)));
      will(returnValue(true));

      oneOf(identityFinder).findIdentity(with(any(FindIdentityRequest.class)));
      will(returnValue(Optional.of(identity)));

      oneOf(tokens).issueToken(
              newTokenRequest()
                      .grantType(GrantType.JWT)
                      .client(jwtClient)
                      .identity(identity)
                      .scopes(Sets.newHashSet("CanDoX", "CanDoY"))
                      .when(anyInstantTime)
                      .params(ImmutableMap.of("::index::", "::1::"))
                      .build());
      will(returnValue(new TokenResponse(true, aNewToken().build(), "")));

      oneOf(idTokenFactory).create(with(any(String.class)),with(any(String.class)),with(any(Identity.class)),with(any(Long.class)),with(any(DateTime.class)));
      will(returnValue(Optional.of("::id_token::")));
    }});

    controller.handleAsOf(aNewRequest().params(ImmutableMap.of("assertion", String.format("%s.%s.%s", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9", body, signature),
            "scope", "CanDoX CanDoY", "::index::", "::1::")).header("Host", "::host::").build(),
            anyInstantTime);
  }

  @Test
  public void assertionIsEmpty() throws IOException {
    Response response = controller.handleAsOf(newJwtRequest("", "::host::"), new DateTime());
    assertThat(new RsPrint(response).print(), containsString("invalid_request"));
  }

  @Test
  public void headerIsMissing() throws IOException {
    Response response = controller.handleAsOf(newJwtRequest(String.format("%s.%s", body, signature), "::host::"), new DateTime());
    assertThat(new RsPrint(response).printBody(), containsString("invalid_request"));
  }

  private Request newJwtRequest(String assertion, String hostHeader) {
    return aNewRequest().param("assertion", assertion).header("Host", hostHeader).build();
  }

  private Request newJwtRequest(String assertion, String scope, String hostHeader) {
    return aNewRequest().param("assertion", assertion).param("scope", scope).header("Host", hostHeader).build();
  }

}
