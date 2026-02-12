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
package com.github.benmanes.caffeine.cache;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.benmanes.caffeine.cache.impl.CaffeineWeakCache;

/**
 * A benchmark that evaluates the eviction performance of a cache with weak references.
 * The cache is prepopulated for a 100% eviction rate to mimic worst case behavior.
 * <p>
 * {@snippet lang="shell" :
 * ./gradlew jmh -PincludePattern=EvictionWeakRefBenchmark --rerun
 * }
 *
 * @author fredrik.hammarberg00@outlook.com (Fredrik Hammarberg)
 */
@State(Scope.Benchmark)
@SuppressWarnings({"CanonicalAnnotationSyntax", "LexicographicalAnnotationAttributeListing",
    "JavadocDeclaration", "NotNullFieldNotInitialized", "unused"})
public class EvictionWeakRefBenchmark {

  @Param({"1", "100", "10000", "1000000", "10000000"})
  int size;

  BasicCache<Integer, Boolean> cache;

  @State(Scope.Thread)
  public static class ThreadState {
    int key;
  }

  @Setup
  public void setup() {
    cache = new CaffeineWeakCache<>(size);
    for (int i = 0; i < size; i++) {
      cache.put(Integer.MIN_VALUE + i, true);
    }
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    cache.cleanUp();
  }

  @Benchmark
  public void evict(ThreadState threadState) {
    cache.put(threadState.key++, true);
  }
}
