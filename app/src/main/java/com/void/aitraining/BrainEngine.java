package com.aitraining.sim;

import android.content.res.AssetManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Small mobile LIF runner for a subnetwork extracted from the BANC v888
 * female Drosophila connectome. Wiring is measured data; dynamics and
 * game-to-neuron encoders are explicitly calibrated approximations.
 */
public final class BrainEngine {
    private final int nS, nI, nM, n;
    private final long[] neuronIds;
    private final int[] motorLeg;
    private final float[] weights;
    private final float[] voltage, refractory, synaptic;
    private final boolean[] previousSpikes, currentSpikes;
    private final int[] motorCounts = new int[6];
    private int totalSpikes;
    private int lastWindowSpikes;
    private float stride, turn;
    private int activeCount;

    private static final float REST = -65f;
    private static final float THRESHOLD = -52f;
    private static final float RESET = -70f;
    private static final float TAU_MS = 10f;
    private static final float SYN_GAIN = 24f;
    private static final int STEPS_PER_CALL = 100; // 100 x 1 ms = 100 ms real-time window

    public BrainEngine(AssetManager assets) throws IOException {
        byte[] raw = readAll(assets.open("banc_subnet.bin"));
        ByteBuffer b = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        if (b.remaining() < 20 || b.get() != 'B' || b.get() != 'A' || b.get() != 'N' || b.get() != 'C') {
            throw new IOException("Invalid BANC network asset");
        }
        int version = b.getInt();
        if (version != 1) throw new IOException("Unsupported BANC asset version " + version);
        nS = b.getInt(); nI = b.getInt(); nM = b.getInt();
        n = nS + nI + nM;
        if (n < 8 || n > 64 || nS < 1 || nM < 1 || b.remaining() < n * 8 + nM * 4 + n * n * 4) {
            throw new IOException("Invalid BANC network dimensions");
        }
        neuronIds = new long[n];
        for (int i = 0; i < n; i++) neuronIds[i] = b.getLong();
        motorLeg = new int[nM];
        for (int i = 0; i < nM; i++) motorLeg[i] = Math.max(0, Math.min(5, b.getInt()));
        weights = new float[n * n];
        for (int i = 0; i < weights.length; i++) weights[i] = b.getFloat();
        voltage = new float[n]; refractory = new float[n]; synaptic = new float[n];
        previousSpikes = new boolean[n]; currentSpikes = new boolean[n];
        for (int i = 0; i < n; i++) voltage[i] = REST;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toByteArray();
        }
    }

    public synchronized String stepJson(float touch, float gait, float obstacle) {
        touch = clamp(touch, 0f, 1f);
        gait = clamp(gait, 0f, 1f);
        obstacle = clamp(obstacle, 0f, 1f);
        int windowSpikes = 0;
        int[] windowMotor = new int[6];

        for (int t = 0; t < STEPS_PER_CALL; t++) {
            for (int j = 0; j < n; j++) synaptic[j] = 0f;
            for (int i = 0; i < n; i++) {
                if (!previousSpikes[i]) continue;
                int row = i * n;
                for (int j = 0; j < n; j++) synaptic[j] += weights[row + j] * SYN_GAIN;
            }

            for (int i = 0; i < n; i++) {
                float external = 0f;
                if (i < nS) {
                    int receptorClass = i / 6;
                    float sensitivity = receptorClass == 0 ? 26f : receptorClass == 1 ? 17f : 11f;
                    external = touch * sensitivity + obstacle * (sensitivity * 0.8f)
                            + gait * (4f + (i % 3) * 1.5f);
                }
                if (refractory[i] > 0f) refractory[i] = Math.max(0f, refractory[i] - 1f);
                voltage[i] += (-(voltage[i] - REST) + external + synaptic[i]) / TAU_MS;
                boolean fired = refractory[i] <= 0f && voltage[i] >= THRESHOLD;
                currentSpikes[i] = fired;
                if (fired) {
                    voltage[i] = RESET;
                    refractory[i] = 2f;
                    windowSpikes++;
                    totalSpikes++;
                    if (i >= nS + nI) {
                        int leg = motorLeg[i - nS - nI];
                        windowMotor[leg]++;
                    }
                }
            }
            System.arraycopy(currentSpikes, 0, previousSpikes, 0, n);
        }

        int left = windowMotor[0] + windowMotor[2] + windowMotor[4];
        int right = windowMotor[1] + windowMotor[3] + windowMotor[5];
        int allMotor = left + right;
        stride = clamp(allMotor / Math.max(2f, nM * 0.22f), 0f, 1f);
        turn = clamp((right - left) / Math.max(1f, allMotor), -1f, 1f);
        activeCount = 0;
        JSONArray active = new JSONArray();
        for (int i = 0; i < n; i++) {
            if (previousSpikes[i]) { active.put(i); activeCount++; }
        }
        lastWindowSpikes = windowSpikes;

        try {
            JSONObject result = new JSONObject();
            result.put("source", "BANC v888");
            result.put("neurons", n);
            result.put("windowSpikes", windowSpikes);
            result.put("totalSpikes", totalSpikes);
            result.put("activeCount", activeCount);
            result.put("active", active);
            result.put("stride", stride);
            result.put("turn", turn);
            result.put("left", left);
            result.put("right", right);
            result.put("motor", new JSONArray(windowMotor));
            result.put("sensoryNeurons", nS);
            result.put("interneurons", nI);
            result.put("motorNeurons", nM);
            return result.toString();
        } catch (Exception e) {
            return "{\"error\":\"snapshot encoding failed\"}";
        }
    }

    public synchronized String infoJson() {
        try {
            JSONObject root = new JSONObject();
            root.put("source", "BANC v888");
            root.put("doi", "10.7910/DVN/7WTH1N");
            root.put("license", "CC BY 4.0");
            root.put("neurons", n);
            root.put("sensory", nS);
            root.put("interneurons", nI);
            root.put("motor", nM);
            JSONArray ids = new JSONArray();
            JSONArray edges = new JSONArray();
            for (long id : neuronIds) ids.put(Long.toString(id));
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    float w = weights[i * n + j];
                    if (w > 0f) {
                        JSONArray edge = new JSONArray();
                        edge.put(i); edge.put(j); edge.put(w);
                        edges.put(edge);
                    }
                }
            }
            root.put("ids", ids);
            root.put("edges", edges);
            return root.toString();
        } catch (Exception e) {
            return "{\"error\":\"brain metadata unavailable\"}";
        }
    }

    public synchronized String statusJson() {
        return String.format(Locale.US, "{\"stride\":%.4f,\"turn\":%.4f,\"spikes\":%d}", stride, turn, totalSpikes);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
