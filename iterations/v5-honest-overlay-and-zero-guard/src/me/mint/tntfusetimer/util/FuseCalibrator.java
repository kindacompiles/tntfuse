package me.mint.tntfusetimer.util;

import me.mint.tntfusetimer.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityTNTPrimed;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Learns the Bed Wars fuse offset by observation instead of trusting a hardcoded constant.
 *
 * <p>The client counts an 80 tick fuse down locally, but Hypixel detonates Bed Wars TNT
 * early and simply removes the entity. Whatever the fuse read at the moment the entity
 * vanished is exactly the correction we need, so every detonation is a free measurement.</p>
 */
public final class FuseCalibrator {

    /** Ignore TNT we have not watched for at least this long, to avoid sampling junk. */
    private static final int MIN_TRACKED_TICKS = 5;

    /** How many recent detonations to remember. */
    private static final int MAX_SAMPLES = 20;

    /** How many samples are needed before the learned value is trusted. */
    private static final int MIN_SAMPLES = 3;

    /** A fuse reading above this is implausible for a detonation and is discarded. */
    private static final int MAX_PLAUSIBLE_FUSE = 79;

    /**
     * A fuse reading below this means the client's own countdown simply ran out, which is
     * the <em>absence</em> of an early server detonation rather than evidence of one.
     *
     * <p>Without this guard, running with forceBedwarsMode enabled outside a real Bed Wars
     * game would sample ordinary vanilla TNT expiring at 0, learn an offset of 0, and
     * persist that over a perfectly good value.</p>
     */
    private static final int MIN_PLAUSIBLE_FUSE = 3;

    private static final Map<Integer, Tracked> tracked = new HashMap<Integer, Tracked>();
    private static final Deque<Integer> samples = new ArrayDeque<Integer>();

    private FuseCalibrator() {
    }

    private static final class Tracked {
        int lastFuse;
        int ticksObserved;
        boolean seenThisTick;
    }

    public static int getSampleCount() {
        return samples.size();
    }

    public static boolean hasCalibration() {
        return samples.size() >= MIN_SAMPLES;
    }

    /**
     * The offset learned from observation, to add to a raw fuse value, so negative
     * in practice.
     */
    public static int getLearnedOffset() {
        return -median();
    }

    /** Drops in-flight tracking but keeps what has already been learned. */
    public static void clearTracking() {
        tracked.clear();
    }

    /** Throws away everything, including learned samples. */
    public static void reset() {
        tracked.clear();
        samples.clear();
    }

    public static void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            tracked.clear();
            return;
        }

        for (Tracked entry : tracked.values()) {
            entry.seenThisTick = false;
        }

        for (Object candidate : mc.theWorld.loadedEntityList) {
            if (!(candidate instanceof EntityTNTPrimed)) {
                continue;
            }
            EntityTNTPrimed tnt = (EntityTNTPrimed) candidate;
            Integer id = Integer.valueOf(tnt.getEntityId());

            Tracked entry = tracked.get(id);
            if (entry == null) {
                entry = new Tracked();
                tracked.put(id, entry);
            }
            entry.lastFuse = tnt.fuse;
            entry.ticksObserved++;
            entry.seenThisTick = true;
        }

        // Anything that was here last tick and is gone now was detonated (or despawned).
        // The fuse it still had left is the measurement we want.
        for (Iterator<Map.Entry<Integer, Tracked>> it = tracked.entrySet().iterator(); it.hasNext(); ) {
            Tracked entry = it.next().getValue();
            if (entry.seenThisTick) {
                continue;
            }
            it.remove();

            if (entry.ticksObserved >= MIN_TRACKED_TICKS
                    && entry.lastFuse >= MIN_PLAUSIBLE_FUSE
                    && entry.lastFuse <= MAX_PLAUSIBLE_FUSE) {
                addSample(entry.lastFuse);
            }
        }
    }

    private static void addSample(int fuse) {
        samples.addLast(Integer.valueOf(fuse));
        while (samples.size() > MAX_SAMPLES) {
            samples.removeFirst();
        }
        writeBackIfChanged();
    }

    /**
     * Folds what we have learned straight back into {@code bedwarsTickOffset}.
     *
     * <p>This is what makes calibration persistent: the config field is the single
     * source of truth for the offset, so the render path stays trivial and the learned
     * value is already on disk for the next launch. It also means the manual value acts
     * as the seed while the first few samples are still being gathered.</p>
     */
    private static void writeBackIfChanged() {
        if (!hasCalibration()) {
            return;
        }
        int learned = getLearnedOffset();
        if (learned != ModConfig.bedwarsTickOffset) {
            ModConfig.setBedwarsTickOffset(learned);
        }
    }

    /**
     * Median rather than mean: a TNT that despawns on a chunk edge produces a wild reading,
     * and one bad sample should not drag the offset around.
     */
    private static int median() {
        if (samples.isEmpty()) {
            return 0;
        }
        List<Integer> sorted = new ArrayList<Integer>(samples);
        Collections.sort(sorted);
        int size = sorted.size();
        if ((size & 1) == 1) {
            return sorted.get(size / 2).intValue();
        }
        return (sorted.get(size / 2 - 1).intValue() + sorted.get(size / 2).intValue()) / 2;
    }
}
