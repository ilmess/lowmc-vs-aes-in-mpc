# LowMC vs AES in MPC

This repository compares AES, LowMC, and a baseline implementation in a two-party MPC setting using FRESCO TinyTables.

## Folders

```text
aes/
lowmc/
baseline/
```

Each folder contains the implementation, Maven files, and test scripts.

## How to Run

### LowMC

```bash
cd lowmc/test
chmod +x *.sh
./setup_lowmc.sh 128 128 22 20
./run.sh
```

Run multiple times:

```bash
./run_x_times.sh 10
```

### AES

```bash
cd aes/test
chmod +x *.sh
./setup_aes.sh
./run.sh
```

Run multiple times:

```bash
./run_x_times.sh 10
```

### Baseline

```bash
cd baseline/test
chmod +x *.sh
./setup_baseline.sh
./run.sh
```

Run multiple times:

```bash
./run_x_times.sh 10
```

## Output

Each run writes results to:

```text
experiment_log.txt
```

The log contains:

```text
PREPRO=<time> ms ONLINE=<time> ms TOTAL=<time> ms
```

## Idea

Each experiment runs two local MPC parties, measures preprocessing time and online time, then adds them together.

This makes it easy to reproduce and compare AES, LowMC, and the baseline.