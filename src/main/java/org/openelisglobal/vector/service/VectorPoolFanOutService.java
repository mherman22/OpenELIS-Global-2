package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Vector surveillance pool fan-out (V-02 Phase 3.5 — left unchecked in the V-02
 * task plan and not delivered by PR #3559).
 *
 * <p>
 * When a vector order specifies {@code vecPoolCount > 1} (a pool with multiple
 * organisms), an implementation creates one child {@link SampleItem} per
 * organism under the order's existing parent SampleItem. Each child carries its
 * own {@code sortOrder} (1..N) and {@code parent_sample_item_id} pointing back
 * to the pool, so display labels can be constructed by walking the parent chain
 * (e.g. {@code DEV000309840001.1.5}).
 *
 * <h2>Where the pool tests live</h2>
 *
 * <p>
 * The parent SampleItem is the "pool" — it owns the {@code analysis} rows for
 * pool-level pathogen tests (NS1 RT-PCR, etc.). Children are created
 * <strong>without analyses</strong>: a pool of 10 mosquitoes runs <em>one</em>
 * PCR on the combined extract; one analysis row, attached to the parent.
 * Children carry per-specimen data only (species ID, physiological state,
 * lifecycle stage), and acquire their own analyses only when (a) the pool tests
 * positive and the lab decides to deconvolute, or (b) the lab sub-pools and the
 * chain repeats. This is the supervisor's principle: <em>"test analysis should
 * be on pool and not the sample_item until you reach one vector per pool."</em>
 * The recursion lands at a leaf SampleItem which is itself a pool of one.
 *
 * <h2>What implementations do NOT do</h2>
 *
 * <ul>
 * <li>Copy or create analyses on the children — none until deconvolution</li>
 * <li>Create {@code SampleItemAliquotRelationship} rows — these are scoped to
 * the deconvolution / aliquoting workflow (Feature 001-sample-management).
 * Intake-time fan-out uses the simpler {@code parent_sample_item_id +
 *       sortOrder} convention which display-label generation can walk.</li>
 * <li>Touch the parent's quantity — the parent retains its pool-size quantity;
 * children are each {@code 1.0}.</li>
 * </ul>
 *
 * @see <a href=
 *      "../../../../../../../../specs/vector-surveillance/tasks.md">specs/vector-surveillance/tasks.md
 *      Phase 3.5</a>
 */
public interface VectorPoolFanOutService {

    /**
     * Create {@code poolCount} child SampleItems under {@code parent}. No-op when
     * {@code poolCount <= 1} (a "pool" of one is the same physical thing as the
     * parent — the children would be empty rows).
     *
     * @param parent    the parent pool SampleItem (must already be persisted)
     * @param poolCount number of organisms in the pool; values <= 1 trigger no-op
     * @param sysUserId audit trail user id
     * @return the persisted child SampleItems (empty list when no fan-out
     *         occurred). Callers use the returned list to wire downstream
     *         per-specimen artefacts (e.g. barcode labels) without re-querying.
     */
    List<SampleItem> fanOut(SampleItem parent, int poolCount, String sysUserId);

    /**
     * Convenience wrapper — fan out across every parent SampleItem in a list. Used
     * by the order-entry persist flow which iterates all the order's items.
     *
     * @return all persisted child SampleItems across every parent (empty when no
     *         fan-out occurred). Order matches iteration order of {@code parents}.
     */
    List<SampleItem> fanOutAll(List<SampleItem> parents, int poolCount, String sysUserId);
}
