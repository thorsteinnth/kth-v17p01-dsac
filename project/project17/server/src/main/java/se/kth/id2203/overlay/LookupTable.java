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
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.NodeAssignment;
import se.kth.id2203.networking.NetAddress;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class LookupTable implements NodeAssignment {

    private static final long serialVersionUID = -8766981433378303267L;

    private final TreeMultimap<Integer, NetAddress> partitions = TreeMultimap.create();

    final static Logger LOG = LoggerFactory.getLogger(LookupTable.class);

    public Collection<NetAddress> lookup(String key) {
        /*int keyHash = key.hashCode();
        Integer partition = partitions.keySet().floor(keyHash);
        if (partition == null) {
            partition = partitions.keySet().last();
        }
        return partitions.get(partition);*/

        // TODO We are assuming that the partition keys are 0...n,
        // so the partition index we get from modulus corresponds to a key in the partition map
        // This may not be the case ... what if a partition is removed? There is no guarantee that the partition keys
        // will be an unbroken sequence of numbers

        int keyHash = key.hashCode();
        Integer partitionIndex = keyHash % partitions.size();

        if (partitions.keySet().contains(partitionIndex))
        {
            LOG.debug("Lookup for key " + key + " - partition index: " + partitionIndex + " - partition key: " + partitionIndex);
            return partitions.get(partitionIndex);
        }
        else
        {
            // The index does not exist as a key in the parition map, default to last partition
            LOG.debug("Lookup for key " + key + " - partition index: " + partitionIndex + " - partition key: " + partitions.keySet().last());
            return partitions.get(partitions.keySet().last());
        }
    }

    public Collection<NetAddress> getNodes() {
        return partitions.values();
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

    static LookupTable generate(ImmutableSet<NetAddress> nodes) {
        LookupTable lut = new LookupTable();

        // TODO Figure out how to partition
        // Let's put one node per key to begin with
        int i = 0;
        for (NetAddress node : nodes)
        {
            lut.partitions.put(i++, node);
        }

        return lut;
    }

}
