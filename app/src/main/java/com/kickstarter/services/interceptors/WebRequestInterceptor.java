package com.kickstarter.services.interceptors;

import android.net.Uri;

import com.kickstarter.libs.Build;
import com.kickstarter.libs.CurrentUserType;
import com.kickstarter.libs.InternalToolsType;
import com.kickstarter.libs.perimeterx.PerimeterXClientType;
import com.kickstarter.libs.utils.WebUtils;
import com.kickstarter.libs.utils.extensions.UriExt;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static com.kickstarter.libs.utils.ObjectUtils.isNotNull;

/**
 * Interceptor for web requests to Kickstarter, not API requests. Used by web views and the web client.
 */
public final class WebRequestInterceptor implements Interceptor {
  private final @NonNull CurrentUserType currentUser;
  private final @NonNull String endpoint;
  private final @NonNull InternalToolsType internalTools;
  private final @NonNull Build build;
  private final @NonNull PerimeterXClientType pxManager;

  public WebRequestInterceptor(final @NonNull CurrentUserType currentUser, final @NonNull String endpoint,
                               final @NonNull InternalToolsType internalTools, final @NonNull Build build,
                               final @NonNull PerimeterXClientType manager) {
    this.currentUser = currentUser;
    this.endpoint = endpoint;
    this.internalTools = internalTools;
    this.build = build;
    this.pxManager = manager;
  }

  @Override
  public Response intercept(final @NonNull Chain chain) throws IOException {
    return chain.proceed(request(chain.request()));
  }

  private Request request(final @NonNull Request initialRequest) {
    if (!shouldIntercept(initialRequest)) {
      return initialRequest;
    }

    final Request.Builder requestBuilder = initialRequest.newBuilder()
      .header("User-Agent", WebUtils.INSTANCE.userAgent(this.build));

    final String basicAuthorizationHeader = this.internalTools.basicAuthorizationHeader();
    if (this.currentUser.exists()) {
      requestBuilder.addHeader("Authorization", "token " + this.currentUser.getAccessToken());
    } else if (shouldAddBasicAuthorizationHeader(initialRequest) && isNotNull(basicAuthorizationHeader)) {
      requestBuilder.addHeader("Authorization", basicAuthorizationHeader);
    }

    this.pxManager.addHeaderTo(requestBuilder);

    return requestBuilder.build();
  }

  private boolean shouldIntercept(final @NonNull Request request) {
    return UriExt.isWebUri(Uri.parse(request.url().toString()), this.endpoint);
  }

  private boolean shouldAddBasicAuthorizationHeader(final @NonNull Request request) {
    if (this.currentUser.exists()) {
      return false;
    }
    final Uri initialRequestUri = Uri.parse(request.url().toString());
    return UriExt.isHivequeenUri(initialRequestUri, this.endpoint) || UriExt.isStagingUri(initialRequestUri, this.endpoint);
  }
}
