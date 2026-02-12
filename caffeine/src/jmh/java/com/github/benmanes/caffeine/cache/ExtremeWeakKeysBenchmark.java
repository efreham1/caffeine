/*
 * Copyright 2014 Ben Manes. All Rights Reserved.
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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.benmanes.caffeine.cache.impl.CaffeineWeakCache;
import site.ycsb.generator.NumberGenerator;
import site.ycsb.generator.ScrambledZipfianGenerator;

/**
 * A benchmark that evaluates an extremely large weak keys cache with intense usage.
 * The cache uses weak keys and weak values (ints), maintains strong key references
 * to prevent garbage collection, and periodically drops these references to allow
 * weak references to be collected.
 * <p>
 * This benchmark maintains a large pool of key objects with strong references that
 * are used heavily, then periodically cleared to trigger weak reference cleanup.
 * <p>
 * {@snippet lang="shell" :
 * ./gradlew jmh -PincludePattern=ExtremeWeakKeysBenchmark -PjavaVersion=21 --rerun
 * }
 *
 * @author fredrik.hammarberg00@outlook.com (Fredrik Hammarberg)
 */
@State(Scope.Benchmark)
@SuppressWarnings({"CanonicalAnnotationSyntax", "IdentifierName",
    "LexicographicalAnnotationAttributeListing", "JavadocDeclaration",
    "NotNullFieldNotInitialized", "PMD.MethodNamingConventions", "unused"})
public class ExtremeWeakKeysBenchmark {
  // 10 million entries in the cache - extreme size
  private static final int CACHE_SIZE = 6_000_000;
  // Maximum number of strong key references to maintain at once
  private static final int ACTIVE_STRONG_REFS = 4_000_000;

  private BasicCache<Key, Integer> cache;
  private ConcurrentLinkedQueue<Key> activeReferences;
  private AtomicInteger activeRefCount;
  private AtomicLong operationCount;
  private AtomicLong garbageCollectionCount;

  @AuxCounters
  public static class Counters {
    public long operationCount;
    public long garbageCollectionCount;
  }

  @State(Scope.Thread)
  public static class ThreadState {
    ThreadLocalRandom random;
    NumberGenerator keyGenerator;
    int operationCounter;

    @Setup(Level.Trial)
    public void setup() {
      random = ThreadLocalRandom.current();
      keyGenerator = new ScrambledZipfianGenerator(CACHE_SIZE);
      operationCounter = 0;
    }
  }

  @State(Scope.Thread)
  public static class DropState {
    int dropCounter;
  }

  /** A simple key object that can be weakly referenced. */
  static class Key {
    final int id;
    final String data;

    Key(int id) {
      this.id = id;
      // Add some data to make keys more realistic
      this.data = "Key_" + id + "_" + System.nanoTime();
    }

    @Override
    public int hashCode() {
      return id;
    }

    @Override
    public boolean equals(@org.jspecify.annotations.Nullable Object obj) {
      return (obj instanceof Key) && id == ((Key) obj).id;
    }

    @Override
    public String toString() {
      return "Key(" + id + ")";
    }
  }

  @Setup(Level.Trial)
  public void setup() {
    cache = new CaffeineWeakCache<>(CACHE_SIZE);

    // Initialize active references array
    activeReferences = new ConcurrentLinkedQueue<>();
    activeRefCount = new AtomicInteger();
    operationCount = new AtomicLong();
    garbageCollectionCount = new AtomicLong();

    // Populate cache with initial keys
    NumberGenerator initialKeyGenerator = new ScrambledZipfianGenerator(CACHE_SIZE);
    int populateCount = CACHE_SIZE; // Populate the entire cache
    for (int i = 0; i < populateCount; i++) {
      int index = initialKeyGenerator.nextValue().intValue();
      var key = new Key(index);
      cache.put(key, index);
      trackKey(key);
    }
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    cache.clear();
  }

  @Benchmark
  @Group("extremeWeakKeys")
  @GroupThreads(11)
  public void put(ThreadState threadState) {
    int keyIndex = threadState.keyGenerator.nextValue().intValue();
    var key = new Key(keyIndex);
    trackKey(key);
    cache.put(key, threadState.random.nextInt(Integer.MAX_VALUE));
    operationCount.incrementAndGet();
  }

  @Benchmark
  @Group("extremeWeakKeys")
  @GroupThreads(1)
  public void dropStrongReferences(DropState dropState) {
    if (activeRefCount.get() > ACTIVE_STRONG_REFS) {
      Key dropped = activeReferences.poll();
      if (dropped != null) {
        activeRefCount.decrementAndGet();
        garbageCollectionCount.incrementAndGet();
      }
    }
  }

  private void trackKey(Key key) {
    if (activeRefCount.get() < ACTIVE_STRONG_REFS) {
      activeReferences.offer(key);
      int count = activeRefCount.incrementAndGet();
    }
  }
}
