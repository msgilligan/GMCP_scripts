package com.github.abrarsyed.gmcp

import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import org.gradle.internal.os.OperatingSystem

import com.google.common.io.Files

import difflib.DiffUtils

class Main
{
	public static OperatingSystem os = OperatingSystem.WINDOWS
	
	// to be made configureable by gradle
	public static String MC_VERSION = "1.5.1";
	public static String FORGE_VERSION = "7.7.2.682";  // maven style ???

	public static void main(args)
	{
		os = Util.getOS()

		Constants.DIR_LOGS.mkdirs()
		Constants.DIR_TEMP.mkdirs()
		Constants.DIR_RESOURCES.mkdirs()

		downloadStuff()

		println "MERGING JARS !!!!!!!!!!!!"

		mergeJars(Constants.JAR_CLIENT, Constants.JAR_SERVER)

		println "DeObfuscating With SpecialSource !!!!!!!!!!!!"

		deobfuscate(Constants.JAR_MERGED, Constants.JAR_DEOBF)

		println "Applying Exceptor (MCInjector) !!!!!!!!!!!!"

		inject()

		println "UNZIPPING !!!!!!!!!!!!"

		Util.unzip(Constants.JAR_EXCEPTOR, Constants.DIR_EXTRACTED, false)

		println "COPYING CLASSES!!!!!!!"

		copyClasses(Constants.DIR_EXTRACTED, Constants.DIR_CLASSES)

		println "DECOMPILING !!!!!!!!!!!!"

		decompile()

		println "APPLY FF FIXES!!!!!!!"

		FFPatcher.processDir(Constants.DIR_SOURCES)

		println "APPLYING MCP PATCHES!!!!!!!"

		patch()

		println "REMAPPING SOURCES AND INJECTING JAVADOCS!!!!!!!!"

		renameSources(Constants.DIR_SOURCES)

		println "COMPLETE!"
	}

	def static mergeJars(File client, File server)
	{
		//JarBouncer.MCPMerger(client, server, new File());
	}

	def static decompile()
	{
		Constants.DIR_SOURCES.mkdirs()
		JarBouncer.fernFlower(Constants.DIR_CLASSES.getPath(), Constants.DIR_SOURCES.getPath())
	}

	def static deobfuscate(File inJar, File outJar)
	{
		// load mapping
		JarMapping mapping = new JarMapping()
		mapping.loadMappings(new File(Constants.DIR_RESOURCES, "srgs/client.srg"))

		// load jar
		Jar input = Jar.init(inJar)

		// make remapper
		JarRemapper remapper = new JarRemapper(mapping)

		// remap jar
		remapper.remapJar(input, outJar)
	}

	def static inject()
	{
		JarBouncer.injector(Constants.JAR_DEOBF, Constants.JAR_EXCEPTOR, new File(Constants.DIR_RESOURCES, "joined.exc"))
	}

	def static copyClasses(File inDir, File outDir)
	{
		outDir.mkdirs()

		inDir.eachFileRecurse
		{
			// check ignored packages....
			if (isIgnored(it.getPath()))
			{
				return
			}

			def out = new File(it.getAbsolutePath().replace(inDir.absolutePath, outDir.absolutePath))
			if (it.isFile() && Files.getFileExtension(it.getPath()) == "class")
			{
				out.createNewFile()
				Files.copy(it, out)
			}
			else if (it.isDirectory())
			{
				out.mkdirs()
			}
		}
	}

	def static boolean isIgnored(String str)
	{
		switch(str)
		{
			case ~/.*?paulscode.*/: return true
			case ~/.*?com\\jcraft.*/: return true
			case ~/.*?isom.*/: return true
			case ~/.*?ibxm.*/: return true
			case ~/.*?de\\matthiasmann\\twl.*/: return true
			case ~/.*?org\\xmlpull.*/: return true
			case ~/.*?javax\\xml.*/: return true
			case ~/.*?com\\fasterxml.*/: return true
			case ~/.*?javax\\ws.*/: return true
			default: return false
		}
	}

	def static patch()
	{
		// USELESS!!!!
		// have to generate diffs... maybe...
		def rawPatch = Arrays.asList(new File(Constants.DIR_RESOURCES, "patches/client.patch").text.split(System.lineSeparator))

		def patchMap = [:]
		def patternDiff = /diff.*?minecraft\\(.+?) .*?/
		def patternStart = /^\+\+\+/

		def currentFile, startIndex = 0, endIndex = 0
		rawPatch.eachWithIndex
		{ obj, int i ->
			def matcher = obj =~ patternDiff
			if (matcher)
			{
				currentFile = matcher[0][1]
				endIndex = i-1
				if (endIndex > 0)
				{
					patchMap[currentFile] = DiffUtils.parseUnifiedDiff(rawPatch.subList(startIndex, endIndex))
					endIndex = 0
				}
				return
			}

			matcher = obj =~ patternStart
			if (matcher)
			{
				startIndex = i+1
			}
		}

		println "seems to have loaded patches"

		def currentLines, newLines, text, file
		patchMap.each
		{
			println "writing for "+it.getKey()
			file = new File(Constants.DIR_SOURCES, it.getKey())
			currentLines = Arrays.asList(file.text.split(System.lineSeparator))
			newLines = DiffUtils.patch(currentLines, it.getValue())
			text = newLines.join(System.lineSeparator)
			file.write(text)
		}

		println "seems to have patched the lines now."
	}

	def static downloadStuff()
	{
		def root = new File(Constants.DIR_MC_JARS)
		if (!root.exists() || !root.isDirectory())
			root.mkdirs()
			
		println "Downloading Minecraft"
		def mcver = MC_VERSION.replace('.', '_')
		Util.download(String.format(Constants.URL_MC_JAR, mcver), Constants.JAR_CLIENT)
		Util.download(String.format(Constants.URL_MCSERVER_JAR, mcver), Constants.JAR_SERVER)

		println "Downloading libraries"
		Constants.LIBRARIES.each {
			Util.download(Constants.URL_LIB_ROOT + it, new File(root, it))
		}

		println "Downloading natives"
		def natives = Constants.NATIVES[os.toString()]
		def nativesJar = new File(Constants.DIR_TEMP, "natives.jar")
		Util.download(natives, nativesJar)

		Util.unzip(natives, new File(root, "natives"), true)
		nativesJar.delete()
		
		println "Downloading Forge"
		def forge = new File(Constants.DIR_TEMP, "forge.zip");
		Util.download(String.format(Constants.URL_FORGE, MC_VERSION, FORGE_VERSION), forge)
		Util.unzip(forge, Constants.DIR_TEMP, true)
		forge.delete()
	}

	def static renameSources(File dir)
	{
		def methods = new File(Constants.DIR_RESOURCES, "csvs/methods.csv")
		def fields = new File(Constants.DIR_RESOURCES, "csvs/fields.csv")
		def params = new File(Constants.DIR_RESOURCES, "csvs/params.csv")

		SourceRemapper remapper = new SourceRemapper(methods, fields, params)

		dir.eachFileRecurse {
			if (it.isFile())
				remapper.remapFile(it)
		}
	}
}
