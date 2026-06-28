package thesis.lowmc;

import dk.alexandra.fresco.demo.cli.CmdLineUtil;
import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.binary.Binary;
import dk.alexandra.fresco.framework.builder.binary.ProtocolBuilderBinary;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.value.SBool;
import dk.alexandra.fresco.suite.ProtocolSuite;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LowMC<ResourcePoolT extends ResourcePool>
    implements Application<List<Boolean>, ProtocolBuilderBinary> {

  // Set by the setup script at compile time for the selected LowMC
  // configuration, e.g. ./setup_lowmc.sh 128 128 32 10 for LowMC-10. 
  private static final int BLOCK_SIZE = 128;
  private static final int KEY_SIZE = 128;
  private static final int ROUNDS = 30;
  private static final int SBOXES = 30;

  private final int myId;
  private final boolean[] myInput;
  private final LowMCParameters params;

  public LowMC(int myId, boolean[] myInput) {
    this.myId = myId;
    this.myInput = myInput;
    this.params = LowMCParameters.generate();
  }
  // Main Method
  // ---------------------------------------------------------------------------------------------------------------------

  public static <ResourcePoolT extends ResourcePool> void main(String[] args) throws IOException {
    CmdLineUtil<ResourcePoolT, ProtocolBuilderBinary> util = new CmdLineUtil<>();

    util.addOption(Option.builder("in")
        .longOpt("input")
        .hasArg()
        .desc("Party 1 gives plaintext, 32 hex chars. Party 2 gives key, 32 hex chars.")
        .build());

    CommandLine cmd = util.parse(args);
    int myId = util.getNetworkConfiguration().getMyId();

    boolean[] input = readInput(cmd, myId);

    ProtocolSuite<ResourcePoolT, ProtocolBuilderBinary> suite = util.getProtocolSuite();

    SecureComputationEngine<ResourcePoolT, ProtocolBuilderBinary> sce = new SecureComputationEngineImpl<>(suite,
        util.getEvaluator());

    LowMC<ResourcePoolT> app = new LowMC<>(myId, input);

    // Measure execution time

    long startTime = System.nanoTime();

    List<Boolean> ciphertextBits = sce.runApplication(app, util.getResourcePool(), util.getNetwork());

    long endTime = System.nanoTime();

    double elapsedMs = (endTime - startTime) / 1_000_000.0;

    util.closeNetwork();
    sce.close();

    boolean[] ciphertext = new boolean[BLOCK_SIZE];

    for (int i = 0; i < BLOCK_SIZE; i++) {
      ciphertext[i] = ciphertextBits.get(i);
    }

    System.out.println("Ciphertext: " + bitsToHex(ciphertext));
    System.out.println("Execution time: " + elapsedMs + " ms");
  }

  // Handle Input and Output
  // ----------------------------------------------------------------------------------------------------------

  private static boolean[] readInput(CommandLine cmd, int myId) {

    if (myId != 1 && myId != 2) {
      return new boolean[BLOCK_SIZE];
    }

    if (!cmd.hasOption("in")) {
      throw new IllegalArgumentException("Missing input");
    }

    String input = cmd.getOptionValue("in").toLowerCase();

    if (myId == 1) {
      if (input.length() != BLOCK_SIZE / 4) {
        throw new IllegalArgumentException(
            "Party 1 plaintext must be exactly " + BLOCK_SIZE / 4 + " hex characters");
      }

      return hexToBits(input, BLOCK_SIZE);
    }

    if (input.length() != KEY_SIZE / 4) {
      throw new IllegalArgumentException(
          "Party 2 key must be exactly " + KEY_SIZE / 4 + " hex characters");
    }

    return hexToBits(input, KEY_SIZE);
  }

  private static boolean[] hexToBits(String hex, int bitLength) {
    boolean[] bits = new boolean[bitLength];

    for (int i = 0; i < bitLength / 4; i++) {
      int value = Character.digit(hex.charAt(hex.length() - 1 - i), 16);

      if (value < 0) {
        throw new IllegalArgumentException("Input must contain only hexadecimal characters");
      }

      for (int bit = 0; bit < 4; bit++) {
        bits[4 * i + bit] = ((value >>> bit) & 1) == 1;
      }
    }

    return bits;
  }

  private static String bitsToHex(boolean[] bits) {
    StringBuilder hex = new StringBuilder();

    for (int i = bits.length / 4 - 1; i >= 0; i--) {
      int value = 0;

      for (int bit = 0; bit < 4; bit++) {
        if (bits[4 * i + bit]) {
          value |= 1 << bit;
        }
      }

      hex.append(Integer.toHexString(value));
    }

    return hex.toString();
  }

  // Handle FRESCO Computation
  // --------------------------------------------------------------------------------------------------------

  @Override
  public DRes<List<Boolean>> buildComputation(ProtocolBuilderBinary builder) {
    return builder.seq(seq -> {
      Binary bin = seq.binary();

      List<DRes<SBool>> plaintext = new ArrayList<>();
      List<DRes<SBool>> key = new ArrayList<>();

      for (int i = 0; i < BLOCK_SIZE; i++) {
        boolean bit = myId == 1 && myInput[i];
        plaintext.add(bin.input(bit, 1));
      }

      for (int i = 0; i < KEY_SIZE; i++) {
        boolean bit = myId == 2 && myInput[i];
        key.add(bin.input(bit, 2));
      }

      List<DRes<SBool>> ciphertext = encrypt(seq, plaintext, key, params);

      List<DRes<Boolean>> opened = new ArrayList<>();

      for (DRes<SBool> bit : ciphertext) {
        opened.add(bin.open(bit));
      }

      return () -> opened;
    }).seq((seq, opened) -> {
      List<Boolean> result = new ArrayList<>();

      for (DRes<Boolean> bit : opened) {
        result.add(bit.out());
      }

      return () -> result;
    });
  }

  // LowMC Implementation
  // -------------------------------------------------------------------------------------------------------------

  private static List<DRes<SBool>> encrypt(
      ProtocolBuilderBinary builder,
      List<DRes<SBool>> plaintext,
      List<DRes<SBool>> key,
      LowMCParameters params) {

    List<DRes<SBool>> state = xorVectors(builder, plaintext, computeRoundKey(builder, key, params.keyMatrices[0]));

    for (int round = 0; round < ROUNDS; round++) {
      state = applySBoxes(builder, state);
      state = multiplyMatrix(builder, params.linearMatrices[round], state);
      state = xorConstant(builder, state, params.roundConstants[round]);
      state = xorVectors(builder, state,
          computeRoundKey(builder, key, params.keyMatrices[round + 1]));
    }

    return state;
  }

  private static List<DRes<SBool>> applySBoxes(
      ProtocolBuilderBinary builder,
      List<DRes<SBool>> input) {

    Binary bin = builder.binary();
    List<DRes<SBool>> output = new ArrayList<>(Collections.nCopies(BLOCK_SIZE, null));

    for (int box = 0; box < SBOXES; box++) {
      int i = box * 3;

      DRes<SBool> a = input.get(i + 2);
      DRes<SBool> b = input.get(i + 1);
      DRes<SBool> c = input.get(i);

      DRes<SBool> ab = bin.and(a, b);
      DRes<SBool> ac = bin.and(a, c);
      DRes<SBool> bc = bin.and(b, c);

      output.set(i + 2, xorMany(builder, a, bc));
      output.set(i + 1, xorMany(builder, a, b, ac));
      output.set(i, xorMany(builder, a, b, c, ab));
    }

    for (int i = 3 * SBOXES; i < BLOCK_SIZE; i++) {
      output.set(i, input.get(i));
    }

    return output;
  }

  private static List<DRes<SBool>> multiplyMatrix(
      ProtocolBuilderBinary builder,
      boolean[][] matrix,
      List<DRes<SBool>> vector) {

    Binary bin = builder.binary();
    List<DRes<SBool>> result = new ArrayList<>();

    for (int row = 0; row < BLOCK_SIZE; row++) {
      DRes<SBool> value = bin.known(false);

      for (int col = 0; col < BLOCK_SIZE; col++) {
        if (matrix[row][col]) {
          value = bin.xor(value, vector.get(col));
        }
      }

      result.add(value);
    }

    return result;
  }

  private static List<DRes<SBool>> xorVectors(
      ProtocolBuilderBinary builder,
      List<DRes<SBool>> left,
      List<DRes<SBool>> right) {

    Binary bin = builder.binary();
    List<DRes<SBool>> result = new ArrayList<>();

    for (int i = 0; i < left.size(); i++) {
      result.add(bin.xor(left.get(i), right.get(i)));
    }

    return result;
  }

  private static List<DRes<SBool>> xorConstant(
      ProtocolBuilderBinary builder,
      List<DRes<SBool>> vector,
      boolean[] constant) {

    Binary bin = builder.binary();
    List<DRes<SBool>> result = new ArrayList<>();

    for (int i = 0; i < vector.size(); i++) {
      if (constant[i]) {
        result.add(bin.xor(vector.get(i), bin.known(true)));
      } else {
        result.add(vector.get(i));
      }
    }

    return result;
  }

  // Helper Functions
  // -----------------------------------------------------------------------------------------------------------------
  @SafeVarargs
  private static DRes<SBool> xorMany(
      ProtocolBuilderBinary builder,
      DRes<SBool>... bits) {

    Binary bin = builder.binary();
    DRes<SBool> result = bin.known(false);

    for (DRes<SBool> bit : bits) {
      result = bin.xor(result, bit);
    }

    return result;
  }

  // Generator Classes/Functions
  // -------------------------------------------------------------------------------------------------------

  static class LowMCParameters {
    boolean[][][] linearMatrices = new boolean[ROUNDS][BLOCK_SIZE][BLOCK_SIZE];
    boolean[][] roundConstants = new boolean[ROUNDS][BLOCK_SIZE];
    boolean[][][] keyMatrices = new boolean[ROUNDS + 1][BLOCK_SIZE][KEY_SIZE];

    static LowMCParameters generate() {
      LowMCParameters params = new LowMCParameters();
      GrainGenerator generator = new GrainGenerator();

      for (int round = 0; round < ROUNDS; round++) {
        do {
          for (int row = 0; row < BLOCK_SIZE; row++) {
            for (int col = 0; col < BLOCK_SIZE; col++) {
              params.linearMatrices[round][row][col] = generator.nextBit();
            }
          }
        } while (rank(params.linearMatrices[round], BLOCK_SIZE) != BLOCK_SIZE);
      }

      for (int round = 0; round < ROUNDS; round++) {
        for (int i = 0; i < BLOCK_SIZE; i++) {
          params.roundConstants[round][i] = generator.nextBit();
        }
      }

      for (int round = 0; round <= ROUNDS; round++) {
        do {
          for (int row = 0; row < BLOCK_SIZE; row++) {
            for (int col = 0; col < KEY_SIZE; col++) {
              params.keyMatrices[round][row][col] = generator.nextBit();
            }
          }
        } while (rank(params.keyMatrices[round], KEY_SIZE) < KEY_SIZE);
      }

      return params;
    }

    private static int rank(boolean[][] matrix, int width) {
      boolean[][] copy = new boolean[matrix.length][width];

      for (int i = 0; i < matrix.length; i++) {
        copy[i] = Arrays.copyOf(matrix[i], width);
      }

      int rank = 0;

      for (int col = width - 1; col >= 0 && rank < copy.length; col--) {
        int pivot = rank;

        while (pivot < copy.length && !copy[pivot][col]) {
          pivot++;
        }

        if (pivot == copy.length) {
          continue;
        }

        boolean[] temp = copy[rank];
        copy[rank] = copy[pivot];
        copy[pivot] = temp;

        for (int row = rank + 1; row < copy.length; row++) {
          if (copy[row][col]) {
            for (int j = 0; j < width; j++) {
              copy[row][j] ^= copy[rank][j];
            }
          }
        }

        rank++;
      }

      return rank;
    }
  }

  private static List<DRes<SBool>> computeRoundKey(
      ProtocolBuilderBinary builder,
      List<DRes<SBool>> key,
      boolean[][] keyMatrix) {

    Binary bin = builder.binary();
    List<DRes<SBool>> roundKey = new ArrayList<>();

    for (int row = 0; row < BLOCK_SIZE; row++) {
      DRes<SBool> value = bin.known(false);

      for (int col = 0; col < KEY_SIZE; col++) {
        if (keyMatrix[row][col]) {
          value = bin.xor(value, key.get(col));
        }
      }

      roundKey.add(value);
    }

    return roundKey;
  }

  static class GrainGenerator {
    private final boolean[] state = new boolean[80];
    private int index = 0;

    GrainGenerator() {
      Arrays.fill(state, true);

      for (int i = 0; i < 160; i++) {
        update();
        index = (index + 1) % 80;
      }
    }

    boolean nextBit() {
      while (true) {
        update();
        boolean choice = state[index];

        index = (index + 1) % 80;

        update();
        boolean bit = state[index];

        index = (index + 1) % 80;

        if (choice) {
          return bit;
        }
      }
    }

    private void update() {
      state[index] = state[index]
          ^ state[(index + 13) % 80]
          ^ state[(index + 23) % 80]
          ^ state[(index + 38) % 80]
          ^ state[(index + 51) % 80]
          ^ state[(index + 62) % 80];
    }
  }
}
