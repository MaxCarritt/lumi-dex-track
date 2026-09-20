package com.lumidex.track;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashSet;

/** Read-only BDSP and Luminescent revision-1 species caught-state decoder. */
public final class SaveParser {
    public static final class Result {
        public final List<Integer> species;
        public final List<int[]> forms;
        Result(List<Integer> species, List<int[]> forms) { this.species = species; this.forms = forms; }
    }
    public static boolean isLuminescent(byte[] data) {
        return data.length == 0xEF0A4 && (data[0] & 255) == 0x34 && data[1] == 1 && data[2] == -1 && data[3] == -1;
    }
    public static boolean hasValidChecksum(byte[] data) throws Exception {
        if (data.length < 0xE9828) return false;
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data, 0, 0xE9818);
        md.update(new byte[16]);
        md.update(data, 0xE9828, data.length - 0xE9828);
        return Arrays.equals(md.digest(), Arrays.copyOfRange(data, 0xE9818, 0xE9828));
    }
    public static List<Integer> parse(byte[] data) throws Exception {
        return parseResult(data).species;
    }
    public static Result parseResult(byte[] data) throws Exception {
        int size = data.length;
        if (size != 0xE9828 && size != 0xEDC20 && size != 0xEED8C && size != 0xEF0A4)
            throw new IllegalArgumentException("Unsupported save size. Select the game's raw SaveData.bin folder.");
        boolean lumi = isLuminescent(data);
        int expectedRevision = size == 0xE9828 ? 0x25 : size == 0xEDC20 ? 0x2C : size == 0xEED8C ? 0x32 : 0x34;
        if (!lumi && ((data[0] & 255) != expectedRevision || data[1] != 0 || data[2] != 0 || data[3] != 0))
            throw new IllegalArgumentException("Unsupported save revision.");
        // The supplied Lumi rev1 save does not validate with vanilla BDSP's MD5.
        // The monitor requires two identical complete reads and reports this limitation.
        if (!lumi && !hasValidChecksum(data))
            throw new IllegalArgumentException("Save is incomplete or its checksum is invalid; waiting for the next save.");
        List<Integer> caught = new ArrayList<>();
        int count = lumi ? 1025 : 493;
        for (int species = 1; species <= count; species++) {
            int index = species - 1;
            int state;
            if (lumi) {
                state = ((data[0x7A328 + index / 2] & 255) >> ((index % 2) * 4)) & 15;
            } else {
                int offset = 0x7A328 + index * 4;
                state = (data[offset] & 255) | ((data[offset+1] & 255) << 8) | ((data[offset+2] & 255) << 16) | ((data[offset+3] & 255) << 24);
            }
            if (state < 0 || state > 3) throw new IllegalArgumentException("Unsupported or incomplete Pokédex layout.");
            if (state == 3) caught.add(species);
        }
        List<int[]> forms = new ArrayList<>();
        if (lumi) readOwnedForms(data, forms);
        return new Result(caught, forms);
    }

    /** Extract valid species/form pairs from party, boxes, and daycare records. */
    private static void readOwnedForms(byte[] save, List<int[]> out) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        int[][] ranges = {{0x14098, 6}, {0x14EF4, 1200}, {0x96080, 2}};
        for (int[] range : ranges) for (int slot = 0; slot < range[1]; slot++) {
            int start = range[0] + slot * 0x158;
            if (start + 0x148 > save.length) continue;
            byte[] record = Arrays.copyOfRange(save, start, start + 0x158);
            if (allZero(record)) continue;
            byte[] plain = decrypt(record);
            if (plain == null) continue;
            int species = u16(plain, 8), form = plain[0x24] & 255;
            if (species < 1 || species > 1025 || !validChecksum(plain)) continue;
            String key = species + ":" + form;
            if (seen.add(key)) out.add(new int[]{species, form});
        }
    }
    private static boolean allZero(byte[] b) { for (byte v : b) if (v != 0) return false; return true; }
    private static int u16(byte[] b, int p) { return (b[p] & 255) | ((b[p + 1] & 255) << 8); }
    private static boolean validChecksum(byte[] b) {
        int sum = 0; for (int p = 8; p < 0x148; p += 2) sum = (sum + u16(b, p)) & 0xFFFF;
        return sum == u16(b, 6);
    }
    private static byte[] decrypt(byte[] encrypted) {
        byte[] b = encrypted.clone();
        int pv = (b[0]&255)|((b[1]&255)<<8)|((b[2]&255)<<16)|((b[3]&255)<<24);
        int sv = (pv >>> 13) & 31;
        int[] order = {0,1,2,3, 0,1,3,2, 0,2,1,3, 0,3,1,2, 0,2,3,1, 0,3,2,1,
            1,0,2,3, 1,0,3,2, 2,0,1,3, 3,0,1,2, 2,0,3,1, 3,0,2,1,
            1,2,0,3, 1,3,0,2, 2,1,0,3, 3,1,0,2, 2,3,0,1, 3,2,0,1,
            1,2,3,0, 1,3,2,0, 2,1,3,0, 3,1,2,0, 2,3,1,0, 3,2,1,0,
            0,1,2,3, 0,1,3,2, 0,2,1,3, 0,3,1,2, 0,2,3,1, 0,3,2,1,
            1,0,2,3, 1,0,3,2};
        long seed = pv & 0xFFFFFFFFL;
        for (int p = 8; p < 0x148; p += 2) { seed = (seed * 0x41C64E6DL + 0x6073) & 0xFFFFFFFFL; int x = (int)(seed >>> 16); b[p] ^= (byte)x; b[p+1] ^= (byte)(x >>> 8); }
        byte[] shuffled = b.clone(); int index = (sv % 32) * 4;
        for (int block = 0; block < 4; block++) System.arraycopy(b, 8 + order[index + block] * 80, shuffled, 8 + block * 80, 80);
        return shuffled;
    }
}
