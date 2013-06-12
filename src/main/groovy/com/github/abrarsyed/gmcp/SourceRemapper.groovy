package com.github.abrarsyed.gmcp

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader

class SourceRemapper
{
	def Map methods
	def Map fields
	def Map params
	def Map packages

	final String PACKAGE = 'package net.minecraft.src;'
	final String IMPORT = /(?m)^import net\.minecraft\.src\.(\w+);/
	final String CUSTOM_IMPORT = /(?m)^import (?:%s)(\w+);?[\r\n]/
	final String METHOD_SMALL = /func_[0-9]+_[a-zA-Z_]+/
	final String FIELD_SMALL = /field_[0-9]+_[a-zA-Z_]+/
	final String PARAM = /p_[\w]+_\d+_/
	final String METHOD = /(?m)^((?: |\t)*)(?:\w+ )*(/+METHOD_SMALL+/)\(/  // captures indent and name
	final String FIELD = /(?m)^((?: |\t)*)(?:\w+ )*(/+FIELD_SMALL+/) *(?:=|;)/ // capures indent and name

	SourceRemapper(files)
	{
		def reader = getReader(files["methods"])
		methods = [:]
		reader.readAll().each
		{
			methods[it[0]] = [name:it[1], javadoc:it[3]]
		}

		reader = getReader(files["fields"])
		fields = [:]
		reader.readAll().each
		{
			fields[it[0]] = [name:it[1], javadoc:it[3]]
		}

		reader = getReader(files["params"])
		params = [:]
		reader.readAll().each
		{
			params[it[0]] = it[1]
		}

		reader = getReader(files["packages"])
		packages = [:]
		reader.readAll().each
		{
			packages[it[0]] = it[1]
		}
	}

	private CSVReader getReader(File file)
	{
		return new CSVReader(new FileReader(file), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.DEFAULT_ESCAPE_CHARACTER, 1, false)
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
		newPackage.replace("\\",'.').replace("/", '.')
	}
	
	private String getCustomImport(String pack)
	{
		String.format(CUSTOM_IMPORT, convertPackagePath(pack)).replace(".", /\./)
	}

	def remapFile(File root, File file)
	{
		def className = file.name.replace(".java", '')
		def text = file.text
		def newline, matcher, thisPack, remImport

		// change package declaration
		if (packages[className])
		{
			// change package definition
			thisPack = convertPackagePath(packages[className])
			newline = "package "+thisPack+";"
			text = text.replace(PACKAGE, newline)
			
			// custom import.
			remImport = getCustomImport(thisPack)

			// change imports
			text.findAll(IMPORT) { match, imported ->
				// change to new packages
				if (packages[imported])
				{
					newline = "import "+thisPack+"."+imported+";"
					text = text.replace(match, newline)
				}
				
				// remove if non existant.
				if (match ==~ remImport)
				{
					text = text.replace(match, "")
				}
			}
		}

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
				text = text.replace(match, newline)
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
				text = text.replace(match, newline)
			}
		}

		// FAR all parameters
		matcher = text =~ PARAM
		while(matcher.find())
		{
			if (params[matcher.group()])
				text = text.replace(matcher.group(), params[matcher.group()])
		}

		// FAR all methods
		matcher = text =~ METHOD_SMALL
		while(matcher.find())
		{
			if (methods[matcher.group()])
				text = text.replace(matcher.group(), methods[matcher.group()]['name'])
		}

		// FAR all fields
		matcher = text =~ FIELD_SMALL
		while(matcher.find())
		{
			if (fields[matcher.group()])
				text = text.replace(matcher.group(), fields[matcher.group()]['name'])
		}

		if (packages[className])
		{
			// delete old file.
			file.delete()

			// replace file with new file
			file = new File(root, packages[className])
			file.mkdirs()
			file = new File(file, className+".java")
		}

		// write file
		file.write(text)
	}
}