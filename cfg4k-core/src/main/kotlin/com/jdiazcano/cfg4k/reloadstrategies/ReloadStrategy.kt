/*
 * Copyright 2015-2016 Javier Díaz-Cano Martín-Albo (javierdiazcanom@gmail.com)
 *
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

package com.jdiazcano.cfg4k.reloadstrategies

import com.jdiazcano.cfg4k.providers.ConfigProvider

/**
 * A reload strategy defines when a reload of the config loader will be reloaded.
 */
interface ReloadStrategy {

    /**
     * Registers a config provider to be reloaded once the time comes.
     */
    fun register(configProvider: ConfigProvider)

    /**
     * Deregisters the config provider so it will not be reloaded anymore.
     */
    fun deregister(configProvider: ConfigProvider)
}
