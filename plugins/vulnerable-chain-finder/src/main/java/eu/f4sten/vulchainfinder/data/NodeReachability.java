/*
 * Copyright 2021 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.f4sten.vulchainfinder.data;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class NodeReachability {

    public long targetNode;
    public Map<Long, Long> nextStepTowardsTarget = new HashMap<>();

    public NodeReachability(long impactingNode) {
        this.targetNode = impactingNode;
    }

    public boolean isReachingTarget(long nodeId) {
        return targetNode == nodeId || nextStepTowardsTarget.containsKey(nodeId);
    }

    public List<Long> getShortestPath(long nodeId) {
        if (!isReachingTarget(nodeId)) {
            throw new InvalidParameterException("Node does not reach target");
        }

        final var path = new ArrayList<Long>();
        path.add(nodeId);

        var curNode = nodeId;
        while (curNode != targetNode) {
            curNode = nextStepTowardsTarget.get(curNode);
            path.add(curNode);
        }
        return path;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
