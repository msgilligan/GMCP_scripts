package com.github.abrarsyed.gmcp

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader

class SourceRemapper
{
	def Map methods
	def Map fields
	def Map params

	final String METHOD = /^( {4}|\t)(?:[\w]+ )*(/+METHOD_SMALL+/)\(/
	final String FIELD = /^( {4}|\t)(?:[\w]+ )*(/+FIELD_SMALL+/) *(?:=|;)/
	final String METHOD_SMALL = /func_[0-9]+_[a-zA-Z_]+/
	final String FIELD_SMALL = /field_[0-9]+_[a-zA-Z_]+/
	final String PARAM = /p_[\w]+_\d+_/

	SourceRemapper(File methodCSV, File fieldCSV, File paramCSV)
	{
		def reader = getReader(methodCSV)
		methods = [:]
		reader.readAll().each
		{
			methods[it[0]] = [name:it[1], javadoc:it[3]]
		}

		reader = getReader(fieldCSV)
		fields = [:]
		reader.readAll().each
		{
			fields[it[0]] = [name:it[1], javadoc:it[3]]
		}

		reader = getReader(paramCSV)
		params = [:]
		reader.readAll().each
		{
			params[it[0]] = it[1]
		}
	}

	def remapFile(File file)
	{
		def text = file.text;file
		def javadoc, newline

		// search methods to javadoc
		text.findAll(METHOD) { match, indent, name ->
			// rename
			newline = match.replaceAll(name, methods[name]['name'])

			// get javadoc
			javadoc = buildJavadoc(indent, methods[name]['javadoc'])

			// replace method in-file
			text.replace(match, javadoc+newline)
		}

		// search for fields to javadoc
		text.findAll(FIELD) { match, indent, name ->
			// rename
			newline = match.replaceAll(name, fields[name]['name'])

			// get javadoc
			javadoc = buildJavadoc(indent, fields[name]['javadoc'])

			// replace method in-file
			text.replace(match, javadoc+newline)
		}

		// FAR all parameters
		def match = text =~ PARAM
		while(match.find())
		{
				match.replaceAll(params[match.group()])
		}

		// FAR all methods
		match = text =~ METHOD_SMALL
		while(match.find())
		{
			if (methods[match.group()])
				match.replaceAll(methods[match.group()]['name'])
		}

		// FAR all fields
		match = text =~ FIELD_SMALL
		while(match.find())
		{
			if (fields[match.group()])
				match.replaceAll(fields[match.group()]['name'])
		}

		file.write(text)
	}

	private buildJavadoc(indent, javadoc)
	{
		def out = indent+"/**"+System.lineSeparator
		out += indent+" * "+javadoc+System.lineSeparator
		out += indent+" */"+System.lineSeparator
	}

	private CSVReader getReader(File file)
	{
		return new CSVReader(new FileReader(file), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.DEFAULT_ESCAPE_CHARACTER, 1, false)
	}
}
