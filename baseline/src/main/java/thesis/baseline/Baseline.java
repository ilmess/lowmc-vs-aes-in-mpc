package thesis.baseline;

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
import org.apache.commons.cli.Option;

import java.io.IOException;

public class Baseline implements Application<Boolean, ProtocolBuilderBinary> {

  private final int myId;

  public Baseline(int myId) {
    this.myId = myId;
  }

  public static <ResourcePoolT extends ResourcePool> void main(String[] args) throws IOException {
    CmdLineUtil<ResourcePoolT, ProtocolBuilderBinary> util = new CmdLineUtil<>();

    util.addOption(Option.builder("in")
        .longOpt("input")
        .hasArg()
        .desc("ignored by baseline")
        .build());

    util.parse(args);

    int myId = util.getNetworkConfiguration().getMyId();

    ProtocolSuite<ResourcePoolT, ProtocolBuilderBinary> suite = util.getProtocolSuite();

    SecureComputationEngine<ResourcePoolT, ProtocolBuilderBinary> sce = new SecureComputationEngineImpl<>(suite,
        util.getEvaluator());

    Baseline app = new Baseline(myId);

    long startTime = System.nanoTime();

    Boolean result = sce.runApplication(app, util.getResourcePool(), util.getNetwork());

    long endTime = System.nanoTime();

    double elapsedMs = (endTime - startTime) / 1_000_000.0;

    util.closeNetwork();
    sce.close();

    System.out.println("Result: " + result);
    System.out.println("Execution time: " + elapsedMs + " ms");
  }

  @Override
  public DRes<Boolean> buildComputation(ProtocolBuilderBinary builder) {
    return builder.seq(seq -> {
      Binary bin = seq.binary();

      DRes<SBool> a = bin.input(myId == 1, 1);
      DRes<SBool> b = bin.input(myId == 2, 2);

      DRes<SBool> and = bin.and(a, b);

      return bin.open(and);
    });
  }
}