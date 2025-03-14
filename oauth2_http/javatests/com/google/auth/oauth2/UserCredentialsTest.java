/*
 * Copyright 2015, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.auth.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.client.json.GenericJson;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.client.util.Clock;
import com.google.auth.RequestMetadataCallback;
import com.google.auth.TestUtils;
import com.google.auth.http.AuthHttpConstants;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test case for {@link UserCredentials}. */
@RunWith(JUnit4.class)
public class UserCredentialsTest extends BaseSerializationTest {

  static final String CLIENT_SECRET = "jakuaL9YyieakhECKL2SwZcu";
  static final String CLIENT_ID = "ya29.1.AADtN_UtlxN3PuGAxrN2XQnZTVRvDyVWnYq4I6dws";
  static final String REFRESH_TOKEN = "1/Tl6awhpFjkMkSJoj1xsli0H2eL5YsMgU_NKPY2TyGWY";
  private static final String ACCESS_TOKEN = "1/MkSJoj1xsli0AccessToken_NKPY2";
  private static final String QUOTA_PROJECT = "sample-quota-project-id";
  private static final Collection<String> SCOPES = Collections.singletonList("dummy.scope");
  private static final URI CALL_URI = URI.create("http://googleapis.com/testapi/v1/foo");
  public static final String DEFAULT_ID_TOKEN =
      "eyJhbGciOiJSUzI1NiIsImtpZCI6ImRmMzc1ODkwOGI3OTIyO"
          + "TNhZDk3N2EwYjk5MWQ5OGE3N2Y0ZWVlY2QiLCJ0eXAiOiJKV1QifQ.eyJhdWQiOiJodHRwczovL2Zvby5iYXIiL"
          + "CJhenAiOiIxMDIxMDE1NTA4MzQyMDA3MDg1NjgiLCJleHAiOjE1NjQ0NzUwNTEsImlhdCI6MTU2NDQ3MTQ1MSwi"
          + "aXNzIjoiaHR0cHM6Ly9hY2NvdW50cy5nb29nbGUuY29tIiwic3ViIjoiMTAyMTAxNTUwODM0MjAwNzA4NTY4In0"
          + ".redacted";

  @Test(expected = IllegalStateException.class)
  public void constructor_accessAndRefreshTokenNull_throws() {
    UserCredentials.newBuilder().setClientId(CLIENT_ID).setClientSecret(CLIENT_SECRET).build();
  }

  @Test
  public void constructor() {
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setQuotaProjectId(QUOTA_PROJECT)
            .build();
    assertEquals(CLIENT_ID, credentials.getClientId());
    assertEquals(CLIENT_SECRET, credentials.getClientSecret());
    assertEquals(REFRESH_TOKEN, credentials.getRefreshToken());
    assertEquals(QUOTA_PROJECT, credentials.getQuotaProjectId());
  }

  @Test
  public void createScoped_same() {
    UserCredentials userCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .build();
    assertSame(userCredentials, userCredentials.createScoped(SCOPES));
  }

  @Test
  public void createScopedRequired_false() {
    UserCredentials userCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .build();
    assertFalse(userCredentials.createScopedRequired());
  }

  @Test
  public void fromJson_hasAccessToken() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);
    GenericJson json = writeUserJson(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN, null, null);

    GoogleCredentials credentials = UserCredentials.fromJson(json, transportFactory);

    Map<String, List<String>> metadata = credentials.getRequestMetadata(CALL_URI);
    TestUtils.assertContainsBearerToken(metadata, ACCESS_TOKEN);
  }

  @Test
  public void fromJson_hasTokenUri() throws IOException {
    String tokenUrl = "token.url.xyz";
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);
    GenericJson json = writeUserJson(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN, null, tokenUrl);

    UserCredentials credentials = UserCredentials.fromJson(json, transportFactory);
    assertEquals(URI.create(tokenUrl), credentials.toBuilder().getTokenServerUri());
  }

  @Test
  public void fromJson_emptyTokenUri() throws IOException {
    String tokenUrl = "";
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);
    GenericJson json = writeUserJson(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN, null, tokenUrl);

    UserCredentials credentials = UserCredentials.fromJson(json, transportFactory);
    assertEquals(OAuth2Utils.TOKEN_SERVER_URI, credentials.toBuilder().getTokenServerUri());
  }

  @Test
  public void fromJson_hasQuotaProjectId() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);
    GenericJson json = writeUserJson(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN, QUOTA_PROJECT, null);

    GoogleCredentials credentials = UserCredentials.fromJson(json, transportFactory);

    Map<String, List<String>> metadata = credentials.getRequestMetadata(CALL_URI);
    assertTrue(metadata.containsKey(GoogleCredentials.QUOTA_PROJECT_ID_HEADER_KEY));
    assertEquals(
        metadata.get(GoogleCredentials.QUOTA_PROJECT_ID_HEADER_KEY),
        Collections.singletonList(QUOTA_PROJECT));
  }

  @Test
  public void getRequestMetadata_initialToken_hasAccessToken() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    UserCredentials userCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(transportFactory)
            .build();

    Map<String, List<String>> metadata = userCredentials.getRequestMetadata(CALL_URI);

    TestUtils.assertContainsBearerToken(metadata, ACCESS_TOKEN);
  }

  @Test
  public void getRequestMetadata_initialTokenRefreshed_throws() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    UserCredentials userCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(transportFactory)
            .build();

    try {
      userCredentials.refresh();
      fail("Should not be able to refresh without refresh token.");
    } catch (IllegalStateException expected) {
      // Expected
    }
  }

  @Test
  public void getRequestMetadata_fromRefreshToken_hasAccessToken() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);
    UserCredentials userCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setHttpTransportFactory(transportFactory)
            .build();

    Map<String, List<String>> metadata = userCredentials.getRequestMetadata(CALL_URI);

    TestUtils.assertContainsBearerToken(metadata, ACCESS_TOKEN);
  }

  @Test
  public void getRequestMetadata_customTokenServer_hasAccessToken() throws IOException {
    final URI TOKEN_SERVER = URI.create("https://foo.com/bar");
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);
    transportFactory.transport.setTokenServerUri(TOKEN_SERVER);
    UserCredentials userCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setHttpTransportFactory(transportFactory)
            .setTokenServerUri(TOKEN_SERVER)
            .build();

    Map<String, List<String>> metadata = userCredentials.getRequestMetadata(CALL_URI);

    TestUtils.assertContainsBearerToken(metadata, ACCESS_TOKEN);
  }

  @Test
  public void equals_true() throws IOException {
    final URI tokenServer = URI.create("https://foo.com/bar");
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(transportFactory)
            .setTokenServerUri(tokenServer)
            .setQuotaProjectId(QUOTA_PROJECT)
            .build();
    UserCredentials otherCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(transportFactory)
            .setTokenServerUri(tokenServer)
            .setQuotaProjectId(QUOTA_PROJECT)
            .build();
    assertTrue(credentials.equals(otherCredentials));
    assertTrue(otherCredentials.equals(credentials));
  }

  @Test
  public void equals_false_clientId() throws IOException {
    final URI tokenServer1 = URI.create("https://foo1.com/bar");
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setTokenServerUri(tokenServer1)
            .build();
    UserCredentials otherCredentials =
        UserCredentials.newBuilder()
            .setClientId("other client id")
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setTokenServerUri(tokenServer1)
            .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
  }

  @Test
  public void equals_false_clientSecret() throws IOException {
    final URI tokenServer1 = URI.create("https://foo1.com/bar");
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setTokenServerUri(tokenServer1)
            .build();
    UserCredentials otherCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret("other client secret")
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setTokenServerUri(tokenServer1)
            .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
  }

  @Test
  public void equals_false_refreshToken() throws IOException {
    final URI tokenServer1 = URI.create("https://foo1.com/bar");
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    OAuth2Credentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setTokenServerUri(tokenServer1)
            .build();
    OAuth2Credentials otherCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken("otherRefreshToken")
            .setAccessToken(accessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setTokenServerUri(tokenServer1)
            .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
  }

  @Test
  public void equals_false_accessToken() throws IOException {
    final URI tokenServer1 = URI.create("https://foo1.com/bar");
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    AccessToken otherAccessToken = new AccessToken("otherAccessToken", null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setTokenServerUri(tokenServer1)
            .build();
    UserCredentials otherCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(otherAccessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setTokenServerUri(tokenServer1)
            .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
    assertNotEquals(credentials.hashCode(), otherAccessToken.hashCode());
  }

  @Test
  public void equals_false_transportFactory() throws IOException {
    final URI tokenServer1 = URI.create("https://foo1.com/bar");
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    MockTokenServerTransportFactory serverTransportFactory = new MockTokenServerTransportFactory();
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setTokenServerUri(tokenServer1)
            .build();
    UserCredentials otherCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(serverTransportFactory)
            .setTokenServerUri(tokenServer1)
            .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
  }

  @Test
  public void equals_false_tokenServer() throws IOException {
    final URI tokenServer1 = URI.create("https://foo1.com/bar");
    final URI tokenServer2 = URI.create("https://foo2.com/bar");
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setTokenServerUri(tokenServer1)
            .build();
    UserCredentials otherCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setTokenServerUri(tokenServer2)
            .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
  }

  @Test
  public void equals_false_quotaProjectId() throws IOException {
    final String quotaProject1 = "sample-id-1";
    final String quotaProject2 = "sample-id-2";
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setQuotaProjectId(quotaProject1)
            .build();
    UserCredentials otherCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(httpTransportFactory)
            .setQuotaProjectId(quotaProject2)
            .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
  }

  @Test
  public void toString_containsFields() throws IOException {
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    final URI tokenServer = URI.create("https://foo.com/bar");
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(transportFactory)
            .setTokenServerUri(tokenServer)
            .setQuotaProjectId(QUOTA_PROJECT)
            .build();

    String expectedToString =
        String.format(
            "UserCredentials{requestMetadata=%s, temporaryAccess=%s, clientId=%s, refreshToken=%s, "
                + "tokenServerUri=%s, transportFactoryClassName=%s, quotaProjectId=%s}",
            ImmutableMap.of(
                AuthHttpConstants.AUTHORIZATION,
                ImmutableList.of(OAuth2Utils.BEARER_PREFIX + accessToken.getTokenValue())),
            accessToken.toString(),
            CLIENT_ID,
            REFRESH_TOKEN,
            tokenServer,
            MockTokenServerTransportFactory.class.getName(),
            QUOTA_PROJECT);
    assertEquals(expectedToString, credentials.toString());
  }

  @Test
  public void hashCode_equals() throws IOException {
    final URI tokenServer = URI.create("https://foo.com/bar");
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(transportFactory)
            .setTokenServerUri(tokenServer)
            .setQuotaProjectId(QUOTA_PROJECT)
            .build();
    UserCredentials otherCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(transportFactory)
            .setTokenServerUri(tokenServer)
            .setQuotaProjectId(QUOTA_PROJECT)
            .build();
    assertEquals(credentials.hashCode(), otherCredentials.hashCode());
  }

  @Test
  public void serialize() throws IOException, ClassNotFoundException {
    final URI tokenServer = URI.create("https://foo.com/bar");
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(accessToken)
            .setHttpTransportFactory(transportFactory)
            .setTokenServerUri(tokenServer)
            .build();
    UserCredentials deserializedCredentials = serializeAndDeserialize(credentials);
    assertEquals(credentials, deserializedCredentials);
    assertEquals(credentials.hashCode(), deserializedCredentials.hashCode());
    assertEquals(credentials.toString(), deserializedCredentials.toString());
    assertSame(deserializedCredentials.clock, Clock.SYSTEM);
  }

  @Test
  public void fromStream_nullTransport_throws() throws IOException {
    InputStream stream = new ByteArrayInputStream("foo".getBytes());
    try {
      UserCredentials.fromStream(stream, null);
      fail("Should throw if HttpTransportFactory is null");
    } catch (NullPointerException expected) {
      // Expected
    }
  }

  @Test
  public void fromStream_nullStream_throws() throws IOException {
    MockHttpTransportFactory transportFactory = new MockHttpTransportFactory();
    try {
      UserCredentials.fromStream(null, transportFactory);
      fail("Should throw if InputStream is null");
    } catch (NullPointerException expected) {
      // Expected
    }
  }

  @Test
  public void fromStream_user_providesToken() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);
    InputStream userStream =
        writeUserStream(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN, QUOTA_PROJECT);

    UserCredentials credentials = UserCredentials.fromStream(userStream, transportFactory);

    assertNotNull(credentials);
    Map<String, List<String>> metadata = credentials.getRequestMetadata(CALL_URI);
    TestUtils.assertContainsBearerToken(metadata, ACCESS_TOKEN);
  }

  @Test
  public void fromStream_userNoClientId_throws() throws IOException {
    InputStream userStream = writeUserStream(null, CLIENT_SECRET, REFRESH_TOKEN, QUOTA_PROJECT);

    testFromStreamException(userStream, "client_id");
  }

  @Test
  public void fromStream_userNoClientSecret_throws() throws IOException {
    InputStream userStream = writeUserStream(CLIENT_ID, null, REFRESH_TOKEN, QUOTA_PROJECT);

    testFromStreamException(userStream, "client_secret");
  }

  @Test
  public void fromStream_userNoRefreshToken_throws() throws IOException {
    InputStream userStream = writeUserStream(CLIENT_ID, CLIENT_SECRET, null, QUOTA_PROJECT);

    testFromStreamException(userStream, "refresh_token");
  }

  @Test
  public void saveUserCredentials_saved_throws() throws IOException {
    UserCredentials userCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .build();
    File file = File.createTempFile("GOOGLE_APPLICATION_CREDENTIALS", null, null);
    file.deleteOnExit();

    String filePath = file.getAbsolutePath();
    userCredentials.save(filePath);
  }

  @Test
  public void saveAndRestoreUserCredential_saveAndRestored_throws() throws IOException {
    UserCredentials userCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .build();

    File file = File.createTempFile("GOOGLE_APPLICATION_CREDENTIALS", null, null);
    file.deleteOnExit();

    String filePath = file.getAbsolutePath();

    userCredentials.save(filePath);

    FileInputStream inputStream = new FileInputStream(new File(filePath));

    UserCredentials restoredCredentials = UserCredentials.fromStream(inputStream);

    assertEquals(userCredentials.getClientId(), restoredCredentials.getClientId());
    assertEquals(userCredentials.getClientSecret(), restoredCredentials.getClientSecret());
    assertEquals(userCredentials.getRefreshToken(), restoredCredentials.getRefreshToken());
  }

  @Test
  public void getRequestMetadataSetsQuotaProjectId() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);

    UserCredentials userCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setQuotaProjectId("my-quota-project-id")
            .setHttpTransportFactory(transportFactory)
            .build();

    Map<String, List<String>> metadata = userCredentials.getRequestMetadata(CALL_URI);
    assertTrue(metadata.containsKey("x-goog-user-project"));
    List<String> headerValues = metadata.get("x-goog-user-project");
    assertEquals(1, headerValues.size());
    assertEquals("my-quota-project-id", headerValues.get(0));
  }

  @Test
  public void getRequestMetadataNoQuotaProjectId() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);

    UserCredentials userCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setHttpTransportFactory(transportFactory)
            .build();

    Map<String, List<String>> metadata = userCredentials.getRequestMetadata(CALL_URI);
    assertFalse(metadata.containsKey("x-goog-user-project"));
  }

  @Test
  public void getRequestMetadataWithCallback() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);

    UserCredentials userCredentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setQuotaProjectId("my-quota-project-id")
            .setHttpTransportFactory(transportFactory)
            .build();
    final Map<String, List<String>> plainMetadata = userCredentials.getRequestMetadata(CALL_URI);
    final AtomicBoolean success = new AtomicBoolean(false);
    userCredentials.getRequestMetadata(
        null,
        null,
        new RequestMetadataCallback() {
          @Override
          public void onSuccess(Map<String, List<String>> metadata) {
            assertEquals(plainMetadata, metadata);
            success.set(true);
          }

          @Override
          public void onFailure(Throwable exception) {
            fail("Should not throw a failure.");
          }
        });

    assertTrue("Should have run onSuccess() callback", success.get());
  }

  @Test
  public void IdTokenCredentials_WithUserEmailScope_success() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    String refreshToken = MockTokenServerTransport.REFRESH_TOKEN_WITH_USER_SCOPE;

    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(refreshToken, ACCESS_TOKEN);
    InputStream userStream = writeUserStream(CLIENT_ID, CLIENT_SECRET, refreshToken, QUOTA_PROJECT);

    UserCredentials credentials = UserCredentials.fromStream(userStream, transportFactory);
    credentials.refresh();

    assertEquals(ACCESS_TOKEN, credentials.getAccessToken().getTokenValue());

    // verify access token request metrics headers
    Map<String, List<String>> accessTokenRequestHeader =
        transportFactory.transport.getRequest().getHeaders();
    com.google.auth.oauth2.TestUtils.validateMetricsHeader(
        accessTokenRequestHeader, "untracked", "u");

    IdTokenCredentials tokenCredential =
        IdTokenCredentials.newBuilder().setIdTokenProvider(credentials).build();

    assertNull(tokenCredential.getAccessToken());
    assertNull(tokenCredential.getIdToken());

    // trigger the refresh like it would happen during a request build
    tokenCredential.getRequestMetadata(CALL_URI);

    assertEquals(DEFAULT_ID_TOKEN, tokenCredential.getAccessToken().getTokenValue());
    assertEquals(DEFAULT_ID_TOKEN, tokenCredential.getIdToken().getTokenValue());

    // verify ID token request metrics headers, same as access token request
    Map<String, List<String>> idTokenRequestHeader =
        transportFactory.transport.getRequest().getHeaders();
    com.google.auth.oauth2.TestUtils.validateMetricsHeader(idTokenRequestHeader, "untracked", "u");
  }

  @Test
  public void IdTokenCredentials_NoRetry_RetryableStatus_throws() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    String refreshToken = MockTokenServerTransport.REFRESH_TOKEN_WITH_USER_SCOPE;

    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(refreshToken, ACCESS_TOKEN);
    InputStream userStream = writeUserStream(CLIENT_ID, CLIENT_SECRET, refreshToken, QUOTA_PROJECT);

    MockLowLevelHttpResponse response408 = new MockLowLevelHttpResponse().setStatusCode(408);
    MockLowLevelHttpResponse response429 = new MockLowLevelHttpResponse().setStatusCode(429);

    UserCredentials credentials = UserCredentials.fromStream(userStream, transportFactory);

    try {
      transportFactory.transport.addResponseSequence(response408, response429);
      credentials.refresh();
      fail("Should not be able to use credential without exception.");
    } catch (GoogleAuthException ex) {
      assertTrue(ex.getMessage().contains("com.google.api.client.http.HttpResponseException: 408"));
      assertTrue(ex.isRetryable());
      assertEquals(0, ex.getRetryCount());
    }

    IdTokenCredentials tokenCredential =
        IdTokenCredentials.newBuilder().setIdTokenProvider(credentials).build();

    assertNull(tokenCredential.getAccessToken());
    assertNull(tokenCredential.getIdToken());

    // trigger the refresh like it would happen during a request build
    try {
      tokenCredential.getRequestMetadata(CALL_URI);
      fail("Should not be able to use credential without exception.");
    } catch (GoogleAuthException ex) {
      assertTrue(ex.getMessage().contains("com.google.api.client.http.HttpResponseException: 429"));
      assertTrue(ex.isRetryable());
      assertEquals(0, ex.getRetryCount());
    }
  }

  @Test
  public void idTokenWithAudience_non2xxError() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.setError(new IOException("404 Not Found"));
    String refreshToken = MockTokenServerTransport.REFRESH_TOKEN_WITH_USER_SCOPE;
    InputStream userStream = writeUserStream(CLIENT_ID, CLIENT_SECRET, refreshToken, QUOTA_PROJECT);

    UserCredentials credentials = UserCredentials.fromStream(userStream, transportFactory);

    IdTokenCredentials tokenCredential =
        IdTokenCredentials.newBuilder().setIdTokenProvider(credentials).build();

    assertThrows(GoogleAuthException.class, tokenCredential::refresh);
  }

  @Test
  public void refreshAccessToken_4xx_5xx_NonRetryableFails() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    String refreshToken = MockTokenServerTransport.REFRESH_TOKEN_WITH_USER_SCOPE;

    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(refreshToken, ACCESS_TOKEN);
    InputStream userStream = writeUserStream(CLIENT_ID, CLIENT_SECRET, refreshToken, QUOTA_PROJECT);

    UserCredentials credentials = UserCredentials.fromStream(userStream, transportFactory);

    for (int status = 400; status < 600; status++) {
      if (OAuth2Utils.TOKEN_ENDPOINT_RETRYABLE_STATUS_CODES.contains(status)) {
        continue;
      }

      MockLowLevelHttpResponse mockResponse = new MockLowLevelHttpResponse().setStatusCode(status);
      try {
        transportFactory.transport.addResponseSequence(mockResponse);
        credentials.refresh();
        fail("Should not be able to use credential without exception.");
      } catch (GoogleAuthException ex) {
        assertFalse(ex.isRetryable());
        assertEquals(0, ex.getRetryCount());
      }
    }
  }

  @Test
  public void IdTokenCredentials_NoUserEmailScope_throws() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);
    InputStream userStream =
        writeUserStream(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN, QUOTA_PROJECT);

    UserCredentials credentials = UserCredentials.fromStream(userStream, transportFactory);

    IdTokenCredentials tokenCredential =
        IdTokenCredentials.newBuilder().setIdTokenProvider(credentials).build();

    String expectedMessageContent =
        "UserCredentials can obtain an id token only when authenticated through"
            + " gcloud running 'gcloud auth login --update-adc' or 'gcloud auth application-default"
            + " login'. The latter form would not work for Cloud Run, but would still generate an"
            + " id token.";

    try {
      tokenCredential.refresh();
      fail("Should not be able to use credential without exception.");
    } catch (IOException expected) {
      assertTrue(expected.getMessage().equals(expectedMessageContent));
    }
  }

  @Test
  public void userCredentials_toBuilder_copyEveryAttribute() {
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessToken(new AccessToken(ACCESS_TOKEN, new Date()))
            .setHttpTransportFactory(httpTransportFactory)
            .setTokenServerUri(URI.create("https://foo1.com/bar"))
            .setQuotaProjectId(QUOTA_PROJECT)
            .setExpirationMargin(Duration.of(10, ChronoUnit.SECONDS))
            .setRefreshMargin(Duration.of(12, ChronoUnit.MINUTES))
            .build();

    UserCredentials otherCredentials = credentials.toBuilder().build();
    assertEquals(credentials, otherCredentials);
  }

  static GenericJson writeUserJson(
      String clientId,
      String clientSecret,
      String refreshToken,
      String quotaProjectId,
      String tokenUrl) {
    GenericJson json = new GenericJson();
    if (clientId != null) {
      json.put("client_id", clientId);
    }
    if (clientSecret != null) {
      json.put("client_secret", clientSecret);
    }
    if (refreshToken != null) {
      json.put("refresh_token", refreshToken);
    }
    if (quotaProjectId != null) {
      json.put("quota_project_id", quotaProjectId);
    }
    if (tokenUrl != null) {
      json.put("token_uri", tokenUrl);
    }
    json.put("type", GoogleCredentials.USER_FILE_TYPE);
    return json;
  }

  static InputStream writeUserStream(
      String clientId, String clientSecret, String refreshToken, String quotaProjectId)
      throws IOException {
    GenericJson json = writeUserJson(clientId, clientSecret, refreshToken, quotaProjectId, null);
    return TestUtils.jsonToInputStream(json);
  }

  private static void testFromStreamException(InputStream stream, String expectedMessageContent) {
    try {
      UserCredentials.fromStream(stream);
      fail(
          String.format(
              "Should throw exception with message containing '%s'", expectedMessageContent));
    } catch (IOException expected) {
      assertTrue(expected.getMessage().contains(expectedMessageContent));
    }
  }
}
