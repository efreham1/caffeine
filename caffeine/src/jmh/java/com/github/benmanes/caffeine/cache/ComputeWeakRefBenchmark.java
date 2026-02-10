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

import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

import site.ycsb.generator.NumberGenerator;
import site.ycsb.generator.ScrambledZipfianGenerator;

/**
 * A benchmark that evaluates the compute performance with weak references.
 * <p>
 * {@snippet lang="shell" :
 * ./gradlew jmh -PincludePattern=ComputeWeakRefBenchmark -PjavaVersion=21 --rerun
 * }
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
@State(Scope.Benchmark)
@SuppressWarnings({"IdentifierName", "LexicographicalAnnotationAttributeListing",
    "NotNullFieldNotInitialized", "PMD.MethodNamingConventions", "unused"})
public class ComputeWeakRefBenchmark {
  static final int SIZE = (2 << 14);
  static final int MASK = SIZE - 1;
  static final int ITEMS = SIZE / 3;
  static final Integer COMPUTE_KEY = SIZE / 2;
  static final Function<Integer, Boolean> mappingFunction = any -> true;

  Function<Integer, Boolean> benchmarkFunction;
  final Integer[] ints;

  @State(Scope.Thread)
  public static class ThreadState {
    static final Random random = new Random();
    int index = random.nextInt();
  }

  public ComputeWeakRefBenchmark() {
    ints = new Integer[SIZE];
    NumberGenerator generator = new ScrambledZipfianGenerator(ITEMS);
    for (int i = 0; i < SIZE; i++) {
      ints[i] = generator.nextValue().intValue();
    }
  }

  @Setup
  @SuppressWarnings("ReturnValueIgnored")
  public void setup() {
    Cache<Integer, Boolean> cache = Caffeine.newBuilder()
        .weakKeys()
        .weakValues()
        .build();
    benchmarkFunction = key -> cache.get(key, mappingFunction);
    Arrays.stream(ints).forEach(benchmarkFunction::apply);
  }

  @Benchmark @Threads(32)
  public Boolean compute_sameKey(ThreadState threadState) {
    return benchmarkFunction.apply(COMPUTE_KEY);
  }

  @Benchmark @Threads(32)
  public Boolean compute_spread(ThreadState threadState) {
    return benchmarkFunction.apply(ints[threadState.index++ & MASK]);
  }
}
