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

import java.util.Random;

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
 * A benchmark that loosely evaluates the put and removal performance of the cache with
 * weak references. The cache is written using a Zipf distribution of keys to incur lock
 * contention on popular entries.
 * <p>
 * This benchmark uses Caffeine with weakKeys() and weakValues() to test weak reference handling.
 * <p>
 * The performance of this benchmark is expected to be unrealistic due to removing entries. A cache
 * that optimistically skips locking when the entry is absent receives a benefit, but sacrifices
 * linearizability with a computation. In real-world usages the rate of explicit writes is
 * relatively rare compared to reads. Thus, this benchmark is only for diagnosing performance
 * concerns and should not be used to compare implementations.
 * <p>
 * {@snippet lang="shell" :
 * ./gradlew jmh -PincludePattern=PutRemoveWeakRefBenchmark --rerun
 * }
 *
 * @author fredrik.hammarberg00@outlook.com (Fredrik Hammarberg)
 */
@State(Scope.Group)
@SuppressWarnings({"CanonicalAnnotationSyntax", "LexicographicalAnnotationAttributeListing",
    "JavadocDeclaration", "NotNullFieldNotInitialized"})
public class PutRemoveWeakRefBenchmark {
  private static final int SIZE = (2 << 14);
  private static final int MASK = SIZE - 1;
  private static final int ITEMS = SIZE / 3;

  BasicCache<Integer, Boolean> cache;
  Integer[] ints;

  @State(Scope.Thread)
  public static class ThreadState {
    static final Random random = new Random();
    int index = random.nextInt();
  }

  @Setup
  public void setup() {
    ints = new Integer[SIZE];
    cache = new CaffeineWeakCache<>(2 * SIZE);

    // Enforce full initialization of internal structures
    for (int i = 0; i < 2 * SIZE; i++) {
      cache.put(i, true);
    }
    cache.clear();

    // Populate using a realistic access distribution
    NumberGenerator generator = new ScrambledZipfianGenerator(ITEMS);
    for (int i = 0; i < SIZE; i++) {
      ints[i] = generator.nextValue().intValue();
      cache.put(ints[i], true);
    }
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    cache.cleanUp();
  }

  @Benchmark @Group @GroupThreads(4)
  public void put(ThreadState threadState) {
    cache.put(ints[threadState.index++ & MASK], true);
  }

  @Benchmark @Group @GroupThreads(4)
  public void remove(ThreadState threadState) {
    cache.remove(ints[threadState.index++ & MASK]);
  }
}
