# jarizard-jvm-backend

Download: https://ln5.sync.com/dl/725e306f0/tkd7skej-6e7avynd-qvf7df4m-ri95wbp3

## What it is
A library that creates jar files using the jar executable included in the JDK.

Also used as the backend in my GUI implementation: https://github.com/mpierson531/jarizard

## How to use it as a library
Collect input files or directories, an output file path (and optional main-class path, version, etc.), then call ```new Backend().jarIt(*paramters*)```. </br>
Now you have a new jar file!

## DSL
This also includes a simple DSL, which can be parsed from a file, then turned into a jar. </br>
The file containing the DSL code should be a ```.txt``` file.

### Examples of the DSL

```
input "path"

input = "path"

input {
  "path"
  "otherPath"
}
```

All of these syntaxes can be applied to all parameters (input, output, mainclass, version, useCompression, dependencies). </br>
Quotes are not necessary if the file path doesn't have any spaces. Quotes can be single or double.

Compression is used by default by the jar executable. If you don't want the resulting jar to be compressed, do this:

```
compress = 0

// or

compress = false
```

Once you have the DSL file, simply call ```new Backend().jarIt(dslFile)```

## Dependencies
As of right now, only Maven dependencies are supported.

DSL Example:
```
dependency = "org.jetbrains.kotlin.kotlin-stdlib 1.9.0"

dependencies {
  "org.jetbrains.kotlin.kotlin-stdlib 1.9.0"
  "org.scala-lang.scala-library 2.10.0"
}
```

Paths like this can easily be directly translated into Maven URLs, which is why only Maven dependencies are supported right now.

## Notes
As of right now, file paths are assumed to be absolute.

It is also assumed that the system this is run on has the ```JAVA_HOME``` environment variable set.

