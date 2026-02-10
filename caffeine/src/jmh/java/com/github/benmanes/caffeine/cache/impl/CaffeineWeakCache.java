/*
 * Copyright 2015 Ben Manes. All Rights Reserved.
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
package com.github.benmanes.caffeine.cache.impl;

import org.jspecify.annotations.Nullable;

import com.github.benmanes.caffeine.cache.BasicCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Caffeine cache with weak keys and weak values.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
public final class CaffeineWeakCache<K, V> implements BasicCache<K, V> {
  private final Cache<K, V> cache;

  public CaffeineWeakCache(int maximumSize) {
    cache = Caffeine.newBuilder()
        .initialCapacity(maximumSize)
        .maximumSize(maximumSize)
        .weakKeys()
        .weakValues()
        .build();
  }

  @Override
  public @Nullable V get(K key) {
    return cache.getIfPresent(key);
  }

  @Override
  public void put(K key, V value) {
    cache.put(key, value);
  }

  @Override
  public void remove(K key) {
    cache.invalidate(key);
  }

  @Override
  public void clear() {
    cache.invalidateAll();
  }

  @Override
  public void cleanUp() {
    cache.cleanUp();
  }
}
