/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.vcs.log.newgraph.impl;

import com.intellij.vcs.log.newgraph.PermanentGraph;
import com.intellij.vcs.log.newgraph.PermanentGraphLayout;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PermanentGraphLayoutBuilder {

  @NotNull
  public static PermanentGraphLayout build(@NotNull PermanentGraph graph) {
    return build(graph, new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        return 0;
      }
    });
  }

  @NotNull
  public static PermanentGraphLayout build(@NotNull PermanentGraph graph, @NotNull Comparator<Integer> compareTwoHeaderNodeIndex) {
    List<Integer> heads = new ArrayList<Integer>();
    for (int i = 0; i < graph.nodesCount(); i++) {
      if (graph.getUpNodes(i).size() == 0) {
        heads.add(i);
      }
    }
    Collections.sort(heads, compareTwoHeaderNodeIndex);
    PermanentGraphLayoutBuilder builder = new PermanentGraphLayoutBuilder(graph, heads);
    return builder.build();
  }

  private final PermanentGraph myGraph;
  private final int[] myLayoutIndex;

  private final List<Integer> myHeadNodeIndex;
  private final int[] myStartLayoutIndexForHead;

  private final int[] stackForDFS;

  private int currentLayoutIndex = 1;

  private PermanentGraphLayoutBuilder(PermanentGraph graph, List<Integer> headNodeIndex) {
    myGraph = graph;
    myLayoutIndex = new int[graph.nodesCount()];

    myHeadNodeIndex = headNodeIndex;
    myStartLayoutIndexForHead = new int[headNodeIndex.size()];

    stackForDFS = new int[myGraph.nodesCount()];
  }

  private void dfs(int nodeIndex) {
    int stackIndex = 0;
    stackForDFS[0] = nodeIndex;
    while (stackIndex >= 0) {
      int currentNodeIndex = stackForDFS[stackIndex];
      boolean firstVisit = myLayoutIndex[currentNodeIndex] == 0;
      if (firstVisit)
        myLayoutIndex[currentNodeIndex] = currentLayoutIndex;

      int childWithoutLayoutIndex = -1;
      for (int childNodeIndex : myGraph.getDownNodes(currentNodeIndex)) {
        if (childNodeIndex != myGraph.nodesCount() && myLayoutIndex[childNodeIndex] == 0) {
          childWithoutLayoutIndex = childNodeIndex;
          break;
        }
      }

      if (childWithoutLayoutIndex == -1) {
        stackIndex--;
        if (firstVisit)
          currentLayoutIndex++;
      } else {
        stackIndex++;
        stackForDFS[stackIndex] = childWithoutLayoutIndex;
      }
    }
  }

  @NotNull
  private PermanentGraphLayout build() {
    for(int i = 0; i < myHeadNodeIndex.size(); i++) {
      int headNodeIndex = myHeadNodeIndex.get(i);
      myStartLayoutIndexForHead[i] = currentLayoutIndex;

      dfs(headNodeIndex);
    }

    return new PermanentGraphLayoutImpl(myLayoutIndex, myHeadNodeIndex, myStartLayoutIndexForHead);
  }




  private static class PermanentGraphLayoutImpl implements PermanentGraphLayout {
    private final int[] myLayoutIndex;

    private final List<Integer> myHeadNodeIndex;
    private final int[] myStartLayoutIndexForHead;

    private PermanentGraphLayoutImpl(int[] layoutIndex, List<Integer> headNodeIndex, int[] startLayoutIndexForHead) {
      myLayoutIndex = layoutIndex;
      myHeadNodeIndex = headNodeIndex;
      myStartLayoutIndexForHead = startLayoutIndexForHead;
    }

    @Override
    public int getLayoutIndex(int nodeIndex) {
      return myLayoutIndex[nodeIndex];
    }

    @Override
    public int getOneOfHeadNodeIndex(int nodeIndex) {
      return getHeadNodeIndex(getLayoutIndex(nodeIndex));
    }

    @Override
    public int getHeadNodeIndex(int layoutIndex) {
      return myHeadNodeIndex.get(getHeadOrder(layoutIndex));
    }

    private int getHeadOrder(int layoutIndex) {
      int a = 0;
      int b = myStartLayoutIndexForHead.length - 1;
      while (b > a) {
        int middle = (a + b + 1) / 2;
        if (myStartLayoutIndexForHead[middle] <= layoutIndex)
          a = middle;
        else
          b = middle - 1;
      }
      return a;
    }

    @Override
    public int getStartLayout(int layoutIndex) {
      return myStartLayoutIndexForHead[getHeadOrder(layoutIndex)];
    }
  }
}
