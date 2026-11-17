package com.aitraining.sim;

import android.content.res.AssetManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Full FlyWire FAFB v783 connectome runner.
 * Connectivity and signed weights come from the published Shiu et al. v783 model.
 * This is a lightweight Android LIF approximation, not the original Brian2 runtime.
 * Game stimulus channels and game-director mapping are experimental engineering
 * interfaces; they are not claimed to be biologically annotated sensory/motor cells.
 */
public final class BrainEngine {
    private final int n, m;
    private final long[] ids;
    private final int[] offsets, targets;
    private final float[] weights, voltage, input;
    private final int[] refractory;
    private int totalSpikes, lastSpikes, activeCount;
    private float activity, stride, turn;
    private int director;

    private static final int STEPS_PER_CALL = 10;
    private static final float REST = -52f, RESET = -52f, THRESHOLD = -45f;
    private static final float LEAK = 1f / 20f, INPUT_DECAY = 0.81873075f;
    private static final float SYNAPSE_GAIN = 0.275f;

    public BrainEngine(AssetManager assets) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(assets.open("flywire_brain.bin"), 1048576)) {
            byte[] header = new byte[16];
            readFully(in, header, 16);
            ByteBuffer h = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            if (h.get() != 'F' || h.get() != 'L' || h.get() != 'Y' || h.get() != 'W') throw new IOException("Invalid FlyWire asset");
            int version = h.getInt(); n = h.getInt(); m = h.getInt();
            if (version != 1 || n < 100000 || n > 200000 || m < 1000000 || m > 30000000)
                throw new IOException("Invalid FlyWire dimensions: " + n + " neurons, " + m + " links");
            // Stream the large asset directly into arrays; avoid a second full-size byte array.
            ids = new long[n]; offsets = new int[n + 1]; targets = new int[m]; weights = new float[m];
            byte[] chunk = new byte[1048576];
            readLongs(in, ids, chunk);
            readInts(in, offsets, chunk);
            readInts(in, targets, chunk);
            readFloats(in, weights, chunk);
            if (in.read() != -1) throw new IOException("Unexpected trailing FlyWire asset bytes");
        }
        voltage = new float[n]; input = new float[n]; refractory = new int[n];
        for (int i = 0; i < n; i++) voltage[i] = REST;
    }

    private static void readFully(InputStream in, byte[] b, int len) throws IOException {
        int p = 0;
        while (p < len) { int k = in.read(b, p, len - p); if (k < 0) throw new IOException("Truncated FlyWire asset"); p += k; }
    }
    private static void readLongs(InputStream in, long[] out, byte[] chunk) throws IOException {
        int p = 0;
        while (p < out.length) {
            int count = Math.min(out.length - p, chunk.length / 8), bytes = count * 8;
            readFully(in, chunk, bytes);
            ByteBuffer b = ByteBuffer.wrap(chunk, 0, bytes).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < count; i++) out[p++] = b.getLong();
        }
    }
    private static void readInts(InputStream in, int[] out, byte[] chunk) throws IOException {
        int p = 0;
        while (p < out.length) {
            int count = Math.min(out.length - p, chunk.length / 4), bytes = count * 4;
            readFully(in, chunk, bytes);
            ByteBuffer b = ByteBuffer.wrap(chunk, 0, bytes).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < count; i++) out[p++] = b.getInt();
        }
    }
    private static void readFloats(InputStream in, float[] out, byte[] chunk) throws IOException {
        int p = 0;
        while (p < out.length) {
            int count = Math.min(out.length - p, chunk.length / 4), bytes = count * 4;
            readFully(in, chunk, bytes);
            ByteBuffer b = ByteBuffer.wrap(chunk, 0, bytes).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < count; i++) out[p++] = b.getFloat();
        }
    }

    public synchronized String stepJson(float touch,float gait,float obstacle) {
        touch=clamp(touch,0,1); gait=clamp(gait,0,1); obstacle=clamp(obstacle,0,1);
        int spikes=0, active=0;
        int third=n*2/3;
        int left=0,right=0;
        for(int t=0;t<STEPS_PER_CALL;t++){
            for(int i=0;i<n;i++){
                input[i]*=INPUT_DECAY;
                if(refractory[i]>0){refractory[i]--;continue;}
                voltage[i]+=((REST-voltage[i])+input[i])*LEAK;
            }
            // Experimental game-to-brain stimulation ports spread over the complete
            // index space. These are not asserted to be identified sensory neurons.
            float stim=touch*34f+obstacle*25f+gait*8f;
            if(stim>0f){
                for(int k=0;k<24;k++){
                    int ix=(int)(((long)k*n)/24L);
                    voltage[ix]+=stim;
                }
            }
            for(int i=0;i<n;i++){
                if(refractory[i]>0) continue;
                if(voltage[i]>=THRESHOLD){
                    voltage[i]=RESET; refractory[i]=2; spikes++; totalSpikes++;
                    if(i>=third){active++; if((i&1)==0)left++;else right++;}
                    for(int e=offsets[i];e<offsets[i+1];e++){
                        input[targets[e]]+=weights[e]*SYNAPSE_GAIN;
                    }
                }
            }
        }
        lastSpikes=spikes; activeCount=active;
        activity=spikes/(float)STEPS_PER_CALL;
        stride=clamp(active/(float)Math.max(1,n/1500),0,1);
        turn=clamp((right-left)/(float)Math.max(1,right+left),-1,1);
        // The game director converts emergent population activity into a small
        // procedural event vocabulary. This is gameplay logic, not fly behaviour.
        director=activity>0.01f ? (totalSpikes/17)%3+1 : 0;
        try{
            JSONObject o=new JSONObject();
            o.put("source","FlyWire FAFB v783 / Shiu LIF model");
            o.put("neurons",n); o.put("connections",m);
            o.put("windowSpikes",spikes); o.put("totalSpikes",totalSpikes);
            o.put("activeCount",active); o.put("activity",activity);
            o.put("stride",stride); o.put("turn",turn); o.put("director",director);
            return o.toString();
        }catch(Exception e){return "{\"error\":\"snapshot failed\"}";}
    }

    public synchronized String infoJson(){
        try{
            JSONObject o=new JSONObject();
            o.put("source","FlyWire FAFB v783");
            o.put("model","Shiu et al. whole-brain leaky integrate-and-fire");
            o.put("neurons",n); o.put("connections",m);
            o.put("license","See FlyWire data terms and cited publication");
            JSONArray sampleIds=new JSONArray(), edges=new JSONArray();
            for(int i=0;i<Math.min(n,240);i++) sampleIds.put(Long.toString(ids[i]));
            int emitted=0;
            for(int i=0;i<n && emitted<900;i++){
                for(int e=offsets[i];e<offsets[i+1] && emitted<900;e++){
                    JSONArray edge=new JSONArray(); edge.put(i); edge.put(targets[e]); edge.put(weights[e]);
                    edges.put(edge); emitted++;
                }
            }
            o.put("sampleIds",sampleIds); o.put("sampleEdges",edges);
            return o.toString();
        }catch(Exception e){return "{\"error\":\"brain metadata unavailable\"}";}
    }

    public synchronized String statusJson(){
        return "{\"stride\":"+stride+",\"turn\":"+turn+",\"spikes\":"+totalSpikes+",\"activity\":"+activity+"}";
    }
    private static float clamp(float x,float a,float b){return Math.max(a,Math.min(b,x));}
}
