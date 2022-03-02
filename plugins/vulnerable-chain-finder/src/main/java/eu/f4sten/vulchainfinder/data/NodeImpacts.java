package eu.f4sten.vulchainfinder.data;

import java.util.Map;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class NodeImpacts {
    public Long impactingNode;
    public Map<Long, Long> impactMap;

    public NodeImpacts(Long impactingNode,
                       Map<Long, Long> impactMap) {
        this.impactingNode = impactingNode;
        this.impactMap = impactMap;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NodeImpacts &&
            EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
