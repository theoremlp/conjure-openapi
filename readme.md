# Conjure OpenAPI [![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](https://opensource.org/licenses/Apache-2.0)

CLI to generate OpenAPI yaml specifications from [Conjure API definitions](https://github.com/palantir/conjure);
it implements the contract described
[here](https://github.com/palantir/conjure/blob/master/docs/rfc/002-contract-for-conjure-generators.md).

## Usage

    Usage: conjure-openapi generate <input> <output>

    Generate OpenAPI yaml for a Conjure API
        <input>      Path to the input IR file
        <output>     Output directory for generated source

## Conjure Generator

Run conjure-openapi through gradle-conjure using the [conjureGenerators](https://github.com/palantir/gradle-conjure?tab=readme-ov-file#configurations) configuration and specifying the jar we publish to [Maven Central](https://central.sonatype.com/artifact/com.theoremlp.conjure.openapi/conjure-openapi):

```
dependencies {
    conjureGenerators 'com.theoremlp.conjure.openapi:conjure-openapi:<version>'
}
```

## Contributing

To contribute to conjure-openapi:

- Install Java 17 - `brew tap homebrew/cask-versions && brew install --cask homebrew/cask-versions/zulu17`
- Setup `JAVA_HOME` - `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`
- Install Intellij via the [Jetbrains Toolbox](https://www.jetbrains.com/toolbox-app/)
- Open the project using Intellij

## License

This project is made available under the [Apache 2.0 License](./LICENSE).
