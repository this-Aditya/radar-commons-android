# Android Avro implementation

Avro implementation that does not depend on ThreadLocal, external JSON library or ClassValue. It is binary compatible with classes generated by the Avro Tools class generator. It also includes JSON and binary encoders and decoders, but Codecs, Utf8 and logical type support is removed.

The code is largely copied from [Apache Avro](https://github.com/apache/avro) Java Avro implementation.
