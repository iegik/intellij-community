/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.tree;

import com.intellij.lang.Language;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.containers.ContainerUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

public class TokenSetTest {
  private IElementType T1, T2, T3, T4, T5, T6;
  private TokenSet S1, S12, S3, S34, S5;

  @Before
  public void setUp() {
    T1 = new IElementType("T1", Language.ANY);
    T2 = new IElementType("T2", Language.ANY);
    S1 = TokenSet.create(T1);
    S12 = TokenSet.create(T1, T2);
    fakeElements(1, 128);
    T3 = new IElementType("T3", Language.ANY);
    T4 = new IElementType("T4", Language.ANY);
    S3 = TokenSet.create(T3);
    S34 = TokenSet.create(T3, T4);
    fakeElements(201, 204);
    T5 = new IElementType("T5", Language.ANY);
    T6 = new IElementType("T6", Language.ANY);
    S5 = TokenSet.create(T5);
  }

  @Test
  public void create() {
    check(S1, T1);
    check(S12, T1, T2);
    check(S3, T3);
    check(S34, T3, T4);
  }

  @Test
  public void getTypes() throws Exception {
    assertArrayEquals(IElementType.EMPTY_ARRAY, TokenSet.EMPTY.getTypes());
    assertArrayEquals(new IElementType[]{T1, T2}, S12.getTypes());
    assertArrayEquals(new IElementType[]{T3, T4}, S34.getTypes());
    assertEquals("[]", TokenSet.EMPTY.toString());
    assertEquals("[T1, T2]", S12.toString());
    assertEquals("[T3, T4]", S34.toString());
  }

  @Test
  public void orSet() {
    check(TokenSet.orSet(S1, S12, S3), T1, T2, T3);
    check(TokenSet.orSet(S1, S3), T1, T3);
  }

  @Test
  public void andSet() {
    check(TokenSet.andSet(S1, S12), T1);
    check(TokenSet.andSet(S12, S34));
  }

  @SuppressWarnings("deprecation")
  @Test
  public void andNot() throws Exception {
    final TokenSet S123 = TokenSet.orSet(S12, S3);
    check(S123.minus(S12), T3);
    check(S123.minus(S5), T1, T2, T3);
    check(TokenSet.andNot(S123, S12), T3);
    check(TokenSet.andNot(S123, S5), T1, T2, T3);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void not() throws Exception {
    check(TokenSet.not(S12));
    check(TokenSet.not(S34), T1, T2);
    check(TokenSet.not(S5), T1, T2, T3, T4, T6);
  }

  private static void fakeElements(int from, int to) {
    for (int i = from; i <= to; i++) {
      new IElementType("Test element #" + i, Language.ANY);
    }
  }

  private void check(TokenSet set, IElementType... elements) {
    final Set<IElementType> expected = ContainerUtil.newHashSet(elements);
    for (IElementType t : Arrays.asList(T1, T2, T3, T4, T5, T6)) {
      if (expected.contains(t)) {
        assertTrue("missed: " + t, set.contains(t));
      }
      else {
        assertFalse("unexpected: " + t, set.contains(t));
      }
    }
  }

  @Test
  public void performance() throws Exception {
    final IElementType[] elementTypes = IElementType.enumerate(IElementType.TRUE);
    final TokenSet set = TokenSet.create();
    final int shift = new Random().nextInt(500000);

    PlatformTestUtil.startPerformanceTest("TokenSet.contains() performance", 25, new ThrowableRunnable() {
      @Override
      public void run() throws Throwable {
        for (int i = 0; i < 1000000; i++) {
          final IElementType next = elementTypes[((i + shift) % elementTypes.length)];
          assertFalse(set.contains(next));
        }
      }
    }).cpuBound().assertTiming();
  }
}
