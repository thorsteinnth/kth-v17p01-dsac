/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.overlay;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.TreeMultimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.NodeAssignment;
import se.kth.id2203.networking.NetAddress;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class LookupTable implements NodeAssignment {

    // Let's use consistent hashing

    private static final long serialVersionUID = -8766981433378303267L;

    private final TreeMultimap<Integer, NetAddress> partitions = TreeMultimap.create();

    final static Logger LOG = LoggerFactory.getLogger(LookupTable.class);

    public Collection<NetAddress> lookup(String key) {

        int keyHash = hashKey(key);

        // A partition is responsible for the keys >= its own key
        Integer partitionKey = partitions.keySet().floor(keyHash);

        if (partitionKey == null)
        {
            // Did not find the partition by looking down from the key hash
            // Selecting the last partition
            partitionKey = partitions.keySet().last();
        }

        LOG.debug("Lookup for key " + key + " - key hash: " + keyHash + " - partition key: " + partitionKey);

        return partitions.get(partitionKey);
    }

    private static int hashKey(String key)
    {
        return key.hashCode() % 100;
    }

    private static int hashNode(NetAddress node)
    {
        return node.hashCode() % 100;
    }

    public Collection<NetAddress> getNodes() {
        return partitions.values();
    }

    public void removeNode(NetAddress node)
    {
        Integer partitionKey = getKeyForNode(node);

        if (partitions.get(partitionKey).size() == 1)
        {
            // We are about to lose the last node in the partition

            // TODO
            // Should give other partitions the responsibility for this partition's
            // data. The threshold for doing this might be higher.
            // e.g. if we want to maintain replication degree 3 - then we might move responsibility
            // for the data in this partition to some other partition if we have fewer than 3 nodes left.
            // Or just take this partition offline - so it doesn't respond to any requests (at least not writes)
        }

        partitions.remove(partitionKey, node);
    }

    // TODO Find better way to tell node what replication group it is in
    public Collection<NetAddress> getPartitionForNode(NetAddress node)
    {
        Integer partitionKey = getKeyForNode(node);
        return partitions.get(partitionKey);
    }

    /**
     * Reverse lookup in the partitions map
     */
    private Integer getKeyForNode(NetAddress node)
    {
        for (Integer partitionKey : partitions.keySet())
        {
            Collection<NetAddress> nodesInPartition = partitions.get(partitionKey);
            if (nodesInPartition.contains(node))
                return partitionKey;
        }

        LOG.error("Could not find partition for node: " + node);
        return -1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LookupTable(\n");
        for (Integer key : partitions.keySet()) {
            sb.append(key);
            sb.append(" -> ");
            sb.append(Iterables.toString(partitions.get(key)));
            sb.append("\n");
        }
        sb.append(")");
        return sb.toString();
    }

    static LookupTable generate(ImmutableSet<NetAddress> nodes)
    {
        LookupTable lut = new LookupTable();

        int replicationDegree = 3;
        double replicationGroupCount = Math.floor(nodes.size()/replicationDegree);
        if (replicationGroupCount == 0)
            replicationGroupCount = 1;
        List<Integer> replicationGroupKeys = new ArrayList<>();

        int i = 0;
        for (NetAddress node : nodes)
        {
            // First create partitions
            if (i < replicationGroupCount)
            {
                int key = hashNode(node);
                lut.partitions.put(key, node);
                replicationGroupKeys.add(key);
            }
            else
            {
                // Distribute rest of nodes to replication groups
                int destinationReplicationGroupIndex =  (int)(i % replicationGroupCount);
                int key = replicationGroupKeys.get(destinationReplicationGroupIndex);
                lut.partitions.put(key, node);
            }

            i++;
        }

        return lut;
    }
}
