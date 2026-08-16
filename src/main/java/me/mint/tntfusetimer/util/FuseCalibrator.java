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
 *
 * <p><b>This only ever runs in a genuinely detected Bed Wars game.</b> That single
 * restriction, enforced by the caller, is what keeps the data clean. Every bad value this
 * ever produced came from sampling outside a real game, where vanilla TNT and lag from large
 * explosions generate readings that look plausible but mean nothing.</p>
 *
 * <p>Measurements are in-memory only; nothing is requested over the network.</p>
 */
public final class FuseCalibrator {

    /** Ignore TNT we have not watched for at least this long, to avoid sampling junk. */
    private static final int MIN_TRACKED_TICKS = 5;

    /** How many recent detonations to remember. */
    private static final int MAX_SAMPLES = 20;

    /** How many samples are needed before the learned value is trusted. */
    private static final int MIN_SAMPLES = 10;

    /** Highest fuse value that can exist; anything else is a bad read, not a measurement. */
    private static final int MAX_FUSE = 80;

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

    /** How many samples are needed before the learned value is used. */
    public static int getRequiredSamples() {
        return MIN_SAMPLES;
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

        // Anything that was here last tick and is gone now was detonated. The fuse it still
        // had left is the measurement we want.
        for (Iterator<Map.Entry<Integer, Tracked>> it = tracked.entrySet().iterator(); it.hasNext(); ) {
            Tracked entry = it.next().getValue();
            if (entry.seenThisTick) {
                continue;
            }
            it.remove();

            if (entry.ticksObserved >= MIN_TRACKED_TICKS
                    && entry.lastFuse >= 0
                    && entry.lastFuse < MAX_FUSE) {
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
     * <p>The config field is the single source of truth for the offset, so the render path
     * stays trivial and the learned value is already on disk for the next launch. The manual
     * value acts as the seed while the first samples are gathered.</p>
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
     * Median rather than mean, so one stray reading cannot drag the offset around.
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
