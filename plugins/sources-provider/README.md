# FASTEN Sources Provider

The Sources Provider plugin normalizes the payloads of upstream Kafka topics such that downstream source code analyzers are simplified.

In the typical deployment, one Sources Provider plugin is subscribed to each MetadataDB{C|Python|Java}Extension topic.

The plugin's output looks as follows at the specified Kafka output topic(s).
```
{
  "forge": "...",
  "product": "...",
  "version": "...",
  "sourcePath": "..."
}
```

The supported inputs are described below.

### Java/Maven (fasten.MetadataDBJavaExtension[.priority].out)
The plugin tried to locate a payload that includes following fields:
 - `forge`
 - `groupId`
 - `artifactId`
 - `version`
 - `sourcesUrl`
 
If `forge` is equal to "mvn" the `sourcesUrl` is queried to download a sources jar, which is unpacked at a directory which is then pointed to by  `sourcePath`. `groupId` and `artifactId` are combined into `product`. 

### C/Debian (fasten.MetadataDBCExtension[.priority].out)
The plugin tried to locate a payload that includes following fields:
- `forge`
- `product`
- `version`
- `sourcePath`

These fields are passed along to the output without modification.

### Python/PyPI (fasten.MetadataDBPythonExtension[.priority].out)
The plugin tried to locate a payload that includes following fields:
- `forge`
- `product`
- `version`
- `sourcePath`

These fields are passed along to the output without modification.

## Required parameters
The following CLI parameters need to be set:
- `--plugin` [eu.f4sten.sourcesprovider.Main] (cannot be different)
- `--kafka.url` [localhost:19092]
- `--sourcesprovider.kafkaIn` [fasten.MetadataDBJavaExtension]
- `--instanceId` [sourcesprovider-1]
- `--baseDir` [/mnt/fasten]