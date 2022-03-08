package eu.f4sten.vulchainfinder.utils;

import com.google.common.collect.BiMap;
import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.vulchainfinder.data.NodeReachability;
import eu.fasten.core.data.DirectedGraph;
import eu.fasten.core.data.FastenURI;
import eu.fasten.core.data.vulnerability.Vulnerability;
import eu.fasten.core.vulchains.VulnerableCallChain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ImpactPropagator {

    private final DirectedGraph graph;
    private final BiMap<Long, String> idUriMap;

    private Set<NodeReachability> impacts;

    public ImpactPropagator(DirectedGraph graph, BiMap<Long, String> idUriMap) {
        this.graph = graph;
        this.idUriMap = idUriMap;
    }

    public DirectedGraph getGraph() {
        return graph;
    }

    public BiMap<Long, String> getIdUriMap() {
        return idUriMap;
    }

    public Set<NodeReachability> getImpacts() {
        return impacts;
    }

    public void propagateUrisImpacts(final Set<FastenURI> uris) {

        impacts = uris.stream()
            .map(nodeUri -> idUriMap.inverse().get(nodeUri.toString()))
            .filter(this::nodeIdExists)
            .map(this::propagateNodeImpacts)
            .collect(Collectors.toSet());
    }

    public NodeReachability propagateNodeImpacts(final Long nodeId) {

        final var res = new NodeReachability(nodeId);

        final var nodesToVisit = new LinkedList<Long>();
        nodesToVisit.add(nodeId);

        while (!nodesToVisit.isEmpty()) {
            final var currentTarget = nodesToVisit.poll();

            for (final var srcToTarget : graph.incomingEdgesOf(currentTarget)) {
                final var source = srcToTarget.firstLong();
                if (res.nextStepTowardsTarget.containsKey(source) || currentTarget.equals(source)) {
                    continue;
                }

                res.nextStepTowardsTarget.put(source, currentTarget);
                nodesToVisit.add(source);
            }
        }
        return res;
    }

    private boolean nodeIdExists(final Long nodeId) {
        return nodeId != null && graph.containsVertex(nodeId);
    }

    public Set<VulnerableCallChain> extractApplicationVulChains(
        final Map<FastenURI, List<Vulnerability>> vulCallables,
        final MavenId appId) {
        final Set<VulnerableCallChain> result = new HashSet<>();

        for (final var appNode : extractPackageNodes(appId)) {
            for (final var impact : impacts) {
                if (thereIsNoImpactForNode(appNode, impact)) {
                    continue;
                }

                final var currentVul =
                    vulCallables.get(FastenURI.create(idUriMap.get(impact.targetNode)));
                final var chains = extractUriChainsForNode(appNode, impact);
                result.add(new VulnerableCallChain(currentVul, chains));
            }
        }

        return result;
    }

    private boolean thereIsNoImpactForNode(final Long appNode, final NodeReachability impact) {
        return !impact.nextStepTowardsTarget.containsKey(appNode);
    }

    private List<FastenURI> extractUriChainsForNode(final Long appNode,
                                                    final NodeReachability impact) {
        final var result = new ArrayList<FastenURI>();

        final var appVulIdChains = extractChainFromAppNodeToVulNode(appNode, impact);
        for (final var nodeId : appVulIdChains) {
            result.add(FastenURI.create(idUriMap.get(nodeId)));
        }

        return result;
    }

    private ArrayList<Long> extractChainFromAppNodeToVulNode(final Long appNode,
                                                             final NodeReachability vulImpact) {
        final var result = new ArrayList<>(Collections.singletonList(appNode));

        var currentNode = appNode;
        while (!currentNode.equals(vulImpact.targetNode)) {
            currentNode = vulImpact.nextStepTowardsTarget.get(currentNode);
            result.add(currentNode);
        }

        return result;
    }

    private HashSet<Long> extractPackageNodes(final MavenId id) {
        final var result = new HashSet<Long>();

        final var appUri = createPackageUri(id);
        for (final var uri : idUriMap.values()) {
            if (uri.startsWith(appUri)) {
                result.add(idUriMap.inverse().get(uri));
            }
        }

        return result;
    }

    private String createPackageUri(final MavenId appId) {
        return String.format("%s%s:%s$%s", "fasten://mvn!",
            appId.groupId, appId.artifactId, appId.version);
    }
}
