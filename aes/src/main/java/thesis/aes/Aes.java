package thesis.aes;

import dk.alexandra.fresco.demo.cli.CmdLineUtil;
import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.binary.Binary;
import dk.alexandra.fresco.framework.builder.binary.ProtocolBuilderBinary;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.util.ByteAndBitConverter;
import dk.alexandra.fresco.framework.value.SBool;
import dk.alexandra.fresco.lib.bristol.BristolCrypto;
import dk.alexandra.fresco.suite.ProtocolSuite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public class Aes implements Application<List<Boolean>, ProtocolBuilderBinary> {

    private Boolean[] in;
    private int id;

    private static final int BLOCK_SIZE = 128;
    private static final int INPUT_LENGTH = 32;

    public Aes(int id, Boolean[] in) {
        this.in = in;
        this.id = id;
    }

    public static <ResourcePoolT extends ResourcePool> void main(
            String[] args) throws IOException {

        CmdLineUtil<ResourcePoolT, ProtocolBuilderBinary> util = new CmdLineUtil<>();

        util.addOption(
                Option.builder("in")
                        .desc("32 character hex input")
                        .longOpt("input")
                        .hasArg()
                        .build());

        CommandLine cmd = util.parse(args);

        Boolean[] input;

        int myId = util.getNetworkConfiguration().getMyId();

        if (myId == 1 || myId == 2) {

            if (!cmd.hasOption("in")) {
                throw new IllegalArgumentException(
                        "Player 1 and 2 must submit input");
            }

            if (cmd.getOptionValue("in").length() != INPUT_LENGTH) {

                throw new IllegalArgumentException(
                        "Input must be 32 hex characters");
            }

            input = ByteAndBitConverter.toBoolean(
                    cmd.getOptionValue("in"));

        } else {

            input = ByteAndBitConverter.toBoolean(
                    "00000000000000000000000000000000");
        }

        ProtocolSuite<ResourcePoolT, ProtocolBuilderBinary> suite = util.getProtocolSuite();

        SecureComputationEngine<ResourcePoolT, ProtocolBuilderBinary> sce = new SecureComputationEngineImpl<>(
                suite,
                util.getEvaluator());

        Aes aes = new Aes(myId, input);

        long startTime = System.nanoTime();

        List<Boolean> aesResult = sce.runApplication(
                        aes,
                        util.getResourcePool(),
                        util.getNetwork());

        long endTime = System.nanoTime();

        double elapsedMs = (endTime - startTime) / 1_000_000.0;

        util.closeNetwork();
        sce.close();

        boolean[] result = new boolean[BLOCK_SIZE];

        for (int i = 0; i < BLOCK_SIZE; i++) {
            result[i] = aesResult.get(i);
        }

        System.out.println("AES Ciphertext: " + ByteAndBitConverter.toHex(result));
        System.out.println("Execution time: " + elapsedMs + " ms");
    }

    @Override
    public DRes<List<Boolean>> buildComputation(
            ProtocolBuilderBinary producer) {

        return producer.seq(seq -> {

            Binary bin = seq.binary();

            List<DRes<SBool>> keyInputs = new ArrayList<>();

            List<DRes<SBool>> plainInputs = new ArrayList<>();

            if (id == 1) {

                for (boolean b : in) {
                    plainInputs.add(
                            bin.input(b, 1));

                    keyInputs.add(
                            bin.input(false, 2));
                }

            } else {

                for (boolean b : in) {

                    plainInputs.add(
                            bin.input(false, 1));

                    keyInputs.add(
                            bin.input(b, 2));
                }
            }

            return BristolCrypto
                    .using(seq)
                    .AES(
                            plainInputs,
                            keyInputs);

        }).seq((seq, aesResult) -> {

            List<DRes<Boolean>> opened = new ArrayList<>();

            for (SBool bit : aesResult) {

                opened.add(
                        seq.binary().open(bit));
            }

            return () -> opened;

        }).seq((seq, opened) ->

        () -> opened.stream()
                .map(DRes::out)
                .collect(Collectors.toList()));
    }
}