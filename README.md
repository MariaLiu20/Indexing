# Indexing

Implement an inverted index with positional information. Explore indexing and query processing on the scenes from the complete works of Shakespeare.

## Installation

Download [json-simple-1.1.1.jar](http://www.java2s.com/Code/Jar/j/Downloadjsonsimple111jar.htm). Add the library to the class path in IntelliJ by selecting
```File > Project Structure > Modules```.
Under the
```Dependencies``` tab, click on the ```+``` icon, locate the Jar, and then click ```Ok```.
Load the project in IntelliJ to compile.

## Usage

```Java
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

# returns 68
indexing.getTermFreq("you", 192)
