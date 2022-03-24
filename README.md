# Indexing

Foobar is a Python library for dealing with word pluralization.

## Installation

Download [json-simple-1.1.1.jar](http://www.java2s.com/Code/Jar/j/Downloadjsonsimple111jar.htm). Add the library to the class path in IntelliJ by selecting
File > Project Structure > Modules
Under the
Dependencies``` tab, click on the ```+``` icon, locate the Jar, and then click ```Ok

## Usage

```Java
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

# returns 'words'
foobar.pluralize('word')

# returns 'geese'
foobar.pluralize('goose')

# returns 'phenomenon'
foobar.singularize('phenomena')