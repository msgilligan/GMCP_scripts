package com.github.abrarsyed.gmcp

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader

class SourceRemapper
{
	def Map methods
	def Map fields
	def Map params

	final String METHOD_SMALL = /func_[0-9]+_[a-zA-Z_]+/
	final String FIELD_SMALL = /field_[0-9]+_[a-zA-Z_]+/
	final String PARAM = /p_[\w]+_\d+_/
	final String METHOD = /(?m)^((?: |\t)*)(?:\w+ )*(/+METHOD_SMALL+/)\(/  // captures indent and name
	final String FIELD = /(?m)^((?: |\t)*)(?:\w+ )*(/+FIELD_SMALL+/) *(?:=|;)/ // capures indent and name

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
		def text = file.text
		def newline

		// search methods to javadoc
		text.findAll(METHOD) { match, indent, name ->

			if (methods[name])
			{
				// rename
				newline = match.replaceAll(name, methods[name]['name'])

				// get javadoc
				if (methods[name]['javadoc'])
				{
					newline = buildJavadoc(indent, methods[name]['javadoc'])+newline
				}

				// replace method in-file
				text.replace(match, newline)
			}
		}

		// search for fields to javadoc
		text.findAll(FIELD) { match, indent, name ->

			if (fields[name])
			{
				// rename
				newline = match.replaceAll(name, fields[name]['name'])

				// get javadoc
				if (fields[name]['javadoc'])
				{
					newline = buildJavadoc(indent, fields[name]['javadoc'])+newline
				}

				// replace method in-file
				text.replace(match, newline)
			}
		}

		// FAR all parameters
		def match = text =~ PARAM
		while(match.find())
		{
			text = text.replace(match.group(), params[match.group()])
		}

		// FAR all methods
		match = text =~ METHOD_SMALL
		while(match.find())
		{
			if (methods[match.group()])
				text = text.replace(match.group(), methods[match.group()]['name'])
		}

		// FAR all fields
		match = text =~ FIELD_SMALL
		while(match.find())
		{
			if (fields[match.group()])
				text = text.replace(match.group(), fields[match.group()]['name'])
		}
		
		// write file
		file.write(text)
	}

	private buildJavadoc(indent, javadoc)
	{
		def out = indent+"/**"+System.lineSeparator
		out += indent+" * "+javadoc+System.lineSeparator
		out += indent+" */"+System.lineSeparator
	}
	
	/**
	 * Converts a package path to a proper package declaration
	 */
	private String convertPackagePath(String newPackage)
	{
		return newPackage.replace('[\\]','.').replace('[/]', '.');
	}

	private CSVReader getReader(File file)
	{
		return new CSVReader(new FileReader(file), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.DEFAULT_ESCAPE_CHARACTER, 1, false)
	}
}
