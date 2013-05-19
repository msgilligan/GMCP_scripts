package com.github.abrarsyed.gmcp

import net.md_5.specialsource.Jar;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;

import com.github.abrarsyed.gmcp.Util.OperatingSystem
import com.google.common.io.Files

import cpw.mods.fml.common.asm.transformers.MCPMerger;

import difflib.DiffUtils

class Main
{
	public static final File tmp = new File("tmp")
	public static final File resources = new File("resources")
	public static final File logs = new File(tmp, "logs")

	public static final File extracted = new File(tmp, "extracted")
	public static final File classes = new File(tmp, "classes")
	public static final File sources = new File(tmp, "sources")
	public static final File SS_JAR = new File(tmp, "Minecraft_SS.jar")
	public static final File EXC_JAR = new File(tmp, "Minecraft_EXC.jar")

	public static final File CLIENT_JAR = new File(tmp, "jars/Minecraft_Client.jar")
	public static final File SERVER_JAR = new File(tmp, "jars/Minecraft_Server.jar")
	public static final File JAR = new File(tmp, "jars/Minecraft.jar")

	public static OperatingSystem os

	public static void main(args)
	{
		os = Util.getOS()

		logs.mkdirs()
		tmp.mkdirs()
		resources.mkdirs()

		downloadStuff()
		
		println "MERGING JARS !!!!!!!!!!!!"
		
		mergeJars(CLIENT_JAR, SERVER_JAR)

		println "DeObfuscating With SpecialSource !!!!!!!!!!!!"

		deobfuscate(JAR, SS_JAR)

		println "Applying Exceptor (MCInjector) !!!!!!!!!!!!"

		inject()

		println "UNZIPPING !!!!!!!!!!!!"

		Util.unzip(EXC_JAR, extracted, false)

		println "COPYING CLASSES!!!!!!!"

		copyClasses(extracted, classes)

		println "DECOMPILING !!!!!!!!!!!!"

		decompile()

		println "APPLY FF FIXES!!!!!!!"

		FFPatcher.processDir(sources)

		println "APPLYING MCP PATCHES!!!!!!!"

		patch()

		println "REMAPPING SOURCES AND INJECTING JAVADOCS!!!!!!!!"

		renameSources(sources)

		println "COMPLETE!"
	}
	
	def static mergeJars(File client, File server)
	{
		//JarBouncer.MCPMerger(client, server, new File());
	}

	def static decompile()
	{
		sources.mkdirs()
		JarBouncer.fernFlower(classes.getPath(), sources.getPath())
	}

	def static deobfuscate(File inJar, File outJar)
	{
		// load mapping
		JarMapping mapping = new JarMapping();
		mapping.loadMappings(new File(resources, "srgs/client.srg"));

		// load jar
		Jar input = Jar.init(inJar);

		// make remapper
		JarRemapper remapper = new JarRemapper(mapping);

		// remap jar
		remapper.remapJar(input, outJar);
	}

	def static inject()
	{
		JarBouncer.injector(SS_JAR, EXC_JAR, new File(resources, "joined.exc"))
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
		def rawPatch = Arrays.asList(new File(resources, "patches/client.patch").text.split(System.lineSeparator))

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
			file = new File(sources, it.getKey())
			currentLines = Arrays.asList(file.text.split(System.lineSeparator))
			newLines = DiffUtils.patch(currentLines, it.getValue())
			text = newLines.join(System.lineSeparator)
			file.write(text)
		}

		println "seems to have patched the lines now."
	}

	def static downloadStuff()
	{
		def root = new File(tmp, "jars")
		if (!root.exists() || !root.isDirectory())
			root.mkdirs()

		ConfigParser parser = new ConfigParser(resources.path+"/mc_versions.cfg")

		def version = parser.getProperty("default", "current_ver")
		println "downloading Minecraft"
		Util.download(parser.getProperty(version, "client_url"), CLIENT_JAR)
		Util.download(parser.getProperty(version, "server_url"), SERVER_JAR)

		def dls = parser.getProperty("default", "libraries").split(/\s/)
		def url = parser.getProperty("default", "base_url")
		println "downloading libraries"
		dls.each
		{
			Util.download(url+it, new File(root, it))
		}

		println "downlaoding natives"
		dls = parser.getProperty("default", "natives").split(/\s/)[os.ordinal()]
		File nDL = new File(tmp, dls)
		Util.download(url+dls, nDL)

		Util.unzip(nDL, new File(root, "natives"), true)
		nDL.delete()
	}

	def static renameSources(File dir)
	{
		def methods = new File(resources, "csvs/methods.csv")
		def fields = new File(resources, "csvs/fields.csv")
		def params = new File(resources, "csvs/params.csv")

		SourceRemapper remapper = new SourceRemapper(methods, fields, params)

		dir.eachFileRecurse {
			if (it.isFile())
				remapper.remapFile(it)
		}
	}
}
