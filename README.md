# MCAPL/Gwendolen Search-and-Rescue Case Study

This repository contains the **MCAPL/Gwendolen Search-and-Rescue case study** used in our work on the Event-B formalisation and verification of BDI agents.

The repository is based on the existing MCAPL framework. The complete MCAPL source tree is retained so that the exact Gwendolen and AJPF setup used in our experiments, including non-deterministic agent execution, can be reproduced directly.

## Search-and-Rescue Example

The case-study files are located in:

```text
src/examples/gwendolen/search_rescue/
```

The example models a search-and-rescue drone and contains instances ranging from **2 to 10 locations**.

For each instance `n`, where `n = 2, ..., 10`, the directory contains:

```text
search_rescue_agent<n>.gwen   Gwendolen agent program
search_rescue_agent<n>.ail    AIL execution configuration
search_rescue_agent<n>.jpf    AJPF model-checking configuration
search_rescue_agent<n>.psl    properties checked by AJPF
```

The shared environment is:

```text
SearchRescueEnv.java
```

The `.ail` configurations use MCAPL's `NondetGwendolenAgentBuilder` to allow nondeterministic choices during execution and model checking.

## Running the Example

After MCAPL has been installed and configured normally, the examples can be run using the corresponding `.ail` files.

For example, the 2-location instance uses:

```text
src/examples/gwendolen/search_rescue/search_rescue_agent2.ail
```

The corresponding AJPF model-checking configuration is:

```text
src/examples/gwendolen/search_rescue/search_rescue_agent2.jpf
```

Replace `2` with any value from `3` to `10` to run or verify the corresponding problem size.

The repository preserves the exact case-study programs and configurations used for the execution and scalability experiments reported in the paper.
