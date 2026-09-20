import com.lumidex.track.SaveParser;
import java.security.MessageDigest;
import java.util.*;
public class SaveParserTest {
    static void sign(byte[] data) throws Exception {
        Arrays.fill(data, 0xE9818, 0xE9828, (byte)0);
        byte[] digest = MessageDigest.getInstance("MD5").digest(data);
        System.arraycopy(digest, 0, data, 0xE9818, 16);
    }
    static void rejects(byte[] data) throws Exception {
        try { SaveParser.parse(data); throw new AssertionError("Invalid save accepted"); }
        catch (IllegalArgumentException expected) { }
    }
    public static void main(String[] args) throws Exception {
        for (int size : new int[]{0xE9828, 0xEDC20, 0xEED8C, 0xEF0A4}) {
            byte[] data = new byte[size];
            data[0] = (byte)(size == 0xE9828 ? 0x25 : size == 0xEDC20 ? 0x2C : size == 0xEED8C ? 0x32 : 0x34);
            data[0x7A328] = 3; data[0x7A328 + 24*4] = 2; data[0x7A328 + 492*4] = 3;
            sign(data);
            if (!SaveParser.parse(data).equals(Arrays.asList(1,493))) throw new AssertionError("Caught/seen mapping failed");
            data[0] ^= 1; sign(data); rejects(data);
            data[0] ^= 1; sign(data);
            data[42] ^= 1; rejects(data);
            data[0x7A328] = 4; sign(data); rejects(data);
        }
        rejects(new byte[100]);
        byte[] lumi = new byte[0xEF0A4];
        lumi[0] = 0x34; lumi[1] = 1; lumi[2] = -1; lumi[3] = -1;
        lumi[0x7A328] = 0x23; // species 1 caught, species 2 seen
        lumi[0x7A328 + 958/2] = 3; // species 959 caught
        if (!SaveParser.parse(lumi).equals(Arrays.asList(1,959))) throw new AssertionError("Lumi nibble mapping failed");
        lumi[0x7A328] = 0x43; rejects(lumi);
        if (args.length > 0) {
            byte[] sample = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(args[0]));
            SaveParser.Result parsed = SaveParser.parseResult(sample);
            List<Integer> result = parsed.species;
            if (!result.containsAll(Arrays.asList(1,2,4,479)) || result.contains(3)) throw new AssertionError("Sample regression");
            System.out.println("Sample parsed: " + result.size() + " caught species IDs; vanilla MD5 valid=" + SaveParser.hasValidChecksum(sample));
            System.out.print("Owned forms: ");
            for (int[] pair : parsed.forms) System.out.print(pair[0] + ":" + pair[1] + " ");
            System.out.println();
        }
        System.out.println("SaveParser tests passed: caught vs seen, boundaries, sizes, checksum and invalid states");
    }
}
