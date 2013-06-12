package com.github.abrarsyed.gmcp

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader

public class PackageFixer
{
	Map packages

	final String PACKAGE = 'package net.minecraft.src;'
	final String IMPORT = /(?m)^import net\.minecraft\.src\.(\w+);(?:\r\n|\n|\r)/

	public PackageFixer(file)
	{
		CSVReader reader = getReader(file)
		packages = [:]
		reader.readAll().each
		{
			packages.put(it[0], it[1])
		}
	}

	/**
	 * Converts a package path to a proper package declaration
	 */
	private String convertPackagePath(String newPackage)
	{
		newPackage.replace("\\",'.').replace("/", '.')
	}

	public fixPackages(File root, File file)
	{
		def className = file.name.replace(".java", '')

		if (!(packages[className]))
			return

		def text = file.text
		def newline, matcher, thisPack, newPack

		// change package definition
		thisPack = convertPackagePath(packages[className])
		newline = "package "+thisPack+";"
		text = text.replace(PACKAGE, newline)

		// change imports
		text.findAll(IMPORT) { match, imported ->
			// change to new packages
			if (packages[imported])
			{
				newPack = convertPackagePath(packages[imported])
				if (newPack == thisPack)
					newline = "";
				else
					newline = "import "+newPack+"."+imported+";"+System.lineSeparator;
					
				text = text.replace(match, newline)
			}
		}

		// delete old file.
		file.delete()

		// replace file with new file
		def newFile = new File(root, packages[className])
		newFile.mkdirs()
		newFile = new File(newFile, className+".java")
		newFile << text
	}

	private CSVReader getReader(File file)
	{
		return new CSVReader(new FileReader(file), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.DEFAULT_ESCAPE_CHARACTER, 1, false)
	}
}
