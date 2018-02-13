package com.clouway.oauth2.authorization;

import com.clouway.oauth2.codechallenge.CodeChallenge;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Miroslav Genov (miroslav.genov@clouway.com)
 */
public class AuthorizationBuilder {
  private String responseType = "code";
  private String code = "::code::";
  private String clientId = "::client id::";
  private Set<String> scopes = Collections.singleton("scope1");
  private Set<String> redirectURIs = Collections.singleton("redirectURI");
  private String identityId = "::any identity::";
  private CodeChallenge codeChallenge = new CodeChallenge("", "");
  private Map<String, String> params = Maps.newHashMap();

  public static AuthorizationBuilder newAuthorization() {
    return new AuthorizationBuilder();
  }

  public AuthorizationBuilder withCode(String code) {
    this.code = code;
    return this;
  }

  public AuthorizationBuilder withCodeChallenge(CodeChallenge codeChallenge) {
    this.codeChallenge = codeChallenge;
    return this;
  }

  public AuthorizationBuilder withId(String id) {
    this.identityId = id;
    return this;
  }

  public AuthorizationBuilder addParam(String name, String value) {
    this.params.put(name, value);
    return this;
  }

  public Authorization build() {
    return new Authorization(responseType, clientId, identityId, code, scopes, redirectURIs, codeChallenge, params);
  }

}