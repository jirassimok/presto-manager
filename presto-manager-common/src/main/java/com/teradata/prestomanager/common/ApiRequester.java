/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teradata.prestomanager.common;

import org.eclipse.jetty.http.HttpMethod;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.net.URI;
import java.util.concurrent.Future;

import static java.util.Objects.requireNonNull;

public class ApiRequester
{
    private final UriBuilder uriBuilder;
    private final HttpMethod method;
    private final MultivaluedMap<String, Object> headers;
    private final String mediaType;
    @Nullable
    private Entity entity;

    private Client client;

    private ApiRequester(Client client, // TODO: Use AssistedInject to manage this
            UriBuilder uriBuilder, HttpMethod method,
            Entity entity, MultivaluedMap<String, Object> headers,
            String mediaType)
    {
        this.client = requireNonNull(client);
        this.uriBuilder = requireNonNull(uriBuilder);
        this.method = requireNonNull(method);
        this.headers = requireNonNull(headers);
        this.mediaType = requireNonNull(mediaType);

        this.entity = entity;
    }

    public Response send(URI uri)
    {
        Invocation invocation = createInvocation(uri);
        return invocation.invoke();
    }

    public Future<Response> sendAsync(URI uri)
    {
        Invocation invocation = createInvocation(uri);
        return invocation.submit();
    }

    private Invocation createInvocation(URI uri)
    {
        Invocation.Builder builder = client
                .target(uriBuilder.uri(uri).build())
                .request(mediaType)
                .headers(headers);

        Invocation invocation;
        if (entity == null) {
            invocation = builder.build(method.asString());
        }
        else {
            invocation = builder.build(method.asString(), entity);
        }

        return invocation;
    }

    public static Builder builder(Client client, Class<?> resource)
    {
        return new Builder(client, resource);
    }

    public static class Builder
    {
        private Client client;
        private UriBuilder uriBuilder;
        private Class<?> resource;
        private HttpMethod method;
        private Entity entity;
        private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        private String mediaType = MediaType.TEXT_PLAIN;

        private Builder(Client client, Class<?> resource)
        {
            this.client = requireNonNull(client);
            this.resource = requireNonNull(resource);
            uriBuilder = UriBuilder.fromResource(resource);
        }

        public ApiRequester build()
        {
            return new ApiRequester(client,
                    uriBuilder, method, entity, headers, mediaType);
        }

        public Builder pathMethod(String method)
        {
            uriBuilder.path(resource, requireNonNull(method));
            return this;
        }

        public Builder resolveTemplate(String name, Object value)
        {
            uriBuilder.resolveTemplate(requireNonNull(name), requireNonNull(value));
            return this;
        }

        public Builder queryParam(String name, Object... values)
        {
            for (Object o : values) {
                requireNonNull(o);
            }

            uriBuilder.queryParam(requireNonNull(name), requireNonNull(values));
            return this;
        }

        public Builder httpMethod(HttpMethod method)
        {
            this.method = requireNonNull(method);
            return this;
        }

        public Builder header(String name, String value)
        {
            headers.add(requireNonNull(name), requireNonNull(value));
            return this;
        }

        public Builder accept(String mediaType)
        {
            this.mediaType = requireNonNull(mediaType);
            return this;
        }

        public Builder entity(Entity<?> entity)
        {
            this.entity = entity;
            return this;
        }
    }
}
