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
package com.teradata.prestomanager.common.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.binder.LinkedBindingBuilder;

public class JsonReaderModule
    implements Module
{
    private final boolean useCustomMapper;

    public JsonReaderModule(boolean useCustomMapper)
    {
        this.useCustomMapper = useCustomMapper;
    }

    public JsonReaderModule()
    {
        this(false);
    }

    @Override
    public void configure(Binder binder)
    {
        binder.bind(JsonResponseReader.class)
                .to(JsonResponseReaderImpl.class)
                .in(Scopes.SINGLETON);

        if (!useCustomMapper) {
            bindJsonReaderMapper(binder).to(ObjectMapper.class);
        }
    }

    /**
     * Start a binding for the {@link ObjectMapper} to use in {@link JsonResponseReaderImpl}
     */
    public static LinkedBindingBuilder<ObjectMapper> bindJsonReaderMapper(Binder binder)
    {
        return binder.bind(ObjectMapper.class).annotatedWith(JsonResponseMapper.class);
    }
}