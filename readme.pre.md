# clip

Composable, declarative command-line parsing for Scala.

Clip is a Scala library for creating command line interfaces, with minimal
boilerplate. It is highly configurable, comes with sensible defaults out of
the box, and has batteries included.

It aims to make the process of writing command line tools quick and fun.

[include:intro]

Clip is available for Scala on the JVM and Scala Native.

You can include it directly from maven central:

```scala
mvn"io.crashbox::clip::0.1.1"
```

It relies on `os-lib`. JDK 21 or greater is required. Certain features only work
on Linux and macOS, x86_64 and arm64.

## Feature tour

- [Annotation-based commands](#annotation-based-commands)
- [Parameter annotations](#parameter-annotations)
  - [Repeated parameters](#repeated-parameters)
  - [Parameter types](#parameter-types)
  - [Eager parameters](#eager-parameters)
- [Subcommands `app server`, `app fetch`, etc](#subcommands-app-server-app-fetch-etc)
  - [Sharing parameters and values](#sharing-parameters-and-values)
  - [Nested subcommands](#nested-subcommands)
- [Exception handling and error codes](#exception-handling-and-error-codes)
- [Automatically generated bash completion, with dynamic completions](#automatically-generated-bash-completion-with-dynamic-completions)
- [Utilities](#utilities)
  - [Output formatting](#output-formatting)
  - [ANSI color support](#ansi-color-support)
  - [Reading input](#reading-input)
  - [Launching applications](#launching-applications)
  - [Progress bars](#progress-bars)
  - [User directories](#user-directories)
- [Custom API traits](#custom-api-traits)

> [!NOTE]
> the examples shown here assume that the example code can be run as
> `./app`. If you'd like to play along, you have two options:
> 1. clone the repo, and run `./mill examples.<name of example>` instead of `./app`
> 2. use scala-cli, `./scala <example file> --` instead of `./app`

### Annotation-based commands

[include:minimal]

### Parameter annotations

[include:argannots]

#### Repeated parameters

[include:argrepeated]

#### Parameter types

[include:argtypes]

[include:argtypes2]

#### Eager parameters

[include:argeager]

### Subcommands `app server`, `app fetch`, etc

[include:commands]

#### Sharing parameters and values

[include:commandvalues]

#### Nested subcommands

[include:commandnested]

### Exception handling and error codes

[include:exceptions]

### Automatically generated bash completion, with dynamic completions

[include:completion1]

[include:completion2]

### Utilities

In addition to command line *parsing*, clip has many features built-in which
help you build *complete* terminal apps.

#### Output formatting

[include:utils-output]

#### ANSI color support

[include:utils-ansi]

#### Reading input

[include:utils-input]

#### Launching applications

[include:utils-launch]

#### Progress bars

[include:utils-progress]

#### User directories

[include:utils-userdirs]

### Custom API traits

[include:apitraits]

## Internals

Clip is architected around 3 layers:

1. A core command-line parsing system, which iterates over an array of strings
   and associates values to parameters. This is implemented in the `clip.getopt`
   package. This package is fully self-contained, and could be extracted if you
   want to build a different higher-level API, all the while still using the
   same command line syntax.

2. A core command modeling layer. This uses a bunch of case classes to model a
   hierarchy of commands. Each command is essentially a node that has:

     - a definition of parameters
     - a function to call which use said parameters
     - zero or more children

   This layer also implements an `invoke` function which, starting from a root
   command, parses arguments (using the lower layer), and selects child commands
   until calling a final command's function. This layer is contained in the
   `clip.dispatch` package.

3. A meta-programming layer, which defines API traits, annotations and macros to
   construct layer 2 commands based on Scala syntax, and looks up readers. This
   layer is contained in the `clip.derivation` package.

Any frequently used values and types are exported into the root `clip` package.

## Acknowledgments

Clip is inspired by a similarly-named Python project,
[Click](https://click.palletsprojects.com/en/stable/). It borrows many of its
concepts, particularly the CLI utilities.

The shell-completion helpers for formatting completions and debugging are copied
from kubectl and docker.

## See also

Clip is by no means the only library you can use to build command line
interfaces in Scala.

Here are a few other options to check out:

- [scopt](https://github.com/scopt/scopt) A flexible command-line argument
  parser.
- [mainargs](https://github.com/com-lihaoyi/mainargs) Also a declarative
  approach to building CLIs, based on annotations. Handles subcommand grouping
  differently than clip, and I don't think it is designed for nestable
  subcommands which can share data.
- [scala-argparse](github.com/jodersky/scala-argparse) Library for building
  command-line parsers, from the same author of this project. It can be thought
  of as the predecessor to this project, and shares the command-line parser
  code. If you've used it, you'll notice the command-line syntax looks very
  similar.

## Changes

- 0.1.1 2026-03-26

  Bump Scala version to avoid "sun.misc.Unsafe" deprecation warnings in newer JDKs.

- 0.1.0 2025-11-19

  Initial release
